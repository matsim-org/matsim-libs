package org.matsim.core.mobsim.qsim.qnetsimengine.parking;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;

import java.util.Map;
import java.util.stream.Collectors;

public class PlanBasedParkingCapacityInitializer implements ParkingCapacityInitializer {
	private static final Logger log = LogManager.getLogger(PlanBasedParkingCapacityInitializer.class);
	private final Population population;
	private final Network network;

	@Inject
	PlanBasedParkingCapacityInitializer(Network network, Population population) {
		this.network = network;
		this.population = population;
	}

    @Override
    public Map<Id<Link>, ParkingInitialCapacity> initialize() {
		ZeroParkingCapacityInitializer zeroParkingCapacityInitializer = new ZeroParkingCapacityInitializer(network);
		Map<Id<Link>, ParkingInitialCapacity> capacity = zeroParkingCapacityInitializer.initialize();

		population.getPersons().values().stream()
			.map(p -> PopulationUtils.getFirstActivityOfDayBeforeDepartingWithCar(p.getSelectedPlan()))
			.map(a -> a.getLinkId())
			.collect(Collectors.groupingBy(l -> l, Collectors.counting()))
			.forEach((linkId, count) -> {
				capacity.compute(linkId, (l,p) -> {
					if (p==null) {
						log.warn("Link {} has no parking capacity defined. Assuming 0 capacity and {} initial parking spots.", l, count);
						return new ParkingInitialCapacity(0, count.intValue());
					}

					if(count > p.capacity()){
						log.warn("Link {} has less parking capacity than estimated initial parking spots. Assuming {} initial parking spots.", l, count);
					}
					return new ParkingInitialCapacity(p.capacity(), count.intValue());
				});
			});

		return capacity;
    }

}
