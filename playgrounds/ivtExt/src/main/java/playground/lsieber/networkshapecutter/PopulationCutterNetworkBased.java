package playground.lsieber.networkshapecutter;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;

import playground.clruch.prep.PopulationTools;

public class PopulationCutterNetworkBased extends PopulationCutter {

    private final Network network;

    public PopulationCutterNetworkBased(Network network) {
        this.network = network;
    }

    @Override
    public void process(Population population) {
        PopulationTools.elminateOutsideNetwork(population, network);
    }

}
