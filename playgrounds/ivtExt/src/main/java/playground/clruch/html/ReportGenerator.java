package playground.clruch.html;

import java.io.File;
import java.nio.file.Files;
import java.text.DecimalFormat;

import org.matsim.core.utils.misc.Time;

import ch.ethz.idsc.queuey.html.HTMLUtils;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.io.Export;
import ch.ethz.idsc.tensor.io.Import;
import ch.ethz.idsc.tensor.red.Mean;
import playground.clruch.analysis.AnalyzeSummary;

/** @author Claudio Ruch based on initial version by gjoel */
public class ReportGenerator {

    private final String REPORT_NAME = "report";
    private final String TITLE = "AV Simulation Report";

    private final String IMAGE_FOLDER = "../data"; // relative to report folder

    private final DecimalFormat d = new DecimalFormat("#0.00");

    private final double link2km = 0.001;

    private File reportFolder;

    public void main(String[] args) throws Exception {
        String outputdirectory = args[1];
        from(new File(args[0]), outputdirectory);
    }

    public void from(File configFile, String outputdirectory) throws Exception {

        // create folder
        String reportLocation = (outputdirectory + "/" + REPORT_NAME);
        reportFolder = new File(configFile.getParent(), reportLocation);
        reportFolder.mkdir();
        System.out.println("the report is located at " + reportFolder.getAbsolutePath());

        // extract necessary data
        ScenarioParameters scenarioParametersingleton = ScenarioParameters.INSTANCE;
        String scenarioParametersFilename = outputdirectory + "/data/scenarioParameters.obj";
        Export.object(new File(scenarioParametersFilename), scenarioParametersingleton);
        String analyzeSummaryFileName = outputdirectory + "/data/analyzeSummary.obj";
        System.out.println("loading analyze summary from " + analyzeSummaryFileName);
        AnalyzeSummary analyzeSummary = Import.object(new File(analyzeSummaryFileName));
        saveConfigs(configFile);
        
        HTMLUtils htmlUtils = new HTMLUtils();

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
        htmlUtils.insertTextLeft(scenarioParametersingleton.user + //
                "\n" + scenarioParametersingleton.date);
        htmlUtils.newLine();
        htmlUtils.insertTextLeft("Iterations:");
        htmlUtils.insertTextLeft(String.valueOf(scenarioParametersingleton.iterations));
        htmlUtils.newLine();
        htmlUtils.insertLink("av.xml", "AV File");
        htmlUtils.insertLink("av_config.xml", "AV_Config File");
        htmlUtils.newLine();
        htmlUtils.insertTextLeft("Dispatcher:" + //
                "\nVehicles:" + //
                "\nRebalancing Period:" + //
                "\nRedispatching Period:");
        htmlUtils.insertTextLeft(scenarioParametersingleton.dispatcher + //
                "\n" + analyzeSummary.numVehicles + //
                "\n" + Time.writeTime(scenarioParametersingleton.rebalancingPeriod) + //
                "\n" + Time.writeTime(scenarioParametersingleton.redispatchPeriod));
        htmlUtils.newLine();
        htmlUtils.insertTextLeft("Network:" + //
                "\nVirtual Nodes:" + //
                "\nPopulation:" + //
                "\nRequests:");
        htmlUtils.insertTextLeft(scenarioParametersingleton.networkName + //
                "\n" + scenarioParametersingleton.virtualNodes + //
                "\n" + scenarioParametersingleton.populationSize + //
                "\n" + analyzeSummary.numRequests);

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
                "\n\tWith Customer:" + //
                "\n" + //
                "\nAverage Trip Distance:" //
        );
        htmlUtils.insertTextLeft(" " + //
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
        htmlUtils.insertImgRight(IMAGE_FOLDER + "/stackedDistance.png", 250, 400);
        if (scenarioParametersingleton.EMDks != null) {
            htmlUtils.newLine();
            htmlUtils.insertTextLeft("Minimum Fleet Size:" + //
                    "\nAverage Earth Movers Distance:");
            htmlUtils.insertTextLeft((int) Math.ceil(scenarioParametersingleton.minimumFleet) + //
                    "\n" + d.format(Mean.of(scenarioParametersingleton.EMDks).Get().number().doubleValue() * link2km) + " km");
        }

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
                "\n" + Time.writeTime(analyzeSummary.totalWaitTimeMean.Get().number().doubleValue()) + //
                "\n" + Time.writeTime(analyzeSummary.totalWaitTimeQuantile.Get(1).number().doubleValue()) + //
                "\n" + Time.writeTime(analyzeSummary.totalWaitTimeQuantile.Get(2).number().doubleValue()) + //
                "\n" + Time.writeTime(analyzeSummary.maximumWaitTime));
        htmlUtils.newLine();
        htmlUtils.insertImg(IMAGE_FOLDER + "/binnedWaitingTimes.png", 800, 600);
        htmlUtils.insertImg(IMAGE_FOLDER + "/RequestsperWaitingTime.png", 800, 600);

