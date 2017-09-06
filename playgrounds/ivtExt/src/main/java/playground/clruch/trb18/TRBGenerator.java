package playground.clruch.trb18;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.core.gbl.MatsimRandom;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.generator.AVGenerator;

public class TRBGenerator implements AVGenerator {
    final long numberOfVehicles;
    final Network network;
    final String prefix;
    final List<Link> availableLinks;

    long generatedNumberOfVehicles = 0;

    public TRBGenerator(AVGeneratorConfig config, Network network) {
        this.numberOfVehicles = config.getNumberOfVehicles();
        this.network = network;

        String prefix = config.getPrefix();
        this.prefix = prefix == null ? "av_" + config.getParent().getId().toString() + "_" : prefix + "_";

        availableLinks = new LinkedList<Link>(network.getLinks().values()); //.stream().filter(l -> l.getAllowedModes().contains(AVModule.AV_MODE)).collect(Collectors.toList());
    }

    @Override
    public boolean hasNext() {
        return generatedNumberOfVehicles < numberOfVehicles;
    }

    @Override
    public AVVehicle next() {
        generatedNumberOfVehicles++;

        int index = MatsimRandom.getRandom().nextInt(availableLinks.size());

        Id<Vehicle> id = Id.create("av_" + prefix + String.valueOf(generatedNumberOfVehicles), Vehicle.class);

        System.out.println("Vehicle @ " + availableLinks.get(index));

        return new AVVehicle(id, availableLinks.get(index), 4.0, 0.0, 108000.0);
    }

    static public class Factory implements AVGenerator.AVGeneratorFactory {
        @Inject @Named("trb_reduced")
        private Network network;

        @Override
        public AVGenerator createGenerator(AVGeneratorConfig generatorConfig) {
            return new TRBGenerator(generatorConfig, network);
        }
    }
}
