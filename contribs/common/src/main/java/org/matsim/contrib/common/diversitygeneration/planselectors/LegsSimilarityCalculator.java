package org.matsim.contrib.common.diversitygeneration.planselectors;

import java.util.List;

import org.matsim.api.core.v01.population.Leg;

interface LegsSimilarityCalculator {
	double calculateSimilarity( List<Leg> legs1 , List<Leg> legs2 ) ;
}