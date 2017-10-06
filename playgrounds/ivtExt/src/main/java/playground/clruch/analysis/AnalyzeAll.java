package playground.clruch.analysis;

import static playground.clruch.utils.NetworkLoader.loadNetwork;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.alg.Join;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.io.CsvFormat;
import ch.ethz.idsc.tensor.io.MathematicaFormat;
import ch.ethz.idsc.tensor.io.MatlabExport;
import playground.clruch.data.ReferenceFrame;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.StorageSupplier;
import playground.joel.data.TotalData;

/** Created by Joel on 05.04.2017.
 * updated by clruch, aug sept 2017. */
public class AnalyzeAll {
    public static final boolean filter = true; // filter size can be adapted in the diagram creator
    public static final double maxWaitingTime = -1.0; // maximally displayed waiting time in minutes,
                                                      // -1.0 sets it automatically

    public static Scalar waitBinSize = RealScalar.of(5.0); // minimally, in minutes
    public static Scalar totalDistanceBinSize = RealScalar.of(10.0); // minimally, in km
    public static Scalar distanceWCBinSize = RealScalar.of(10.0); // minimally, in km
    // will be stepwise increased if too small

    public static double timeRatio;
    public static double distance;
    public static double distanceWithCust;
    public static double distancePickup;
    public static double distanceRebalance;
    public static Tensor totalWaitTimeQuantile;
    public static Tensor totalWaitTimeMean;
    public static double distanceRatio;

    public static void main(String[] args) throws Exception {
        analyze(new File(args[0]), args[1]);
    }

    public static void saveFile(Tensor table, String name, String dataFolderName) throws Exception {
        Files.write(Paths.get(dataFolderName + "/" + name + ".csv"), (Iterable<String>) CsvFormat.of(table)::iterator);
        Files.write(Paths.get(dataFolderName + "/" + name + ".mathematica"), (Iterable<String>) MathematicaFormat.of(table)::iterator);
        Files.write(Paths.get(dataFolderName + "/" + name + ".m"), (Iterable<String>) MatlabExport.of(table)::iterator);
    }

    static void plot(String csv, String name, String title, int from, int to, Double maxRange, File relativeDirectory) //
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

    static void plot(String csv, String name, String title, int from, int to, File relativeDirectory) throws Exception {
        plot(csv, name, title, from, to, 1.05, relativeDirectory);
    }

    static void collectAndPlot(CoreAnalysis coreAnalysis, DistanceAnalysis distanceAnalysis, File relativeDirectory) //
            throws Exception {
        Tensor summary = Join.of(1, coreAnalysis.summary, distanceAnalysis.summary);
        saveFile(summary, "summary", relativeDirectory.getPath());
        System.out.println("Size of data summary: " + Dimensions.of(summary));

        getTotals(summary, coreAnalysis, relativeDirectory);

        AnalyzeAll.plot("summary", "binnedWaitingTimes", "Waiting Times", 3, 6, maxWaitingTime, relativeDirectory);
        // maximum waiting time in the plot to have this uniform for all simulations
        AnalyzeAll.plot("summary", "binnedTimeRatios", "Occupancy Ratio", 10, 11, relativeDirectory);
        AnalyzeAll.plot("summary", "binnedDistanceRatios", "Distance Ratio", 15, 16, relativeDirectory);
        DiagramCreator.binCountGraph(relativeDirectory, "waitBinCounter", //
                "Requests per Waiting Time", coreAnalysis.waitBinCounter, waitBinSize.number().doubleValue(), //
                100.0 / coreAnalysis.numRequests, "% of requests", //
                "Waiting Times", " sec", 1000, 750);
        DiagramCreator.binCountGraph(relativeDirectory, "totalDistanceVehicle", //
                "Vehicles per Total Distance", distanceAnalysis.tdBinCounter, //
                totalDistanceBinSize.number().doubleValue(), 100.0 / distanceAnalysis.numVehicles, //
                "% of fleet", "Total Distances", " km", //
                1000, 750);
        DiagramCreator.binCountGraph(relativeDirectory, "dwcVehicle", //
                "Vehicles per Distance with Customer", distanceAnalysis.dwcBinCounter, //
                distanceWCBinSize.number().doubleValue(), 100.0 / distanceAnalysis.numVehicles, //
                "% of fleet", "Distances with Customer", " km", //
                1000, 750);
        UniqueDiagrams.distanceStack(relativeDirectory, "stackedDistance", "Distance Partition", //
                distanceRebalance / distance, distancePickup / distance, distanceWithCust / distance);
        UniqueDiagrams.distanceDistribution(relativeDirectory, "distanceDistribution", //
                "Distance Distribution", true, relativeDirectory.getPath());
        UniqueDiagrams.statusDistribution(relativeDirectory, "statusDistribution", //
                "Status Distribution", true, relativeDirectory.getPath());
    }

    static void getTotals(Tensor summary, CoreAnalysis coreAnalysis, File relativeDirectory) {
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

    public static AnalyzeSummary summarize(CoreAnalysis coreAnalysis, DistanceAnalysis distanceAnalysis) {
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

    public static AnalyzeSummary analyze(File config, String outputdirectory) throws Exception {

        // public static final File RELATIVE_DIRECTORY = new File("output", "data");

        String dataFolderName = outputdirectory + "/data";
        File relativeDirectory = new File(dataFolderName);
        File data = new File(config.getParent(), dataFolderName);
        data.mkdir();

        // load system network
        Network network = loadNetwork(config);

        // load coordinate system
        // TODO later remove hard-coded
        MatsimStaticDatabase.initializeSingletonInstance(network, ReferenceFrame.SIOUXFALLS);

        // load simulation data
        StorageSupplier storageSupplier = StorageSupplier.getDefault();
        final int size = storageSupplier.size();
        System.out.println("Found files: " + size);

        // analyze and print files
        CoreAnalysis coreAnalysis = new CoreAnalysis(storageSupplier);
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
}
