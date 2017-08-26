package playground.maalbert.analysis;

import static playground.clruch.utils.NetworkLoader.loadNetwork;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.alg.Join;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.io.CsvFormat;
import playground.clruch.analysis.DiagramCreator;
import playground.clruch.data.ReferenceFrame;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.StorageSupplier;
import playground.joel.data.TotalData;


/**
 * Created by Joel on 05.04.2017.
 */
public class AnalyzeMarc {

    public static void main(String[] args) throws Exception {
        analyze(args);
    }

    static void saveFile(Tensor table, String name) throws Exception {
        //DEBUG START
        String folderName = "data4Matlab/";
        File directory = new File(folderName);
        if (! directory.exists()){
            directory.mkdir();
        }
        Files.write(Paths.get(folderName + name + ".csv"), (Iterable<String>) CsvFormat.of(table)::iterator);
        //DEBUG END

//        //Old version Start
//       Files.write(Paths.get("output/data/" + name + ".csv"), (Iterable<String>) CsvFormat.of(table)::iterator);
//       Files.write(Paths.get("output/data/" + name + ".mathematica"), (Iterable<String>) MathematicaFormat.of(table)::iterator);
//       Files.write(Paths.get("output/data/" + name + ".m"), (Iterable<String>) MatlabExport.of(table)::iterator);
//        //OLD Version END
    }

    static void plot(String csv, String name, String title, int from, int to, Double maxRange) throws Exception {
        Tensor table = CsvFormat.parse(Files.lines(Paths.get("output/data/" + csv + ".csv")));
        System.out.println(Dimensions.of(table));

        table = Transpose.of(table);

        try {
            File dir = new File("output/data");
            DiagramCreator.createDiagram(dir, name, title, table.get(0), table.extract(from, to), maxRange);
        } catch (Exception e) {
            System.out.println("Error creating the diagrams");
        }
    }

    static void plot(String csv, String name, String title, int from, int to) throws Exception {
        plot(csv, name, title, from, to, 1.05);
    }

    static void collectAndPlot(CoreAnalysis coreAnalysis, DistanceAnalysis distanceAnalysis) throws Exception {
        Tensor summary = Join.of(1, coreAnalysis.summary, distanceAnalysis.summary);
        saveFile(summary, "summary");
        AnalyzeMarc.plot("summary", "binnedWaitingTimes", "waiting times", 3, 6, 1200.0); // maximum waiting time in the plot to have this uniform for all
                                                                                         // simulations
        AnalyzeMarc.plot("summary", "binnedTimeRatios", "occupancy ratio", 10, 11);
        AnalyzeMarc.plot("summary", "binnedDistanceRatios", "distance ratio", 13, 14);
        getTotals(summary, coreAnalysis);
    }

    // TODO: get mean and quantiles over entire day, placeholders
    static void getTotals(Tensor table, CoreAnalysis coreAnalysis) {
        int size = table.length();
        double timeRatio = 0;
        double distance = 0;
        double distanceWithCust = 0;
        double mean = coreAnalysis.totalWaitTimeMean.Get().number().doubleValue();
        double quantile50 = coreAnalysis.totalWaitTimeQuantile.Get(1).number().doubleValue();
        double quantile95 = coreAnalysis.totalWaitTimeQuantile.Get(2).number().doubleValue();
        for (int j = 0; j < size; j++) {
            timeRatio += table.Get(j, 10).number().doubleValue();
            distance += table.Get(j, 11).number().doubleValue();
            distanceWithCust += table.Get(j, 12).number().doubleValue();
        }
        timeRatio = timeRatio / size;
        double distanceRatio = distanceWithCust / distance;

        TotalData totalData = new TotalData();
        totalData.generate(String.valueOf(timeRatio), String.valueOf(distanceRatio), String.valueOf(mean), String.valueOf(quantile50), String.valueOf(quantile95),
                new File("output/data/totalData.xml"));
    }

    public static void analyze(String[] args) throws Exception {

        File config = new File(args[0]);
        File data = new File(config.getParent(), "output/data");
        data.mkdir();

        // load system network
        Network network = loadNetwork(new File(args[0]));

        // load coordinate system
        // TODO later remove hard-coded
        MatsimStaticDatabase.initializeSingletonInstance(network, ReferenceFrame.SIOUXFALLS);

        // load simulation data
        StorageSupplier storageSupplier = StorageSupplier.getDefault();
        final int size = storageSupplier.size();
        System.out.println("found files: " + size);

        // analyze and print files
        CoreAnalysis coreAnalysis = new CoreAnalysis(storageSupplier);
        DistanceAnalysis distanceAnalysis = new DistanceAnalysis(storageSupplier);
        ConsensusAnalysis consensusAnalysis = new ConsensusAnalysis(args,storageSupplier);

        try {
            coreAnalysis.analyze();
            distanceAnalysis.analzye();
            consensusAnalysis.analyze();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //collectAndPlot(coreAnalysis, distanceAnalysis);

    }
}
