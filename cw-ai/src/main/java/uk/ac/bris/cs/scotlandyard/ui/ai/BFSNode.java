package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Ticket;

public class BFSNode{

    private byte distance = 0;
    int taxi = 0, bus = 0, underground = 0;
    private boolean visited = false;

    public BFSNode(){

    }

    public BFSNode(int taxi, int bus, int underground){
        visited = true;
        distance = 0;
        this.taxi = taxi;
        this.bus = bus;
        this.underground = underground;
    }

    public BFSNode(BFSNode oldNode, Ticket transport){
        visited = true;
        distance = (byte) (oldNode.distance() + 1);
        taxi = oldNode.getTickets(Ticket.TAXI);
        bus = oldNode.getTickets(Ticket.BUS);
        underground = oldNode.getTickets(Ticket.UNDERGROUND);

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

    public boolean visited(){
        return visited;
    }

    public int getTickets(Ticket t){
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

    public int distance(){
        return distance;
    }

}
