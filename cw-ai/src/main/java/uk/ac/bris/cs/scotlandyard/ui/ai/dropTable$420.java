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
	private static final int maxDepth = 2;

	private Cache cache = null;

	/**
	 * Computes a floating point score for a game configuration stored in a model object using multiple scores
	 *
	 * @param model the model to be scored; not null
	 * @return the score for the given game model
	 */
	private double scoreModel(ScotlandYardAIModel model){
		if(model == null)
			throw new NullPointerException("Model is null");

		double availableMoveScore, distanceScoreAvg, distanceScoreMin;

		// Valid moves scoreModel
		// Normalized to a maximum of 100. 364 is the maximum from the valid moves table.
		// Cached scoring: ; availableMoveScore =  ((double) cache.getValidMoves(model.getCurrentPlayer().location(), model.getCurrentPlayer().tickets())) / 364 * 100;
		availableMoveScore = (double) model.getValidMovesForPlayer(model.getCurrentPlayer()).size() / 364 * 100;

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
		distanceScoreMin = 800 / (minDistance + 1);

		return availableMoveScore * 20 + distanceScoreAvg * 10 - distanceScoreMin * 70;
	}

	/**
	 * Chooses the best move when looking ahead one step from a given model
	 *
	 * @param node the game model
	 * @return a pair of model score and selected move
	 */
	private Pair<Double, Move> chooseMove(ScotlandYardAIModel node){
		double maxScore = -100000.0, currentScore;
		Move bestMove = null;
		ScotlandYardAIModel model;

		// Tries all moves, selects one with biggest score.
		for(Move m : node.getValidMovesForPlayer(node.getCurrentPlayer())){
			model = new ScotlandYardAIModel(node);
			m.visit(model.getCurrentPlayer());
			currentScore = scoreModel(model);
			m.visit(model);
			if(currentScore > maxScore){
				maxScore = currentScore;
				bestMove = m;
			}
		}

		return new Pair<>(maxScore, bestMove);
	}

	/**
	 * MinMax algorithm for finding best move at a given depth. Uses Alpha-Beta pruning.
	 *
	 * @param node current game model
	 * @param depth current search depth
	 * @param maximizer true is it's mr.X's turn to make a move
	 * @param alpha alpha variable for pruning
	 * @param beta beta variable for pruning
	 * @return a pair of model score and selected move
	 */
	private Pair<Double, Move> minMax(ScotlandYardAIModel node, int depth, boolean maximizer, double alpha, double beta){
		Pair<Double, Move> value, bestValue;

		// If the maximum depth is reached, end recursion.
		if(depth == maxDepth)
			return chooseMove(node);

		// If it's mr.X's turn.
		if(maximizer){
			bestValue = new Pair<>(-100000.0, null);
			ScotlandYardAIModel model;

			// Iterate through all valid moves, calling minMax on all of them.
			for(Move m : node.getValidMovesForPlayer(node.getCurrentPlayer())){
				model = new ScotlandYardAIModel(node);
				m.visit(model.getCurrentPlayer());
				m.visit(model);

				value = minMax(model, depth, false, alpha, beta);
				if(value.getKey() > bestValue.getKey() || bestValue.getValue() == null)
					bestValue = new Pair<>(value.getKey(), m);

				// Alpha-Beta pruning.
				alpha = Double.max(alpha, bestValue.getKey());
				if(beta <= alpha)
					break;
			}
			return bestValue;
		}
		// If it's a detective's turn.
		else{
			bestValue = new Pair<>(100000.0, null);
			ScotlandYardAIModel model;

			// Chooses the detective's move's by looking ahead 1, for efficiency.
			for(Move m : node.getValidMovesForPlayer(node.getCurrentPlayer())){
				model = new ScotlandYardAIModel(node);
				m.visit(model.getCurrentPlayer());

				double score = scoreModel(model);
				m.visit(model);

				if(score < bestValue.getKey() || bestValue.getValue() == null)
					bestValue = new Pair<>(score, m);

				// Alpha-Beta pruning.
				alpha = Double.min(alpha, bestValue.getKey());
			}

			// Creates a new game model with the chosen move.
			model = new ScotlandYardAIModel(node);
			bestValue.getValue().visit(model.getCurrentPlayer());
			bestValue.getValue().visit(model);

			// Recurse further.
			if(model.getCurrentPlayerNumber() == 0)
				return  minMax(model, depth + 1, true, alpha, beta);
			else return minMax(model, depth, false, alpha, beta);
		}
	}

	@Override
	// Called at the beginning of the game, used to generate or load needed data.
	public List<Spectator> createSpectators(ScotlandYardView view){
		ScotlandYardAIModel model = new ScotlandYardAIModel(view, -1);
		if(generateFile){
			cache = new Cache(model);
			cache.writeToFile();
		}
		else cache = new Cache();

		return new ArrayList<>();
	}

	@Override
	public Player createPlayer(Colour colour) {
		return new MyPlayer();
	}

	private class MyPlayer implements Player {
		@Override
		public void makeMove(ScotlandYardView view, int location, Set<Move> moves, Consumer<Move> callback) {
			// Initial call to minMax, then we accept selected move.
			Pair<Double, Move> m = minMax(new ScotlandYardAIModel(view, location), 0, true, -100000.0, 100000.0);
			callback.accept(m.getValue());
		}
	}
}
