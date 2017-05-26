package playground.sebhoerl.renault;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.data.Vehicle;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.generator.AVGenerator;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

public class LaDefenseGenerator implements AVGenerator {
    final private Set<Id<Node>> laDefenseFilter;
    final private Network network;
    final private long numberOfVehicles;

    private long currentNumberOfVehicles = 0;

    public LaDefenseGenerator(Network network, Set<Id<Node>> laDefenseFilter, long numberOfVehicles) {
        this.network = network;
        this.laDefenseFilter = laDefenseFilter;
        this.numberOfVehicles = numberOfVehicles;
    }

    @Override
    public boolean hasNext() {
        return currentNumberOfVehicles < numberOfVehicles;
    }

    @Override
    public AVVehicle next() {
        currentNumberOfVehicles++;

        int size = laDefenseFilter.size();
        int item = new Random().nextInt(size);

        Id<Node> selection = laDefenseFilter.iterator().next();

        int i = 0;
        for (Id<Node> current : laDefenseFilter) {
            if (i == item) {
                selection = current;
                break;
            }

            i++;
        }

        Collection<? extends Link> links = network.getNodes().get(selection).getOutLinks().values();

        size = links.size();
        item = new Random().nextInt(size);
        i = 0;

        Link selectedLink = links.iterator().next();

        for (Link link : links) {
            if (i == item) {
                selectedLink = link;
            }

            i++;
        }

        return new AVVehicle(
                Id.create("av_" + String.valueOf(currentNumberOfVehicles), Vehicle.class),
                selectedLink,
                4.0,
                0.0,
                108000.0
        );
    }

    static public class LaDefenseGeneratorFactory implements AVGeneratorFactory {
        @Inject Network network;

        @Inject @Named(LaDefenseModule.LADEFENSE)
        Set<Id<Node>> laDefenseFilter;

        @Override
        public AVGenerator createGenerator(AVGeneratorConfig generatorConfig) {
            long numberOfVehicles = generatorConfig.getNumberOfVehicles();
            return new LaDefenseGenerator(network, laDefenseFilter, numberOfVehicles);
        }
    }
}
