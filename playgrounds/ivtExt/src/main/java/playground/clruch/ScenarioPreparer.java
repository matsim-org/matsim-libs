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
import ch.ethz.idsc.tensor.Tensors;
import playground.clruch.analysis.minimumfleetsize.MinimumFleetSizeCalculator;
import playground.clruch.analysis.minimumfleetsize.MinimumFleetSizeIO;
import playground.clruch.analysis.performancefleetsize.PerformanceFleetSizeCalculator;
import playground.clruch.analysis.performancefleetsize.PerformanceFleetSizeIO;
import playground.clruch.netdata.MatsimCenterVirtualNetworkCreator;
import playground.clruch.netdata.MatsimKMEANSVirtualNetworkCreator;
import playground.clruch.prep.NetworkCutClean;
import playground.clruch.prep.PopulationRequestSchedule;
import playground.clruch.prep.PopulationTools;
import playground.clruch.prep.TheApocalypse;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataIO;
import playground.lsieber.networkshapecutter.PrepSettings;

/** Class to prepare a given scenario for MATSim, includes preparation of netowrk, population, creation of virtualNetwork
 * and travelData objects.
 * 
 * @author clruch */
// TODO clean up thoroughly this file
public class ScenarioPreparer {

    public static void main(String[] args) throws MalformedURLException, Exception {
        run(args);
    }

    public static void run(String[] args) throws MalformedURLException, Exception {

        // load Settings from IDSC Options
        PrepSettings settings = new PrepSettings();

        // 0) load files
        Config config = ConfigUtils.loadConfig(settings.configFile.toString());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();

        Population population = scenario.getPopulation();

        {// 1) cut network (and reduce population to new network)
            if (network == null)
                System.out.println("its the network");
            if (settings.locationSpec == null)
                System.out.println("its the ls");
            NetworkCutClean.elminateOutsideRadius(network, settings.locationSpec.center, settings.locationSpec.radius);
            final File fileExportGz = new File(settings.workingDirectory, settings.NETWORKUPDATEDNAME + ".xml.gz");
            final File fileExport = new File(settings.workingDirectory, settings.NETWORKUPDATEDNAME + ".xml");
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
            System.out.println("saved converted network to: " + settings.workingDirectory + settings.NETWORKUPDATEDNAME + ".xml");
        }

        {// 2) adapt the population to new network
            System.out.println("Original population size: " + population.getPersons().values().size());
            PopulationTools.elminateOutsideNetwork(population, network);
            // System.out.println("Population size after radius cut: " + population.getPersons().values().size());
            if (settings.populationeliminateFreight)
                PopulationTools.eliminateFreight(population);
            System.out.println("Population size after removing freight: " + population.getPersons().values().size());
            if (settings.populationeliminateWalking)
                PopulationTools.eliminateWalking(population);
            System.out.println("Population size after removing walking people: " + population.getPersons().values().size());
            if (settings.populationchangeModeToAV) { // FIXME not sure if this is still required, or should always happen !?
                System.out.println("Population size after conversion to mode AV:" + population.getPersons().values().size());
                PopulationTools.changeModesOfTransportToAV(population);
            }
            System.out.println("Population size after conversion to mode AV:" + population.getPersons().values().size());
            TheApocalypse.decimatesThe(population).toNoMoreThan(settings.maxPopulationSize).people();
            System.out.println("Population after decimation:" + population.getPersons().values().size());
            GlobalAssert.that(population.getPersons().size() > 0);

            final File fileExportGz = new File(settings.workingDirectory, settings.POPULATIONUPDATEDNAME + ".xml.gz");
            final File fileExport = new File(settings.workingDirectory, settings.POPULATIONUPDATEDNAME + ".xml");

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

        System.out.println(network.getLinks().size());
        System.out.println(population.getPersons().size());

        // 3) create virtual Network
        // TODO make this generic for any VirtualNetwork creators.
        VirtualNetwork<Link> virtualNetwork;
        if (settings.centerNetwork) {
            MatsimCenterVirtualNetworkCreator centercreator = new MatsimCenterVirtualNetworkCreator();
            virtualNetwork = centercreator.creatVirtualNetwork(network, 2000.0, Tensors.vector(-900.0, -2300.0));

        } else {
            MatsimKMEANSVirtualNetworkCreator kmeansVirtualNetworkCreator = new MatsimKMEANSVirtualNetworkCreator();
            virtualNetwork = kmeansVirtualNetworkCreator.createVirtualNetwork(population, network, settings.numVirtualNodes, settings.completeGraph);
        }

        final File vnDir = new File(settings.workingDirectory, settings.VIRTUALNETWORKFOLDERNAME);
        vnDir.mkdir(); // create folder if necessary
        GlobalAssert.that(virtualNetwork != null);
        VirtualNetworkIO.toByte(new File(vnDir, settings.VIRTUALNETWORKFILENAME), virtualNetwork);
        System.out.println("saved virtual network byte format to : " + new File(vnDir, settings.VIRTUALNETWORKFILENAME));
        PopulationRequestSchedule prs = new PopulationRequestSchedule(network, population, virtualNetwork);
        prs.exportCsv();
        // 3) generate travelData
        TravelData travelData = new TravelData(virtualNetwork, network, scenario.getPopulation(), settings.dtTravelData);
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
