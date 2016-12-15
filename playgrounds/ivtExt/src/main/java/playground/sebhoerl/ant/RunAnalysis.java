package playground.sebhoerl.ant;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import playground.sebhoerl.av_paper.BinCalculator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RunAnalysis {
    final static String NETWORK_PATH = "/home/sebastian/ant/assets/network.xml";
    final static String BASECASE = "multi";

    final static double START_TIME = 0.0;
    final static double END_TIME = 30.0 * 3600.0;
    final static double INTERVAL = 300.0;

    final static String[] names = {
            "1000_nosub", "1000_sub", "100_nosub", "100_sub", "2000_nosub", "2000_sub", "250_nosub", "250_sub",
            "3000_nosub", "3000_sub", "4000_nosub", "4000_sub", "5000_nosub", "5000_sub", "500_nosub", "500_sub",
            "6000_nosub", "6000_sub", "750_nosub", "750_sub", "8000_nosub", "8000_sub"
            , "baseline_nosub", "baseline_sub"
    };

    //final static String[] names = { "1000_sub", "1000_nosub" };

    private static String getEventsPath(String name) {
        return String.format("/home/sebastian/ant/analysis/data/%s/events_%s.xml.gz", BASECASE, name);
    }

    private static String getOutputPath(String name) {
        return String.format("/home/sebastian/ant/analysis/data/%s/result_%s.json", BASECASE, name);
    }

    public static void main(String[] args) throws InterruptedException {
        BinCalculator binCalculator = BinCalculator.createByInterval(START_TIME, END_TIME, INTERVAL);

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(NETWORK_PATH);

        ExecutorService executor = Executors.newFixedThreadPool(8);

        for (String name : names) {
            AnalysisRunner runner = new AnalysisRunner(binCalculator, network, getEventsPath(name),  getOutputPath(name));
            executor.submit(runner);
            //runner.run();
        }

        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }
}
