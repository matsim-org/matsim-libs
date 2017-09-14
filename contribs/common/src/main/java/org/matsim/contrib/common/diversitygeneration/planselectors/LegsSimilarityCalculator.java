package org.matsim.contrib.common.diversitygeneration.planselectors;

import java.util.List;

import org.matsim.api.core.v01.population.Leg;

public interface LegsSimilarityCalculator {
	// yy kwa points to: 
	// Schüssler, N. and K.W. Axhausen (2009b) Accounting for similarities in destination choice modelling: A concept, paper presented at the 9th Swiss Transport Research Conference, Ascona, October 2009.
	//		und 
	//  Joh, Chang-Hyeon, Theo A. Arentze and Harry J. P. Timmermans (2001). 
	// A Position-Sensitive Sequence Alignment Method Illustrated for Space-Time Activity-Diary Data¹, Environment and Planning A 33(2): 313­338.

	// Mahdieh Allahviranloo has some work on activity pattern similarity (iatbr'15)

	
	double calculateSimilarity( List<Leg> legs1 , List<Leg> legs2 ) ;
}