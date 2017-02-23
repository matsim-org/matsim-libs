package contrib.baseline.modification;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;

public class FreespeedAdjustment {
    static public void main(String[] args) {
        new FreespeedAdjustment().adjustSpeeds(args[0], args[1]);
    }

    public void adjustSpeeds(String networkSourcePath, String networkTargetPath) {
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(networkSourcePath);

        for (Link link : network.getLinks().values()) {
            double speed = link.getFreespeed() * (3600.0 / 1000.0);

            if (link.getAllowedModes().contains(TransportMode.car)) {
                if (speed > 25.0) {
                    if (speed < 45.0) {
                        speed -= 5.0;
                    } else if (speed < 55.0) {
                        speed -= 15.0;
                    } else {
                        speed -= 10.0;
                    }
                }
            }

            link.setFreespeed(speed / (3600.0 / 1000.0));
        }

        new NetworkWriter(network).write(networkTargetPath);
    }
}
