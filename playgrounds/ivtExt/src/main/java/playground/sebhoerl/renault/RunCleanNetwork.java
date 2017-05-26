package playground.sebhoerl.renault;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.network.io.MatsimNetworkReader;

import java.util.Arrays;
import java.util.HashSet;

public class RunCleanNetwork {
    public static void main(String[] args) {
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(args[0]);
        new MultimodalNetworkCleaner(network).run(new HashSet<>(Arrays.asList("car")));
        new NetworkWriter(network).writeV1(args[1]);
    }
}
