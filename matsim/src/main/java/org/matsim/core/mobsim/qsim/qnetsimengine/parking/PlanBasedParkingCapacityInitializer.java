package org.matsim.core.mobsim.qsim.qnetsimengine.parking;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationUtils;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class PlanBasedParkingCapacityInitializer implements ParkingCapacityInitializer {
	private static final Logger log = LogManager.getLogger(PlanBasedParkingCapacityInitializer.class);
	private final Population population;
	private final Network network;
	private final Config config;

	@Inject
	PlanBasedParkingCapacityInitializer(Network network, Population population, Config config) {
		this.network = network;
		this.population = population;
		this.config = config;
	}

	@Override
	public Map<Id<Link>, ParkingInitialCapacity> initialize() {
		ZeroParkingCapacityInitializer zeroParkingCapacityInitializer = new ZeroParkingCapacityInitializer(network, config);
		Map<Id<Link>, ParkingInitialCapacity> capacity = zeroParkingCapacityInitializer.initialize();

		population.getPersons().values().stream()
			.map(p -> PopulationUtils.getFirstActivityOfDayBeforeDepartingWithCar(p.getSelectedPlan()))
			.filter(Objects::nonNull)
			.map(Activity::getLinkId)
			.collect(Collectors.groupingBy(l -> l, Collectors.counting()))
			.forEach((linkId, count) -> {
				capacity.compute(linkId, (l, p) -> {
					if (p == null) {
						log.warn("Link {} has no parking capacity defined. Assuming 0 capacity and {} initial parking spots.", l, count);
						return new ParkingInitialCapacity(0, count.intValue());
					}

					if (count > p.capacity()) {
						log.warn("Link {} has less parking capacity ({}) than estimated initial parking spots ({}).", l, p.capacity(), count);
					}
					return new ParkingInitialCapacity(p.capacity(), count.intValue());
				});
			});

		return capacity;
	}

}
