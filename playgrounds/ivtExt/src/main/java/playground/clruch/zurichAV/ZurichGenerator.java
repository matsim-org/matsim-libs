package playground.clruch.zurichAV;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.core.gbl.MatsimRandom;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.data.AVOperator;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.generator.AVGenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class ZurichGenerator implements AVGenerator {
    final private AVOperator operator;
    final private Collection<Link> permissibleLinks;
    final private long numberOfVehicles;

    private ArrayList<Link> linkCache;
    private long generatedVehicleCount = 0;

    public ZurichGenerator(Collection<Link> permissibleLinks, long numberOfVehicles, AVOperator operator) {
        this.operator = operator;
        this.permissibleLinks = permissibleLinks;
        this.numberOfVehicles = numberOfVehicles;
        this.linkCache = new ArrayList<>(permissibleLinks);
    }

    @Override
    public boolean hasNext() {
        if (generatedVehicleCount >= numberOfVehicles) {
            linkCache = null;
            return false;
        }

        return true;
    }

    @Override
    public AVVehicle next() {
        generatedVehicleCount++;

        return new AVVehicle(
                Id.create("av_" + operator.getId().toString() + "_" + generatedVehicleCount, Vehicle.class),
                linkCache.get(MatsimRandom.getRandom().nextInt(linkCache.size())),
                4.0,
                0.0,
                108000.0,
                operator
                );
    }

    static public class ZurichGeneratorFactory implements AVGeneratorFactory {
        @Inject @Named("zurich")
        private Collection<Link> permissibleLinks;

        @Inject
        private Map<Id<AVOperator>, AVOperator> operators;

        @Override
        public AVGenerator createGenerator(AVGeneratorConfig generatorConfig) {
            long numberOfVehicles = generatorConfig.getNumberOfVehicles();
            return new ZurichGenerator(permissibleLinks, numberOfVehicles, operators.get(generatorConfig.getParent().getId()));
        }
    }
}
