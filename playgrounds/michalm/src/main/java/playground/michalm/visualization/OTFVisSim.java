package playground.michalm.visualization;

import java.util.*;

import org.matsim.run.*;


public class OTFVisSim
{

    public static void main(String[] args)
    {
        String dirName;
        String mviFileName;

        if (args.length == 1 && args[0].equals("test")) {// for testing
            //dirName = "D:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec1\\";
            //dirName = "D:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec2\\";
            //dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\Paj\\";
            dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\NSE\\";

            //mviFileName = "output\\config-verB\\ITERS\\it.10\\10.otfvis.mvi";
            mviFileName = "output\\config-verB\\ITERS\\it.50\\50.otfvis.mvi";
        }
        else if (args.length == 2) {
            dirName = args[0];
            mviFileName = args[1];
        }
        else {
            throw new IllegalArgumentException("Incorrect program arguments: "
                    + Arrays.toString(args));
        }

        OTFVis.playMVI(dirName + mviFileName);
    }
}
