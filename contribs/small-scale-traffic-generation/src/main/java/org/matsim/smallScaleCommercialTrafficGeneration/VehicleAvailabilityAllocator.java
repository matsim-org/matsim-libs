package org.matsim.smallScaleCommercialTrafficGeneration;

import java.util.*;

/**
 * A small allocator to solve the time-window problems in the Vehicle and Service generation using a Best-Fit Allocator. <br>
 * <i>NOTE: This class does not actually change anything in the scenario. It is just a tool to check if Service and Vehicle TimeWindows are
 * compatible.</i>
 */
public class VehicleAvailabilityAllocator {
	private List<Double> availableVehicleTime;

	/**
	 * Prepares the allocator for a vehicle fleet.
	 * @param availableVehicleTime 	This Collection should contain the duration of available-time-frames of the vehicles.
	 *                          	For example: 4x vehicles are available from 1:00 to 4:00 (3 hours), then the {@code availableVehicles} Collection should
	 *                          	contain 4 entries with value: 3*3600=10800. If a vehicle has a non-coherent availability-time-frame, add it as two
	 *                          	separate entries.
	 */
	public VehicleAvailabilityAllocator(List<Double> availableVehicleTime){
		this.availableVehicleTime = availableVehicleTime;
	}

	/**
	 * Prepares the allocator for one vehicle.
	 * @param availableVehicleTime 	This Collection should contain the duration of available-time-frames of the vehicle.
	 */
	public VehicleAvailabilityAllocator(double availableVehicleTime){
		this.availableVehicleTime = new ArrayList<>(1);
		this.availableVehicleTime.add(availableVehicleTime);
	}

	/**
	 * Checks if a vehicle is available for the given amount of time. If not, then the time is set to the largest possible duration,
	 * which can be allocated.
	 * @return the reduced serviceDuration (unchanged, if a vehicle was found, that was available for the full duration)
	 */
	public double makeServiceDurationViable(double serviceDuration){
		for(Double vehicleTime : availableVehicleTime){
			if(vehicleTime >= serviceDuration) return serviceDuration;
		}
		return availableVehicleTime.stream().mapToDouble(v -> v).max().orElseThrow();
	}

	/**
	 * Tries to allocate a single vehicle to the service and reduces the allocated vehicle available time by the serviceDuration.
	 * If no vehicle is available nothing happens. You should then consider to reduce the duration with {@link VehicleAvailabilityAllocator#makeServiceDurationViable}
	 * @return true if a vehicle was allocated, false if no vehicle is available for the given duration
	 */
	public boolean allocateServiceDuration(double serviceDuration){
		//Best-Fit Allocation
		int bestFit = -1;
		double bestRemaining = Double.MAX_VALUE;
		for(int i = 0; i < availableVehicleTime.size(); i++){
			double remaining = availableVehicleTime.get(i) - serviceDuration;
			if(remaining >= 0 && remaining < bestRemaining){
				bestFit = i;
				bestRemaining = remaining;
			}
		}
		if(bestFit == -1) return false;
		//Allocate
		availableVehicleTime.set(bestFit, availableVehicleTime.get(bestFit) - serviceDuration);
		return true;
	}

	/**
	 * This method checks for a given amount of same serviceDurations, whether you can allocate a vehicle to all of them or not.
	 * If not, the Allocator reduces the serviceDurations in a balanced way, so that the duration-cutoff is distributed across all given services.
	 * If you do not care if some services get much more time than others, you can use the {@link VehicleAvailabilityAllocator#makeServiceDurationViable} method.
	 * @param serviceDuration The duration of the services.
	 * @return An array which contains the maximum possible service durations (reverse order)
	 */
	public double makeMultipleServiceDurationsBalancedViable(int serviceAmount, double serviceDuration){
		//Check for serviceDuration first
		int allocatedServices = 0;
		for (Double d : availableVehicleTime) {
			allocatedServices += (int) Math.floor(d / serviceDuration);
		}

		if(allocatedServices >= serviceAmount){
			return serviceDuration;
		}

		//If not found yet, get the best next value
		double lastValue = Double.POSITIVE_INFINITY;
		while(true){
			//Get largest value below lastValue
			double thisValue = 0;
			for(double d : availableVehicleTime){
				if(d > thisValue && d < lastValue) thisValue = d;
			}

			allocatedServices = 0;
			for (Double d : availableVehicleTime) {
				allocatedServices += (int) Math.floor(d / thisValue);
			}

			if(allocatedServices >= serviceAmount){
				return thisValue;
			}

			lastValue = thisValue;
		}
	}
}
