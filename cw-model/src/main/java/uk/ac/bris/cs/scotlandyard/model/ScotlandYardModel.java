package uk.ac.bris.cs.scotlandyard.model;

import java.util.*;
import java.util.List;
import java.util.function.Consumer;

import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;

public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move>, MoveVisitor{

    private List<Boolean> rounds;
    private Graph<Integer, Transport> graph;
    private List<ScotlandYardPlayer> players;
    private HashSet<Spectator> spectators = new HashSet<>();
    private int player = 0, round = 0;
    private int lastBlackLocation = 0;

    public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
                             PlayerConfiguration mrX, PlayerConfiguration firstDetective,
                             PlayerConfiguration... restOfTheDetectives){

        // Null and empty checks.
        if(rounds != null) this.rounds = rounds;
        else throw new NullPointerException("Rounds is null");

        if(graph != null) this.graph = graph;
        else throw new NullPointerException("Graph is null");

        if(rounds.isEmpty()) throw new IllegalArgumentException("Rounds is empty");
        if(graph.isEmpty()) throw new IllegalArgumentException("Graph is empty");

        // mrX colour check.

        if(mrX.colour != Colour.BLACK) throw new IllegalArgumentException("Mr. X should be black");

        // Adding all players to a list.
        ArrayList<PlayerConfiguration> players = new ArrayList<>(Arrays.asList(restOfTheDetectives)); // players is constructed with all elements of restOfDetectives
        players.add(0, firstDetective);
        players.add(0, mrX);

        // Duplicate location check.
        HashSet<Integer> locations = new HashSet<>();
        for(PlayerConfiguration p : players)
            if(locations.contains(p.location))
                throw new IllegalArgumentException("Duplicate location");
            else locations.add(p.location);

        // Duplicate colour check.
        HashSet<Colour> colours = new HashSet<>();
        for(PlayerConfiguration p : players)
            if(colours.contains(p.colour))
                throw new IllegalArgumentException("Duplicate colour");
            else colours.add(p.colour);

        // Valid tickets check.
        for(PlayerConfiguration p : players){
            if(!p.tickets.containsKey(Ticket.TAXI) || !p.tickets.containsKey(Ticket.BUS) || !p.tickets.containsKey(Ticket.UNDERGROUND))
                throw new IllegalArgumentException("Player does not have all tickets");
            if(p.colour == Colour.BLACK){
                if(!p.tickets.containsKey(Ticket.DOUBLE) || !p.tickets.containsKey(Ticket.SECRET))
                    throw new IllegalArgumentException("MrX does not have all tickets");
            }
            else if(p.tickets.getOrDefault(Ticket.DOUBLE, 0) != 0 || p.tickets.getOrDefault(Ticket.SECRET, 0) != 0)
                throw new IllegalArgumentException("Detectives have Double or Secret ticket");
        }

        // Create ScotlandYardPlayer objects and add them to the players list.
        this.players = new ArrayList<>();
        for(PlayerConfiguration p : players)
            this.players.add(new ScotlandYardPlayer(p.player, p.colour, p.location, p.tickets));
    }

    @Override
    public void registerSpectator(Spectator spectator){
        // Null check.
        if(spectator == null)
            throw new NullPointerException("Spectator is null");
        // Duplication check.
        if(spectators.contains(spectator))
            throw new IllegalArgumentException("Spectator already registered");
        spectators.add(spectator);
    }

    @Override
    public void unregisterSpectator(Spectator spectator){
        // Null check.
        if(spectator == null)
            throw new NullPointerException("Spectator is null");
        // Invalid argument check.
        if(!spectators.contains(spectator))
            throw new IllegalArgumentException("Spectator not registered");
        spectators.remove(spectator);
    }

    /**
     * Accepts a move chosen by a player. Processes game logic.
     *
     * @param m the move the player chose; not null.
     * @throws NullPointerException if p is null
     */
    public void accept(Move m){
        // Null check.
        if(m == null)
            throw new NullPointerException("Move is null on callback");

        // Gets the current player's object.
        ScotlandYardPlayer player = null;
        for(ScotlandYardPlayer p : players)
            if(p.colour() == getCurrentPlayer()){
                player = p;
                break;
            }

        // Generates the collection of valid moves and checks that the argument is valid.
        HashSet<Move> validMoves = getValidMovesForPlayer(player);
        if(!validMoves.contains(m))
            throw new IllegalArgumentException("Player accepted a non-valid move");

        // Updates last known mr.X location.
        if(round != 0 && player.isMrX() && rounds.get(round - 1))
            lastBlackLocation = player.location();

        // Increments the current player.
        this.player = (this.player + 1) % players.size();

        // Executes the move using the visitor pattern.
        m.visit(this);

        // if we are at the last player (plus one'd before), we called makeRotate for all players, so don't.
        if(this.player != 0 && !isGameOver())
            startRotate();
    }

    /**
     * Iterates all players and checks if location l is free.
     *
     * @param c the colour of the player to be excluded from the check
     * @param l the number of the node to check
     * @return true or false, depending if location is free
     */
    private boolean isLocationFree(Colour c, Integer l){
        for(ScotlandYardPlayer p : players)
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
    private HashSet<TicketMove> getMovesFromPlayerLocation(ScotlandYardPlayer p){
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
    private HashSet<Move> getDoubleMovesFromPlayerLocation(Set<TicketMove> simpleMoves, ScotlandYardPlayer p){
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
    private HashSet<Move> getValidMovesForPlayer(ScotlandYardPlayer p){
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

    /**
     * Gets the player object from a move object.
     *
     * @param m the move; not null
     * @return the object of the player whose colour is that of the move, or null if not found
     * @throws NullPointerException if m
     */
    private ScotlandYardPlayer getPlayerFromMove(Move m){
        if(m == null)
            throw new NullPointerException("Move is null");

        // Iterate through all players, if colour matches, return.
        for(ScotlandYardPlayer p : players)
            if(p.colour() == m.colour())
                return p;
        return null;
    }

    /**
     * Notify spectators of a move and possible round start, rotation complete or game end
     *
     * @param move the move; never null
     */
    private void notifySpectators(Move move){
        // Notify all spectators.
        for(Spectator s : spectators){
            // Notify spectators that a new round has started, if last player was mr.X
            if(player == 1)
                s.onRoundStarted(this, round);
            // Notify spectators that a move has been made.
            s.onMoveMade(this, move);
            // Notify spectators of a rotation complete if current player is mr.X, or that the game is over.
            if(player == 0 && !isGameOver())
                s.onRotationComplete(this);
            else if(isGameOver())
                s.onGameOver(this, getWinningPlayers());
        }
    }

    public void visit(PassMove move){
        // If the current player is mr.X and there are still round to be played, increment the round number.
        if(move.colour() == Colour.BLACK && round < rounds.size())
            round++;

        // Notify all spectators.
        notifySpectators(move);
    }


    public void visit(TicketMove move){
        // Get the player from the move's colour. Remove the used ticket and update location.
        ScotlandYardPlayer p = getPlayerFromMove(move);
        if(p!= null){
            p.removeTicket(move.ticket());
            p.location(move.destination());
        }

        // If player that executed move is a detective, add used ticket to mr.X.
        if(move.colour() != Colour.BLACK)
            players.get(0).addTicket(move.ticket());

        // Generate a new move that is reported to the spectators. This move conceals mr.X's location, if required.
        TicketMove newMove = move;
        if(move.colour() == Colour.BLACK && (round == 0 || !rounds.get(round)))
            newMove = new TicketMove(move.colour(), move.ticket(), lastBlackLocation);

        // If the current player is mr.X and there are still round to be played, increment the round number.
        if(move.colour() == Colour.BLACK && round < rounds.size())
            round++;

        // Notify all spectators.
        notifySpectators(newMove);
    }

    public void visit(DoubleMove move){
        // Generate new moves that are reported to the spectators. These moves conceals mr.X's location, if required.
        TicketMove first = move.firstMove(), second = move.secondMove();
        if(!rounds.get(round))
            first = new TicketMove(move.firstMove().colour(), move.firstMove().ticket(), lastBlackLocation);
        if(!rounds.get(round + 1))
            second = new TicketMove(move.secondMove().colour(), move.secondMove().ticket(), lastBlackLocation);
        if(rounds.get(round) && !rounds.get(round + 1))
            second = new TicketMove(move.secondMove().colour(), move.secondMove().ticket(), first.destination());
        DoubleMove newMove = new DoubleMove(move.colour(), first, second);

        // We know the player is mr.X. Remove the used ticket.
        players.get(0).removeTicket(Ticket.DOUBLE);

        // Notify spectators of double move use.
        for(Spectator s : spectators)
            s.onMoveMade(this, newMove);

        //Remove the first ticket, update location for first move in the double move.
        players.get(0).removeTicket(move.firstMove().ticket());
        players.get(0).location(move.firstMove().destination());
        // Increment rounds if there are still rounds to be played.
        if(round < rounds.size())
            round++;

        // Notify spectators of a round start and that the first move was made.
        for(Spectator s : spectators){
            s.onRoundStarted(this, round);
            s.onMoveMade(this, newMove.firstMove());
        }

        // Update mr.X's location, if round is not hidden.
        if(rounds.get(round - 1))
            lastBlackLocation = players.get(0).location();
        //Remove the first ticket, update location for second move in the double move.
        players.get(0).removeTicket(move.secondMove().ticket());
        players.get(0).location(move.finalDestination());

        // Increment rounds if there are still rounds to be played.
        if(round < rounds.size())
            round++;

        // Notify all spectators.
        notifySpectators(newMove.secondMove());
    }

    @Override
    public void startRotate(){
        if(round == 0 && isGameOver())
            throw new IllegalStateException("Game already over on start rotate");

        // Get current player's object.
        ScotlandYardPlayer currentPlayer = null;
        for(ScotlandYardPlayer p : players)
            if(p.colour() == getCurrentPlayer()){
                currentPlayer = p;
                break;
            }

        // Get player's valid moves.
        HashSet<Move> moves = getValidMovesForPlayer(currentPlayer);

        // Call makeMove.
        currentPlayer.player().makeMove(this, currentPlayer.location(), moves, this);
    }

    @Override
    public Collection<Spectator> getSpectators(){
        return Collections.unmodifiableSet(spectators);
    }

    @Override
    public List<Colour> getPlayers(){
        List<Colour> playerColours = new ArrayList<>();
        for(ScotlandYardPlayer p : players)
            playerColours.add(p.colour());
        return Collections.unmodifiableList(playerColours);
    }

    @Override
    public Set<Colour> getWinningPlayers(){
        HashSet<Colour> set = new HashSet<>();
        boolean detectivesStuck = true, captured = false;

        // Check for detectives being stuck
        for(ScotlandYardPlayer p : players)
            if(p.isDetective() && !getValidMovesForPlayer(p).contains(new PassMove(p.colour())))
                detectivesStuck = false;

        // Check for MrX capture
        for(ScotlandYardPlayer p : players)
            if(!p.isMrX() && p.location() == players.get(0).location())
                captured = true;

        // MrX win
        if(detectivesStuck || (round == rounds.size() && player == 0))
            set.add(Colour.BLACK);
        // Detectives win
        else if(getValidMovesForPlayer(players.get(0)).contains(new PassMove(Colour.BLACK)) || captured)
            for(ScotlandYardPlayer p : players)
                if(p.isDetective())
                    set.add(p.colour());

        return Collections.unmodifiableSet(set);
    }

    @Override
    public Optional<Integer> getPlayerLocation(Colour colour){
        // Gets player object, then returns location
        for(ScotlandYardPlayer p : players){
            if(p.colour() == colour){
                if(p.colour() == Colour.BLACK && (getCurrentRound() == 0 || !rounds.get(getCurrentRound() - 1)))
                    return Optional.of(lastBlackLocation);
                return Optional.of(p.location());
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket){
        // Gets player object, then returns tickets
        for(ScotlandYardPlayer p : players){
            if(p.colour() == colour)
                return Optional.of(p.tickets().get(ticket));
        }
        return Optional.empty();
    }

    @Override
    public boolean isGameOver(){
        // Last round
        if(round == rounds.size() && player == 0)
            return true;

        // MrX Cannot move
        if(getCurrentPlayer() == Colour.BLACK && getValidMovesForPlayer(players.get(0)).contains(new PassMove(Colour.BLACK)))
            return true;

        // Detectives stuck
        boolean detectivesStuck = true;
        for(ScotlandYardPlayer p : players)
            if(p.isDetective() && !getValidMovesForPlayer(p).contains(new PassMove(p.colour())))
                detectivesStuck = false;
        if(detectivesStuck)
            return true;

        // Captured
        for(ScotlandYardPlayer p : players)
            if(!p.isMrX() && p.location() == players.get(0).location())
                return true;

        return false;
    }


    @Override
    public Colour getCurrentPlayer(){
        return players.get(player).colour();
    }

    @Override
    public int getCurrentRound(){
        return round;
    }

    @Override
    public List<Boolean> getRounds(){
        return Collections.unmodifiableList(rounds);
    }

    @Override
    public Graph<Integer, Transport> getGraph(){
        return new ImmutableGraph<>(graph);
    }

}
