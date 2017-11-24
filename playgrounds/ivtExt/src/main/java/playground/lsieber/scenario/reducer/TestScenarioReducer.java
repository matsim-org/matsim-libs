package playground.lsieber.scenario.reducer;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.core.networks.VirtualNetworkIO;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.Tensors;
import playground.clruch.analysis.minimumfleetsize.MinimumFleetSizeCalculator;
import playground.clruch.analysis.minimumfleetsize.MinimumFleetSizeIO;
import playground.clruch.analysis.performancefleetsize.PerformanceFleetSizeCalculator;
import playground.clruch.analysis.performancefleetsize.PerformanceFleetSizeIO;
import playground.clruch.netdata.MatsimCenterVirtualNetworkCreator;
import playground.clruch.netdata.MatsimKMEANSVirtualNetworkCreator;
import playground.clruch.prep.PopulationRequestSchedule;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataIO;
import playground.clruch.utils.PropertiesExt;

public class TestScenarioReducer {

    /** @author Lukas Sieber
     *         Hint: if Out of Memory error appears give the program more Memory with the -Xmx8192m argument in the Run Configurations VM paart
     * @throws Exception */
    public static void main(String[] args) throws Exception {
        ShapeScenarioReducer shapeScenarioReducer = new ShapeScenarioReducer();
        shapeScenarioReducer.run();
        shapeScenarioReducer.filterPopulationOfTargetAreaOnlyPt();
        shapeScenarioReducer.writeToXML();
        // shapeScenarioReducer.ConvertPtToAV();
        shapeScenarioReducer.addConfigFilesinFolder();
        // adaptedScenarioPreparer(shapeScenarioReducer);

        System.out.println("THIS IS THE END :)");
    }

    private static void adaptedScenarioPreparer(ShapeScenarioReducer shapeScenarioReducer) throws Exception {

        // load options
        File workingDirectory = shapeScenarioReducer.getWorkingDirectory();
        PropertiesExt simOptions = shapeScenarioReducer.getSimOptions();

        // TODO can some of these be directly read from another source? Do all have to be user settings?

        int numVirtualNodes = simOptions.getInt("numVirtualNodes");
        boolean completeGraph = simOptions.getBoolean("completeGraph");
        int dtTravelData = simOptions.getInt("dtTravelData");
        boolean calculatePerformanceFleetSize = simOptions.getBoolean("calculatePerformanceFleetSize");
        boolean centerNetwork = simOptions.getBoolean("centerNetwork");
        String VIRTUALNETWORKFOLDERNAME = simOptions.getString("virtualNetworkDir");
        String VIRTUALNETWORKFILENAME = simOptions.getString("virtualNetworkName");
        String TRAVELDATAFILENAME = simOptions.getString("travelDataName");
        String MINIMUMFLEETSIZEFILENAME = simOptions.getString("minimumFleetSizeFileName");
        String PERFORMANCEFLEETSIZEFILENAME = simOptions.getString("performanceFleetSizeFileName");

        // 0) load files
        Scenario scenario = shapeScenarioReducer.getOriginalScenario();
        Network network = shapeScenarioReducer.getNetwork();
        Population population = shapeScenarioReducer.getPopulation();


        // 3) create virtual Network
        // TODO make this generic for any VirtualNetwork creators.
        VirtualNetwork<Link> virtualNetwork;
        if (centerNetwork) {
            MatsimCenterVirtualNetworkCreator centercreator = new MatsimCenterVirtualNetworkCreator();
            virtualNetwork = centercreator.creatVirtualNetwork(network, 2000.0, Tensors.vector(-900.0, -2300.0));

        } else {
            MatsimKMEANSVirtualNetworkCreator kmeansVirtualNetworkCreator = new MatsimKMEANSVirtualNetworkCreator();
            virtualNetwork = kmeansVirtualNetworkCreator.createVirtualNetwork(population, network, numVirtualNodes, completeGraph);
        }

        final File vnDir = new File(workingDirectory, VIRTUALNETWORKFOLDERNAME);
        vnDir.mkdir(); // create folder if necessary
        GlobalAssert.that(virtualNetwork != null);
        VirtualNetworkIO.toByte(new File(vnDir, VIRTUALNETWORKFILENAME), virtualNetwork);
        System.out.println("saved virtual network byte format to : " + new File(vnDir, VIRTUALNETWORKFILENAME));
        PopulationRequestSchedule prs = new PopulationRequestSchedule(network, population, virtualNetwork);
        prs.exportCsv();
        // 3) generate travelData
        TravelData travelData = new TravelData(virtualNetwork, network, scenario.getPopulation(), dtTravelData);
        TravelDataIO.toByte(new File(vnDir, TRAVELDATAFILENAME), travelData);
        System.out.println("saved travelData byte format to : " + new File(vnDir, TRAVELDATAFILENAME));

        {// 4) calculate minimum and performance fleet size and save results
            MinimumFleetSizeCalculator minimumFleetSizeCalculator = new MinimumFleetSizeCalculator(network, population, virtualNetwork, travelData);
            MinimumFleetSizeIO.toByte(new File(vnDir, MINIMUMFLEETSIZEFILENAME), minimumFleetSizeCalculator);

            if (calculatePerformanceFleetSize) {
                int maxNumberVehiclesPerformanceCalculator = (int) (population.getPersons().size() * 0.3);
                PerformanceFleetSizeCalculator performanceFleetSizeCalculator = //
                        new PerformanceFleetSizeCalculator(virtualNetwork, travelData, maxNumberVehiclesPerformanceCalculator);
                PerformanceFleetSizeIO.toByte(new File(vnDir, PERFORMANCEFLEETSIZEFILENAME), performanceFleetSizeCalculator);
            }
        }
        virtualNetwork.printVirtualNetworkInfo();
        System.out.println("successfully converted simulation data files from in " + workingDirectory);

    }
}
