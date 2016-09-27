package org.matsim.contrib.cadyts.general;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import org.matsim.core.config.Config;
import org.matsim.counts.Counts;

/**
 * Created by GabrielT on 27.09.2016.
 */
public interface CadytsBuilder {


	<T> AnalyticalCalibrator<T> buildCalibratorAndAddMeasurements(Config config, Counts<T> occupCounts,
																  LookUpItemFromId<T> lookUp, Class<T> idType);

	<T> AnalyticalCalibrator<T> buildCalibrator(Config config);
}
