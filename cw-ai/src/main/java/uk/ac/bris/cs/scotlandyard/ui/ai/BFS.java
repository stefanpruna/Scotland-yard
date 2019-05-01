package uk.ac.bris.cs.scotlandyard.ui.ai;

import javafx.util.Pair;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.Node;
import uk.ac.bris.cs.scotlandyard.model.Ticket;
import uk.ac.bris.cs.scotlandyard.model.Transport;

import java.util.LinkedList;
import java.util.List;

// Class that executes a Breadth First Search.
class BFS{
    private BFSNode[][][][][] distances = new BFSNode[200][200][12][9][5];

    /**
     * Main constructor for BFS
     *
     * @param graph the graph on which to run BFS
     * @param players the list of players that are used as start points
     */
    BFS(Graph<Integer, Transport> graph, List<ScotlandYardAIPlayer> players){

        // Iterates through all players to select starting points.
        for(ScotlandYardAIPlayer p : players){

            // Creates an initial node.
            BFSNode[] distance = new BFSNode[graph.getNodes().size() + 1];
            for(int i = 0; i <= graph.getNodes().size(); i++)
                distance[i] = new BFSNode();

            LinkedList<Pair<Integer, BFSNode>> queue = new LinkedList<>();

            // Adds first node to BFS Queue.
            queue.add(new Pair<>(p.location(), new BFSNode(
                    p.tickets().getOrDefault(Ticket.TAXI, 0),
                    p.tickets().getOrDefault(Ticket.BUS, 0),
                    p.tickets().getOrDefault(Ticket.UNDERGROUND, 0)
            )));

            distance[p.location()] = queue.getFirst().getValue();

            // BFS algorithm.
            while(queue.size() != 0){

                Pair<Integer, BFSNode> frontQueue = queue.removeFirst();

                Node<Integer> node = graph.getNode(frontQueue.getKey());

                for(Edge<Integer, Transport> e : graph.getEdgesFrom(node)){
                    // If the node is unvisited or can be reached in the same amount of steps but with different tickets, add it to queue.
                    if((!distance[e.destination().value()].visited() ||
                            distance[e.destination().value()].distance() == frontQueue.getValue().distance() + 1)
                            && frontQueue.getValue().getTickets(Ticket.fromTransport(e.data())) > 0
                    ){

                        distance[e.destination().value()] = new BFSNode(frontQueue.getValue(), Ticket.fromTransport(e.data()));
                        queue.addLast(new Pair<>(
                                e.destination().value(),
                                distance[e.destination().value()])
                        );
                    }
                }
            }
            // Save distances in matrix.
            for(int i = 1; i < 200; i++)
                distances[p.location()][i]
                        [p.tickets().getOrDefault(Ticket.TAXI, 0)]
                        [p.tickets().getOrDefault(Ticket.BUS, 0)]
                        [p.tickets().getOrDefault(Ticket.UNDERGROUND, 0)] = distance[i];
        }
    }

    /**
     *
     * @return the distance matrix
     */
    BFSNode[][][][][] getDistances(){
        return distances;
    }
}
