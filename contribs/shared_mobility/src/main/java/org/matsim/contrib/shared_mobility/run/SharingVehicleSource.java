package org.matsim.contrib.shared_mobility.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.shared_mobility.io.SharingServiceSpecification;
import org.matsim.contrib.shared_mobility.io.SharingVehicleSpecification;
import org.matsim.contrib.shared_mobility.service.SharingUtils;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import java.util.Map;

/**
 * @author steffenaxer, hrewald
 */
public class SharingVehicleSource implements AgentSource {

	public static final Logger LOG = LogManager.getLogger(SharingVehicleSource.class);

	private final QSim qsim;
	private final SharingServiceConfigGroup serviceConfig;
	private final SharingServiceSpecification specification;

	public SharingVehicleSource(QSim qSim, SharingServiceConfigGroup serviceConfig,
								SharingServiceSpecification specification) {
		this.qsim = qSim;
		this.serviceConfig = serviceConfig;
		this.specification = specification;
	}

	@Override
	public void insertAgentsIntoMobsim() {

		Vehicles vehicles = this.qsim.getScenario().getVehicles();
		VehiclesFactory factory = vehicles.getFactory();
		Map<Id<Link>, ? extends Link> links = this.qsim.getScenario().getNetwork().getLinks();
		Map<Id<Vehicle>, MobsimVehicle> mobsimVehicles = qsim.getVehicles();

		// get or create vehicle type
		VehicleType vehicleType = SharingUtils.getOrCreateAndAddVehicleType(this.serviceConfig, vehicles);

		// get or create vehicles
		for (SharingVehicleSpecification svs : specification.getVehicles()) {
			Id<Vehicle> vehicleId = svs.getVehicleId();
			Id<Link> startLink = svs.getStartLinkId().get();

			// check if vehicle already exists
			Vehicle vehicle = SharingUtils.getOrCreateAndAddVehicle(vehicleId, vehicleType, vehicles);

			// check if mobsimVehicle already exists
			MobsimVehicle mobsimVehicle = mobsimVehicles.get(vehicle.getId());
			if (mobsimVehicle != null) {
				LOG.warn("MobsimVehicle with id {} already exists. Skipping creation of sharing vehicle in QSim.",
					vehicleId);
				continue;
			}
			// create mobsimVehicle and add to QSim
			mobsimVehicle = new QVehicleImpl(vehicle);
			((QVehicleImpl) mobsimVehicle).setCurrentLinkId(startLink);
			qsim.addParkedVehicle(mobsimVehicle, startLink);
		}
	}
}
