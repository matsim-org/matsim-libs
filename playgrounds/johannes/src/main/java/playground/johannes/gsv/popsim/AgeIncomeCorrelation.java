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

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import org.matsim.contrib.common.stats.Correlations;
import org.matsim.contrib.common.stats.StatsWriter;
import playground.johannes.synpop.analysis.AnalyzerTask;
import playground.johannes.synpop.analysis.FileIOContext;
import playground.johannes.synpop.analysis.StatsContainer;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Person;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * @author johannes
 *
 */
public class AgeIncomeCorrelation implements AnalyzerTask<Collection<? extends Person>> {

	private final FileIOContext ioContext;

	public AgeIncomeCorrelation(FileIOContext ioContext) {
		this.ioContext = ioContext;
	}

	@Override
	public void analyze(Collection<? extends Person> persons, List<StatsContainer> containers) {
		TDoubleArrayList ages = new TDoubleArrayList();
		TDoubleArrayList incomes = new TDoubleArrayList();

		for(Person person : persons) {
			String aStr = person.getAttribute(CommonKeys.PERSON_AGE);
			String iStr = person.getAttribute(CommonKeys.HH_INCOME);
			if(aStr != null && iStr != null) {
				double age = Double.parseDouble(aStr);
				double income = Double.parseDouble(iStr);

				ages.add(age);
				incomes.add(income);
			}
		}

		TDoubleDoubleHashMap correl = Correlations.mean(ages.toArray(), incomes.toArray());
		try {
			StatsWriter.writeHistogram(correl, "age", "income", String.format("%s/age-income.txt", ioContext.getPath()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
