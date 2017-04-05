package playground.clruch.demo;

import ch.ethz.idsc.tensor.Tensor;
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
 * Created by Claudio on 3/29/2017.
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


        //Create Output Simulation Folder
        //TODO REFACTOR THIS PART, use xml input to save in vNet folder or create new one with certain name within sim data 4 matlab folder?
        String dataPath = "data4Matsim";
        File theDir = new File(dataPath);
        // if the directory does not exist, create it
                if (!theDir.exists()) {
                    System.out.println("creating directory: " + theDir.getName());
                    boolean result = false;

                    try{
                        theDir.mkdir();
                        result = true;
                    }
                    catch(SecurityException se){
                        //handle it
                    }
                    if(result) {
                        System.out.println("DIR created");
                    }
                }


        // analyze and print files
        CoreAnalysis coreAnalysis = new CoreAnalysis(storageSupplier,dataPath);
        DistanceAnalysis distanceAnalysis = new DistanceAnalysis(storageSupplier,dataPath);
        try {
            coreAnalysis.analyze(data.toString());
            distanceAnalysis.analzye();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
