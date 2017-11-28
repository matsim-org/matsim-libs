package playground.clruch;

import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.analysis.performancefleetsize.PerformanceFleetSizeCalculator;
import playground.clruch.net.SimulationServer;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.options.ScenarioOptions;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataGet;
import ch.ethz.matsim.av.framework.AVConfigGroup;

@Deprecated // TODO remove this
enum PerformanceFleetSizeCalculatorStandalone {
    ;

    public static void main(String[] args) throws IOException, InterruptedException {
        File workingDirectory = new File("").getCanonicalFile();
        ScenarioOptions scenOptions = ScenarioOptions.load(workingDirectory);

        // set to true in order to make server wait for at least 1 client, for instance viewer client
        boolean waitForClients = Boolean.valueOf(scenOptions.getString("waitForClients"));

        // open server port for clients to connect to
        SimulationServer.INSTANCE.startAcceptingNonBlocking();
        SimulationServer.INSTANCE.setWaitForClients(waitForClients);

        // load MATSim configs
        File configFile = new File(workingDirectory, scenOptions.getSimulationConfigName());
        System.out.println("loading config file " + configFile.getAbsoluteFile());

        GlobalAssert.that(configFile.exists());
        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);
        Config config = ConfigUtils.loadConfig(configFile.toString(), new AVConfigGroup(), dvrpConfigGroup);

        // load scenario for simulation
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        Population population = scenario.getPopulation();
        GlobalAssert.that(scenario != null && network != null && population != null);

        VirtualNetwork<Link> virtualNetwork = VirtualNetworkGet.readDefault(scenario.getNetwork());
        TravelData travelData = TravelDataGet.readDefault(virtualNetwork);

        {// 4) calculate minimum and performance fleet size and save results
            // MinimumFleetSizeCalculator minimumFleetSizeCalculator = new MinimumFleetSizeCalculator(network, population, virtualNetwork, travelData);
            // MinimumFleetSizeIO.toByte(new File(vnDir, MINIMUMFLEETSIZEFILENAME), minimumFleetSizeCalculator);

            int maxNumberVehiclesPerformanceCalculator = (int) (population.getPersons().size() * 0.3);
            new PerformanceFleetSizeCalculator(virtualNetwork, travelData, maxNumberVehiclesPerformanceCalculator);

        }
    }

}
