package playground.michalm.poznan;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.*;
import org.matsim.visum.*;

import playground.mrieser.pt.converter.Visum2TransitSchedule;


public class CreatePoznanPT
{
    private final static String VISUM_FILE = "d:/OneDrive/Poznan/Visum_2014/network/network.net ";


    public static void main(String[] args)
    {
        ScenarioImpl scenario = (ScenarioImpl)ScenarioUtils.createScenario(ConfigUtils
                .createConfig());

        final VisumNetwork vNetwork = new VisumNetwork();
        new VisumNetworkReader(vNetwork).read(VISUM_FILE);
        Visum2TransitSchedule converter = new Visum2TransitSchedule(vNetwork,
                scenario.getTransitSchedule(), scenario.getTransitVehicles());

    }
}
