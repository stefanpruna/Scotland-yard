package uk.ac.bris.cs.scotlandyard.model;

import java.util.*;

import uk.ac.bris.cs.gamekit.graph.Graph;

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame {

	private List<Boolean> rounds;
	private Graph<Integer, Transport> graph;

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
		players.add(firstDetective);
		players.add(mrX);

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
	}

	@Override
	public void registerSpectator(Spectator spectator) {
		// TODO
		// Test
		throw new RuntimeException("Implement me");
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void startRotate() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Collection<Spectator> getSpectators() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public List<Colour> getPlayers() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public boolean isGameOver() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Colour getCurrentPlayer() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public int getCurrentRound() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public List<Boolean> getRounds() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
		// TODO
		throw new RuntimeException("Implement me");
	}

}
