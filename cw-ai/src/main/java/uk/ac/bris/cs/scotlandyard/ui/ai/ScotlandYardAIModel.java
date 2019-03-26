package uk.ac.bris.cs.scotlandyard.ui.ai;


import com.google.common.collect.ImmutableMap;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.*;

class ScotlandYardAIModel{

    private int round = 0;
    private int currentPlayerNumber = 0;
    private List<ScotlandYardAIPlayer> players = new ArrayList<ScotlandYardAIPlayer>();

    // Immutable:
    private Graph<Integer, Transport> graph;
    private List<Boolean> rounds;

    public ScotlandYardAIModel(ScotlandYardView view){
        graph = view.getGraph();
        rounds = view.getRounds();
        round = view.getCurrentRound();

        for(Colour c : view.getPlayers()){
            if(c == view.getCurrentPlayer())
                break;
            currentPlayerNumber++;
        }

        for(Colour c : view.getPlayers()){
            HashMap<Ticket, Integer> ticketMap = new HashMap<>();

            ticketMap.put(Ticket.SECRET, view.getPlayerTickets(c, Ticket.SECRET).get());
            ticketMap.put(Ticket.DOUBLE, view.getPlayerTickets(c, Ticket.DOUBLE).get());
            ticketMap.put(Ticket.TAXI, view.getPlayerTickets(c, Ticket.TAXI).get());
            ticketMap.put(Ticket.BUS, view.getPlayerTickets(c, Ticket.BUS).get());
            ticketMap.put(Ticket.UNDERGROUND, view.getPlayerTickets(c, Ticket.UNDERGROUND).get());

            players.add(new ScotlandYardAIPlayer(c, view.getPlayerLocation(c).get(), ticketMap));
        }
    }


    public void incrementRound(){
        round += 1;
    }

    public void incrementCurrentPlayer(){
        currentPlayerNumber = (currentPlayerNumber + 1) % players.size();
    }

    public HashSet<Move> getValidMovesForPlayer(ScotlandYardAIPlayer p){
        HashSet<Move> moves = new HashSet<>();

        // Make a HashSet for all valid simple moves
        HashSet<TicketMove> simpleMoves = getMovesFromPlayerLocation(p);
        moves.addAll(simpleMoves);

        // If player has a double ticket, add all double moves
        if(p.tickets().getOrDefault(Ticket.DOUBLE, 0) > 0 && round + 1 < rounds.size())
            moves.addAll(getDoubleMovesFromPlayerLocation(simpleMoves, p));

        if(moves.size() == 0)
            moves.add(new PassMove(p.colour()));

        return moves;
    }

    HashSet<TicketMove> getMovesFromPlayerLocation(ScotlandYardAIPlayer p){
        HashSet<TicketMove> moves = new HashSet<>();
        for(Edge<Integer, Transport> e : getGraph().getEdgesFrom(getGraph().getNode(p.location()))){
            if(!isLocationBusy(p.colour(), e.destination().value())){
                // Checks for normal ticket moves
                if(p.tickets().get(Ticket.fromTransport(e.data())) > 0)
                    moves.add(new TicketMove(p.colour(), Ticket.fromTransport(e.data()), e.destination().value()));
                // Checks for secret ticket move
                if(p.tickets().get(Ticket.SECRET) > 0)
                    moves.add(new TicketMove(p.colour(), Ticket.SECRET, e.destination().value()));
            }
        }
        return moves;
    }

    HashSet<Move> getDoubleMovesFromPlayerLocation(Set<TicketMove> simpleMoves, ScotlandYardAIPlayer p){
        HashSet<Move> moves = new HashSet<>();
        for(TicketMove m : simpleMoves){
            for(Edge<Integer, Transport> e : getGraph().getEdgesFrom(getGraph().getNode(m.destination()))){
                if(!isLocationBusy(p.colour(), e.destination().value())){
                    int c = 0;
                    // If using the same ticket, player must have at least 2
                    if(Ticket.fromTransport(e.data()) == m.ticket())
                        c = 1;
                    // Checks for normal double moves
                    if(p.tickets().get(Ticket.fromTransport(e.data())) > c)
                        moves.add(new DoubleMove(p.colour(), m, new TicketMove(p.colour(), Ticket.fromTransport(e.data()), e.destination().value())));
                    // Checks for double moves containing secret tickets
                    if(p.tickets().get(Ticket.SECRET) > 0)
                        moves.add(new DoubleMove(p.colour(), m, new TicketMove(p.colour(), Ticket.SECRET, e.destination().value())));
                }
            }
        }
        return moves;
    }

    public List<ScotlandYardAIPlayer> getPlayers(){
        return Collections.unmodifiableList(players);
    }

    public Set<Colour> getWinningPlayers(){
        HashSet<Colour> set = new HashSet<>();
        boolean detectivesStuck = true, captured = false;

        // Check for detectives being stuck
        for(ScotlandYardAIPlayer p : players)
            if(p.colour() != Colour.BLACK && !getValidMovesForPlayer(p).contains(new PassMove(p.colour())))
                detectivesStuck = false;

        // Check for MrX capture
        for(ScotlandYardAIPlayer p : players)
            if(p.colour() != Colour.BLACK && p.location() == players.get(0).location())
                captured = true;

        // MrX win
        if(detectivesStuck || (round == rounds.size() && currentPlayerNumber == 0))
            set.add(Colour.BLACK);
        // Detectives win
        else if(getValidMovesForPlayer(players.get(0)).contains(new PassMove(Colour.BLACK)) || captured)
            for(ScotlandYardAIPlayer p : players)
                if(p.colour() != Colour.BLACK)
                    set.add(p.colour());

        return Collections.unmodifiableSet(set);
    }

    public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
        for(ScotlandYardAIPlayer p : players){
            if(p.colour() == colour)
                return Optional.of(p.tickets().get(ticket));
        }
        return Optional.empty();
    }

    public boolean isGameOver(){
        // Last round
        if(round == rounds.size() && currentPlayerNumber == 0)
            return true;

        // MrX Cannot move
        if(getCurrentPlayerColour() == Colour.BLACK && getValidMovesForPlayer(players.get(0)).contains(new PassMove(Colour.BLACK)))
            return true;

        // Detectives stuck
        boolean detectivesStuck = true;
        for(ScotlandYardAIPlayer p : players)
            if(p.colour() != Colour.BLACK && !getValidMovesForPlayer(p).contains(new PassMove(p.colour())))
                detectivesStuck = false;
        if(detectivesStuck)
            return true;

        // Captured
        for(ScotlandYardAIPlayer p : players)
            if(p.colour() != Colour.BLACK && p.colour() == players.get(0).colour())
                return true;

        return false;
    }

    public Colour getCurrentPlayerColour(){
        return players.get(currentPlayerNumber).colour();
    }
    
    public ScotlandYardAIPlayer getCurrentPlayer(){
        return players.get(currentPlayerNumber);
    }

    public int getCurrentRound(){
        return round;
    }

    public List<Boolean> getRounds() {
        return Collections.unmodifiableList(rounds);
    }

    public Graph<Integer, Transport> getGraph() {
        ImmutableGraph<Integer, Transport> g = new ImmutableGraph<>(graph);
        return g;
    }

    boolean isLocationBusy(Colour c, Integer l){
        for(ScotlandYardAIPlayer p : players)
            if(p.location() == l && p.colour() != c && p.colour() != Colour.BLACK)
                return true;
        return false;
    }
}