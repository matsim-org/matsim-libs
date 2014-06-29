package playground.jbischoff.taxi.berlin;

import org.matsim.core.controler.Controler;


public class BerlinNetworkChangeScenMain
{
    public static void main(String[] args)
    {
        Controler c = new Controler("/Users/jb/tucloud/berlin/2kW.15.output_config.xml");
        c.run();
    }
}
