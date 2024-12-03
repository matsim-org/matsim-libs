package org.matsim.smallScaleCommercialTrafficGeneration.data;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.matsim.smallScaleCommercialTrafficGeneration.GenerateSmallScaleCommercialTrafficDemand;

import java.util.Map;

public interface CommercialTourSpecifications {

	/**
	 * Creates the probability distribution for the duration of the services.
	 * The values are given in [min] and have an upperBound.
	 *
	 * @return the probability distribution for the duration of the services
	 */
	Map<GenerateSmallScaleCommercialTrafficDemand.ServiceDurationPerCategoryKey, EnumeratedDistribution<GenerateSmallScaleCommercialTrafficDemand.DurationsBounds>> createStopDurationDistributionPerCategory(
		RandomGenerator rng);

	/**
	 * Creates the distribution of the tour start and the related duration.
	 *
	 * @return the distribution of the tour start and the related duration
	 */
	Map<String, EnumeratedDistribution<GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration>> createTourDistribution(RandomGenerator rng);

	/**
	 * Creates the probability distribution for the tour start times for the day.
	 * The values are given in [h] and have an upperBound.
	 *
	 * @return the probability distribution for the tour start times
	 */

	@Deprecated //use createTourDistribution(String smallScaleCommercialTrafficType) instead
	EnumeratedDistribution<GenerateSmallScaleCommercialTrafficDemand.DurationsBounds> createTourStartTimeDistribution(String smallScaleCommercialTrafficType, RandomGenerator rng);

	/**
	 * Creates the probability distribution for the tour duration for the day.
	 * The values are given in [h] and have an upperBound.
	 *
	 * @return the probability distribution for the tour duration
	 */
	@Deprecated //use createTourDistribution(String smallScaleCommercialTrafficType) instead
	EnumeratedDistribution<GenerateSmallScaleCommercialTrafficDemand.DurationsBounds> createTourDurationTimeDistribution(String smallScaleCommercialTrafficType, RandomGenerator rng);
}
