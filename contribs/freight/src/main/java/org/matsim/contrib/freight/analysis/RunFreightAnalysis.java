package org.matsim.contrib.freight.analysis;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReader;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.MatsimVehicleReader;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import java.io.File;

public class RunFreightAnalysis {
    public static void main(String[] args) {
        RunFreightAnalysis rfa = new RunFreightAnalysis();
        // path to your output files:
        // (For a quick start, run the "RunFAIT" test to generate sample output from the chessboard-example and then run the analysis on that:)
        String basePath = "src/test/output/chessboard/matsim";
        //basePath = "/Users/jakob/debianserv/data/Uni/Master/2020_WS/MATSim_Advanced/matsim-freight/Input_KMT/21_ICEVBEV_NwCE_BVWP_10000it_DC_noTax";
        rfa.runAnalysis(basePath);
    }
    public RunFreightAnalysis() { }
    
    private void runAnalysis(String basePath){
       File networkFile = new File(basePath + "/output_network.xml.gz");
       File carrierFile = new File(basePath + "/output_carriers.xml");
       File vehiclesFile = new File(basePath + "/output_allVehicles.xml.gz");
       File eventsFile = new File(basePath + "/output_events.xml.gz");

       Network network = NetworkUtils.readNetwork(networkFile.getAbsolutePath());

       Carriers carriers = new Carriers();
       new CarrierPlanXmlReader(carriers).readFile(carrierFile.getAbsolutePath());

       Vehicles vehicles = new VehicleUtils().createVehiclesContainer();
       new  MatsimVehicleReader(vehicles).readFile(vehiclesFile.getAbsolutePath());

       EventsManager eventsManager = EventsUtils.createEventsManager();
       FreightAnalysisEventHandler freightEventHandler = new FreightAnalysisEventHandler(network, vehicles,  carriers);
       eventsManager.addHandler(freightEventHandler);

       eventsManager.initProcessing();
       MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);



       eventsReader.readFile(eventsFile.getAbsolutePath());
       eventsManager.finishProcessing();
       freightEventHandler.exportVehicleInfo("freightOutput", true);
       freightEventHandler.exportVehicleTripInfo("freightOutput", true);
       freightEventHandler.exportVehicleTypeStats("freightOutput", true);
       freightEventHandler.exportServiceInfo("freightOutput", true);
       freightEventHandler.exportShipmentInfo("freightOutput", true);
    }
}
