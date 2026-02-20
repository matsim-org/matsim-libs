/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** */

package org.matsim.contrib.drt.extension.benchmark.scenario;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.FleetWriter;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.load.IntegerLoadType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates a synthetic DRT fleet for benchmarks.
 *
 * @author Steffen Axer
 */
public class FleetGenerator {

	private final int vehicleCapacity;
	private final double serviceBeginTime;
	private final double serviceEndTime;

	public FleetGenerator(int vehicleCapacity, double serviceBeginTime, double serviceEndTime) {
		this.vehicleCapacity = vehicleCapacity;
		this.serviceBeginTime = serviceBeginTime;
		this.serviceEndTime = serviceEndTime;
	}

	public Path generate(int numberOfVehicles, Scenario scenario, Path outputPath) {
		List<Link> links = new ArrayList<>(scenario.getNetwork().getLinks().values());
		Random random = new Random(42);
		List<ImmutableDvrpVehicleSpecification> vehicles = new ArrayList<>();

		for (int i = 0; i < numberOfVehicles; i++) {
			Link startLink = links.get(random.nextInt(links.size()));
			vehicles.add(ImmutableDvrpVehicleSpecification.newBuilder()
				.id(Id.create("drt_vehicle_" + i, DvrpVehicle.class))
				.capacity(vehicleCapacity)
				.startLinkId(startLink.getId())
				.serviceBeginTime(serviceBeginTime)
				.serviceEndTime(serviceEndTime)
				.build());
		}

		Path fleetFile = outputPath.resolve("drt_fleet.xml");
		new FleetWriter(vehicles.stream(), new IntegerLoadType("passengers")).write(fleetFile.toString());
		return fleetFile;
	}
}
