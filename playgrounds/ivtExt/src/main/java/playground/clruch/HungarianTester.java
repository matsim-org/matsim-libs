package playground.clruch;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.utils.HungarianAlgorithm;

/**
 * Created by Claudio on 2/27/2017.
 */
public class HungarianTester {
    public static void main(String[] arg){
        System.out.println("Hungarian Bipartite Matching Running now");

        // initialize distance matrix
        int n = 10; // number of workers
        int m = 2; // number of jobs

        final double distancematrix[][] = new double[n][m];
        for(int i = 0; i<n; ++i){
            for(int j = 0; j<m; ++j){
                distancematrix[i][j] = 1.0;
            }
        }



        distancematrix[8][0] = 0.2;
        distancematrix[5][1] = 0.2;
        /*
        distancematrix[1][0] = 3.0;
        distancematrix[1][1] = 1.5;
        distancematrix[1][2] = 1.0;
        */


        for(int i = 0; i<n; ++i){
            for(int j = 0; j<m; ++j){
                System.out.println("Distance from worker " + i + " to job " + j + " is " + distancematrix[i][j]);
            }
        }



        HungarianAlgorithm hungarianAlgorithm = new HungarianAlgorithm(distancematrix);

        // vehicle at position i is assigned to destination matchinghungarianAlgorithm[j]
        int matchinghungarianAlgorithm[] = hungarianAlgorithm.execute(); // O(n^3)


        for(int i = 0; i<n; ++i){
            System.out.println("assigned worker "+ i + " to job "+ matchinghungarianAlgorithm[i]);
        }
    }
}
