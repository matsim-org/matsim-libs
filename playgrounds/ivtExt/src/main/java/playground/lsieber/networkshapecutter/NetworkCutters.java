package playground.lsieber.networkshapecutter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.options.ScenarioOptions;

public enum NetworkCutters {
    SHAPE {
        @Override
        public Network cut(Network originalNetwork, ScenarioOptions scenOptions) throws MalformedURLException, IOException {
            File shapeFile = scenOptions.getShapeFile();
            GlobalAssert.that(shapeFile.exists());
            Network modifiedNetwork = new NetworkCutterShape(shapeFile).process(originalNetwork);
            return modifiedNetwork;
        }
    },
    CIRCULAR {
        @Override
        public Network cut(Network originalNetwork, ScenarioOptions scenOptions) throws MalformedURLException, IOException {
            Network modifiedNetwork = new NetworkCutterRadius(scenOptions.getLocationSpec().center, //
                    scenOptions.getLocationSpec().radius).process(originalNetwork);
            return modifiedNetwork;
        }
    },
    NONE {
        @Override
        public Network cut(Network originalNetwork, ScenarioOptions scenOptions) throws MalformedURLException, IOException {
            return originalNetwork;
        }
    },

    // TODO @Lukas Implement Rectangular Cutter,
    // RECTANGULAR
    ;

    public abstract Network cut(Network network, ScenarioOptions scenOptions) throws MalformedURLException, IOException;

}
