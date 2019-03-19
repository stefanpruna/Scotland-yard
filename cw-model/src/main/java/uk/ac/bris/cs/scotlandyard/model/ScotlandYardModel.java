package uk.ac.bris.cs.scotlandyard.model;

import java.awt.*;
import java.util.*;
import java.util.List;

import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame {

	private List<Boolean> rounds;
	private Graph<Integer, Transport> graph;
	private List<ScotlandYardPlayer> players;

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
			if(p.tickets.getOrDefault(Ticket.TAXI, 0) == 0 ||
					p.tickets.getOrDefault(Ticket.BUS, 0) == 0 ||
					p.tickets.getOrDefault(Ticket.UNDERGROUND, 0) == 0)
				throw new IllegalArgumentException("Player missing tickets");
			if(p.colour != Colour.BLACK){
				if(p.tickets.getOrDefault(Ticket.SECRET, 0) != 0 || p.tickets.getOrDefault(Ticket.DOUBLE, 0) != 0)
					throw new IllegalArgumentException("Detectives have illegal tickets");
			}
			else if(p.tickets.getOrDefault(Ticket.SECRET, 0) == 0 || p.tickets.getOrDefault(Ticket.DOUBLE, 0) == 0)
				throw new IllegalArgumentException("MrX missing tickets");
		}

		this.players = new ArrayList<>();
		for(PlayerConfiguration p : players)
       		this.players.add(new ScotlandYardPlayer(p.player, p.colour, p.location, p.tickets));
	}

	@Override
	public void registerSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void startRotate() {
		//
	}

	@Override
	public Collection<Spectator> getSpectators() {
		// TODO
		throw new RuntimeException("Implement me");
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

		return true;
	}

	@Override
	public Colour getCurrentPlayer() {
		return Colour.BLACK;
	}

	@Override
	public int getCurrentRound() {
		return 0;
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
