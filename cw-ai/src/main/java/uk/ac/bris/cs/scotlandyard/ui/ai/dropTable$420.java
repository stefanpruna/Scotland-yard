package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.function.Consumer;

import javafx.util.Pair;
import uk.ac.bris.cs.scotlandyard.ai.ManagedAI;
import uk.ac.bris.cs.scotlandyard.ai.PlayerFactory;
import uk.ac.bris.cs.scotlandyard.model.*;

@ManagedAI("Mr. dropTable$420")
public class dropTable$420 implements PlayerFactory {

	private static final boolean generateFile = false;
	private static final int maxDepth = 3;

	private Cache cache = null;

	private double scoreModel(ScotlandYardAIModel model){
		double availableMoveScore = 0, distanceScoreAvg = 0, distanceScoreMin = 0;

		// Valid moves scoreModel
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

	private Pair<Double, Move> chooseMove(ScotlandYardAIModel node){
		double maxScore = Double.MIN_VALUE, currentScore;
		Move bestMove = null;
		ScotlandYardAIModel model;

		System.out.println(node.getCurrentPlayerNumber());
		for(Move m : node.getValidMovesForPlayer(node.getCurrentPlayer())){
			model = new ScotlandYardAIModel(node);
			m.visit(model.getCurrentPlayer());
			m.visit(model);
			currentScore = scoreModel(model);
			if(currentScore > maxScore){
				maxScore = currentScore;
				bestMove = m;
			}
		}

		return new Pair<>(maxScore, bestMove);
	}

	private Pair<Double, Move> minmax(ScotlandYardAIModel node, int depth, boolean maximizer, double alpha, double beta){
		Pair<Double, Move> value, bestValue;

		System.out.println(node.getCurrentPlayer().location());

		if(depth == maxDepth)
			return chooseMove(node);

		if(maximizer){
			bestValue = new Pair<>(Double.MIN_VALUE, null);
			ScotlandYardAIModel model;

			for(Move m : node.getValidMovesForPlayer(node.getCurrentPlayer())){
				model = new ScotlandYardAIModel(node);
				m.visit(model.getCurrentPlayer());
				m.visit(model);

				value = minmax(model, depth + 1, false, alpha, beta);
				if(value.getKey() > bestValue.getKey())
					bestValue = value;

				alpha = Double.max(alpha, bestValue.getKey());

				if(beta <= alpha)
					break;
			}
			return bestValue;
		}
		else{
			bestValue = new Pair<>(Double.MAX_VALUE, null);
			ScotlandYardAIModel model;

			for(Move m : node.getValidMovesForPlayer(node.getCurrentPlayer())){
				model = new ScotlandYardAIModel(node);
				m.visit(model.getCurrentPlayer());
				m.visit(model);

				value = minmax(model, depth + 1, true, alpha, beta);
				if(value.getKey() < bestValue.getKey())
					bestValue = value;

				alpha = Double.min(alpha, bestValue.getKey());

				if(beta <= alpha)
					break;
			}
			return bestValue;
		}
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
			callback.accept(minmax(new ScotlandYardAIModel(view, location), 0, true, Double.MIN_VALUE, Double.MAX_VALUE).getValue());
		}
	}
}
