package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.*;

class ScotlandYardAIModel implements  MoveVisitor{

    private int round;
    private int currentPlayerNumber = 0;
    private List<ScotlandYardAIPlayer> players = new ArrayList<>();

    // Immutable:
    private Graph<Integer, Transport> graph;
    private List<Boolean> rounds;

    ScotlandYardAIModel(ScotlandYardAIModel model){
        round = model.getCurrentRound();
        currentPlayerNumber = model.getCurrentPlayerNumber();

        for(ScotlandYardAIPlayer p : model.getPlayers())
            players.add(new ScotlandYardAIPlayer(p));

        graph = model.graph;
        rounds = model.getRounds();
    }

    ScotlandYardAIModel(ScotlandYardView view, int location){
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

            ticketMap.put(Ticket.SECRET, view.getPlayerTickets(c, Ticket.SECRET).orElse(0));
            ticketMap.put(Ticket.DOUBLE, view.getPlayerTickets(c, Ticket.DOUBLE).orElse(0));
            ticketMap.put(Ticket.TAXI, view.getPlayerTickets(c, Ticket.TAXI).orElse(0));
            ticketMap.put(Ticket.BUS, view.getPlayerTickets(c, Ticket.BUS).orElse(0));
            ticketMap.put(Ticket.UNDERGROUND, view.getPlayerTickets(c, Ticket.UNDERGROUND).orElse(0));

            if(c != Colour.BLACK || location == -1)
                players.add(new ScotlandYardAIPlayer(c, view.getPlayerLocation(c).orElse(0), ticketMap));
            else players.add(new ScotlandYardAIPlayer(c, location, ticketMap));
        }
    }


    private void incrementRound(){
        round += 1;
    }

    private void incrementCurrentPlayer(){
        currentPlayerNumber = (currentPlayerNumber + 1) % players.size();
    }

    HashSet<Move> getValidMovesForPlayer(ScotlandYardAIPlayer p){
       // Make a HashSet for all valid simple moves
        HashSet<TicketMove> simpleMoves = getMovesFromPlayerLocation(p);
        HashSet<Move> moves = new HashSet<>(simpleMoves);

        // If player has a double ticket, add all double moves
        if(p.tickets().getOrDefault(Ticket.DOUBLE, 0) > 0 && round + 1 < rounds.size())
            moves.addAll(getDoubleMovesFromPlayerLocation(simpleMoves, p));

        if(moves.size() == 0)
            moves.add(new PassMove(p.colour()));

        return moves;
    }

    private HashSet<TicketMove> getMovesFromPlayerLocation(ScotlandYardAIPlayer p){
        HashSet<TicketMove> moves = new HashSet<>();
        for(Edge<Integer, Transport> e : getGraph().getEdgesFrom(getGraph().getNode(p.location()))){
            if(isLocationFree(p.colour(), e.destination().value())){
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

    private HashSet<Move> getDoubleMovesFromPlayerLocation(Set<TicketMove> simpleMoves, ScotlandYardAIPlayer p){
        HashSet<Move> moves = new HashSet<>();
        for(TicketMove m : simpleMoves){
            for(Edge<Integer, Transport> e : getGraph().getEdgesFrom(getGraph().getNode(m.destination()))){
                if(isLocationFree(p.colour(), e.destination().value())){
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

    List<ScotlandYardAIPlayer> getPlayers(){
        return players;
    }

    ScotlandYardAIPlayer getCurrentPlayer(){
        return players.get(currentPlayerNumber);
    }

    int getCurrentPlayerNumber(){
        return currentPlayerNumber;
    }

    private int getCurrentRound(){
        return round;
    }

    private List<Boolean> getRounds() {
        return rounds;
    }

    Graph<Integer, Transport> getGraph() {
        return graph;
    }

    private boolean isLocationFree(Colour c, Integer l){
        for(ScotlandYardAIPlayer p : players)
            if(p.location() == l && p.colour() != c && p.colour() != Colour.BLACK)
                return false;
        return true;
    }

    @Override
    public void visit(PassMove move){
    }

    @Override
    public void visit(DoubleMove move){
        incrementCurrentPlayer();
        incrementRound();
    }

    @Override
    public void visit(TicketMove move){
        if(currentPlayerNumber == 0){
            incrementRound();
            incrementCurrentPlayer();
        }
        else{
            players.get(0).addTicket(move.ticket());
            incrementCurrentPlayer();
        }
    }
}