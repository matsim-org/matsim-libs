package playground.lsieber.scenario.preparer;

import java.io.File;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.core.networks.VirtualNetworkIO;
import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.analysis.minimumfleetsize.MinimumFleetSizeCalculator;
import playground.clruch.analysis.minimumfleetsize.MinimumFleetSizeIO;
import playground.clruch.analysis.performancefleetsize.PerformanceFleetSizeCalculator;
import playground.clruch.analysis.performancefleetsize.PerformanceFleetSizeIO;
import playground.clruch.options.ScenarioOptions;
import playground.clruch.prep.PopulationRequestSchedule;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataIO;
import playground.lsieber.networkshapecutter.VirtualNetworkCreators;

public enum VirtualNetworkPreparer {
    ;
    private static VirtualNetwork<Link> virtualNetwork;

    public static void run(Network network, Population population, ScenarioOptions scenOptions) throws Exception {

        VirtualNetworkCreators virtualNetworkCreators = scenOptions.getVirtualNetworkCreator();
        virtualNetwork = virtualNetworkCreators.create(network, population, scenOptions);

        //
        final File vnDir = new File(scenOptions.getVirtualNetworkName());
        System.out.println("vnDir = " +  vnDir.getAbsolutePath());
        vnDir.mkdir(); // create folder if necessary
        GlobalAssert.that(virtualNetwork != null);
        VirtualNetworkIO.toByte(new File(vnDir, scenOptions.getVirtualNetworkName()), virtualNetwork);
        System.out.println("saved virtual network byte format to : " + new File(vnDir, scenOptions.getVirtualNetworkName()));
        PopulationRequestSchedule prs = new PopulationRequestSchedule(network, population, virtualNetwork);
        prs.exportCsv();
        // 3) generate travelData
        TravelData travelData = new TravelData(virtualNetwork, network, population, scenOptions.getdtTravelData());
        TravelDataIO.toByte(new File(vnDir, scenOptions.getTravelDataName()), travelData);
        System.out.println("saved travelData byte format to : " + new File(vnDir, scenOptions.getTravelDataName()));

        {// 4) calculate minimum and performance fleet size and save results
            MinimumFleetSizeCalculator minimumFleetSizeCalculator = new MinimumFleetSizeCalculator(network, population, virtualNetwork, travelData);
            MinimumFleetSizeIO.toByte(new File(vnDir, scenOptions.getMinFleetName()), minimumFleetSizeCalculator);

            if (scenOptions.calculatePerfFleetSize()) {
                int maxNumberVehiclesPerformanceCalculator = (int) (population.getPersons().size() * 0.3);
                PerformanceFleetSizeCalculator performanceFleetSizeCalculator = //
                        new PerformanceFleetSizeCalculator(virtualNetwork, travelData, maxNumberVehiclesPerformanceCalculator);
                PerformanceFleetSizeIO.toByte(new File(vnDir, scenOptions.getPerformFleetName()), performanceFleetSizeCalculator);
            }
        }
        virtualNetwork.printVirtualNetworkInfo();
        System.out.println("successfully converted simulation data files from in " + MultiFileTools.getWorkingDirectory());
    }
}
