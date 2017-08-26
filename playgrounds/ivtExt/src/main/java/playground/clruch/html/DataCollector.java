package playground.clruch.html;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.misc.Time;

import ch.ethz.idsc.tensor.io.Export;
import playground.clruch.analysis.AnalyzeSummary;
import playground.clruch.analysis.TripDistances;
import playground.clruch.analysis.minimumfleetsize.MinimumFleetSizeCalculator;
import playground.clruch.analysis.performancefleetsize.PerformanceFleetSizeCalculator;
import playground.clruch.traveldata.TravelData;
import playground.clruch.utils.GlobalAssert;
import playground.joel.helpers.EasyDijkstra;

/** @author Claudio Ruch based on initial version by gjoel */
public class DataCollector {

    public static AnalyzeSummary analyzeSummary;

    public static void store(File configFile, Controler controler, //
            MinimumFleetSizeCalculator minimumFleetSizeCalculator, //
            PerformanceFleetSizeCalculator performanceFleetSizeCalculator, //
            AnalyzeSummary analyzeSummaryIn, //
            Network network, Population population, TravelData travelData) throws Exception {

        analyzeSummary = analyzeSummaryIn;

        // collect the data
        readStopwatch(configFile);

        minimumFleetSizeCalculator.plot();
        performanceFleetSizeCalculator.saveAndPlot();

        LeastCostPathCalculator dijkstra = EasyDijkstra.prepDijkstra(network);
        new TripDistances(dijkstra, travelData, population, network);

    }



    private static void readStopwatch(File configFile) {
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
                    if (lineInt == 1)
                        startTime = sections[startTimePos];
                    endTime = sections[endTimePos];
                }
                lineString = reader.readLine();
                lineInt++;
            }
            analyzeSummary.computationTime = Time.writeTime(Time.parseTime(endTime) - Time.parseTime(startTime));
            Export.object(new File("output/data/analyzeSummary.obj"), analyzeSummary);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
