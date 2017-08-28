package playground.clruch;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Properties;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import playground.clruch.analysis.minimumfleetsize.MinimumFleetSizeCalculator;
import playground.clruch.analysis.minimumfleetsize.MinimumFleetSizeIO;
import playground.clruch.analysis.performancefleetsize.PerformanceFleetSizeCalculator;
import playground.clruch.analysis.performancefleetsize.PerformanceFleetSizeIO;
import playground.clruch.data.LocationSpec;
import playground.clruch.netdata.KMEANSVirtualNetworkCreator;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkIO;
import playground.clruch.prep.NetworkCutClean;
import playground.clruch.prep.PopulationRequestSchedule;
import playground.clruch.prep.PopulationTools;
import playground.clruch.prep.TheApocalypse;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataIO;
import playground.clruch.utils.GZHandler;
import playground.clruch.utils.GlobalAssert;

/** Class to prepare a given scenario for MATSim, includes preparation of netowrk, population, creation of virtualNetwork
 * and travelData objects.
 * 
 * @author clruch */
public class ScenarioPreparer {

    // TODO load these from String
    private final static String VIRTUALNETWORKFOLDERNAME = "virtualNetwork";
    private final static String VIRTUALNETWORKFILENAME = "virtualNetwork";
    private final static String MINIMUMFLEETSIZEFILENAME = "minimumFleetSizeCalculator";
    private final static String PERFORMANCEFLEETSIZEFILENAME = "performanceFleetSizeCalculator";
    private final static String TRAVELDATAFILENAME = "travelData";
    private final static String NETWORKUPDATEDNAME = "networkConverted";
    private final static String POPULATIONUPDATEDNAME = "populationConverted";

    public static void main(String[] args) throws MalformedURLException, Exception {
        run(args);
    }

    public static void run(String[] args) throws MalformedURLException, Exception {

        File workingDirectory = new File("").getCanonicalFile();
        Properties simOptions = ScenarioOptions.load(workingDirectory);        


        File configFile = new File(workingDirectory, simOptions.getProperty("fullConfig"));
        System.out.println("loading config file to get data " + configFile.getAbsoluteFile());        

        // TODO wrap properties with new class, class contains function getBoolean... 
        boolean populationeliminateFreight = Boolean.valueOf(simOptions.getProperty("populationeliminateFreight"));
        boolean populationeliminateWalking = Boolean.valueOf(simOptions.getProperty("populationeliminateWalking"));
        boolean populationchangeModeToAV = Boolean.valueOf(simOptions.getProperty("populationchangeModeToAV"));
        LocationSpec ls = LocationSpec.fromString(simOptions.getProperty("LocationSpec")); 
        int numVirtualNodes = Integer.valueOf(simOptions.getProperty("numVirtualNodes"));
        boolean completeGraph = Boolean.valueOf(simOptions.getProperty("completeGraph"));
        int maxPopulationSize = Integer.valueOf(simOptions.getProperty("maxPopulationSize"));
        int dtTravelData = Integer.valueOf(simOptions.getProperty("dtTravelData"));

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
            System.out.println("saved converted network to: " +workingDirectory + NETWORKUPDATEDNAME + ".xml");
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
        KMEANSVirtualNetworkCreator kmeansVirtualNetworkCreator = new KMEANSVirtualNetworkCreator();
        VirtualNetwork virtualNetwork = kmeansVirtualNetworkCreator.createVirtualNetwork(population, network, numVirtualNodes, completeGraph);
        final File vnDir = new File(workingDirectory, VIRTUALNETWORKFOLDERNAME);
        vnDir.mkdir(); // create folder if necessary
        VirtualNetworkIO.toByte(new File(vnDir, VIRTUALNETWORKFILENAME), virtualNetwork);
        VirtualNetworkIO.toXML(new File(vnDir, VIRTUALNETWORKFILENAME + ".xml").toString(), virtualNetwork);
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

            int maxNumberVehiclesPerformanceCalculator = (int) (population.getPersons().size() * 0.3);
            PerformanceFleetSizeCalculator performanceFleetSizeCalculator = //
                    new PerformanceFleetSizeCalculator(virtualNetwork, travelData, maxNumberVehiclesPerformanceCalculator);
            PerformanceFleetSizeIO.toByte(new File(vnDir, PERFORMANCEFLEETSIZEFILENAME), performanceFleetSizeCalculator);

        }

        System.out.println("successfully converted simulation data files from in " + workingDirectory);
    }
}
