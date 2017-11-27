package playground.lsieber.networkshapecutter;

import org.matsim.api.core.v01.population.Population;

interface PopulationCutterInterface {
    void process(Population population);

    void printCutSummary();

    void checkNetworkConsistency();

}
