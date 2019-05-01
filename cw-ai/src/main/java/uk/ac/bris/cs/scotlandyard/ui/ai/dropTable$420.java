package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.function.Consumer;

import javafx.util.Pair;
import uk.ac.bris.cs.scotlandyard.ai.ManagedAI;
import uk.ac.bris.cs.scotlandyard.ai.PlayerFactory;
import uk.ac.bris.cs.scotlandyard.model.*;

@ManagedAI("Mr. dropTable$420")
class dropTable$420 implements PlayerFactory {

	private static final boolean generateFile = false;
	private static final int maxDepth = 2;

	private Cache cache = null;

	private double scoreModel(ScotlandYardAIModel model){
		double availableMoveScore, distanceScoreAvg, distanceScoreMin;

		// Valid moves scoreModel
		// Normalized to a maximum of 100. 364 is the maximum from the valid moves table.
		//availableMoveScore =  ((double) cache.getValidMoves(model.getCurrentPlayer().location(), model.getCurrentPlayer().tickets())) / 364 * 100;
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

	private Pair<Double, Move> chooseMove(ScotlandYardAIModel node){
		double maxScore = -100000.0, currentScore;
		Move bestMove = null;
		ScotlandYardAIModel model;

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

	private Pair<Double, Move> minmax(ScotlandYardAIModel node, int depth, boolean maximizer, double alpha, double beta){
		Pair<Double, Move> value, bestValue;

		if(depth == maxDepth)
			return chooseMove(node);

		if(maximizer){
			bestValue = new Pair<>(-100000.0, null);
			ScotlandYardAIModel model;

			for(Move m : node.getValidMovesForPlayer(node.getCurrentPlayer())){
				model = new ScotlandYardAIModel(node);
				m.visit(model.getCurrentPlayer());
				m.visit(model);

				value = minmax(model, depth, false, alpha, beta);
				if(value.getKey() > bestValue.getKey() || bestValue.getValue() == null)
					bestValue = new Pair<>(value.getKey(), m);

				alpha = Double.max(alpha, bestValue.getKey());

				if(beta <= alpha)
					break;
			}
			return bestValue;
		}
		else{
			bestValue = new Pair<>(100000.0, null);
			ScotlandYardAIModel model;

			for(Move m : node.getValidMovesForPlayer(node.getCurrentPlayer())){
				model = new ScotlandYardAIModel(node);
				m.visit(model.getCurrentPlayer());

				double score = scoreModel(model);
				m.visit(model);

				if(score < bestValue.getKey() || bestValue.getValue() == null)
					bestValue = new Pair<>(score, m);

				alpha = Double.min(alpha, bestValue.getKey());
			}

			model = new ScotlandYardAIModel(node);
			bestValue.getValue().visit(model.getCurrentPlayer());
			bestValue.getValue().visit(model);

			if(model.getCurrentPlayerNumber() == 0)
				return  minmax(model, depth + 1, true, alpha, beta);
			else return minmax(model, depth, false, alpha, beta);
		}
	}

	@Override
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
			Pair<Double, Move> m = minmax(new ScotlandYardAIModel(view, location), 0, true, -100000.0, 100000.0);
			callback.accept(m.getValue());
		}
	}
}
