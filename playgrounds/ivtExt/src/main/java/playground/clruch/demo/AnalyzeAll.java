package playground.clruch.demo;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import playground.clruch.gfx.MatsimStaticDatabase;
import playground.clruch.gfx.helper.SiouxFallstoWGS84;
import playground.clruch.net.StorageSupplier;

import java.io.File;

import static playground.clruch.demo.utils.NetworkLoader.loadNetwork;

/**
 * Created by Claudio on 3/29/2017.
 */
public class AnalyzeAll {
    public static void main(String[] args) throws Exception {
        analyze(args);
    }

    public static void analyze(String[] args){

        // load system network
        Network network = loadNetwork(args);

        // load coordinate system
        // TODO later remove hard-coded
        CoordinateTransformation ct;
        //ct = new CH1903LV03PlustoWGS84(); // <- switzerland
        ct = new SiouxFallstoWGS84(); // <- sioux falls
        MatsimStaticDatabase.initializeSingletonInstance(network, ct);

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
            coreAnalysis.analyze();
            distanceAnalysis.analzye();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
