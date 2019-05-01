package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.*;

// Models a game.
class ScotlandYardAIModel implements MoveVisitor{

    private int round;
    private int currentPlayerNumber = 0;
    private List<ScotlandYardAIPlayer> players = new ArrayList<>();

    // Immutable.
    private Graph<Integer, Transport> graph;
    private List<Boolean> rounds;

    /**
     * Constructs a copy of the model from a previous one
     *
     * @param model previous model
     */
    ScotlandYardAIModel(ScotlandYardAIModel model){
        round = model.getCurrentRound();
        currentPlayerNumber = model.getCurrentPlayerNumber();

        for(ScotlandYardAIPlayer p : model.getPlayers())
            players.add(new ScotlandYardAIPlayer(p));

        graph = model.graph;
        rounds = model.getRounds();
    }

    /**
     * Constructs a model from a ScotlandYardView and possibly a new location for mr.X
     *
     * @param view the view the model is constructed from
     * @param location if mr.X should be moved to a new location, this should be != -1
     */
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

    /**
     * Increments current round by 1
     */
    private void incrementRound(){
        round += 1;
    }

    /**
     * Increments current player
     */
    private void incrementCurrentPlayer(){
        currentPlayerNumber = (currentPlayerNumber + 1) % players.size();
    }


    /**
     * Iterates all players and checks if location l is free.
     *
     * @param c the colour of the player to be excluded from the check
     * @param l the number of the node to check
     * @return true or false, depending if location is free
     */
    private boolean isLocationFree(Colour c, Integer l){
        for(ScotlandYardAIPlayer p : players)
            if(p.location() == l && p.colour() != c && p.colour() != Colour.BLACK)
                return false;
        return true;
    }

    /**
     * Generates a HashSet of TicketMoves with all possible 'simple' moves for a player
     *
     * @param p the player to generate moves for; not null
     * @return a HashSet of all available simple moves for player
     * @throws NullPointerException if p is null
     */
    private HashSet<TicketMove> getMovesFromPlayerLocation(ScotlandYardAIPlayer p){
        if(p == null)
            throw new NullPointerException("Player is null");

        HashSet<TicketMove> moves = new HashSet<>();
        // Iterates through all available edges from current player position.
        for(Edge<Integer, Transport> e : graph.getEdgesFrom(graph.getNode(p.location()))){
            if(isLocationFree(p.colour(), e.destination().value())){
                // Checks for normal ticket moves.
                if(p.tickets().get(Ticket.fromTransport(e.data())) > 0)
                    moves.add(new TicketMove(p.colour(), Ticket.fromTransport(e.data()), e.destination().value()));
                // Checks for secret ticket move.
                if(p.tickets().get(Ticket.SECRET) > 0)
                    moves.add(new TicketMove(p.colour(), Ticket.SECRET, e.destination().value()));
            }
        }
        return moves;
    }

    /**
     * Generates a HashSet of Moves with all possible double moves for a player
     *
     * @param simpleMoves = the collection of initial moves that are available to the player; not null
     * @param p           the player to generate moves for; not null
     * @return a HashSet of all available double moves for player
     * @throws NullPointerException if p or simpleMoves is null
     */
    private HashSet<Move> getDoubleMovesFromPlayerLocation(Set<TicketMove> simpleMoves, ScotlandYardAIPlayer p){
        if(simpleMoves == null)
            throw new NullPointerException("SimpleMoves is null");
        if(p == null)
            throw new NullPointerException("Player is null");

        HashSet<Move> moves = new HashSet<>();
        // Iterates through all available simple moves in order to generate double moves.
        for(TicketMove m : simpleMoves){
            // Iterates through all available edges from simple move position.
            for(Edge<Integer, Transport> e : graph.getEdgesFrom(graph.getNode(m.destination()))){
                if(isLocationFree(p.colour(), e.destination().value())){
                    int c = 0;
                    // If using the same ticket, player must have at least 2.
                    if(Ticket.fromTransport(e.data()) == m.ticket())
                        c = 1;
                    // Checks for normal double moves.
                    if(p.tickets().get(Ticket.fromTransport(e.data())) > c)
                        moves.add(new DoubleMove(p.colour(), m, new TicketMove(p.colour(), Ticket.fromTransport(e.data()), e.destination().value())));
                    // Checks for double moves containing secret tickets.
                    if(p.tickets().get(Ticket.SECRET) > 0)
                        moves.add(new DoubleMove(p.colour(), m, new TicketMove(p.colour(), Ticket.SECRET, e.destination().value())));
                }
            }
        }
        return moves;
    }

    /**
     * Generates a HashSet of Moves with all possible moves for a player
     *
     * @param p the player to generate moves for; not null
     * @return a HashSet of all available moves for player
     * @throws NullPointerException if p is null
     */
    HashSet<Move> getValidMovesForPlayer(ScotlandYardAIPlayer p){
        if(p == null)
            throw new NullPointerException("Player is null");

        // Make a HashSet for all valid simple moves.
        HashSet<TicketMove> simpleMoves = getMovesFromPlayerLocation(p);

        HashSet<Move> moves = new HashSet<>(simpleMoves);

        // If player has a double ticket, add all double moves.
        if(p.tickets().getOrDefault(Ticket.DOUBLE, 0) > 0 && round + 1 < rounds.size())
            moves.addAll(getDoubleMovesFromPlayerLocation(simpleMoves, p));

        // If player has no moves available, add pass move.
        if(moves.size() == 0)
            moves.add(new PassMove(p.colour()));

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

    @Override
    public void visit(PassMove move){
        // Increment current player.
        incrementCurrentPlayer();
    }

    @Override
    public void visit(DoubleMove move){
        // Increment round and current player.
        incrementCurrentPlayer();
        incrementRound();
    }

    @Override
    public void visit(TicketMove move){
        // If it's mr.X's turn, increment round.
        if(currentPlayerNumber == 0)
            incrementRound();
        // If it's detective's turn, add ticket to mr.X.
        else
            players.get(0).addTicket(move.ticket());
        // Increment current player.
        incrementCurrentPlayer();
    }
}