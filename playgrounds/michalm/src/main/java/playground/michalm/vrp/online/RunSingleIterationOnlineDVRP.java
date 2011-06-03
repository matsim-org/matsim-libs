package playground.michalm.vrp.online;

import java.io.*;

import playground.michalm.vrp.online.*;


public class RunSingleIterationOnlineDVRP
{
    public static void main(String[] args) throws IOException
    {
        String[] arguments = new String[9];
        arguments[0] = "input\\test\\single_iteration\\";
        arguments[1] = "config-verB.xml";
        arguments[2] = "dvrp\\";
        arguments[3] = "A101.txt";
        arguments[4] = "arc_times.txt.gz";
        arguments[5] = "arc_costs.txt.gz";
        arguments[6] = "arc_paths.txt.gz";
        arguments[7] = "A101_scen.txt";
        arguments[8] = "algorithm.txt";

        SingleIterOnlineDVRPLauncher.main(arguments);
    }
}
