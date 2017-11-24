package playground.lsieber.networkshapecutter;

import java.io.IOException;
import java.net.MalformedURLException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;

public enum PopulationCutters {
    NETWORKBASED {
        public void cut(Network network, Population population) throws MalformedURLException, IOException {
            new PopulationCutterNetworkBased(network).process(population);
        }
    };

    public abstract void cut(Network network, Population population) throws MalformedURLException, IOException;

}
