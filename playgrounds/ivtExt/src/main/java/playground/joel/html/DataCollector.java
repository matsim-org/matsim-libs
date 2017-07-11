package playground.joel.html;

import ch.ethz.idsc.tensor.Tensor;
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
import org.matsim.core.utils.misc.Time;
import playground.clruch.ScenarioServer;
import playground.clruch.net.StorageUtils;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.utils.GlobalAssert;
import playground.joel.analysis.*;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.config.AVOperatorConfig;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;

import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.zip.DataFormatException;

/**
 * Created by Joel on 27.06.2017.
 */
public class DataCollector {

    public static File avConfigOld;
    static File folder;
    public static File avOld;
    static File avConfig;
    static File av;

    public static ScenarioParameters scenarioParameters;
    public static AnalyzeSummary analyzeSummary;

    public static void store(String[] args, Controler controler, MinimumFleetSizeCalculator minimumFleetSizeCalculator, //
                             AnalyzeSummary analyzeSummaryIn, ScenarioParameters scenarioParametersIn) throws Exception {

        scenarioParameters = scenarioParametersIn; // new ScenarioParameters();
        analyzeSummary = analyzeSummaryIn;
        collectData(controler, minimumFleetSizeCalculator);
        readStopwatch(args);

        saveConfigs(args);

        minimumFleetSizeCalculator.plot(args);
        TripDistances.analyze();
    }

    public static File report(String[] args) {
        return new File(StorageUtils.OUTPUT, "report");
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

    public static void collectData(Controler controler, MinimumFleetSizeCalculator minimumFleetSizeCalculator) {

        Scenario scenario = controler.getScenario();
        scenarioParameters.populationSize = scenario.getPopulation().getPersons().values().size();
        Network network = scenario.getNetwork();
        scenarioParameters.networkName = network.getName();
        VirtualNetwork virtualNetwork = VirtualNetworkGet.readDefault(network);
        if (virtualNetwork != null) {
            scenarioParameters.virtualNodes = virtualNetwork.getvNodesCount();
            scenarioParameters.minFleet = minimumFleetSizeCalculator.calculateMinFleet();
            scenarioParameters.EMDks = minimumFleetSizeCalculator.EMDks;
            scenarioParameters.minimumFleet = minimumFleetSizeCalculator.minimumFleet;
        }

    }

    public static void readStopwatch(String[] args) {
        File stopwatch = new File(StorageUtils.OUTPUT, "stopwatch.txt");
        try {
            BufferedReader reader = new BufferedReader(new FileReader(stopwatch));
            String startTime = "00:00:00";
            int startTimePos = 0;
            String endTime = "00:00:00";
            int endTimePos = 0;
            String lineString = reader.readLine();
            int lineInt = 0;
            while (lineString != null) {
                String[] sections = lineString.split("\t");
                if (lineInt == 0) {
                    startTimePos = Arrays.asList(sections).indexOf("BEGIN iteration");
                    endTimePos = Arrays.asList(sections).indexOf("END iteration");
                    GlobalAssert.that(startTimePos != endTimePos);
                } else {
                    if (lineInt == 1) startTime = sections[startTimePos];
                    endTime = sections[endTimePos];
                }
                lineString = reader.readLine();
                lineInt++;
            }
            scenarioParameters.iterations = lineInt - 1;
            analyzeSummary.computationTime = Time.writeTime(Time.parseTime(endTime) - Time.parseTime(startTime));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
