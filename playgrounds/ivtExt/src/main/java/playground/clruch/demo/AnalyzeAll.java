package playground.clruch.demo;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import playground.clruch.gfx.MatsimStaticDatabase;
import playground.clruch.gfx.ReferenceFrame;
import playground.clruch.gfx.helper.SiouxFallstoWGS84;
import playground.clruch.net.StorageSupplier;

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
        MatsimStaticDatabase.initializeSingletonInstance(network, ReferenceFrame.SIOUXFALLS);

        // load simulation data
        StorageSupplier storageSupplier = StorageSupplier.getDefault();
        final int size = storageSupplier.size();
        System.out.println("found files: " + size);

        // analyze and print files
        CoreAnalysis coreAnalysis = new CoreAnalysis(storageSupplier);
        DistanceAnalysis distanceAnalysis = new DistanceAnalysis(storageSupplier);
        try {
            coreAnalysis.analyze();
            distanceAnalysis.analzye();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
