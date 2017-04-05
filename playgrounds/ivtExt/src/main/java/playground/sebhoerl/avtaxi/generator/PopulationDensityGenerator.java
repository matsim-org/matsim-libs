package playground.sebhoerl.avtaxi.generator;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.core.gbl.MatsimRandom;

import com.google.inject.Inject;

import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;

/**
 * class generates {@link AVVehicle}s and places them according to the density of the {@link Population}.
 * 
 * all vehicles are created in this iteration. after that, no more AVVehiles are added to the system.
 */
public class PopulationDensityGenerator implements AVGenerator {
    private static final Logger log = Logger.getLogger(PopulationDensityGenerator.class);
    final long numberOfVehicles;
    long generatedNumberOfVehicles = 0;

    final String prefix;

    private NavigableMap<Double, Link> cumulativeDensity = new TreeMap<>();

    PopulationDensityGenerator(AVGeneratorConfig config, Network network, Population population) {
        this.numberOfVehicles = config.getNumberOfVehicles();

        String prefix = config.getPrefix();
        this.prefix = prefix == null ? "av_" + config.getParent().getId().toString() + "_" : prefix + "_";

        // Determine density
        double sum = 0.0;
        Map<Link, Double> density = new HashMap<>();

        int personCount = 0;
        for (Person person : population.getPersons().values()) {
            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Activity) {
                    Link link = network.getLinks().get(((Activity) planElement).getLinkId());

                    if (density.containsKey(link)) {
                        density.put(link, density.get(link) + 1.0);
                    } else {
                        density.put(link, 1.0);
                    }
                    sum += 1.0;
                }
            }
            ++personCount;
        }
        log.info("personCount " + personCount);

        // Compute relative frequencies and cumulative
        double cumsum = 0.0;

        for (Map.Entry<Link, Double> entry : density.entrySet()) {
            cumulativeDensity.put(cumsum, entry.getKey());
            cumsum += entry.getValue() / sum;

            log.debug("link " + entry.getKey() + "cumsum " + cumsum);
        }
    }

    @Override
    public boolean hasNext() {
        return generatedNumberOfVehicles < numberOfVehicles;
    }

    @Override
    public AVVehicle next() {
        generatedNumberOfVehicles++;

        // Multinomial selection
        double r = MatsimRandom.getRandom().nextDouble();
        Link selectedLink = null;

        selectedLink = cumulativeDensity.lowerEntry(r).getValue();
        log.info("car placed at link " + selectedLink + " r=" + r);

        Id<Vehicle> id = Id.create("av_" + prefix + String.valueOf(generatedNumberOfVehicles), Vehicle.class);
        return new AVVehicle(id, selectedLink, 4.0, 0.0, 108000.0);
    }

    static public class Factory implements AVGenerator.AVGeneratorFactory {
        @Inject
        private Population population;
        @Inject
        private Network network;

        @Override
        public AVGenerator createGenerator(AVGeneratorConfig generatorConfig) {
            return new PopulationDensityGenerator(generatorConfig, network, population);
        }
    }
}
