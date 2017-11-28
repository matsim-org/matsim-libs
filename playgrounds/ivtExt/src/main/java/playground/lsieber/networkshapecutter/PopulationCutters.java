package playground.lsieber.networkshapecutter;

import java.io.IOException;
import java.net.MalformedURLException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;

public enum PopulationCutters {
    NETWORKBASED {
        @Override
        public void cut(Network network, Population population) throws MalformedURLException, IOException {
            new PopulationCutterNetworkBased(network).process(population);
        }
    },
    FULLLEGSINNETWORK{
        @Override
        public void cut(Network network, Population population) {
            new PopulationCutterFullLegsInNetwork(network, null).process(population);
        }
    },
    NONE{
        @Override
        public void cut(Network network, Population population) {
        }
    }
    ;

    public abstract void cut(Network network, Population population) throws MalformedURLException, IOException;

}
