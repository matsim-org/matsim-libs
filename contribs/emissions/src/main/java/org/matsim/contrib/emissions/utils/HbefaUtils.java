package org.matsim.contrib.emissions.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

/** Utilities for correcting common HBEFA vehicle-type configuration errors. */
public final class HbefaUtils {
	public static final String AVERAGE = "average";

	private static Logger log = LogManager.getLogger(HbefaUtils.class);

	private HbefaUtils() {
	}

	/**
	 * Corrects vehicle types whose HBEFA technology and emission concept values were swapped.
	 *
	 * <p>Vehicle types with an {@code AVERAGE} technology and a non-average emission concept
	 * are corrected by switching the two values. Vehicle types with non-average values for both
	 * fields are invalid and cause an {@link IllegalArgumentException}. Vehicle types with a
	 * missing technology or emission concept are ignored.</p>
	 */
	public static void checkAndCorrectHbefaTechnologyAndEmissionConcept(Scenario scenario) {
		checkAndCorrectHbefaTechnologyAndEmissionConcept(scenario.getVehicles());
	}

	/**
	 * Corrects vehicle types whose HBEFA technology and emission concept values were swapped.
	 *
	 * <p>Vehicle types with an {@code AVERAGE} technology and a non-average emission concept
	 * are corrected by switching the two values. Vehicle types with non-average values for both
	 * fields are invalid and cause an {@link IllegalArgumentException}. Vehicle types with a
	 * missing technology or emission concept are ignored.</p>
	 */
	public static void checkAndCorrectHbefaTechnologyAndEmissionConcept(Vehicles vehicles) {
		for (VehicleType vehicleType : vehicles.getVehicleTypes().values()) {
			String technology = VehicleUtils.getHbefaTechnology(vehicleType.getEngineInformation());
			String emissionConcept = VehicleUtils.getHbefaEmissionsConcept(vehicleType.getEngineInformation());

			if (technology == null || emissionConcept == null) {
				log.info("No hbefa technology or emissionConcept present for vehicle type {}. Skipping.",  vehicleType.getId());
				continue;
			}
			boolean averageTechnology = AVERAGE.equals(technology);
			boolean averageEmissionConcept = AVERAGE.equals(emissionConcept);

//			we allow technology = emissionConcept = average
//			we swap technology and emissionConcept when tech = average and concept != average
			if (averageTechnology && !averageEmissionConcept) {
				log.info("Most likely HbefaTechnology {} and HbefaEmissionConcept for vehicle type {} are swapped. Will swap them back." +
					"Correct emission analysis relies on HbefaTechnology, so this should be the detailed param.", technology, emissionConcept, vehicleType.getId());
				VehicleUtils.setHbefaTechnology(vehicleType.getEngineInformation(), emissionConcept);
				VehicleUtils.setHbefaEmissionsConcept(vehicleType.getEngineInformation(), technology);
//				if both != average we have to sort it out manually
			} else if (!averageTechnology && !averageEmissionConcept) {
				log.fatal("Vehicle type {} has non-average HBEFA technology AND emission concept values. " +
					"Please check and fix the engineInformation of your vehicle types manually.", vehicleType.getId());
				throw new IllegalArgumentException("");
			}
		}
	}
}
