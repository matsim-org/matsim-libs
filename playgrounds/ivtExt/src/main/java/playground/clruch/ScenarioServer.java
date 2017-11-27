package playground.clruch;

import java.io.File;
import java.net.MalformedURLException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.analysis.AnalyzeAll;
import playground.clruch.analysis.AnalyzeSummary;
import playground.clruch.analysis.minimumfleetsize.MinimumFleetSizeCalculator;
import playground.clruch.analysis.minimumfleetsize.MinimumFleetSizeGet;
import playground.clruch.analysis.performancefleetsize.PerformanceFleetSizeCalculator;
import playground.clruch.analysis.performancefleetsize.PerformanceFleetSizeGet;
import playground.clruch.html.DataCollector;
import playground.clruch.html.ReportGenerator;
import playground.clruch.net.DatabaseModule;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.SimulationServer;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.prep.acttype.IncludeActTypeOf;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataGet;
import playground.clruch.traveltimetracker.AVTravelTimeModule;
import playground.lsieber.networkshapecutter.PrepSettings;
import playground.lsieber.networkshapecutter.PrepSettings.SettingsType;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.framework.AVQSimProvider;

/** only one ScenarioServer can run at one time, since a fixed network port is
 * reserved to serve the simulation status */
public class ScenarioServer {

    public static void main(String[] args) throws MalformedURLException, Exception {
        simulate();
    }

    /* package */ static void simulate() throws MalformedURLException, Exception {
        PrepSettings settings = new PrepSettings(SettingsType.Server);
        String outputdirectory = settings.config.controler().getOutputDirectory();
        System.out.println("outputdirectory = " + outputdirectory);

        // open server port for clients to connect to
        SimulationServer.INSTANCE.startAcceptingNonBlocking();
        SimulationServer.INSTANCE.setWaitForClients(settings.waitForClients);

        // TODO Bring this into preparer
        IncludeActTypeOf.BaselineCH(settings.config);

        // load scenario for simulation TODO Do Checks in helper functions...
        Scenario scenario = ScenarioUtils.loadScenario(settings.config);
        Network network = scenario.getNetwork();
        Population population = scenario.getPopulation();
        GlobalAssert.that(scenario != null && network != null && population != null);

        MatsimStaticDatabase.initializeSingletonInstance(network, settings.referenceFrame);
        Controler controler = new Controler(scenario);

        controler.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule(0.05));
        controler.addOverridingModule(new DynQSimModule<>(AVQSimProvider.class));
        controler.addOverridingModule(new AVModule());
        controler.addOverridingModule(new DatabaseModule());
        controler.addOverridingModule(new AVTravelTimeModule());

        // run simulation
        controler.run();

        // close port for visualization
        SimulationServer.INSTANCE.stopAccepting();

        // perform analysis of results
        AnalyzeAll analyzeAll = new AnalyzeAll();
        AnalyzeSummary analyzeSummary = analyzeAll.analyze(settings.preparedConfigFile, outputdirectory);
        VirtualNetwork<Link> virtualNetwork = VirtualNetworkGet.readDefault(scenario.getNetwork());

        MinimumFleetSizeCalculator minimumFleetSizeCalculator = null;
        PerformanceFleetSizeCalculator performanceFleetSizeCalculator = null;
        TravelData travelData = null;
        if (virtualNetwork != null) {
            minimumFleetSizeCalculator = MinimumFleetSizeGet.readDefault();
            performanceFleetSizeCalculator = PerformanceFleetSizeGet.readDefault();
            if (performanceFleetSizeCalculator != null) {
                String dataFolderName = outputdirectory + "/data";
                File relativeDirectory = new File(dataFolderName);
                performanceFleetSizeCalculator.saveAndPlot(dataFolderName, relativeDirectory);
            }

            travelData = TravelDataGet.readDefault(virtualNetwork);
        }

        new DataCollector(settings.configFileName, outputdirectory, controler, //
                minimumFleetSizeCalculator, analyzeSummary, network, population, travelData);

        // generate report
        ReportGenerator reportGenerator = new ReportGenerator();
        reportGenerator.from(settings.configFileName, outputdirectory);

        System.out.println("-----> END OF SCENARIO SERVER <-----");

    }
}
