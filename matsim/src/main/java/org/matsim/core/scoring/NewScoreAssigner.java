package org.matsim.core.scoring;


import org.matsim.api.core.v01.population.Population;

interface NewScoreAssigner {

	void assignNewScores(int iteration, ScoringFunctionsForPopulation scoringFunctionsForPopulation, Population population);

}