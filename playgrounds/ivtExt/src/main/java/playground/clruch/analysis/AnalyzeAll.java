package playground.clruch.analysis;

import static playground.clruch.utils.NetworkLoader.loadNetwork;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import ch.ethz.idsc.queuey.datalys.SaveUtils;
import ch.ethz.idsc.queuey.plot.DiagramCreator;
import ch.ethz.idsc.queuey.plot.HistogramPlot;
import ch.ethz.idsc.queuey.plot.UniqueDiagrams;
import ch.ethz.idsc.tensor.RationalScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.alg.Join;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.io.CsvFormat;
import playground.clruch.data.ReferenceFrame;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.StorageSupplier;
import playground.clruch.net.StorageUtils;
import playground.clruch.options.ScenarioOptions;
import playground.joel.data.TotalData;

/** Created by Joel on 05.04.2017.
 * updated by clruch, aug sept 2017. */
public class AnalyzeAll {
    private final boolean filter = true; // filter size can be adapted in the diagram creator
    private final double maxWaitingTime = -1.0; // maximally displayed waiting time in minutes,
                                                // -1.0 sets it automatically

    private Scalar waitBinSize = RealScalar.of(5.0); // minimally, in minutes
    private Scalar totalDistanceBinSize = RealScalar.of(10.0); // minimally, in km
    private Scalar distanceWCBinSize = RealScalar.of(10.0); // minimally, in km
    // will be stepwise increased if too small

    private double timeRatio;
    private double distance;
    private double distanceWithCust;
    private double distancePickup;
    private double distanceRebalance;
    private Tensor totalWaitTimeQuantile;
    private Tensor totalWaitTimeMean;
    private double distanceRatio;

    public AnalyzeAll() {

    }

    /* package */ void plot(String csv, String name, String title, int from, int to, Double maxRange, File relativeDirectory) //
            throws Exception {
        
        Tensor table = CsvFormat.parse(Files.lines(Paths.get(relativeDirectory.getPath() + "/" + csv + ".csv")));
        table = Transpose.of(table);
        try {
            DiagramCreator.createDiagram(relativeDirectory, name, title, table.get(0), table.extract(from, to), //
                    maxRange, filter);
        } catch (Exception e) {
            System.out.println("Error creating the diagrams");
        }
    }

    /* package */ void plot(String csv, String name, String title, int from, int to, File relativeDirectory) throws Exception {
        plot(csv, name, title, from, to, 1.05, relativeDirectory);
    }

    /* package */ void collectAndPlot(CoreAnalysis coreAnalysis, DistanceAnalysis distanceAnalysis, File relativeDirectory) //
            throws Exception {
        Tensor summary = Join.of(1, coreAnalysis.getSummary(), distanceAnalysis.summary);
        SaveUtils.saveFile(summary, "summary", relativeDirectory);
        File summaryDirectory = new File(relativeDirectory,"summary");
        
        System.out.println("Size of data summary: " + Dimensions.of(summary));

        getTotals(summary, coreAnalysis, relativeDirectory);

        plot("summary/summary", "binnedWaitingTimes", "Waiting Times", 3, 6, maxWaitingTime, relativeDirectory);
        // maximum waiting time in the plot to have this uniform for all simulations
        plot("summary/summary", "binnedTimeRatios", "Occupancy Ratio", 10, 11, relativeDirectory);
        plot("summary/summary", "binnedDistanceRatios", "Distance Ratio", 15, 16, relativeDirectory);
        HistogramPlot.of(coreAnalysis.waitBinCounter, relativeDirectory, "Requests per Waiting Time", //
                waitBinSize.number().doubleValue(), "% of requests", "Waiting Times [s]", //
                1000, 750);

        HistogramPlot.of(distanceAnalysis.tdBinCounter.multiply(RationalScalar.of(100, distanceAnalysis.numVehicles)), relativeDirectory,
                "Vehicles per Total Distance", //
                totalDistanceBinSize.number().doubleValue(), "% of fleet", "Total Distances [km]", //
                1000, 750);

        HistogramPlot.of(distanceAnalysis.dwcBinCounter.multiply(RationalScalar.of(100, distanceAnalysis.numVehicles)), relativeDirectory,
                "Vehicles per Distance with Customer", //
                distanceWCBinSize.number().doubleValue(), "% of fleet", "Distances with Customer [km]", //
                1000, 750);

        UniqueDiagrams.distanceStack(relativeDirectory, "stackedDistance", "Distance Partition", //
                distanceRebalance / distance, distancePickup / distance, distanceWithCust / distance);
        UniqueDiagrams.distanceDistribution(relativeDirectory, "distanceDistribution", //
                "Distance Distribution", true, summaryDirectory.getPath());
        UniqueDiagrams.statusDistribution(relativeDirectory, "statusDistribution", //
                "Status Distribution", true, summaryDirectory.getPath());
    }

