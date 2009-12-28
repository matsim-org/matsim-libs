/* *********************************************************************** *
 * project: org.matsim.*
 * IncomeChartUtils
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.dgrether.analysis.charts;

import playground.dgrether.analysis.population.DgAnalysisPopulation;
import playground.dgrether.analysis.population.DgPersonData;
import playground.dgrether.analysis.population.DgPlanData;


/**
 * @author dgrether
 *
 */
public class IncomeChartUtils {

	public static Double calcAverageMoneyDifference(DgAnalysisPopulation group, int groupThreshold) {
		Double deltaMoneySum = 0.0;
		double deltaM = 0.0;
		for (DgPersonData d : group.getPersonData().values()){
			DgPlanData planDataRun1 = d.getPlanData().get(DgAnalysisPopulation.RUNID1);
			DgPlanData planDataRun2 = d.getPlanData().get(DgAnalysisPopulation.RUNID2);
			deltaM = (planDataRun2.getScore() - planDataRun1.getScore()) / 4.58;
			deltaM = deltaM * d.getIncome().getIncome() / 240;
			deltaMoneySum += deltaM;
		}
		Double avg = null;
		if (group.getPersonData().size() > groupThreshold) {
			avg = deltaMoneySum/group.getPersonData().size();
		}
		return avg;
	}
	
}
