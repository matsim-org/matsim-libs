package org.matsim.smallScaleCommercialTrafficGeneration;

import java.util.*;

/**
 * A small allocator to solve the time-window problems in the Vehicle and Service generation. <br>
 * This implementation uses a Best-Fit Allocator.
 * <i>NOTE: This class does not actually change anything in the scenario. It is just a tool to check if Service and Vehicle TimeWindows are
 * compatible.</i>
 */
public class DefaultVehicleAvailabilityAllocator{
	private List<Integer> availableVehicleTime;

	/**
	 * Prepares the allocator.
	 * @param availableVehicleTime 	This Collection should contain the duration of available-time-frames of the vehicles.
	 *                          	For example: 4x vehicles are available from 1:00 to 4:00 (3 hours), then the {@code availableVehicles} Collection should
	 *                          	contain 4 entries with value: 3*3600=10800. If a vehicle has a non-coherent availability-time-frame, add it as two
	 *                          	separate entries.
	 */
	public DefaultVehicleAvailabilityAllocator(List<Integer> availableVehicleTime){
		this.availableVehicleTime = availableVehicleTime;
	}

	/**
	 * Checks if a vehicle is available for the given amount of time. If not, then the time is set to the largest possible duration,
	 * which can be allocated.
	 * @return the reduced serviceDuration (unchanged, if a vehicle was found, that was available for the full duration)
	 */
	public int makeServiceDurationViable(int serviceDuration){
		for(Integer vehicleTime : availableVehicleTime){
			if(vehicleTime >= serviceDuration) return serviceDuration;
		}
		return Collections.max(availableVehicleTime);
	}

	/**
	 * Tries to allocate a single vehicle to the service and reduces the allocated vehicle available time by the serviceDuration.
	 * If no vehicle is available nothing happens. You should then consider to reduce the duration with {@link DefaultVehicleAvailabilityAllocator#makeServiceDurationViable}
	 * @return true if a vehicle was allocated, false if no vehicle is available for the given duration
	 */
	public boolean scheduleServiceDuration(int serviceDuration){
		//Best-Fit Allocation
		int bestFit = -1;
		int bestRemaining = Integer.MAX_VALUE;
		for(int i = 0; i < availableVehicleTime.size(); i++){
			int remaining = availableVehicleTime.get(i) - serviceDuration;
			if(remaining > 0 && remaining < bestRemaining){
				bestFit = i;
				bestRemaining = remaining;
			}
		}
		if(bestFit == -1) return false;
		//Allocate
		availableVehicleTime.set(bestFit, availableVehicleTime.get(bestFit) - serviceDuration);
		return true;
	}
}
