package playground.joel.analysis;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.io.CsvFormat;
import ch.ethz.idsc.tensor.io.MathematicaFormat;
import ch.ethz.idsc.tensor.io.MatlabExport;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import playground.clruch.gfx.ReferenceFrame;
import playground.clruch.gfx.helper.SiouxFallstoWGS84;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.StorageSupplier;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import static playground.clruch.demo.utils.NetworkLoader.loadNetwork;

/**
 * Created by Joel on 05.04.2017.
 */
public class AnalyzeAll {
    public static void main(String[] args) throws Exception {
        analyze(args);
    }

    static void saveFile(Tensor table, String name) throws Exception {
        Files.write(Paths.get("output/data/" + name + ".csv"), (Iterable<String>) CsvFormat.of(table)::iterator);
        Files.write(Paths.get("output/data/" + name + ".mathematica"), (Iterable<String>) MathematicaFormat.of(table)::iterator);
        Files.write(Paths.get("output/data/" + name + ".m"), (Iterable<String>) MatlabExport.of(table)::iterator);
    }

    static void plot(String csv, String name, String title, int from, int to, Double maxRange) throws Exception {
        Tensor table = CsvFormat.parse(
                Files.lines(Paths.get("output/data/" + csv + ".csv")));
        System.out.println(Dimensions.of(table));

        table = Transpose.of(table);

        try{
            File dir = new File("output/data");
            DiagramCreator.createDiagram(dir, name, title, table.get(0), table.extract(from,to), maxRange);
        }catch (Exception e){
            System.out.println("Error creating the diagrams");
        }
    }

    static void plot(String csv, String name, String title, int from, int to) throws Exception {
        plot(csv, name, title, from, to, 1.1);
    }

    public static void analyze(String[] args){

        File config = new File(args[0]);
        File data = new File(config.getParent(), "output/data");
        data.mkdir();

        // load system network
        Network network = loadNetwork(args);

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
        try {
            coreAnalysis.analyze(data.toString());
            distanceAnalysis.analzye();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
