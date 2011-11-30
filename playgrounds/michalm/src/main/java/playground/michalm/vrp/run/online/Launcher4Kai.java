package playground.michalm.vrp.run.online;

import java.io.*;


public class Launcher4Kai
{
    public static void main(String[] args)
        throws IOException
    {
        String[] arguments = new String[5];
        // PATHS
        arguments[0] = "../../maciejewski/input/test/taxi_single_iteration/grid-net";
        arguments[1] = "network.xml";
        arguments[2] = "plans.xml";
        arguments[3] = "depots.xml";

        // OFTVis on?
        arguments[4] = "true";

        SingleIterOnlineDVRPLauncher.main(arguments);
    }
}
