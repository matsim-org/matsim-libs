package org.matsim.contrib.parking.parkingsearchparameterization;

import com.google.inject.Singleton;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.network.kernel.DefaultKernelFunction;
import org.matsim.core.network.kernel.KernelDistance;
import org.matsim.core.network.kernel.NetworkKernelFunction;

import java.util.Set;

/**
 * Delays the arrival of cars (or other modes, see {@link #setParkingSearchModes(Set)}) by a parking search time, which depends on
 * the parking occupancy around the arrival link.
 * Parking capacities are read from the link attributes {@link ParkingUtils#LINK_ON_STREET_SPOTS} and
 * {@link ParkingUtils#LINK_OFF_STREET_SPOTS}. By default, the initial occupancy is derived from the plans
 * ({@link PlanBasedParkingCapacityInitializer}), see {@link #setParkingCapacityInitializer(Class)}. The links considered around
 * the arrival link are determined by {@link DefaultKernelFunction}, which can be replaced by a subsequent overriding module.
 * <p>
 * This is the only module users need to add. It installs the QSim components ({@link ParkingSearchParameterizationQSimModule})
 * itself.
 */
public final class ParkingSearchParameterizationModule extends AbstractModule {
	private final ParkingSearchTimeFunction parkingSearchTimeFunction;
	private final KernelDistance kernelDistance;
	private Class<? extends ParkingCapacityInitializer> parkingCapacityInitializer = PlanBasedParkingCapacityInitializer.class;
	private Set<String> parkingSearchModes = Set.of(TransportMode.car);

	/**
	 * @param parkingSearchTimeFunction maps the occupancy around the arrival link to a parking search time
	 * @param kernelDistance            radius around the arrival link in which the occupancy is considered
	 */
	public ParkingSearchParameterizationModule(ParkingSearchTimeFunction parkingSearchTimeFunction, KernelDistance kernelDistance) {
		this.parkingSearchTimeFunction = parkingSearchTimeFunction;
		this.kernelDistance = kernelDistance;
	}

	public ParkingSearchParameterizationModule setParkingCapacityInitializer(Class<? extends ParkingCapacityInitializer> parkingCapacityInitializer) {
		this.parkingCapacityInitializer = parkingCapacityInitializer;
		return this;
	}

	/**
	 * @param parkingSearchModes network modes of the vehicles that search for parking on arrival. Default: {@link TransportMode#car}.
	 */
	public ParkingSearchParameterizationModule setParkingSearchModes(Set<String> parkingSearchModes) {
		this.parkingSearchModes = Set.copyOf(parkingSearchModes);
		return this;
	}

	@Override
	public void install() {
		// The observer is event handler, controller listener and mobsim listener at the same time and is queried by the search time
		// calculator. All of them need to share one instance.
		bind(ParkingOccupancyObserver.class).in(Singleton.class);
		addEventHandlerBinding().to(ParkingOccupancyObserver.class);
		addControllerListenerBinding().to(ParkingOccupancyObserver.class);
		addMobsimListenerBinding().to(ParkingOccupancyObserver.class);

		bind(ParkingCapacityInitializer.class).to(parkingCapacityInitializer);
		bind(NetworkKernelFunction.class).to(DefaultKernelFunction.class);
		bind(KernelDistance.class).toInstance(kernelDistance);
		bind(ParkingSearchTimeFunction.class).toInstance(parkingSearchTimeFunction);

		// Installing the QSim components here ensures that users only need to add this module.
		installQSimModule(new ParkingSearchParameterizationQSimModule(parkingSearchModes));
	}
}
