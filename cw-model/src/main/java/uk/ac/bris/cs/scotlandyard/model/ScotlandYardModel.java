package uk.ac.bris.cs.scotlandyard.model;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move>, MoveVisitor {

	private List<Boolean> rounds;
	private Graph<Integer, Transport> graph;
	private List<ScotlandYardPlayer> players;
	private HashSet<Spectator> spectators = new HashSet<>();
	private int player = 0, round = 0;

	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
			PlayerConfiguration mrX, PlayerConfiguration firstDetective,
			PlayerConfiguration... restOfTheDetectives) {

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

		// Duplicate location or colour check.
		HashSet<Integer> locations = new HashSet<>();
		for(PlayerConfiguration p : players)
			if(locations.contains(p.location))
				throw new IllegalArgumentException("Duplicate location");
			else locations.add(p.location);

		HashSet<Colour> colours = new HashSet<>();
		for(PlayerConfiguration p : players)
			if(colours.contains(p.colour))
				throw new IllegalArgumentException("Duplicate colour");
			else colours.add(p.colour);

		// Ticket check.

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

		this.players = new ArrayList<>();
		for(PlayerConfiguration p : players)
       		this.players.add(new ScotlandYardPlayer(p.player, p.colour, p.location, p.tickets));
	}

	@Override
	public void registerSpectator(Spectator spectator) {
		if(spectators.contains(spectator))
			throw new IllegalArgumentException("Spectator already registered");
		spectators.add(spectator);
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		if(spectator == null)
			throw new NullPointerException("Spectator is null");
		if(!spectators.contains(spectator))
			throw new IllegalArgumentException("Spectator not registered");
		spectators.remove(spectator);
	}

	public void accept(Move m){
		if(m == null)
			throw new NullPointerException("Move is null on callback");

		ScotlandYardPlayer player = null;
		for(ScotlandYardPlayer p : players)
			if(p.colour() == getCurrentPlayer()){
				player = p;
				break;
			}

		HashSet<Move> validMoves = getValidMovesForPlayer(player);
		if(!validMoves.contains(m))
			throw new IllegalArgumentException("Player accepted a non-valid move");

		m.visit(player);

		this.player = (this.player + 1) % players.size();

		m.visit(this);

		// if we are at the last player (plus one'd before), we called makeRotate for all players, so don't.
		if(this.player != 0)
			startRotate();
	}

	private boolean isLocationBusy(Colour c, Integer l){
		for(ScotlandYardPlayer p : players)
			if(p.location() == l && p.colour() != c && p.colour() != Colour.BLACK)
				return true;
		return false;
	}

	private HashSet<TicketMove> getMovesFromPlayerLocation(ScotlandYardPlayer p){
		HashSet<TicketMove> moves = new HashSet<>();
		for(Edge<Integer, Transport> e : graph.getEdgesFrom(graph.getNode(p.location()))){
			if(!isLocationBusy(p.colour(), e.destination().value())){
				if(p.tickets().get(Ticket.fromTransport(e.data())) > 0)
					moves.add(new TicketMove(p.colour(), Ticket.fromTransport(e.data()), e.destination().value()));
				if(p.tickets().get(Ticket.SECRET) > 0)
					moves.add(new TicketMove(p.colour(), Ticket.SECRET, e.destination().value()));
			}
		}
		return moves;
	}

	private HashSet<Move> getDoubleMovesFromPlayerLocation(Set<TicketMove> simpleMoves, ScotlandYardPlayer p){
		HashSet<Move> moves = new HashSet<>();
		for(TicketMove m : simpleMoves){
			for(Edge<Integer, Transport> e : graph.getEdgesFrom(graph.getNode(m.destination()))){
				if(!isLocationBusy(p.colour(), e.destination().value())){
					int c = 0;
					if(Ticket.fromTransport(e.data()) == m.ticket()) c = 1;
					if(p.tickets().get(Ticket.fromTransport(e.data())) > c)
						moves.add(new DoubleMove(p.colour(), m, new TicketMove(p.colour(), Ticket.fromTransport(e.data()), e.destination().value())));
					if(p.tickets().get(Ticket.SECRET) > 0)
						moves.add(new DoubleMove(p.colour(), m, new TicketMove(p.colour(), Ticket.SECRET, e.destination().value())));
				}
			}
		}
		return moves;
	}

	public void visit(PassMove move) {
		if(move.colour() == Colour.BLACK && round < rounds.size() - 1)
			round++;
		for(Spectator s : spectators){
			if(player == 1)
				s.onRoundStarted(this, round);
			s.onMoveMade(this, move);
			if(player == 0)
				s.onRotationComplete(this);
		}
	}

	public void visit(TicketMove move) {
		if(move.colour() == Colour.BLACK && round < rounds.size() - 1)
			round++;
		for(Spectator s : spectators){
			if(player == 1)
				s.onRoundStarted(this, round);
			s.onMoveMade(this, move);
			if(player == 0)
				s.onRotationComplete(this);
		}
	}

	public void visit(DoubleMove move) {
		DoubleMove newMove = move;
		TicketMove first = move.firstMove(), second = move.secondMove();
		if(rounds.get(round) == false) first = new TicketMove(move.firstMove().colour(), move.firstMove().ticket(), 0);
		if(rounds.get(round + 1) == false) second = new TicketMove(move.secondMove().colour(), move.secondMove().ticket(), 0);
		newMove = new DoubleMove(move.colour(), first, second);

		for(Spectator s : spectators)
			s.onMoveMade(this, newMove);

		if(move.colour() == Colour.BLACK && round < rounds.size() - 1)
			round++;

		for(Spectator s : spectators){
			if(player == 1)
				s.onRoundStarted(this, round);
			s.onMoveMade(this, newMove.firstMove());
		}

		if(move.colour() == Colour.BLACK && round < rounds.size() - 1)
			round++;

		for(Spectator s : spectators){
			if(player == 1)
				s.onRoundStarted(this, round);
			s.onMoveMade(this, newMove.secondMove());
			if(player == 0)
				s.onRotationComplete(this);
		}
	}

	private HashSet<Move> getValidMovesForPlayer(ScotlandYardPlayer p){
		HashSet<Move> moves = new HashSet<>();

		HashSet<TicketMove> simpleMoves = getMovesFromPlayerLocation(p);
		moves.addAll(simpleMoves);
		if(p.tickets().getOrDefault(Ticket.DOUBLE, 0) > 0 && round + 1 < rounds.size())
			moves.addAll(getDoubleMovesFromPlayerLocation(simpleMoves, p));

		if(moves.size() == 0)
			moves.add(new PassMove(p.colour()));

		return moves;
	}

	@Override
	public void startRotate() {
		ScotlandYardPlayer currentPlayer = null;
		for(ScotlandYardPlayer p : players)
			if(p.colour() == getCurrentPlayer()){
				currentPlayer = p;
				break;
			}

		HashSet<Move> moves = getValidMovesForPlayer(currentPlayer);

		currentPlayer.player().makeMove(this, currentPlayer.location(), moves, this);
	}

	@Override
	public Collection<Spectator> getSpectators() {
		return Collections.unmodifiableSet(spectators);
	}

	@Override
	public List<Colour> getPlayers() {
		List<Colour> playerColours = new ArrayList<>();
		for(ScotlandYardPlayer p : players)
			playerColours.add(p.colour());
		return Collections.unmodifiableList(playerColours);
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		HashSet<Colour> set = new HashSet<>();
		return Collections.unmodifiableSet(set);
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		for(ScotlandYardPlayer p : players){
			if(p.colour() == colour){
				if(p.colour() == Colour.BLACK && !rounds.get(getCurrentRound()))
					return Optional.of(0);
				return Optional.of(p.location());
			}
		}
		return Optional.empty();
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		for(ScotlandYardPlayer p : players){
			if(p.colour() == colour)
				return Optional.of(p.tickets().get(ticket));
		}
		return Optional.empty();
	}

	@Override
	public boolean isGameOver() {
		// Start of game
		if(getCurrentRound() == 0) return false;

		return false;
	}


	@Override
	public Colour getCurrentPlayer() {
		return players.get(player).colour();
	}

	@Override
	public int getCurrentRound() {
		return round;
	}

	@Override
	public List<Boolean> getRounds() {
		return Collections.unmodifiableList(rounds);
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
		ImmutableGraph<Integer, Transport> g = new ImmutableGraph<>(graph);
		return g;
	}

}
