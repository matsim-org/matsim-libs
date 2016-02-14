package playground.michalm.poznan.supply;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.*;
import org.matsim.core.network.*;
import org.matsim.core.network.algorithms.NetworkCleaner;
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
        config.transit().setUseTransit(true);
        Scenario scenario = ScenarioUtils.createScenario(config);

        final VisumNetwork vNetwork = new VisumNetwork();
        new VisumNetworkReader(vNetwork).read(visumFile);
        Visum2TransitSchedule converter = new Visum2TransitSchedule(vNetwork,
                scenario.getTransitSchedule(), scenario.getTransitVehicles());

        // $TSYS:CODE;NAME;TYPE;PCU
        // 1S;osobowe;PrT;1.000
        // 2D;dostawcze;PrT;1.000
        // 3C;ciężarowe;PrT;2.500
        // 4R;Rower;PrT;1.000
        // 5S_zewn;osobowe_zewn;PrT;1.000
        // 6D_zewn;dostawcze_zewn;PrT;1.000
        // 7C_zewn;ciężarowe_zewn;PrT;2.000
        // 8Cc_zewn;ciężarowe ciężkie_zewn;PrT;3.000
        // A;Autobusy ZTM;PuT;1.000
        // AT;Tramwaj;PuT;1.000
        // KP;Komunikacja podmiejska;PuT;1.000
        // TKR;Kolej Regionalna;PuT;1.000
        // TKS;Kolej IC;PuT;1.000
        // U;Przewozy_PKS;PuT;1.000
        // UAM;Autobus marketowy;PuT;1.000
        // W;Przejścia piesze;PuTWalk;1.000
        // WP;Pieszo;PuTWalk;1.000
        // WP-2;Pieszo-2;PuT;1.000         

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

        Network network = NetworkUtils.createNetwork();
        new CreatePseudoNetwork(scenario.getTransitSchedule(), network, "tr_").createNetwork();

        new NetworkCleaner().run(network);

        List<Node> nodesToRemove = new ArrayList<>();
        for (Node n : network.getNodes().values()) {
            if (n.getInLinks().size() == 0 && n.getOutLinks().size() == 0) {
                nodesToRemove.add(n);
            }
        }

        for (Node n : nodesToRemove) {
            network.removeNode(n.getId());
        }

        new NetworkCleaner().run(network);

        new NetworkWriter(network).write(transitNetworkFile);
        //new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(transitScheduleWithNetworkFile);
    }


    public static void main(String[] args)
    {
        //String visumFile = "d:/GoogleDrive/Poznan/Visum_2014/network/network_ver.4.net";
        //String visumFile = "d:/GoogleDrive/Poznan/Visum_2014/network/network_ver.5_(33N).net";
        String visumFile = "d:/GoogleDrive/Poznan/Visum_2014/network/A {ZTM.net";
        String outDir = "d:/PP-rad/poznan/test/";
        String transitScheduleWithNetworkFile = outDir + "transitSchedule.xml";
        String transitNetworkFile = outDir + "pt_network.xml";
        String vehicleFile = outDir + "pt_vehicles.xml";

        go(visumFile, transitScheduleWithNetworkFile, transitNetworkFile, vehicleFile);
    }
}
