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

				List<ScotlandYardAIPlayer> playerList = new LinkedList<>(model.getPlayers());
				playerList.remove(0);
				BFS bfsFromDetectives = new BFS(model.getGraph(), playerList, model.getPlayers().get(0).location());
				System.out.println(bfsFromDetectives.getMinimumDistance());
			}
		}

		return move;
	}

	public static double score(ScotlandYardAIModel model){

		double availableMoveScore = 0;


		Set<Move> moves = model.getValidMovesForPlayer(model.getCurrentPlayer());
		//TODO normalizare
		availableMoveScore = moves.size();

		List<ScotlandYardAIPlayer> playerList = new LinkedList<>(model.getPlayers());
		playerList.remove(0);
		BFS bfsFromDetectives = new BFS(model.getGraph(), playerList, model.getPlayers().get(0).location());

		double distanceScoreAvg = bfsFromDetectives.getAverageDistance();
		double distanceScoreMin = bfsFromDetectives.getMinimumDistance();


		double score = availableMoveScore * 30 + distanceScoreAvg * 10 + distanceScoreMin * 60;

		return score;
	}


	@Override
	public Player createPlayer(Colour colour) {
		return new MyPlayer();
	}

	private static class MyPlayer implements Player {

		private final Random random = new Random();

		@Override
		public void makeMove(ScotlandYardView view, int location, Set<Move> moves, Consumer<Move> callback) {
			Move move = chooseMove(view, location, moves);
			callback.accept(move);
		}
	}
}
