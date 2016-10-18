package playground.sebhoerl.avtaxi.generator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import playground.sebhoerl.avtaxi.config.AVOperatorConfig;
import playground.sebhoerl.avtaxi.data.AVOperator;
import playground.sebhoerl.avtaxi.data.AVVehicle;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PopulationDensityGenerator implements AVGenerator {
    final long numberOfVehicles;
    final Id<AVOperator> operatorId;
    final String prefix;

    long generatedNumberOfVehicles = 0;

    private List<Id<Link>> linkList = new LinkedList<Id<Link>>();
    private Map<Id<Link>, Double> cumulativeDensity = new HashMap<Id<Link>, Double>();

    public PopulationDensityGenerator(AVOperatorConfig config, Population population) {
        String numberOfVehicles = config.getDispatcherConfig().getValue("numberOfVehicles");

        if (numberOfVehicles == null) {
            throw new IllegalStateException("PopulationDensityGenerator needs the numberOfVehicles!");
        }

        this.numberOfVehicles = Long.parseLong(numberOfVehicles);
        this.operatorId = config.getId();

        String prefix = config.getDispatcherConfig().getValue("prefix");
        this.prefix = prefix == null ? "" : prefix + "_";

        // Determine density
        double sum = 0.0;
        Map<Id<Link>, Double> density = new HashMap<Id<Link>, Double>();

        for (Person person : population.getPersons().values()) {
            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Activity) {
                    Id<Link> linkId = ((Activity) planElement).getLinkId();

                    if (density.containsKey(linkId)) {
                        density.put(linkId, density.get(linkId) + 1.0);
                    } else {
                        density.put(linkId, 1.0);
                    }

                    sum += 1.0;
                }

                break;
            }
        }

        // Compute relative frequencies and cumulative
        double cumsum = 0.0;

        for (Id<Link> linkId : density.keySet()) {
            cumsum += cumulativeDensity.get(linkId) / sum;
            cumulativeDensity.put(linkId, cumsum);
            linkList.add(linkId);
        }

        long id = 1;

        // Create vehicles
        for (Id<Link> linkId : selection) {
            AVVehicle vehicle = new AVVehicle(
                    Id.create("av" + String.valueOf(id), Vehicle.class),
                    network.getLinks().get(linkId),
                    4.0,
                    0.0,
                    108000.0,
                    operator
            );

            data.addVehicle(vehicle);
            operator.getDispatcher().addVehicle(vehicle);

            id++;
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
        Id<Link> selectedLink = null;

        for (Id<Link> linkId : linkList) {
            if (r <= cumulativeDensity.get(linkId)) {
                selectedLink = linkId;
                break;
            }
        }

        Id<Vehicle> id = Id.create("av_" + prefix + String.valueOf(generatedNumberOfVehicles), Vehicle.class);
        return new AVVehicle(id, selectedLink, 4.0, 0.0, 108000.0);
    }
}
