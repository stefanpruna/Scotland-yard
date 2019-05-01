package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Ticket;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// Class that caches the distance and the valid moves from all nodes (and to all nodes).
public class Cache{

    final private File distanceFile = new File("distance.dat");
    final private File validMoveFile = new File("moves.dat");

    private byte[][][][][] distance = new byte[200][200][12][9][5];
    private int[][][][][][] validMoves = new int[200][3][3][3][2][3];

    /**
     * Main constructor for the Cache class
     */
    Cache(){
        // If the files exist, load the contents into memory as matrices.
        if(distanceFile.exists() && validMoveFile.exists()){
            try{
                FileInputStream in = new FileInputStream(distanceFile);
                byte[] tempDist = new byte[200 * 200 * 12 * 9 * 5];
                // Reads the file as a byte array, then stores it as a byte matrix.
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
                // Reads the file as a byte array, then stores it as an int matrix.
                in.read(tempMoves);
                i = 0;

                for(int node = 1; node < 200; node++)
                    for(int taxi = 0; taxi < 3; taxi++)
                        for(int bus = 0; bus < 3; bus++)
                            for(int underground = 0; underground < 3; underground++)
                                for(int doublet = 0; doublet < 2; doublet++)
                                    for(int secret = 0; secret < 3; secret++){
                                        // Compute the int from 4 consecutive bytes in the byte array
                                        for(int b = 0; b < 4; b++)
                                            validMoves[node][taxi][bus][underground][doublet][secret] =
                                                    (validMoves[node][taxi][bus][underground][doublet][secret] << 8) | (tempMoves[b + i] & 0xFF);
                                        i += 4;
                                    }
            }
            catch(Exception e){
                System.out.println("Exception: " + e);
            }
        }
    }

    /**
     * Constructor for when the files are missing
     *
     * @param model the model used to generate the distances
     */
    Cache(ScotlandYardAIModel model){
        // Generates distances from everywhere with every configuration.
        List<ScotlandYardAIPlayer> allPossiblePlayers = new ArrayList<>();
        for(int start = 1; start < 200; start++)
            for(int taxi = 0; taxi < 12; taxi++)
                for(int bus = 0; bus < 9; bus++)
                    for(int underground = 0; underground < 5; underground++)
                        allPossiblePlayers.add(new ScotlandYardAIPlayer(start, taxi, bus, underground));
        // Creates a BFS object to generate all distances, then saves them to cache
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

    /**
     *  Writes the matrices to file
     */
    void writeToFile(){
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

    /**
     *
     * @param from start node
     * @param to end node
     * @param ticketMap player ticket map
     * @return returns the distance from the start node to the end node with the given ticket configuration to start
     */
    int getDistance(int from, int to, HashMap<Ticket, Integer> ticketMap){
        int taxi = ticketMap.getOrDefault(Ticket.TAXI, 0),
            bus = ticketMap.getOrDefault(Ticket.BUS, 0),
            underground = ticketMap.getOrDefault(Ticket.UNDERGROUND, 0);
        taxi = Integer.min(taxi, 11);
        bus = Integer.min(bus, 8);
        underground = Integer.min(underground, 4);

        return distance[from][to][taxi][bus][underground];
    }

    /**
     *
     * @param node the node
     * @param ticketMap the available tickets
     * @return returns the number of valid moves from certain node with given ticket configuration
     */
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
