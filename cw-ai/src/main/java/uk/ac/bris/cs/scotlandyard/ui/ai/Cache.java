package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.scotlandyard.model.Colour;
import uk.ac.bris.cs.scotlandyard.model.Ticket;
import uk.ac.bris.cs.scotlandyard.model.Transport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Cache{

    final private File distanceFile = new File("distance.dat");
    final private File validMoveFile = new File("moves.dat");

    private byte[][][][][] distance = new byte[200][200][12][9][5];
    private int[][][][][][] validMoves = new int[200][3][3][3][2][3];

    public Cache(){
        if(distanceFile.exists() && validMoveFile.exists()){
            try{
                FileInputStream in = new FileInputStream(distanceFile);
                byte[] tempDist = new byte[200 * 200 * 12 * 9 * 5];
                in.read(tempDist);
                int i = 0;
                for(int start = 1; start < 200; start++)
                    for(int to = 1; to < 200; to++)
                        for(int taxi = 0; taxi < 12; taxi++)
                            for(int bus = 0; bus < 9; bus++)
                                for(int underground = 0; underground < 5; underground++)
                                    distance[start][to][taxi][bus][underground] = tempDist[i++];

                in = new FileInputStream(validMoveFile);
                byte[] tempMoves = new byte[4 * 200 * 162];
                in.read(tempMoves);
                i = 0;

                for(int node = 1; node < 200; node++)
                    for(int taxi = 0; taxi < 3; taxi++)
                        for(int bus = 0; bus < 3; bus++)
                            for(int underground = 0; underground < 3; underground++)
                                for(int doublet = 0; doublet < 2; doublet++)
                                    for(int secret = 0; secret < 3; secret++){
                                        for(int b = 0; b < 4; b++)
                                            validMoves[node][taxi][bus][underground][doublet][secret] =
                                                    (validMoves[node][taxi][bus][underground][doublet][secret] << 8) | (tempMoves[b + i] & 0xFF); // muie java
                                        i += 4;
                                    }
            }
            catch(Exception e){
                System.out.println("Exception: " + e);
            }
        }
    }

    public Cache(ScotlandYardAIModel model){
        // Generates distances from everywhere with every config
        List<ScotlandYardAIPlayer> allPossiblePlayers = new ArrayList<>();
        for(int start = 1; start < 200; start++)
            for(int taxi = 0; taxi < 12; taxi++)
                for(int bus = 0; bus < 9; bus++)
                    for(int underground = 0; underground < 5; underground++)
                        allPossiblePlayers.add(new ScotlandYardAIPlayer(start, taxi, bus, underground));

        BFS allBfs = new BFS(model.getGraph(), allPossiblePlayers);
        for(int start = 1; start < 200; start++)
            for(int to = 1; to < 200; to++)
                for(int taxi = 0; taxi < 12; taxi++)
                    for(int bus = 0; bus < 9; bus++)
                        for(int underground = 0; underground < 5; underground++)
                            distance[start][to][taxi][bus][underground] = (byte) allBfs.getDistances()[start][to][taxi][bus][underground].distance();

        // Generates number of valid moves
        for(int node = 1; node < 200; node++)
            for(int taxi = 0; taxi < 3; taxi++)
                for(int bus = 0; bus < 3; bus++)
                    for(int underground = 0; underground < 3; underground++)
                        for(int doublet = 0; doublet < 2; doublet++)
                            for(int secret = 0; secret < 3; secret++)
                                validMoves[node][taxi][bus][underground][doublet][secret] =
                                        model.getValidMovesForPlayer(
                                               new ScotlandYardAIPlayer(node, taxi, bus, underground, doublet, secret)
                                        ).size();
    }

    public void writeToFile(){
        try{
            OutputStream o = new FileOutputStream(distanceFile);
            for(int start = 1; start < 200; start++)
                for(int to = 1; to < 200; to++)
                    for(int taxi = 0; taxi < 12; taxi++)
                        for(int bus = 0; bus < 9; bus++)
                            o.write(distance[start][to][taxi][bus]);
            o.close();
            o = new FileOutputStream(validMoveFile);
            for(int node = 1; node < 200; node++)
                for(int taxi = 0; taxi < 3; taxi++)
                    for(int bus = 0; bus < 3; bus++)
                        for(int underground = 0; underground < 3; underground++)
                            for(int doublet = 0; doublet < 2; doublet++)
                                for(int secret = 0; secret < 3; secret++)
                                    for(int b = 0; b < 4; b++)
                                        o.write((byte) (validMoves[node][taxi][bus][underground][doublet][secret] >> ((3 - b) * 8)));
            o.close();
        }
        catch (Exception e){
            System.out.println("Exception: " + e);
        }
    }

    public int getDistance(int from, int to, HashMap<Ticket, Integer> ticketMap){
        int taxi = ticketMap.getOrDefault(Ticket.TAXI, 0),
            bus = ticketMap.getOrDefault(Ticket.BUS, 0),
            underground = ticketMap.getOrDefault(Ticket.UNDERGROUND, 0);
        taxi = Integer.min(taxi, 11);
        bus = Integer.min(bus, 8);
        underground = Integer.min(underground, 4);

        return distance[from][to][taxi][bus][underground];
    }

    public int getValidMoves(int node, HashMap<Ticket, Integer> ticketMap){
        int taxi = ticketMap.getOrDefault(Ticket.TAXI, 0),
            bus = ticketMap.getOrDefault(Ticket.BUS, 0),
            underground = ticketMap.getOrDefault(Ticket.UNDERGROUND, 0),
            doublet = ticketMap.getOrDefault(Ticket.DOUBLE, 0),
            secret = ticketMap.getOrDefault(Ticket.SECRET, 0);

        taxi = Integer.min(taxi, 2);
        bus = Integer.min(bus, 2);
        underground = Integer.min(underground, 2);
        doublet = Integer.min(doublet, 1);
        secret = Integer.min(secret, 2);

        return validMoves[node][taxi][bus][underground][doublet][secret];
    }

}
