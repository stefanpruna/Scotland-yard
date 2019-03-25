package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.HashMap;

public class ScotlandYardAIPlayer implements MoveVisitor{
    private Colour colour;
    private int location;
    private HashMap<Ticket, Integer> tickets;

    public ScotlandYardAIPlayer(Colour c, int location, HashMap<Ticket, Integer> tickets){
        colour = c;
        this.location = location;
        this.tickets = tickets;
    }

    public void setLocation(int l){
        location = l;
    }

    public int location(){
        return location;
    }

    public Colour colour(){
        return colour;
    }

    public HashMap<Ticket, Integer> tickets(){
        return tickets;
    }

    private void removeTicket(Ticket t){
        int initialTickets = tickets.get(t);
        tickets.replace(t, initialTickets - 1);
    }

    private void addTicket(Ticket t){
        int initialTickets = tickets.getOrDefault(t, 0);
        if(tickets.containsKey(t))
            tickets.replace(t, initialTickets + 1);
        else tickets.put(t, initialTickets + 1);
    }

    @Override
    public void visit(PassMove move){
    }

    @Override
    public void visit(DoubleMove move){
        removeTicket(Ticket.DOUBLE);
        removeTicket(move.firstMove().ticket());
        removeTicket(move.secondMove().ticket());
        setLocation(move.secondMove().destination());
    }

    @Override
    public void visit(TicketMove move){
        removeTicket(move.ticket());
        setLocation(move.destination());
    }
}
