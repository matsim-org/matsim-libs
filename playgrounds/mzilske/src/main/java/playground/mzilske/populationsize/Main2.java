/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * Main2.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.mzilske.populationsize;

public class Main2 {

	public static void main(String[] args) {
		final ExperimentResource experiment = new ExperimentResource("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/");
		final RegimeResource uncongested = experiment.getRegime("uncongested");


//        uncongested.getMultiRateRun("cutoffexact").cutOffExact(1.0, 1.0);

//        uncongested.getMultiRateRun("cutoffexact").simulateRate("100-100", 3);
//        uncongested.getMultiRateRun("cutoff").simulateRate("100-100", 3);
//        uncongested.getMultiRateRun("cutoff").simulateRate("100-0", 3);
//        uncongested.getMultiRateRun("cutoff").simulateRate("50-50", 3);
//        uncongested.getMultiRateRun("cutoff").simulateRate("70-30", 3);
//        uncongested.getMultiRateRun("cutoff").simulateRate("90-10", 3);
//        uncongested.getMultiRateRun("cutoff").simulateRate("90-10", 10);


//        uncongested.getMultiRateRun("brute-corrected-prior").simulateRate("0", 3, 100.0);
//        uncongested.getMultiRateRun("brute-corrected-prior").simulateRate("0", 3, 100.0);
        uncongested.getMultiRateRun("brute").simulateRate("5", 2, 100.0);


//        uncongested.getMultiRateRun("random").persodisthisto();
//        uncongested.getMultiRateRun("random").summary();


//
//        uncongested.getMultiRateRun("cutoff").cutoffRate(1.0, 0.5);
//        uncongested.getMultiRateRun("cutoff").simulateRate("100-50", 3);
//        uncongested.getMultiRateRun("cutoff").simulateRate("90-10", 10);



//        uncongested.getMultiRateRun("random").twoRatesRandom("5");
//        uncongested.getMultiRateRun("random").twoRatesRandom("0");

//        uncongested.getMultiRateRun("wurst").simulateRate("0", 10);
//        uncongested.getMultiRateRun("random").simulateRate("0", 3, 100.0);
//        uncongested.getMultiRateRun("wurst").twoRates("5");
//        uncongested.getMultiRateRun("wurst").simulateRate("5", 10);
//        uncongested.getMultiRateRun("random").simulateRate("5", 3, 100.0);

//        uncongested.getMultiRateRun("wurst").twoRates("0");
//        uncongested.getMultiRateRun("wurst").simulateRate("0", 10);


//        uncongested.getMultiRateRun("cutoff").summary();
//        uncongested.getMultiRateRun("wurst").summary();
//        Scenario baseScenario = uncongested.getBaseRun().getOutputScenario();
//        Population basePopulation = baseScenario.getPopulation();

//        Map<Id, Double> travelledDistancePerPerson = PowerPlans.travelledDistancePerPerson(basePopulation, baseScenario.getNetwork());
//        CloneHistogram.cloneHistogram(basePopulation, travelledDistancePerPerson, uncongested.getMultiRateRun("cadyts").getRateRun("twotimes_0", "10"));
//        CloneHistogram.cloneHistogram(basePopulation, travelledDistancePerPerson, uncongested.getMultiRateRun("cadyts").getRateRun("two_5", "10"));
//        CloneHistogram.cloneHistogram(basePopulation, travelledDistancePerPerson, uncongested.getMultiRateRun("cadyts").getRateRun("two_10", "10"));

//        CloneHistogram.cloneHistogram(basePopulation, travelledDistancePerPerson, uncongested.getMultiRateRun("cadyts").getRateRun("two_20", "5"));
//        CloneHistogram.cloneHistogram(basePopulation, travelledDistancePerPerson, uncongested.getMultiRateRun("cadyts").getRateRun("two_50", "5"));


//        uncongested.getMultiRateRun("cadyts").twoRates("5");


//        uncongested.getMultiRateRun("cadyts").twoRates("0");


//        uncongested.getMultiRateRun("cadyts").twoRates("5");

//        uncongested.getMultiRateRun("cadyts").simulateRate("two_0", 5);

//        uncongested.getMultiRateRun("cadyts").simulateRate("twotimes_5", 1);

//        uncongested.getMultiRateRun("cadyts").simulateRate("twotimes_50", 1);

//        uncongested.getMultiRateRun("cadyts").simulateRate("twotimes_0", 1);
//        uncongested.getMultiRateRun("cadyts").simulateRate("two_10", 5);
//        uncongested.getMultiRateRun("cadyts").twoRates("20");
//        uncongested.getMultiRateRun("cadyts").simulateRate("two_20", 5);
//        uncongested.getMultiRateRun("cadyts").twoRates("50");
//        uncongested.getMultiRateRun("cadyts").simulateRate("two_50", 5);

//        uncongested.getMultiRateRun("cadyts").simulateRate("two_0", 10);

//        uncongested.getMultiRateRun("cadyts").simulateRate("two_5", 10);
//        uncongested.getMultiRateRun("cadyts").simulateRate("two_10", 10);
//        uncongested.getMultiRateRun("cadyts").simulateRate("two_20", 10);

//        uncongested.getMultiRateRun("cadyts").twoRatesRolling("50");

//        uncongested.getMultiRateRun("cadyts").simulateRate("twotwotwo_50", 1);



//       uncongested.getMultiRateRun("cadyts").getRateRun("two_5", "5").cloneStatistics();
//         uncongested.getMultiRateRun("cadyts").distances2();

//         uncongested.getMultiRateRun("cadyts").errors();
	}

}
