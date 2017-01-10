package playground.michalm.poznan;

import org.matsim.core.config.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.InvertedNetworkRoutingGuiceModule;


public class PoznanControler
{
    public static void main(final String[] args)
    {
        //String configFile = "d:/bartekp/poznan/input/poznan/22_11/matsim/poznan_22_11.xml";
        //String configFile = "d:/PP-rad/poznan/test/config.xml";
        String configFile = "d:/temp/wal/Hetmanska28czerwca_1agent_with_lanes.xml";
        Config config = ConfigUtils.loadConfig(configFile);

        //MatsimRandom.reset(43);
        Controler controler = new Controler(config);
        if (config.controler().isLinkToLinkRoutingEnabled()) {
            controler.addOverridingModule(new InvertedNetworkRoutingGuiceModule());
        }

        //controler.addOverridingModule(new OTFVisLiveModule());

        controler.run();
    }
}
