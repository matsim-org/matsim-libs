package playground.clruch;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.core.networks.VirtualNetworkIO;
import ch.ethz.idsc.queuey.util.GZHandler;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.analysis.minimumfleetsize.MinimumFleetSizeCalculator;
import playground.clruch.analysis.minimumfleetsize.MinimumFleetSizeIO;
import playground.clruch.analysis.performancefleetsize.PerformanceFleetSizeCalculator;
import playground.clruch.analysis.performancefleetsize.PerformanceFleetSizeIO;
import playground.clruch.data.LocationSpec;
import playground.clruch.netdata.MatsimCenterVirtualNetworkCreator;
import playground.clruch.netdata.MatsimKMEANSVirtualNetworkCreator;
import playground.clruch.prep.NetworkCutClean;
import playground.clruch.prep.PopulationRequestSchedule;
import playground.clruch.prep.PopulationTools;
import playground.clruch.prep.TheApocalypse;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataIO;
import playground.clruch.utils.PropertiesExt;

/** Class to prepare a given scenario for MATSim, includes preparation of netowrk, population, creation of virtualNetwork
 * and travelData objects.
 * 
 * @author clruch */
// TODO clean up thoroughly this file
public class ScenarioPreparer {

    private final static String NETWORKUPDATEDNAME = "networkConverted";
    private final static String POPULATIONUPDATEDNAME = "populationConverted";

    public static void main(String[] args) throws MalformedURLException, Exception {
        run(args);
    }

    public static void run(String[] args) throws MalformedURLException, Exception {

        // load options
        File workingDirectory = new File("").getCanonicalFile();
        PropertiesExt simOptions = PropertiesExt.wrap(ScenarioOptions.load(workingDirectory));

        File configFile = new File(workingDirectory, simOptions.getString("fullConfig"));
        System.out.println("loading config file to get data " + configFile.getAbsoluteFile());

        // TODO wrap properties with new class, class contains function getBoolean...
        boolean populationeliminateFreight = simOptions.getBoolean("populationeliminateFreight");
        boolean populationeliminateWalking = simOptions.getBoolean("populationeliminateWalking");
        boolean populationchangeModeToAV = simOptions.getBoolean("populationchangeModeToAV");
        LocationSpec ls = simOptions.getLocationSpec();
        int numVirtualNodes = simOptions.getInt("numVirtualNodes");
        boolean completeGraph = simOptions.getBoolean("completeGraph");
        int maxPopulationSize = simOptions.getInt("maxPopulationSize");
        int dtTravelData = simOptions.getInt("dtTravelData");
        boolean calculatePerformanceFleetSize = simOptions.getBoolean("calculatePerformanceFleetSize");
        boolean centerNetwork = simOptions.getBoolean("centerNetwork");
        String VIRTUALNETWORKFOLDERNAME = simOptions.getString("virtualNetworkDir");
        String VIRTUALNETWORKFILENAME = simOptions.getString("virtualNetworkName");
        String TRAVELDATAFILENAME = simOptions.getString("travelDataName");
        String MINIMUMFLEETSIZEFILENAME = simOptions.getString("minimumFleetSizeFileName");
        String PERFORMANCEFLEETSIZEFILENAME = simOptions.getString("performanceFleetSizeFileName");

        // 0) load files
        Config config = ConfigUtils.loadConfig(configFile.toString());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();

        Population population = scenario.getPopulation();

        {// 1) cut network (and reduce population to new network)
            NetworkCutClean.elminateOutsideRadius(network, ls.center, ls.radius);
            final File fileExportGz = new File(workingDirectory, NETWORKUPDATEDNAME + ".xml.gz");
            final File fileExport = new File(workingDirectory, NETWORKUPDATEDNAME + ".xml");
            {
                // write the modified population to file
                NetworkWriter nw = new NetworkWriter(network);
                nw.write(fileExportGz.toString());
            }
            // extract the created .gz file
            try {
                GZHandler.extract(fileExportGz, fileExport);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("saved converted network to: " + workingDirectory + NETWORKUPDATEDNAME + ".xml");
        }

        {// 2) adapt the population to new network
            System.out.println("Original population size: " + population.getPersons().values().size());
            PopulationTools.elminateOutsideNetwork(population, network);
            System.out.println("Population size after radius cut: " + population.getPersons().values().size());
            if (populationeliminateFreight)
                PopulationTools.eliminateFreight(population);
            System.out.println("Population size after removing freight: " + population.getPersons().values().size());
            if (populationeliminateWalking)
                PopulationTools.eliminateWalking(population);
            System.out.println("Population size after removing walking people: " + population.getPersons().values().size());
            if (populationchangeModeToAV) { // FIXME not sure if this is still required, or should always happen !?
                System.out.println("Population size after conversion to mode AV:" + population.getPersons().values().size());
                PopulationTools.changeModesOfTransportToAV(population);
            }
            System.out.println("Population size after conversion to mode AV:" + population.getPersons().values().size());
            TheApocalypse.decimatesThe(population).toNoMoreThan(maxPopulationSize).people();
            System.out.println("Population after decimation:" + population.getPersons().values().size());
            GlobalAssert.that(population.getPersons().size() > 0);

            final File fileExportGz = new File(workingDirectory, POPULATIONUPDATEDNAME + ".xml.gz");
            final File fileExport = new File(workingDirectory, POPULATIONUPDATEDNAME + ".xml");

            {
                // write the modified population to file
                PopulationWriter pw = new PopulationWriter(population);
                pw.write(fileExportGz.toString());
            }

            // extract the created .gz file
            try {
                GZHandler.extract(fileExportGz, fileExport);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 3) create virtual Network
        // TODO make this generic for any VirtualNetwork creators. 
        VirtualNetwork<Link> virtualNetwork;
        if(centerNetwork){
            MatsimCenterVirtualNetworkCreator centercreator = new MatsimCenterVirtualNetworkCreator();
            virtualNetwork = centercreator.creatVirtualNetwork(network);            
        }else{
            MatsimKMEANSVirtualNetworkCreator kmeansVirtualNetworkCreator = new MatsimKMEANSVirtualNetworkCreator();
            virtualNetwork = kmeansVirtualNetworkCreator.createVirtualNetwork(population, network, numVirtualNodes, completeGraph);            
        }

        final File vnDir = new File(workingDirectory, VIRTUALNETWORKFOLDERNAME);
        vnDir.mkdir(); // create folder if necessary
        (new VirtualNetworkIO<Link>()).toByte(new File(vnDir, VIRTUALNETWORKFILENAME), virtualNetwork);
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
