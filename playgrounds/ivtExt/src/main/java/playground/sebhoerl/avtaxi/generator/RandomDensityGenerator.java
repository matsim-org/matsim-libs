package playground.sebhoerl.avtaxi.generator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.core.gbl.MatsimRandom;

import com.google.inject.Inject;

import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;

/**
 * class generates {@link AVVehicle}s and places them randomly.
 *
 * all vehicles are created in this iteration. after that, no more AVVehiles are added to the system.
 */
public class RandomDensityGenerator implements AVGenerator {
    private static final Logger log = Logger.getLogger(RandomDensityGenerator.class);
    final long numberOfVehicles;
    long generatedNumberOfVehicles = 0;

    final String prefix;

    private Network network;

    RandomDensityGenerator(AVGeneratorConfig config, Network networkIn, Population population) {

        this.numberOfVehicles = config.getNumberOfVehicles();

        String prefix = config.getPrefix();
        this.prefix = prefix == null ? "av_" + config.getParent().getId().toString() + "_" : prefix + "_";

        network = networkIn;

    }

    @Override
    public boolean hasNext() {
        return generatedNumberOfVehicles < numberOfVehicles;
    }

    @Override
    public AVVehicle next() {
        generatedNumberOfVehicles++;

        int bound = network.getLinks().size();
        int elemRand = MatsimRandom.getRandom().nextInt(bound);
        Link linkGen = network.getLinks().values().stream().skip(elemRand).findFirst().get();

        log.info("car placed at link " + linkGen);

        Id<Vehicle> id = Id.create("av_" + prefix + String.valueOf(generatedNumberOfVehicles), Vehicle.class);
        return new AVVehicle(id, linkGen, 4.0, 0.0, 108000.0);
    }

    static public class Factory implements AVGenerator.AVGeneratorFactory {
        @Inject
        private Population population;
        @Inject
        private Network network;

        @Override
        public AVGenerator createGenerator(AVGeneratorConfig generatorConfig) {
            return new RandomDensityGenerator(generatorConfig, network, population);
        }
    }
}
