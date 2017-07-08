package playground.joel.html;

import playground.clruch.net.StorageUtils;
import playground.joel.analysis.AnalyzeSummary;

import java.io.File;
import java.text.DecimalFormat;

/**
 * Created by Joel on 28.06.2017.
 */
public class ReportGenerator {

    final static String REPORT_NAME = "report";
    final static String TITLE = "AV Simulation Report";

    final static String IMAGE_FOLDER = "../data"; // relative to report folder

    final static DecimalFormat d = new DecimalFormat("#0.00");

    final static double link2km = 0.001;

    public static void main(String[] args) throws Exception {
        from(args);
    }

    public static void from(String[] args) throws Exception {
        File config = new File(args[0]);
        File file = new File(StorageUtils.OUTPUT, "report");
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
                "a {padding-left: 20px;}", //
                "#pre_left {float: left; width: 300px;}", "#pre_right {float: right; width: 300px;}", //
                "img {display: block; margin-left: auto; margin-right: auto;}" //

        );
        htmlUtils.body();
        // ----------------------------------------------
        htmlUtils.title(TITLE);

        htmlUtils.insertSubTitle("General/Aggregate Information");
        htmlUtils.insertTextLeft("User:" + //
                "\nTimestamp:");
        htmlUtils.insertTextLeft(scenarioParameters.user + //
                "\n" + scenarioParameters.date);
        htmlUtils.newLine();
        htmlUtils.insertTextLeft("Iterations:");
        htmlUtils.insertTextLeft(String.valueOf(scenarioParameters.iterations));
        htmlUtils.newLine();
        htmlUtils.insertLink("av.xml", "AV File");
        htmlUtils.insertLink("av_config.xml", "AV_Config File");
        htmlUtils.newLine();
        htmlUtils.insertTextLeft("Dispatcher:" + //
                "\nVehicles:" + //
                "\nRebalancing Period:" + //
                "\nRedispatching Period:");
        htmlUtils.insertTextLeft(scenarioParameters.dispatcher + //
                "\n" + analyzeSummary.numVehicles + //
                "\n" + scenarioParameters.rebalancingPeriod + " sec" + //
                "\n" + scenarioParameters.redispatchPeriod + " sec");
        htmlUtils.newLine();
        htmlUtils.insertTextLeft("Network:" + //
                "\nVirtual Nodes:" + //
                "\nPopulation:" + //
                "\nRequests:");
        htmlUtils.insertTextLeft(scenarioParameters.networkName + //
                "\n" + scenarioParameters.virtualNodes + //
                "\n" + scenarioParameters.populationSize + //
                "\n" + analyzeSummary.numRequests);
        htmlUtils.newLine();
        htmlUtils.insertTextLeft("Average Trip Distance:");
        htmlUtils.insertTextLeft(d.format( //
                analyzeSummary.distanceWithCust*link2km/analyzeSummary.numRequests) + " km");

        htmlUtils.insertSubTitle("Aggregate Results");
        htmlUtils.insertTextLeft("Computation Time:");
        htmlUtils.insertTextLeft(analyzeSummary.computationTime);
        htmlUtils.newLine();
        htmlUtils.insertTextLeft(htmlUtils.bold("Waiting Times") + //
                "\n\tMean:" + //
                "\n\t50% quantile:" + //
                "\n\t95% quantile:" + //
                "\n\tMaximum:" + //
                "\n" + //
                "\nOccupancy Ratio:" + //
                "\nDistance Ratio:" + //
                "\n" + //
                "\n" + htmlUtils.bold("Distances") + //
                "\n\tTotal:" + //
                "\n\tRebalancing:" + //
                "\n\tPickup:" + //
                "\n\tWith Customer:" //
        );
        htmlUtils.insertTextLeft(" " + //
                "\n" + d.format(analyzeSummary.totalWaitTimeMean.Get().number().doubleValue()/60) + " min" + //
                "\n" + d.format(analyzeSummary.totalWaitTimeQuantile.Get(1).number().doubleValue()/60) + " min" + //
                "\n" + d.format(analyzeSummary.totalWaitTimeQuantile.Get(2).number().doubleValue()/60)+ " min" + //
                "\n" + d.format(analyzeSummary.maximumWaitTime/60) + " min" + //
                "\n" + //
                "\n" + d.format(analyzeSummary.occupancyRatio*100) + "%" + //
                "\n" + d.format(analyzeSummary.distanceRatio*100)+ "%" + //
                "\n\n" + //
                "\n" + d.format(analyzeSummary.distance*link2km) + " km" + //
                "\n" + d.format(analyzeSummary.distanceRebalance*link2km) + " km" + //
                "\n" + d.format(analyzeSummary.distancePickup*link2km) + " km" + //
                "\n" + d.format(analyzeSummary.distanceWithCust*link2km) + " km" //
        );
        htmlUtils.insertImgRight(IMAGE_FOLDER + "/stackedDistance.png", 250, 400);

        htmlUtils.insertSubTitle("Wait Times");
        htmlUtils.insertTextLeft("Requests:");
        htmlUtils.insertTextLeft(String.valueOf(analyzeSummary.numRequests));
        htmlUtils.newLine();
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
        htmlUtils.newLine();
        htmlUtils.insertImg(IMAGE_FOLDER + "/binnedWaitingTimes.png", 800, 600);
        htmlUtils.insertImg(IMAGE_FOLDER + "/waitBinCounter.png", 800, 600);

        htmlUtils.insertSubTitle("Fleet Performance");
        htmlUtils.insertTextLeft( //
                "Occupancy Ratio:" + //
                "\nDistance Ratio:" //
        );
        htmlUtils.insertTextLeft( //
                d.format(analyzeSummary.occupancyRatio*100) + "%" + //
                "\n" + d.format(analyzeSummary.distanceRatio*100)+ "%" //
        );
        htmlUtils.newLine();
        htmlUtils.insertImg(IMAGE_FOLDER + "/binnedTimeRatios.png", 800, 600);
        htmlUtils.insertImg(IMAGE_FOLDER + "/binnedDistanceRatios.png", 800, 600);
        htmlUtils.insertImg(IMAGE_FOLDER + "/distanceDistribution.png", 800, 600);
        htmlUtils.insertImg(IMAGE_FOLDER + "/totalDistanceVehicle.png", 800, 600);
        htmlUtils.insertImg(IMAGE_FOLDER + "/dwcVehicle.png", 800, 600);

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
