package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Ticket;

// Class that stores all needed information for a Breadth First Search.
class BFSNode{

    private byte distance = 0;
    private int taxi = 0, bus = 0, underground = 0;
    private boolean visited = false;

    /**
     * Constructor for default node
     */
    BFSNode(){

    }

    /**
     * Constructor for initial node
     *
     * @param taxi number of taxi tickets
     * @param bus number of bus tickets
     * @param underground number of underground tickets
     */
    BFSNode(int taxi, int bus, int underground){
        visited = true;
        this.taxi = taxi;
        this.bus = bus;
        this.underground = underground;
    }

    /**
     * Constructor for reached node
     *
     * @param oldNode the node from which the player is coming from
     * @param transport the transportation method (ticket) used by player to reach current node
     */
    BFSNode(BFSNode oldNode, Ticket transport){
        visited = true;
        distance = (byte) (oldNode.distance() + 1);
        taxi = oldNode.getTickets(Ticket.TAXI);
        bus = oldNode.getTickets(Ticket.BUS);
        underground = oldNode.getTickets(Ticket.UNDERGROUND);

        // Delete used ticket from configuration
        switch(transport){
            case TAXI:
                taxi--;
                break;
            case BUS:
                bus--;
                break;
            case UNDERGROUND:
                underground--;
                break;
            default:
                break;
        }
    }

    /**
     *
     * @return if the node has been visited by player
     */
    boolean visited(){
        return visited;
    }

    /**
     *
     * @param t the ticket type
     * @return returns the number of tickets of type t remaining
     */
    int getTickets(Ticket t){
        switch(t){
            case TAXI:
                return taxi;
            case BUS:
                return bus;
            case UNDERGROUND:
                return underground;
            default:
                return 0;
        }
    }

    /**
     *
     * @return the distance from the start node to this
     */
    int distance(){
        return distance;
    }

}
