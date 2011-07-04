package playground.michalm.visualization;

import java.util.*;

import org.matsim.run.*;


public class OTFVisSimLive
{
    public static void main(String[] args)
    {
        String dirName;
        String cfgFileName;

        if (args.length == 1 && args[0].equals("test")) {// for testing
            dirName = "D:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec1\\";
            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec2\\";
            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\Paj\\";
            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\NSE\\";

            cfgFileName = "config-verB_with_vrp.xml";
        }
        else if (args.length == 2) {
            dirName = args[0];
            cfgFileName = args[1];
        }
        else {
            throw new IllegalArgumentException("Incorrect program arguments: "
                    + Arrays.toString(args));
        }

        OTFVis.playConfig(dirName + cfgFileName);
    }
}