    /* package */ void getTotals(Tensor summary, CoreAnalysis coreAnalysis, File relativeDirectory) {
        int size = summary.length();
        timeRatio = 0;
        distance = 0;
        distanceWithCust = 0;
        distancePickup = 0;
        distanceRebalance = 0;
        totalWaitTimeMean = coreAnalysis.totalWaitTimeMean;
        totalWaitTimeQuantile = coreAnalysis.totalWaitTimeQuantile;
        for (int j = 0; j < size; j++) {
            timeRatio += summary.Get(j, 10).number().doubleValue();
            distance += summary.Get(j, 11).number().doubleValue();
            distanceWithCust += summary.Get(j, 12).number().doubleValue();
            distancePickup += summary.Get(j, 13).number().doubleValue();
            distanceRebalance += summary.Get(j, 14).number().doubleValue();
        }
        timeRatio = timeRatio / size;
        distanceRatio = distanceWithCust / distance;

        System.out.println("===================================");
        System.out.println("totalWaitTimeMean: " + totalWaitTimeMean);
        TotalData totalData = new TotalData();
        totalData.generate(String.valueOf(timeRatio), String.valueOf(distanceRatio), //
                String.valueOf(totalWaitTimeMean.Get().number().doubleValue()), //
                String.valueOf(totalWaitTimeQuantile.Get(1).number().doubleValue()), //
                String.valueOf(totalWaitTimeQuantile.Get(2).number().doubleValue()), //
                new File(relativeDirectory, "totalData.xml"));
    }

    private AnalyzeSummary summarize(CoreAnalysis coreAnalysis, DistanceAnalysis distanceAnalysis) {
        AnalyzeSummary analyzeSummary = new AnalyzeSummary();
        analyzeSummary.numVehicles = distanceAnalysis.numVehicles;
        analyzeSummary.numRequests = coreAnalysis.numRequests;
        analyzeSummary.occupancyRatio = timeRatio;
        analyzeSummary.distance = distance;
        analyzeSummary.distanceWithCust = distanceWithCust;
        analyzeSummary.distancePickup = distancePickup;
        analyzeSummary.distanceRebalance = distanceRebalance;
        analyzeSummary.distanceRatio = distanceRatio;
        analyzeSummary.totalDistancesPerVehicle = distanceAnalysis.totalDistancesPerVehicle;
        analyzeSummary.distancesWCPerVehicle = distanceAnalysis.distancesWCPerVehicle;
        analyzeSummary.totalWaitTimeMean = coreAnalysis.totalWaitTimeMean;
        analyzeSummary.totalWaitTimeQuantile = coreAnalysis.totalWaitTimeQuantile;
        analyzeSummary.maximumWaitTime = coreAnalysis.maximumWaitTime;
        return analyzeSummary;
    }

    public AnalyzeSummary analyze(File config, String outputdirectory) throws Exception {

        String dataFolderName = outputdirectory;
        File relativeDirectory = new File(dataFolderName, "data");
        if (!relativeDirectory.exists()) {
            relativeDirectory.mkdir();
        }
        File data = new File(config.getParent(), dataFolderName);
        System.out.println("searching data in directory: " + dataFolderName);
        data.mkdir();
        StorageUtils storageUtils = new StorageUtils(data);
        storageUtils.printStorageProperties();

        // load system network
        Network network = loadNetwork(config);

        // load coordinate system
        // TODO later remove hard-coded
        MatsimStaticDatabase.initializeSingletonInstance(network, ReferenceFrame.SIOUXFALLS);

        // load simulation data
        StorageSupplier storageSupplier = new StorageSupplier(storageUtils.getFirstAvailableIteration());
        final int size = storageSupplier.size();
        System.out.println("Found files: " + size);

        // analyze and print files
        CoreAnalysis coreAnalysis = new CoreAnalysis(storageSupplier, this);
        DistanceAnalysis distanceAnalysis = new DistanceAnalysis(storageSupplier);
        try {
            coreAnalysis.analyze();
            distanceAnalysis.analzye();
        } catch (Exception e) {
            e.printStackTrace();
        }

        collectAndPlot(coreAnalysis, distanceAnalysis, relativeDirectory);

        return summarize(coreAnalysis, distanceAnalysis);

    }

    public void setwaitBinSize(Scalar waitBinSize) {
        // TODO test input
        this.waitBinSize = waitBinSize;
    }

    public Scalar getwaitbinSize() {
        return waitBinSize;
    }

    public void settotalDistanceBinSize(Scalar totalDistanceBinSize) {
        // TODO test input
        this.totalDistanceBinSize = totalDistanceBinSize;
    }

    public Scalar gettotalDistanceBinSize() {
        return totalDistanceBinSize;
    }

    public void setdistanceWCBinSize(Scalar distanceWCBinSize) {
        this.distanceWCBinSize = distanceWCBinSize;
    }

    public Scalar getdistanceWCBinSize() {
        return distanceWCBinSize;
    }

    /** to be executed in simulation directory to perform analysis
     * 
     * @throws Exception */
    public void main(String[] args) throws Exception {
        File workingDirectory = new File("").getCanonicalFile();
        ScenarioOptions scenOptions = ScenarioOptions.load(workingDirectory);
        File configFile = new File(workingDirectory, scenOptions.getString("simuConfig"));
        Config config = ConfigUtils.loadConfig(configFile.toString());
        String outputdirectory = config.controler().getOutputDirectory();
        // StorageUtils storageUtils = new StorageUtils(new File(outputdirectory));
        analyze(configFile, outputdirectory);
    }
}
