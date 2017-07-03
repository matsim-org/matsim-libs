package playground.joel.html;

import ch.ethz.idsc.tensor.io.Export;
import ch.ethz.idsc.tensor.io.Import;
import ch.ethz.idsc.tensor.io.Serialization;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.VehiclesConfigGroup;
import org.matsim.core.controler.Controler;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.utils.GlobalAssert;
import playground.joel.analysis.AnalyzeSummary;
import playground.joel.analysis.CoreAnalysis;
import playground.joel.analysis.DistanceAnalysis;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.config.AVOperatorConfig;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.DataFormatException;

/**
 * Created by Joel on 27.06.2017.
 */
public class DataCollector {
    /*
    public static int numRequests;
    public static int numVehicles;
    public static int populationSize;
    public static int iterations;
    public static int redispatchPeriod;
    public static int rebalancingPeriod;
    public static int virtualNodes;

    public static String dispatcher;
    public static String networkName;
    public static String user;
    public static String date;
    */


    static File avConfigOld;
    static File folder;
    static File avOld;
    static File avConfig;
    static File av;

    public static ScenarioParameters scenarioParameters;
    public static AnalyzeSummary analyzeSummary;

    public static void store(String[] args, Controler controler, AnalyzeSummary analyzeSummaryIn) throws Exception {

        scenarioParameters = new ScenarioParameters();
        analyzeSummary = analyzeSummaryIn;
        collectData(controler);

        saveConfigs(args);

    }

    public static File report(String[] args) {
        avConfigOld = new File(args[0]);
        folder = avConfigOld.getParentFile();
        avOld = new File(folder, "av.xml");

        return new File(folder, "output/report");
    }

    public static void saveConfigs(String[] args) throws Exception {
        File report = report(args);
        report.mkdir();
        Export.object(new File(report, "scenarioParameters.obj"), scenarioParameters);
        Export.object(new File(report, "analyzeSummary.obj"), analyzeSummary);
        avConfig = new File(report, "av_config.xml");
        av = new File(report, "av.xml");

        try {
            Files.deleteIfExists(avConfig.toPath());
            Files.copy(avConfigOld.toPath(), avConfig.toPath());
            Files.deleteIfExists(av.toPath());
            Files.copy(avOld.toPath(), av.toPath());
        } catch (Exception e) {
            System.out.println("ERROR: unable to create backups!");
        }
        GlobalAssert.that(av.exists() && avConfig.exists());
    }

    public static ScenarioParameters loadScenarioData(String[] args) throws Exception {
        return Import.object(new File(report(args), "scenarioParameters.obj"));
    }

    public static AnalyzeSummary loadAnalysisData(String[] args) throws Exception {
        return Import.object(new File(report(args), "analyzeSummary.obj"));
    }

    public static void collectData(Controler controler) {

        Scenario scenario = controler.getScenario();
        Network network = scenario.getNetwork();
        scenarioParameters.networkName = network.getName();
        VirtualNetwork virtualNetwork = VirtualNetworkGet.readDefault(network);
        if (virtualNetwork != null) {
            scenarioParameters.virtualNodes = virtualNetwork.getvNodesCount();
        }

        Config config = controler.getConfig();
        /*
        AVConfigGroup avConfigGroup = (AVConfigGroup) config.getModules().get("av");
        AVOperatorConfig operatorConfig = ;
        GlobalAssert.that(operatorConfig != null);
        AVDispatcherConfig dispatcherConfig = operatorConfig.getDispatcherConfig();
        AVGeneratorConfig generatorConfig = operatorConfig.getGeneratorConfig();

        scenarioParameters.numVehicles = (int) generatorConfig.getNumberOfVehicles();
        scenarioParameters.populationSize = scenario.getPopulation().getPersons().keySet().size();
        scenarioParameters.iterations = controler.getIterationNumber();
        scenarioParameters.redispatchPeriod = Integer.parseInt(dispatcherConfig.getParams().get("redispatchPeriod"));
        scenarioParameters.rebalancingPeriod = Integer.parseInt(dispatcherConfig.getParams().get("rebalancingPeriod"));

        scenarioParameters.dispatcher = generatorConfig.getStrategyName();
        */


    }

}
