package org.matsim.contrib.common.diversitygeneration.planselectors;

import java.util.List;

import org.matsim.api.core.v01.population.Activity;

interface ActivitiesSimilarityCalculator {
	double calculateSimilarity( List<Activity> activities1 , List<Activity> activities2 ) ;
}