package playground.clruch.analysis;

import static playground.clruch.utils.NetworkLoader.loadNetwork;

import java.io.File;

import org.matsim.api.core.v01.network.Network;

import playground.clruch.data.ReferenceFrame;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.StorageSupplier;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkGet;

/**
 * Created by Joel on 05.04.2017.
 */
public class AnalyzeVirtualNetwork {
    public static void main(String[] args) throws Exception {
        analyze(args);
    }

    public static void analyze(String[] args) throws Exception {

        File config = new File(args[0]);
        File data = new File(config.getParent(), "output/data");
        data.mkdir();

        // load system network
        Network network = loadNetwork(args);

        MatsimStaticDatabase.initializeSingletonInstance(network, ReferenceFrame.SIOUXFALLS);

        VirtualNetwork virtualNetwork = VirtualNetworkGet.readDefault(network);

        // load simulation data
        StorageSupplier storageSupplier = StorageSupplier.getDefault();
        final int size = storageSupplier.size();
        System.out.println("found files: " + size);
        VirtualNetworkAnalysis vna = new VirtualNetworkAnalysis(storageSupplier, virtualNetwork);
        vna.analyze();

    }
}
