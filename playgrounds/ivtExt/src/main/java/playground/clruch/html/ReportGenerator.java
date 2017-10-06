package playground.clruch.html;

import java.io.File;
import java.nio.file.Files;
import java.text.DecimalFormat;

import org.matsim.core.utils.misc.Time;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.io.Export;
import ch.ethz.idsc.tensor.io.Import;
import ch.ethz.idsc.tensor.red.Mean;
import playground.clruch.analysis.AnalyzeSummary;

/** @author Claudio Ruch based on initial version by gjoel */
public class ReportGenerator {

    final static String REPORT_NAME = "report";
    final static String TITLE = "AV Simulation Report";

    final static String IMAGE_FOLDER = "../data"; // relative to report folder

    final static DecimalFormat d = new DecimalFormat("#0.00");

    final static double link2km = 0.001;

    private static File reportFolder;

    public static void main(String[] args) throws Exception {
        String outputdirectory = args[1];
        from(new File(args[0]),outputdirectory);
    }

    public static void from(File configFile, String outputdirectory) throws Exception {

        // create folder
        String reportLocation = (outputdirectory + "/" + REPORT_NAME);
        reportFolder = new File(configFile.getParent(), reportLocation);
        reportFolder.mkdir();
        System.out.println("the report is located at " + reportFolder.getAbsolutePath());

        // extract necessary data
        ScenarioParameters scenarioParametersingleton = ScenarioParameters.INSTANCE;
        String scenarioParametersFilename = outputdirectory +"/data/scenarioParameters.obj";
        Export.object(new File(scenarioParametersFilename), scenarioParametersingleton);
        String analyzeSummaryFileName = outputdirectory + "/data/analyzeSummary.obj";
        System.out.println("loading analyze summary from " +  analyzeSummaryFileName);
        AnalyzeSummary analyzeSummary = Import.object(new File(analyzeSummaryFileName));  
        saveConfigs(configFile);

        // write report
        // -------------------------------------------------------------------------------------------------------------
        HtmlUtils.html();
        HtmlUtils.insertCSS(//
                "h2 {float: left; width: 100%; padding: 10px}", //
                "pre {font-family: verdana; float: left; width: 100%; padding-left: 20px;}", //
                "p {float: left; width: 80%; padding-left: 20px;}", //
                "a {padding-left: 20px;}", //
                "#pre_left {float: left; width: 300px;}", "#pre_right {float: right; width: 300px;}", //
                "img {display: block; margin-left: auto; margin-right: auto;}" //

        );
        HtmlUtils.body();
        // ----------------------------------------------
        HtmlUtils.title(TITLE);

        HtmlUtils.insertSubTitle("General/Aggregate Information");
        HtmlUtils.insertTextLeft("User:" + //
                "\nTimestamp:");
        HtmlUtils.insertTextLeft(scenarioParametersingleton.user + //
                "\n" + scenarioParametersingleton.date);
        HtmlUtils.newLine();
        HtmlUtils.insertTextLeft("Iterations:");
        HtmlUtils.insertTextLeft(String.valueOf(scenarioParametersingleton.iterations));
        HtmlUtils.newLine();
        HtmlUtils.insertLink("av.xml", "AV File");
        HtmlUtils.insertLink("av_config.xml", "AV_Config File");
        HtmlUtils.newLine();
        HtmlUtils.insertTextLeft("Dispatcher:" + //
                "\nVehicles:" + //
                "\nRebalancing Period:" + //
                "\nRedispatching Period:");
        HtmlUtils.insertTextLeft(scenarioParametersingleton.dispatcher + //
                "\n" + analyzeSummary.numVehicles + //
                "\n" + Time.writeTime(scenarioParametersingleton.rebalancingPeriod) + //
                "\n" + Time.writeTime(scenarioParametersingleton.redispatchPeriod));
        HtmlUtils.newLine();
        HtmlUtils.insertTextLeft("Network:" + //
                "\nVirtual Nodes:" + //
                "\nPopulation:" + //
                "\nRequests:");
        HtmlUtils.insertTextLeft(scenarioParametersingleton.networkName + //
                "\n" + scenarioParametersingleton.virtualNodes + //
                "\n" + scenarioParametersingleton.populationSize + //
                "\n" + analyzeSummary.numRequests);

        HtmlUtils.insertSubTitle("Aggregate Results");
        HtmlUtils.insertTextLeft("Computation Time:");
        HtmlUtils.insertTextLeft(analyzeSummary.computationTime);
        HtmlUtils.newLine();
        HtmlUtils.insertTextLeft(HtmlUtils.bold("Waiting Times") + //
                "\n\tMean:" + //
                "\n\t50% quantile:" + //
                "\n\t95% quantile:" + //
                "\n\tMaximum:" + //
                "\n" + //
                "\nOccupancy Ratio:" + //
                "\nDistance Ratio:" + //
                "\n" + //
                "\n" + HtmlUtils.bold("Distances") + //
                "\n\tTotal:" + //
                "\n\tRebalancing:" + //
                "\n\tPickup:" + //
                "\n\tWith Customer:" + //
                "\n" + //
                "\nAverage Trip Distance:" //
        );
        HtmlUtils.insertTextLeft(" " + //
                "\n" + Time.writeTime(analyzeSummary.totalWaitTimeMean.Get().number().doubleValue()) + //
                "\n" + Time.writeTime(analyzeSummary.totalWaitTimeQuantile.Get(1).number().doubleValue()) + //
                "\n" + Time.writeTime(analyzeSummary.totalWaitTimeQuantile.Get(2).number().doubleValue()) + //
                "\n" + Time.writeTime(analyzeSummary.maximumWaitTime) + //
                "\n" + //
                "\n" + d.format(analyzeSummary.occupancyRatio * 100) + "%" + //
                "\n" + d.format(analyzeSummary.distanceRatio * 100) + "%" + //
                "\n\n" + //
                "\n" + d.format(analyzeSummary.distance * link2km) + " km" + //
                "\n" + d.format(analyzeSummary.distanceRebalance * link2km) + " km (" + //
                d.format(100 * analyzeSummary.distanceRebalance / analyzeSummary.distance) + "%)" + //
                "\n" + d.format(analyzeSummary.distancePickup * link2km) + " km (" + //
                d.format(100 * analyzeSummary.distancePickup / analyzeSummary.distance) + "%)" + //
                "\n" + d.format(analyzeSummary.distanceWithCust * link2km) + " km (" + //
                d.format(100 * analyzeSummary.distanceWithCust / analyzeSummary.distance) + "%)" + //
                "\n" + //
                "\n" + d.format(link2km * analyzeSummary.distanceWithCust / analyzeSummary.numRequests) + " km");
        HtmlUtils.insertImgRight(IMAGE_FOLDER + "/stackedDistance.png", 250, 400);
        if (scenarioParametersingleton.EMDks != null) {
            HtmlUtils.newLine();
            HtmlUtils.insertTextLeft("Minimum Fleet Size:" + //
                    "\nAverage Earth Movers Distance:");
            HtmlUtils.insertTextLeft((int) Math.ceil(scenarioParametersingleton.minimumFleet) + //
                    "\n" + d.format(Mean.of(scenarioParametersingleton.EMDks).Get().number().doubleValue() * link2km) + " km");
        }

        HtmlUtils.insertSubTitle("Wait Times");
        HtmlUtils.insertTextLeft("Requests:");
        HtmlUtils.insertTextLeft(String.valueOf(analyzeSummary.numRequests));
        HtmlUtils.newLine();
        HtmlUtils.insertTextLeft(HtmlUtils.bold("Waiting Times") + //
                "\n\tMean:" + //
                "\n\t50% quantile:" + //
                "\n\t95% quantile:" + //
                "\n\tMaximum:");
        HtmlUtils.insertTextLeft(" " + //
                "\n" + Time.writeTime(analyzeSummary.totalWaitTimeMean.Get().number().doubleValue()) + //
                "\n" + Time.writeTime(analyzeSummary.totalWaitTimeQuantile.Get(1).number().doubleValue()) + //
                "\n" + Time.writeTime(analyzeSummary.totalWaitTimeQuantile.Get(2).number().doubleValue()) + //
                "\n" + Time.writeTime(analyzeSummary.maximumWaitTime));
        HtmlUtils.newLine();
        HtmlUtils.insertImg(IMAGE_FOLDER + "/binnedWaitingTimes.png", 800, 600);
        HtmlUtils.insertImg(IMAGE_FOLDER + "/waitBinCounter.png", 800, 600);

        HtmlUtils.insertSubTitle("Fleet Performance");
        HtmlUtils.insertTextLeft( //
                "Occupancy Ratio:" + //
                        "\nDistance Ratio:" //
        );
        HtmlUtils.insertTextLeft( //
                d.format(analyzeSummary.occupancyRatio * 100) + "%" + //
                        "\n" + d.format(analyzeSummary.distanceRatio * 100) + "%" //
        );
        HtmlUtils.newLine();
        HtmlUtils.insertImg(IMAGE_FOLDER + "/binnedTimeRatios.png", 800, 600);
        HtmlUtils.insertImg(IMAGE_FOLDER + "/binnedDistanceRatios.png", 800, 600);
        if (scenarioParametersingleton.EMDks != null) {
            HtmlUtils.insertTextLeft("Average Trip Distance:");
            HtmlUtils.insertTextLeft(d.format( //
                    link2km * analyzeSummary.distanceWithCust / analyzeSummary.numRequests) + " km");
            HtmlUtils.newLine();
        }
        HtmlUtils.insertImg(IMAGE_FOLDER + "/tripDistances.png", 800, 600);
        HtmlUtils.insertImg(IMAGE_FOLDER + "/distanceDistribution.png", 800, 600);
        HtmlUtils.insertImg(IMAGE_FOLDER + "/totalDistanceVehicle.png", 800, 600);
        HtmlUtils.insertImg(IMAGE_FOLDER + "/dwcVehicle.png", 800, 600);
        HtmlUtils.insertImg(IMAGE_FOLDER + "/statusDistribution.png", 800, 600);
        if (scenarioParametersingleton.EMDks != null) {
            HtmlUtils.newLine();
            HtmlUtils.insertTextLeft("Minimum Fleet Size:" + //
                    "\nAverage Earth Movers Distance:");
            HtmlUtils.insertTextLeft((int) Math.ceil(scenarioParametersingleton.minimumFleet) + //
                    "\n" + d.format(Mean.of(scenarioParametersingleton.EMDks).Get().number().doubleValue() * link2km) + " km");
            HtmlUtils.newLine();
            HtmlUtils.insertImg(IMAGE_FOLDER + "/minFleet.png", 800, 600);
            HtmlUtils.insertImg(IMAGE_FOLDER + "/EMD.png", 800, 600);
        }

        HtmlUtils.insertImgIfExists(IMAGE_FOLDER + "/availbilitiesByNumberVehicles.png", reportFolder.getAbsolutePath(), 800, 600);

        // ----------------------------------------------
        HtmlUtils.footer();
        HtmlUtils.insertLink("http://www.idsc.ethz.ch/", "www.idsc.ethz.ch");
        HtmlUtils.footer();
        // ----------------------------------------------
        HtmlUtils.body();
        HtmlUtils.html();

        // save document
        // -------------------------------------------------------------------------------------------------------------
        try {
            HtmlUtils.saveFile(REPORT_NAME, outputdirectory);
        } catch (Exception e) {
            System.err.println("Not able to save report. ");
            e.printStackTrace();
        }
    }

    private static void saveConfigs(File configFile) throws Exception {
        // copy configFile
        GlobalAssert.that(configFile.exists());
        Files.copy(configFile.toPath(), new File(reportFolder, "av_config.xml").toPath());
        GlobalAssert.that(configFile.exists());

        // copy av.xml file
        File avFile = new File(configFile.getParentFile(), "av.xml");
        GlobalAssert.that(avFile.exists());
        Files.copy(avFile.toPath(), new File(reportFolder, "av.xml").toPath());
        GlobalAssert.that(avFile.exists());
    }
}
