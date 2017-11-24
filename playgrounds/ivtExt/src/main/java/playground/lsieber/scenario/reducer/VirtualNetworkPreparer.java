package playground.lsieber.scenario.reducer;

import java.io.File;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.core.networks.VirtualNetworkIO;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.analysis.minimumfleetsize.MinimumFleetSizeCalculator;
import playground.clruch.analysis.minimumfleetsize.MinimumFleetSizeIO;
import playground.clruch.analysis.performancefleetsize.PerformanceFleetSizeCalculator;
import playground.clruch.analysis.performancefleetsize.PerformanceFleetSizeIO;
import playground.clruch.prep.PopulationRequestSchedule;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataIO;
import playground.lsieber.networkshapecutter.PrepSettings;

public enum VirtualNetworkPreparer {
    ;
    private static VirtualNetwork<Link> virtualNetwork;

    public static void run(Network network, Population population, PrepSettings settings) throws Exception {

        virtualNetwork = settings.createVirtualNetworkCreator().create(network, population, settings);

        final File vnDir = new File(settings.workingDirectory, settings.VIRTUALNETWORKFOLDERNAME);
        vnDir.mkdir(); // create folder if necessary
        GlobalAssert.that(virtualNetwork != null);
        VirtualNetworkIO.toByte(new File(vnDir, settings.VIRTUALNETWORKFILENAME), virtualNetwork);
        System.out.println("saved virtual network byte format to : " + new File(vnDir, settings.VIRTUALNETWORKFILENAME));
        PopulationRequestSchedule prs = new PopulationRequestSchedule(network, population, virtualNetwork);
        prs.exportCsv();
        // 3) generate travelData
        TravelData travelData = new TravelData(virtualNetwork, network, population, settings.dtTravelData);
        TravelDataIO.toByte(new File(vnDir, settings.TRAVELDATAFILENAME), travelData);
        System.out.println("saved travelData byte format to : " + new File(vnDir, settings.TRAVELDATAFILENAME));

        {// 4) calculate minimum and performance fleet size and save results
            MinimumFleetSizeCalculator minimumFleetSizeCalculator = new MinimumFleetSizeCalculator(network, population, virtualNetwork, travelData);
            MinimumFleetSizeIO.toByte(new File(vnDir, settings.MINIMUMFLEETSIZEFILENAME), minimumFleetSizeCalculator);

            if (settings.calculatePerformanceFleetSize) {
                int maxNumberVehiclesPerformanceCalculator = (int) (population.getPersons().size() * 0.3);
                PerformanceFleetSizeCalculator performanceFleetSizeCalculator = //
                        new PerformanceFleetSizeCalculator(virtualNetwork, travelData, maxNumberVehiclesPerformanceCalculator);
                PerformanceFleetSizeIO.toByte(new File(vnDir, settings.PERFORMANCEFLEETSIZEFILENAME), performanceFleetSizeCalculator);
            }
        }
        virtualNetwork.printVirtualNetworkInfo();
        System.out.println("successfully converted simulation data files from in " + settings.workingDirectory);
    }
}