        htmlUtils.insertSubTitle("Fleet Performance");
        htmlUtils.insertTextLeft( //
                "Occupancy Ratio:" + //
                        "\nDistance Ratio:" //
        );
        htmlUtils.insertTextLeft( //
                d.format(analyzeSummary.occupancyRatio * 100) + "%" + //
                        "\n" + d.format(analyzeSummary.distanceRatio * 100) + "%" //
        );
        htmlUtils.newLine();
        htmlUtils.insertImg(IMAGE_FOLDER + "/binnedTimeRatios.png", 800, 600);
        htmlUtils.insertImg(IMAGE_FOLDER + "/binnedDistanceRatios.png", 800, 600);
        if (scenarioParametersingleton.EMDks != null) {
            htmlUtils.insertTextLeft("Average Trip Distance:");
            htmlUtils.insertTextLeft(d.format( //
                    link2km * analyzeSummary.distanceWithCust / analyzeSummary.numRequests) + " km");
            htmlUtils.newLine();
        }
        htmlUtils.insertImg(IMAGE_FOLDER + "/TripsperDistance.png", 800, 600);
        htmlUtils.insertImg(IMAGE_FOLDER + "/distanceDistribution.png", 800, 600);
        htmlUtils.insertImg(IMAGE_FOLDER + "/VehiclesperTotalDistance.png", 800, 600);
        htmlUtils.insertImg(IMAGE_FOLDER + "/VehiclesperDistancewithCustomer.png", 800, 600);
        htmlUtils.insertImg(IMAGE_FOLDER + "/statusDistribution.png", 800, 600);
        if (scenarioParametersingleton.EMDks != null) {
            htmlUtils.newLine();
            htmlUtils.insertTextLeft("Minimum Fleet Size:" + //
                    "\nAverage Earth Movers Distance:");
            htmlUtils.insertTextLeft((int) Math.ceil(scenarioParametersingleton.minimumFleet) + //
                    "\n" + d.format(Mean.of(scenarioParametersingleton.EMDks).Get().number().doubleValue() * link2km) + " km");
            htmlUtils.newLine();
            htmlUtils.insertImg(IMAGE_FOLDER + "/minFleet.png", 800, 600);
            htmlUtils.insertImg(IMAGE_FOLDER + "/EMD.png", 800, 600);
        }

        htmlUtils.insertImgIfExists(IMAGE_FOLDER + "/availbilitiesByNumberVehicles.png", reportFolder.getAbsolutePath(), 800, 600);

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
            htmlUtils.saveFile(REPORT_NAME, reportFolder);
        } catch (Exception e) {
            System.err.println("Not able to save report. ");
            e.printStackTrace();
        }
    }

    private void saveConfigs(File configFile) throws Exception {
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
