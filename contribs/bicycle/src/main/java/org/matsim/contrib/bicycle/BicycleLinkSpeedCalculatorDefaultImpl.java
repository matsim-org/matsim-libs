package org.matsim.contrib.bicycle;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.vehicles.Vehicle;

import org.matsim.vehicles.VehicleType;

import java.util.Objects;

public final class BicycleLinkSpeedCalculatorDefaultImpl implements BicycleLinkSpeedCalculator {
	private static final Logger log = LogManager.getLogger(BicycleLinkSpeedCalculatorDefaultImpl.class );
	@Inject private BicycleParams params;
	@Inject private BicycleConfigGroup bicycleConfigGroup;
	@Inject private QSimConfigGroup qSimConfigGroup;
	@Inject private Config config;
	@Inject private BicycleLinkSpeedCalculatorDefaultImpl() {
	}

	/**
	 * for unit testing
	 */
	BicycleLinkSpeedCalculatorDefaultImpl( Config config ) {
		this.bicycleConfigGroup = ConfigUtils.addOrGetModule( config, BicycleConfigGroup.class );
		this.qSimConfigGroup = config.qsim();
	}

	@Override
	public double getMaximumVelocity(QVehicle qVehicle, Link link, double time) {
		if (isBike(qVehicle)){
			return getMaximumVelocityForLink( link, qVehicle.getVehicle() );
		} else{
			return Double.NaN;
			// (this now works because the link speed calculator returns the default for all combinations of (vehicle, link, time) that
			// are not answered by a specialized link speed calculator.  kai, jun'23)
		}

	}
	@Override
	public double getMaximumVelocityForLink(Link link, Vehicle vehicle) {

		// prior to matsim 12.0 routers would not pass a vehicle. This is why we have a fallback for a default value from the config
//		double maxBicycleSpeed = vehicle == null ? bicycleConfigGroup.getMaxBicycleSpeedForRouting() : vehicle.getType().getMaximumVelocity();
		// when using bicycle, one should always have vehicles with individual maximum speeds, possibly by using modeVehicleTypes.

		double maxBicycleSpeed = vehicle.getType().getMaximumVelocity();
		double bicycleInfrastructureFactor = computeInfrastructureFactor(link);
		double surfaceFactor = params.computeSurfaceFactor(link);
		double gradientFactor = computeGradientFactor(link);
		double speed = maxBicycleSpeed * bicycleInfrastructureFactor * surfaceFactor * gradientFactor;
		return Math.min(speed, link.getFreespeed());
	}

//	private double getDefaultMaximumVelocity(QVehicle qVehicle, Link link, double time) {
//		return Math.min(qVehicle.getMaximumVelocity(), link.getFreespeed(time));
//	}

	/**
	 * Based on "Flügel et al. -- Empirical speed models for cycling in the Oslo road network" (not yet published!)
	 * Positive gradients (uphill): Roughly linear decrease in speed with increasing gradient
	 * At 9% gradient, cyclists are 42.7% slower
	 * Negative gradients (downhill):
	 * Not linear; highest speeds at 5% or 6% gradient; at gradients higher than 6% braking
	 */
	private double computeGradientFactor(Link link) {

		double factor = 1;
		if (link.getFromNode().getCoord().hasZ() && link.getToNode().getCoord().hasZ()) {
			double fromZ = link.getFromNode().getCoord().getZ();
			double toZ = link.getToNode().getCoord().getZ();
			if (toZ > fromZ) { // No positive speed increase for downhill, only decrease for uphill
				double reduction = 1 - 5 * ((toZ - fromZ) / link.getLength());
				factor = Math.max(0.1, reduction); // maximum reduction is 0.1
			}
		}

		return factor;
	}

	private double computeInfrastructureFactor(Link link) {
		var speedFactor = link.getAttributes().getAttribute(BicycleUtils.BICYCLE_INFRASTRUCTURE_SPEED_FACTOR);
		return speedFactor == null ? 1.0 : Double.parseDouble(speedFactor.toString());
	}

	static boolean hasNotAttribute(Link link, String attributeName) {
		return link.getAttributes().getAttribute(attributeName) == null;
	}

	private boolean isBike(QVehicle qVehicle) {
//		return qVehicle.getVehicle().getType().getId().toString().equals(bicycleConfigGroup.getBicycleMode());

		// the above is what I found.  With mode vehicles, the vehicle ID is indeed abused for the model.  But we should not rely on this.
		// Unfortunately, backwards compatibility may now fail ... I have seen mode vehicles different from car but having car as network
		// mode.  kai, jun'23

		final VehicleType vehicleType = qVehicle.getVehicle().getType();

		// the below consistentcy check is to broad; need a version that is more narrow ...

//		if ( qSimConfigGroup.getVehiclesSource()== QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData ) {
//			if ( !vehicleType.getId().toString().equals( vehicleType.getNetworkMode() ) ) {
//				throw new RuntimeException( "You are using mode vehicles but the network mode of the vehicle type is wrong: vehType.id=" + vehicleType.getId()
//									    + "; vehType.mode=" + vehicleType.getNetworkMode() );
//			}
//		}

		// ... more narrow version coming here ...
		if (
				qVehicle.getVehicle().getType().getId().toString().equals( bicycleConfigGroup.getBicycleMode() )
									      && !vehicleType.getNetworkMode().equals( bicycleConfigGroup.getBicycleMode() )
		) {
				throw new RuntimeException( "You are using mode vehicles but the network mode of the vehicle type is wrong: vehType.id=" + vehicleType.getId()
									    + "; vehType.mode=" + vehicleType.getNetworkMode() );
		}


		return vehicleType.getNetworkMode().equals(bicycleConfigGroup.getBicycleMode() );
	}
}
