/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.popsim;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.analysis.AnalyzerTask;
import playground.johannes.sna.math.DummyDiscretizer;
import playground.johannes.sna.math.Histogram;
import playground.johannes.sna.math.InterpolatingDiscretizer;
import playground.johannes.sna.math.LinearDiscretizer;
import playground.johannes.sna.util.TXTWriter;
import playground.johannes.socialnetworks.statistics.Correlations;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainPerson;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * @author johannes
 *
 */
public class AgeIncomeCorrelation extends AnalyzerTask {

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.analysis.AnalyzerTask#analyze(java.util.Collection, java.util.Map)
	 */
	@Override
	public void analyze(Collection<PlainPerson> persons, Map<String, DescriptiveStatistics> results) {
		TDoubleArrayList ages = new TDoubleArrayList();
		TDoubleArrayList incomes = new TDoubleArrayList();
		
		for(Person person : persons) {
			String aStr = person.getAttribute(CommonKeys.PERSON_AGE);
			String iStr = person.getAttribute(CommonKeys.HH_INCOME);
//			String mStr = person.getAttribute(CommonKeys.HH_MEMBERS);
			
//			if(aStr != null && iStr != null && mStr != null) {
			if(aStr != null && iStr != null) {
				double age = Double.parseDouble(aStr);
				double income = Double.parseDouble(iStr);
//				double members = Double.parseDouble(mStr);
				
				ages.add(age);
//				incomes.add(income/members);
				incomes.add(income);
			}
		}
		
		try {
//			TDoubleDoubleHashMap hist = Histogram.createHistogram(ages.toNativeArray(), new LinearDiscretizer(5), false);
			TDoubleDoubleHashMap hist = Histogram.createHistogram(ages.toNativeArray(), new DummyDiscretizer(), false);
			TXTWriter.writeMap(hist, "age", "n", getOutputDirectory() + "/age.txt");
			
			hist = Histogram.createHistogram(incomes.toNativeArray(), new LinearDiscretizer(500), false);
//			hist = Histogram.createHistogram(incomes.toNativeArray(), new InterpolatingDiscretizer(incomes.toNativeArray()), false);
			TXTWriter.writeMap(hist, "income", "n", getOutputDirectory() +  "/income.txt");
			
			TXTWriter.writeScatterPlot(ages, incomes, "age", "income", getOutputDirectory() + "/age.income.txt");
			
			TXTWriter.writeMap(Correlations.mean(ages.toNativeArray(), incomes.toNativeArray()), "age", "income", getOutputDirectory() + "/age.income.mean.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
