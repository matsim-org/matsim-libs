package playground.michalm.poznan;

import org.matsim.api.core.v01.*;
import org.matsim.core.config.*;
import org.matsim.core.network.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.visum.*;

import playground.mrieser.pt.converter.Visum2TransitSchedule;


public class CreatePoznanPT
{
    public static void go(String visumFile, String transitScheduleWithNetworkFile,
            String transitNetworkFile, String vehicleFile)
    {
        Config config = ConfigUtils.createConfig();
        config.scenario().setUseTransit(true);
        config.scenario().setUseVehicles(true);
        Scenario scenario = ScenarioUtils.createScenario(config);

        final VisumNetwork vNetwork = new VisumNetwork();
        new VisumNetworkReader(vNetwork).read(visumFile);
        Visum2TransitSchedule converter = new Visum2TransitSchedule(vNetwork,
                scenario.getTransitSchedule(), scenario.getTransitVehicles());

        converter.registerTransportMode("S", TransportMode.car);
        converter.registerTransportMode("D", TransportMode.car);
        converter.registerTransportMode("C", TransportMode.car);
        
        converter.registerTransportMode("R", TransportMode.bike);
        
        converter.registerTransportMode("S_zewn", TransportMode.car);
        converter.registerTransportMode("D_zewn", TransportMode.car);
        converter.registerTransportMode("C_zewn", TransportMode.car);
        converter.registerTransportMode("Cc_zewn", TransportMode.car);

        converter.registerTransportMode("A", TransportMode.pt);
        converter.registerTransportMode("AT", TransportMode.pt);
        converter.registerTransportMode("KP", TransportMode.pt);
        converter.registerTransportMode("TKR", TransportMode.pt);
        converter.registerTransportMode("TKS", TransportMode.pt);
        converter.registerTransportMode("U", TransportMode.pt);
        converter.registerTransportMode("UAM", TransportMode.pt);
        
        converter.registerTransportMode("W", TransportMode.walk);
        converter.registerTransportMode("WP", TransportMode.transit_walk);
        converter.registerTransportMode("WP-2", TransportMode.transit_walk);
        converter.convert();

        new TransitScheduleWriterV1(scenario.getTransitSchedule())
                .write(transitScheduleWithNetworkFile);
        new VehicleWriterV1(scenario.getTransitVehicles()).writeFile(vehicleFile);

        NetworkImpl network = NetworkImpl.createNetwork();
        new CreatePseudoNetwork(scenario.getTransitSchedule(), network, "tr_").createNetwork();
        new NetworkWriter(network).write(transitNetworkFile);
        //new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(transitScheduleWithNetworkFile);
    }


    public static void main(String[] args)
    {
        String visumFile = "d:/Google Drive/Poznan/Visum_2014/network/network_ver.4.net";
        String transitScheduleWithNetworkFile = "d:/transitSchedule.xml";
        String transitNetworkFile = "d:/pt_network.xml";
        String vehicleFile = "d:/pt_vehicles.xml";

        go(visumFile, transitScheduleWithNetworkFile, transitNetworkFile, vehicleFile);
    }
}
