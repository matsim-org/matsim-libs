package playground.clruch.html;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.misc.Time;

import ch.ethz.idsc.tensor.io.Export;
import ch.ethz.idsc.tensor.io.Import;
import playground.clruch.analysis.AnalyzeSummary;
//import playground.clruch.analysis.TripDistances;
import playground.clruch.analysis.TripDistances;
import playground.clruch.analysis.minimumfleetsize.MinimumFleetSizeCalculator;
import playground.clruch.analysis.performancefleetsize.PerformanceFleetSizeCalculator;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.traveldata.TravelData;
import playground.clruch.utils.GlobalAssert;
import playground.joel.helpers.EasyDijkstra;

/**
 * Created by Joel on 27.06.2017.
 */
public class DataCollector {

    static File avConfigOld;
    static File folder;
    static File avOld;
    static File avConfig;
    static File av;

    public static ScenarioParameters scenarioParameters;
    public static AnalyzeSummary analyzeSummary;

    public static void store(File configFile, Controler controler, MinimumFleetSizeCalculator minimumFleetSizeCalculator, //
                             PerformanceFleetSizeCalculator performanceFleetSizeCalculator, //
                             AnalyzeSummary analyzeSummaryIn, ScenarioParameters scenarioParametersIn,//
                             Network network, Population population, TravelData travelData) throws Exception {

        scenarioParameters = scenarioParametersIn; // new ScenarioParameters();
        analyzeSummary = analyzeSummaryIn;
        collectData(controler, minimumFleetSizeCalculator, performanceFleetSizeCalculator);
        readStopwatch(configFile);

        saveConfigs(configFile);

        minimumFleetSizeCalculator.plot();
        performanceFleetSizeCalculator.saveAndPlot();
        
        LeastCostPathCalculator dijkstra = EasyDijkstra.prepDijkstra(network);
        TripDistances tdn = new TripDistances(dijkstra, travelData, population, network);
       
    }

    public static File report(File configFile) {
        folder = configFile.getParentFile();
        avOld = new File(folder, "av.xml");

        return new File(folder, "output/report");
    }

    public static void saveConfigs(File configFile) throws Exception {
        File report = report(configFile);
        report.mkdir();
        Export.object(new File(report, "scenarioParameters.obj"), scenarioParameters);
        Export.object(new File(report, "analyzeSummary.obj"), analyzeSummary);
        avConfig = new File(configFile.getParentFile(), "av_config.xml");
        av = new File(configFile.getParentFile(), "av.xml");

        try {
            Files.deleteIfExists(avConfig.toPath());
            Files.copy(avConfigOld.toPath(), avConfig.toPath());
            Files.deleteIfExists(av.toPath());
            Files.copy(avOld.toPath(), av.toPath());
        } catch (Exception e) {
            System.out.println("ERROR: unable to create backups!");
        }
        System.out.println(av.getAbsolutePath());
        System.out.println(avConfig.getAbsolutePath());
        GlobalAssert.that(av.exists() && avConfig.exists());
    }

    public static ScenarioParameters loadScenarioData(File configFile) throws Exception {
        return Import.object(new File(report(configFile), "scenarioParameters.obj"));
    }

    public static AnalyzeSummary loadAnalysisData(File configFile) throws Exception {
        return Import.object(new File(report(configFile), "analyzeSummary.obj"));
    }

    public static void collectData(Controler controler, MinimumFleetSizeCalculator minimumFleetSizeCalculator, //
                                   PerformanceFleetSizeCalculator performanceFleetSizeCalculator) throws InterruptedException {

        Scenario scenario = controler.getScenario();
        scenarioParameters.populationSize = scenario.getPopulation().getPersons().values().size();
        Network network = scenario.getNetwork();
        scenarioParameters.networkName = network.getName();
        VirtualNetwork virtualNetwork = VirtualNetworkGet.readDefault(network);
        if (virtualNetwork != null) {
            scenarioParameters.virtualNodes = virtualNetwork.getvNodesCount();
            // TODO load from file instead of calculating
            scenarioParameters.minFleet = minimumFleetSizeCalculator.getMinFleet();
            scenarioParameters.EMDks = minimumFleetSizeCalculator.getEMDk();
            scenarioParameters.minimumFleet = minimumFleetSizeCalculator.minimumFleet;
            scenarioParameters.availabilities =  performanceFleetSizeCalculator.getAvailabilities();
        }

    }

    public static void readStopwatch(File configFile) {
        File stopwatch = new File(configFile.getParent(), "output/stopwatch.txt");
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
