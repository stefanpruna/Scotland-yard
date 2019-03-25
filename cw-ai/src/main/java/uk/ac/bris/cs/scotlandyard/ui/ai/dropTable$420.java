package uk.ac.bris.cs.scotlandyard.ui.ai;

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

	public static Move chooseMove(ScotlandYardView view, int location, Set<Move> moves){
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

		return move;
	}

	public static double score(ScotlandYardAIModel model){

		double availableMoveScore = 0;


		Set<Move> moves = model.getValidMovesForPlayer(model.getCurrentPlayer());
		//TODO normalizare
		availableMoveScore = moves.size();


		double score = availableMoveScore * 100;

		return score;
	}

	private int[] runDijkstraFomPlayer(ScotlandYardAIModel model, Colour colour){
		Graph<Integer, Transport> g = model.getGraph();

		//nodeQueue.push(start);
		while(nodeQueue.size() != 0){
			int node = nodeQueue.remove();
			for(Edge<Integer, Transport> e : g.getEdgesFrom(g.getNode(node))){

			}
		}
		return null;
	}


	// TODO create a new player here
	@Override
	public Player createPlayer(Colour colour) {
		return new MyPlayer();
	}

	// TODO A sample player that selects a random move
	private static class MyPlayer implements Player {

		private final Random random = new Random();


		@Override
		public void makeMove(ScotlandYardView view, int location, Set<Move> moves, Consumer<Move> callback) {
			Move move = chooseMove(view, location, moves);
			callback.accept(move);
		}
	}
}
