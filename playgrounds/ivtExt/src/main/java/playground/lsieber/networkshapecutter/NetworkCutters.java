package playground.lsieber.networkshapecutter;

import java.io.IOException;
import java.net.MalformedURLException;

import org.matsim.api.core.v01.network.Network;

public enum NetworkCutters {
    SHAPE {
        public Network cut(Network originalNetwork, PrepSettings settings) throws MalformedURLException, IOException {
            Network modifiedNetwork = new NetworkCutterShape(settings.getFile(SHAPEFILE)).process(originalNetwork);
            return modifiedNetwork;
        }
    },
    CIRCULAR {
        public Network cut(Network originalNetwork, PrepSettings settings) throws MalformedURLException, IOException {
            Network modifiedNetwork = new NetworkCutterRadius(settings.locationSpec.center, settings.locationSpec.radius).process(originalNetwork);
            return modifiedNetwork;
        }
    },
    NONE {
        public Network cut(Network originalNetwork, PrepSettings settings) throws MalformedURLException, IOException {
            return originalNetwork;
        }
    },

    // TODO @Lukas Implement Rectangular Cutter,
    // RECTANGULAR
    ;

    public static final String SHAPEFILE = "shapefilePath";

    public abstract Network cut(Network network, PrepSettings settings) throws MalformedURLException, IOException;

}
