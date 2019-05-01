package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.HashMap;

// Models a player.
public class ScotlandYardAIPlayer implements MoveVisitor{
    private Colour colour;
    private int location;
    private HashMap<Ticket, Integer> tickets;

    /**
     * Constructs a copy of the player from a previous one
     *
     * @param player previous player
     */
    ScotlandYardAIPlayer(ScotlandYardAIPlayer player){
        colour = player.colour();
        location = player.location();
        tickets = new HashMap<>(player.tickets());
    }

    /**
     * Constructs a new player
     *
     * @param c the colour
     * @param location the location
     * @param tickets the ticket configuration of the new layer
     */
    ScotlandYardAIPlayer(Colour c, int location, HashMap<Ticket, Integer> tickets){
        colour = c;
        this.location = location;
        this.tickets = tickets;
    }

    /**
     * Constructs a new player
     *
     * @param location the location of new player
     * @param taxi the number of taxi tickets
     * @param bus the number of bus tickets
     * @param underground the number of underground tickets
     */
    ScotlandYardAIPlayer(int location, int taxi, int bus, int underground){
        colour = Colour.WHITE;
        this.location = location;
        HashMap<Ticket, Integer> tickets = new HashMap<>();
        tickets.put(Ticket.TAXI, taxi);
        tickets.put(Ticket.BUS, bus);
        tickets.put(Ticket.UNDERGROUND, underground);
        this.tickets = tickets;
    }

    /**
     * Constructs a new player
     *
     * @param location the location of new player
     * @param taxi the number of taxi tickets
     * @param bus the number of bus tickets
     * @param underground the number of underground tickets
     * @param doublet the number of double tickets
     * @param secret the numbe rof secret tickets
     */
    ScotlandYardAIPlayer(int location, int taxi, int bus, int underground, int doublet, int secret){
        colour = Colour.WHITE;
        this.location = location;
        HashMap<Ticket, Integer> tickets = new HashMap<>();
        tickets.put(Ticket.TAXI, taxi);
        tickets.put(Ticket.BUS, bus);
        tickets.put(Ticket.UNDERGROUND, underground);
        tickets.put(Ticket.DOUBLE, doublet);
        tickets.put(Ticket.SECRET, secret);
        this.tickets = tickets;
    }

    public void setLocation(int l){
        location = l;
    }

    public int location(){
        return location;
    }

    Colour colour(){
        return colour;
    }

    HashMap<Ticket, Integer> tickets(){
        return tickets;
    }

    // Removes a ticker from player
    private void removeTicket(Ticket t){
        int initialTickets = tickets.get(t);
        if(initialTickets != 0)
            tickets.replace(t, initialTickets - 1);
    }

    // Adds a ticker to player
    void addTicket(Ticket t){
        int initialTickets = tickets.getOrDefault(t, 0);
        if(tickets.containsKey(t))
            tickets.replace(t, initialTickets + 1);
        else tickets.put(t, initialTickets + 1);
    }

    @Override
    public void visit(DoubleMove move){
        // Remove used tickets and move player.
        removeTicket(Ticket.DOUBLE);
        removeTicket(move.firstMove().ticket());
        removeTicket(move.secondMove().ticket());
        setLocation(move.secondMove().destination());
    }

    @Override
    public void visit(TicketMove move){
        // Remove used ticket and move player.
        removeTicket(move.ticket());
        setLocation(move.destination());
    }
}
