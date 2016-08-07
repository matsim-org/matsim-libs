/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.patnaIndia.analysis;

import java.io.BufferedWriter;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;

/**
 * @author amit
 */

public class IncomeDistributionFromEventsGenerator {

	private final String dir = "../../../../repos/runs-svn/patnaIndia/run108/jointDemand/calibration/shpNetwork/incomeDependent/c13/";
	private final int iterationNumber = 100;
	private final String plansFile = dir+"/ITERS/it."+iterationNumber+"/"+iterationNumber+".plans.xml.gz";
	private final String personAttributeFile = dir+ "output_personAttributes.xml.gz";
	
	private final SortedMap<Double, SortedMap<String, Integer>> avgInc2mode2Count = new TreeMap<>();
	private final double USD2INRRate = 66.6; // 08 June 2016 

	public static void main(String[] args) {
		IncomeDistributionFromEventsGenerator idg = new IncomeDistributionFromEventsGenerator();
		idg.parseFile();
		idg.writeData();
	}

	private void writeData(){
		try(BufferedWriter writer = IOUtils.getBufferedWriter(dir+"/analysis/avgIncToCount_it."+iterationNumber+".txt")) {
			writer.write("avgIncomUSD\tmode\tcount\n");

			for (Double d : this.avgInc2mode2Count.keySet()){
				for(String str : this.avgInc2mode2Count.get(d).keySet()){
					writer.write(Math.round(d/USD2INRRate)+"\t"+str+"\t"+this.avgInc2mode2Count.get(d).get(str)+"\n");
				}
//				writer.write(Math.round(d/USD2INRRate)+"\t"+"total\t"+MapUtils.intValueSum(this.avgInc2mode2Count.get(d))+"\n");
			}
			writer.close();

		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason :"+e);
		}
	}

	private void parseFile(){
		Config config = ConfigUtils.createConfig();
		config.addCoreModules();
		config.plans().setInputFile(plansFile);
		config.plans().setInputPersonAttributeFile(personAttributeFile);
		
		Scenario sc = ScenarioUtils.loadScenario(config);
		
		for (Person p: sc.getPopulation().getPersons().values()) {

			if(! PatnaPersonFilter.isPersonBelongsToUrban(p.getId())) continue;

			String mode =  ((Leg) p.getSelectedPlan().getPlanElements().get(1)).getMode();

			double inc = (double) sc.getPopulation().getPersonAttributes().getAttribute(p.getId().toString(), PatnaUtils.INCOME_ATTRIBUTE);
			SortedMap<String, Integer > mode2counter = this.avgInc2mode2Count.get(inc);

			if(mode2counter==null) {
				mode2counter = new TreeMap<>();
				mode2counter.put(mode, 1);
				this.avgInc2mode2Count.put(inc, mode2counter);
			} else {
				if (mode2counter.containsKey(mode)) mode2counter.put(mode, mode2counter.get(mode)+1);
				else mode2counter.put(mode, 1);
			}
		}
	}
}