package playground.joel.html;

import playground.joel.analysis.AnalyzeSummary;

import java.io.File;
import java.text.DecimalFormat;

/**
 * Created by Joel on 28.06.2017.
 */
public class ReportGenerator {

    final static String REPORT_NAME = "report";

    final static String TITLE = "AV Simulation Report";

    final static DecimalFormat d = new DecimalFormat("#0.00");

    public static void main(String[] args) throws Exception {
        from(args);
    }

    public static void from(String[] args) throws Exception {
        File config = new File(args[0]);
        File file = new File(config.getParent(), "output/report");
        file.mkdir();

        ScenarioParameters scenarioParameters = DataCollector.loadScenarioData(args);
        AnalyzeSummary analyzeSummary = DataCollector.loadAnalysisData(args);

        // write report
        // -------------------------------------------------------------------------------------------------------------
        htmlUtils.html();
        htmlUtils.insertCSS(//
                "h2 {float: left; width: 100%; padding: 10px}", //
                "pre {font-family: verdana; float: left; width: 100%; padding-left: 20px;}", //
                "p {float: left; width: 80%; padding-left: 20px;}", //
                "#pre_left {float: left; width: 40%;}", "#pre_right {float: right; width: 40%;}", //
                "img {float: right;}"

        );
        htmlUtils.body();
        // ----------------------------------------------
        htmlUtils.title(TITLE);

        htmlUtils.insertSubTitle("General/Aggregate Information");
        htmlUtils.insertTextLeft("User:" + //
                "\nTimestamp:");
        htmlUtils.insertTextLeft(scenarioParameters.user + //
                "\n" + scenarioParameters.date);
        htmlUtils.insertTextLeft("Iterations:");
        htmlUtils.insertTextLeft(String.valueOf(scenarioParameters.iterations));
        htmlUtils.insertTextLeft("Network:" + //
                "\nVirtual Nodes:" + //
                "\nPopulation:" + //
                "\nRequests:");
        htmlUtils.insertTextLeft(scenarioParameters.networkName + //
                "\n" + scenarioParameters.virtualNodes + //
                "\n" + scenarioParameters.populationSize + //
                "\n" + analyzeSummary.numRequests);
        htmlUtils.insertTextLeft("Dispatcher:" + //
                "\nVehicles:" + //
                "\nRebalancing Period:" + //
                "\nRedispatching Period:");
        htmlUtils.insertTextLeft(scenarioParameters.dispatcher + //
                "\n" + analyzeSummary.numVehicles + //
                "\n" + scenarioParameters.rebalancingPeriod + //
                "\n" + scenarioParameters.redispatchPeriod);

        htmlUtils.insertSubTitle("Aggregate Results");
        htmlUtils.insertTextLeft(htmlUtils.bold("Waiting Times") + //
                "\n\tMean:" + //
                "\n\t50% quantile:" + //
                "\n\t95% quantile:" + //
                "\n\tMaximum:");
        htmlUtils.insertTextLeft(" " + //
                "\n" + d.format(analyzeSummary.totalWaitTimeMean.Get().number().doubleValue()/60) + " min" + //
                "\n" + d.format(analyzeSummary.totalWaitTimeQuantile.Get(1).number().doubleValue()/60) + " min" + //
                "\n" + d.format(analyzeSummary.totalWaitTimeQuantile.Get(2).number().doubleValue()/60)+ " min" + //
                "\n" + d.format(analyzeSummary.maximumWaitTime/60) + " min");
        htmlUtils.insertTextLeft("Occupancy Ratio:" + //
                "\nDistance Ratio:");
        htmlUtils.insertTextLeft(d.format(analyzeSummary.occupancyRatio*100) + "%" + //
                "\n" + d.format(analyzeSummary.distanceRatio*100)+ "%");
        htmlUtils.insertTextLeft(htmlUtils.bold("Distances") + //
                "\n\tRebalancing:" + //
                "\n\tPickup:" + //
                "\n\tWith Customer:");
        // TODO: insert correct distance transformation
        htmlUtils.insertTextLeft(" " + //
                "\n" + d.format(analyzeSummary.distanceRebalance/1000) + " km" + //
                "\n" + d.format(analyzeSummary.distancePickup/1000) + " km" + //
                "\n" + d.format(analyzeSummary.distanceWithCust/1000) + " km");

        htmlUtils.insertSubTitle("Wait Times");

        htmlUtils.insertSubTitle("Fleet Performance");

        // ----------------------------------------------
        htmlUtils.footer();
        htmlUtils.insertLink("http://www.idsc.ethz.ch/", "www.idsc.ethz.ch");
        htmlUtils.footer();
        // ----------------------------------------------
        htmlUtils.body();
        htmlUtils.html();

        // save document
        // -------------------------------------------------------------------------------------------------------------
        try {
            htmlUtils.saveFile(REPORT_NAME);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
