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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.math.stat.descriptive.rank.Percentile;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.ListUtils;
import playground.agarwalamit.utils.MapUtils;
import playground.agarwalamit.utils.NumberUtils;

/**
 * @author amit
 */

public class IncomeDistributionGenerator {

	private final String dir = 
			//PatnaUtils.INPUT_FILES_DIR+"/simulationInputs/urban/shpNetwork/";
	"../../../../repos/runs-svn/patnaIndia/run108/jointDemand/calibration/shpNetwork/multiModalCadytsAndIncome/c7/";
	private final int iterationNumber = 200;
		private final String plansFile = dir+"/ITERS/it."+iterationNumber+"/"+iterationNumber+".plans.xml.gz";
//	private final String plansFile = PatnaUtils.INPUT_FILES_DIR+"/simulationInputs/urban/shpNetwork/initial_urban_plans_1pct.xml.gz";
		private final String personAttributeFile = dir+ "output_personAttributes.xml.gz";
//	private final String personAttributeFile = PatnaUtils.INPUT_FILES_DIR+"/simulationInputs/urban/shpNetwork/initial_urban_persionAttributes_1pct.xml.gz";

	private final SortedMap<Double, SortedMap<String, Integer>> avgInc2mode2Count = new TreeMap<>();

	public static void main(String[] args) {
		IncomeDistributionGenerator idg = new IncomeDistributionGenerator();
		idg.parseFile();
		idg.writeData();
		idg.printStats();
	}

	private void writeData(){
				try(BufferedWriter writer = IOUtils.getBufferedWriter(dir+"/analysis/avgIncToCount_it."+iterationNumber+".txt")) {
//		try(BufferedWriter writer = IOUtils.getBufferedWriter(dir+"/avgIncToCount_1pct.txt")) {
			writer.write("avgIncomUSD\tmode\tcount\tshareOfModeInPct\tshareOfIncGrpInPct\n");

			Map<String,Double> mode2Legs = new HashMap<>();
			for (Double d : this.avgInc2mode2Count.keySet()){
				for(String str : this.avgInc2mode2Count.get(d).keySet()){
					if (mode2Legs.containsKey(str)) mode2Legs.put(str, mode2Legs.get(str)+this.avgInc2mode2Count.get(d).get(str));
					else  mode2Legs.put(str, (double) this.avgInc2mode2Count.get(d).get(str));
				}
			}
			
			for (Double d : this.avgInc2mode2Count.keySet()){
				double incSum = MapUtils.intValueSum(this.avgInc2mode2Count.get(d));
				for(String str : this.avgInc2mode2Count.get(d).keySet()){
					double sum = mode2Legs.get(str);
					double count = this.avgInc2mode2Count.get(d).get(str);
					writer.write(Math.round(d/PatnaUtils.INR_USD_RATE)+"\t"+str+"\t"+count
							+"\t"+NumberUtils.round(100*count/sum, 2)+
							"\t"+NumberUtils.round(100*count/incSum, 2)+"\n");
				}
				//writer.write(Math.round(d/USD2INRRate)+"\t"+"total\t"+MapUtils.intValueSum(this.avgInc2mode2Count.get(d))+"\n");
			}
			writer.close();

		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason :"+e);
		}
	}

	private void printStats(){
		Map<String,List<Double> > mode2list = new HashMap<>();
		List<Double> allModesList= new ArrayList<>();

		for(Double d : this.avgInc2mode2Count.keySet()){
			for(String mode : this.avgInc2mode2Count.get(d).keySet()) {
				List<Double> list = mode2list.get(mode);
				if (list==null) list = new ArrayList<>();
				for (int i=0; i< this.avgInc2mode2Count.get(d).get(mode);i++) {
					list.add(d);
					allModesList.add(d);
				}
				mode2list.put(mode, list);
			}
		}

		Percentile ptile = new Percentile();
		for(String mode : mode2list.keySet()) {
			List<Double> list = mode2list.get(mode);
			double d [] = new double [list.size()];
			for(int index = 0 ; index < list.size(); index++){
				d[index] = list.get(index);
			}
			System.out.println(list.size());
			System.out.println("The mean of incomes for mode "+mode+ " is "+ ListUtils.doubleMean(list));
			System.out.println("The 25%, 50% and 75% quartiles for the mode " + mode + " are "+ NumberUtils.quartile(d, 25)+ "\t"
			+ NumberUtils.quartile(d, 50) + "\t"+  NumberUtils.quartile(d, 74));
			System.out.println("The 25%, 50% and 75% quartiles for the mode " + mode + " are "+
			ptile.evaluate(d,25) +"\t"+ptile.evaluate(d,50)+"\t"+ptile.evaluate(d,75));
		}

		// all modes
		double dAllModes [] = new double [allModesList.size()];
		for(int index = 0 ; index < allModesList.size(); index++){
			dAllModes[index] = allModesList.get(index);
		}
		System.out.println(allModesList.size());
		System.out.println("The mean of incomes for all modes is "+ ListUtils.doubleMean(allModesList));
		System.out.println("The 25%, 50% and 75% quartiles for all modes are "+ NumberUtils.quartile(dAllModes, 25)+ "\t"
		+ NumberUtils.quartile(dAllModes, 50) + "\t"+  NumberUtils.quartile(dAllModes, 74));
		System.out.println("The 25%, 50% and 75% quartiles for all modes are "+
		ptile.evaluate(dAllModes,25) +"\t"+ptile.evaluate(dAllModes,50)+"\t"+ptile.evaluate(dAllModes,75));
		
	}

	private void parseFile(){
		Config config = ConfigUtils.createConfig();
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