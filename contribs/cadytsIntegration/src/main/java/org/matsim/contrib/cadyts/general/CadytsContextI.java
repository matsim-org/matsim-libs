package org.matsim.contrib.cadyts.general;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import org.matsim.api.core.v01.population.Person;

public interface CadytsContextI<T> {

	AnalyticalCalibrator<T> getCalibrator() ;

	PlansTranslator<T> getPlansTranslator();

	/**
	 * Returns the weight (e.g. PCU) of the agent for calibration purposes.
	 * Default is 1.0 (standard vehicle or PT passenger).
	 */
	default double getAgentWeight(Person person) {
		return 1.0;
	}
}
