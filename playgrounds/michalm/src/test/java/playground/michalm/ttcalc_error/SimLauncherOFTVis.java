package playground.michalm.ttcalc_error;

import org.matsim.run.*;


public class SimLauncherOFTVis
{
    public static void main(String[] args)
    {
        String cfg = "src/test/java/playground/michalm/ttcalc_error/error_1/config-OFTVis.xml";
        //cfg = "src/test/java/playground/michalm/ttcalc_error/error_2/config-OFTVis.xml";

        OTFVis.playConfig(cfg);
    }
}
