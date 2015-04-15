package playground.michalm.poznan;

import java.util.Arrays;

import org.matsim.contrib.otfvis.OTFVis;


public class OTFVisSim
{

    public static void main(String[] args)
    {
        String dirName;
        String mviFileName;

        if (args.length == 1 && args[0].equals("test")) {// for testing
            dirName = "d:\\eclipse-matsim\\bartekp\\";

            // mviFileName = "output\\config-verB\\ITERS\\it.10\\10.otfvis.mvi";
            mviFileName = "output\\poznan\\ITERS\\it.20\\20.otfvis.mvi";
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
