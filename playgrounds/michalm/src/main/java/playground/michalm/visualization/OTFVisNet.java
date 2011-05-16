package playground.michalm.visualization;

import java.util.*;

import org.matsim.run.*;


public class OTFVisNet
{
    public static void main(String[] args)
    {
        String dirName;
        String netFileName;

        if (args.length == 1 && args[0].equals("test")) {// for testing
            dirName = "D:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec1\\";
            netFileName = "network.xml";
        }
        else if (args.length == 2) {
            dirName = args[0];
            netFileName = args[1];
        }
        else {
            throw new IllegalArgumentException("Incorrect program arguments: "
                    + Arrays.toString(args));
        }

        OTFVis.playNetwork(dirName + netFileName);
    }
}
