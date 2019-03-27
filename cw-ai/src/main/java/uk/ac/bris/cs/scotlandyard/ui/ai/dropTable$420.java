package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;
import uk.ac.bris.cs.scotlandyard.ai.ManagedAI;
import uk.ac.bris.cs.scotlandyard.ai.PlayerFactory;
import uk.ac.bris.cs.scotlandyard.model.*;

@ManagedAI("Mr. dropTable$420")
public class dropTable$420 implements PlayerFactory {

	private Cache cache = null;
	final private boolean generateFile = false;

	private Move chooseMove(ScotlandYardView view, int location, Set<Move> moves){
		double maxScore = -100, currentScore;
		Move move = null;
		ScotlandYardAIModel model;

		for(Move m : moves){
			model = new ScotlandYardAIModel(view);
			m.visit(model.getCurrentPlayer());
			currentScore = score(model);
			if(currentScore > maxScore){
				maxScore = currentScore;
				move = m;
			}
		}

		System.out.println("Chose move with score " + maxScore);
		return move;
	}

	private double score(ScotlandYardAIModel model){

		double availableMoveScore = 0, distanceScoreAvg = 0, distanceScoreMin = 0;

		// Valid moves score
		// Normalized to a maximum of 100. 364 is the maximum from the valid moves table.
		availableMoveScore =  ((double) cache.getValidMoves(model.getCurrentPlayer().location(), model.getCurrentPlayer().tickets())) / 364 * 100;


		// Distance calculations
		List<ScotlandYardAIPlayer> playerList = new LinkedList<>(model.getPlayers());
		playerList.remove(0);

		double minDistance = 1000, avgDistance = 0;
		for(ScotlandYardAIPlayer p : playerList){
			minDistance = Double.min(minDistance, cache.getDistance(p.location(), model.getPlayers().get(0).location(), p.tickets()));
			avgDistance += cache.getDistance(p.location(), model.getPlayers().get(0).location(), p.tickets());
		}
		avgDistance /= playerList.size();
		//

		// Normalized to 100, the maximum distance in the table is 16.
		distanceScoreAvg = avgDistance / 16 * 100;
		distanceScoreMin = 100/Math.pow(2, minDistance);


		double score = availableMoveScore * 20 + distanceScoreAvg * 20 - distanceScoreMin * 60;

		return score;
	}

	@Override
	public List<Spectator> createSpectators(ScotlandYardView view){
		ScotlandYardAIModel model = new ScotlandYardAIModel(view);
		if(generateFile){
			cache = new Cache(model);
			cache.writeToFile();
		}
		else cache = new Cache();

		return Collections.emptyList();
	}

	@Override
	public Player createPlayer(Colour colour) {
		return new MyPlayer();
	}

	private class MyPlayer implements Player {

		private final Random random = new Random();

		@Override
		public void makeMove(ScotlandYardView view, int location, Set<Move> moves, Consumer<Move> callback) {
			System.out.println("Provided moves: " +  moves.size());
			Move move = chooseMove(view, location, moves);
			callback.accept(move);
		}
	}
}
