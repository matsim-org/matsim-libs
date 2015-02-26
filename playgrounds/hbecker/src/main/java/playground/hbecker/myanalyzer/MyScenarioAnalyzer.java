package playground.hbecker.myanalyzer;

import org.matsim.api.core.v01.network.Network;
import playground.boescpa.lib.tools.networkModification.NetworkUtils;
import playground.boescpa.lib.tools.scenarioAnalyzer.ScenarioAnalyzer;
import playground.boescpa.lib.tools.scenarioAnalyzer.eventHandlers.AgentCounter;
import playground.boescpa.lib.tools.scenarioAnalyzer.eventHandlers.ScenarioAnalyzerEventHandler;
import playground.boescpa.lib.tools.scenarioAnalyzer.eventHandlers.TripActivityCrosscorrelator;
import playground.boescpa.lib.tools.scenarioAnalyzer.eventHandlers.TripAnalyzer;

/**
 * Created by beckerh on 26.02.2015.
 */
public class MyScenarioAnalyzer {

    public static void main(String[] args) {
        Network network = NetworkUtils.readNetwork(args[0]);
        String path2EventFile = args[1];
        int scaleFactor = 10;

        try {
            // Analyze the events:
            ScenarioAnalyzerEventHandler[] handlers = {
                    new AgentCounter(network),
                    new TripAnalyzer(network),
                    new TripActivityCrosscorrelator(network)
            };
            ScenarioAnalyzer scenarioAnalyzer = new ScenarioAnalyzer(path2EventFile, scaleFactor, handlers);
            scenarioAnalyzer.analyzeScenario();

            // Return the results:
            scenarioAnalyzer.createResults(path2EventFile + "_analysisResults.csv", null);

        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
