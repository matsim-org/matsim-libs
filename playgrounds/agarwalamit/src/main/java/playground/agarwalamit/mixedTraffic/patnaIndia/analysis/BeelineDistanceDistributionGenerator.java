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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.tripDistance.LegModeBeelineDistanceDistributionFromPlansAnalyzer;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.MapUtils;
import playground.agarwalamit.utils.NumberUtils;

/**
 * @author amit
 */

public class BeelineDistanceDistributionGenerator {

	private final String dir = "../../repos/runs-svn/patnaIndia/run108/jointDemand/calibration/"+PatnaUtils.PCU_2W+"pcu/afterCadyts/c203_14/";
	private final int iterationNumber = 1200;
	private final String plansFile = dir+"/ITERS/it."+iterationNumber+"/"+iterationNumber+".plans.xml.gz";
	private final String personAttributeFile = dir+ "output_personAttributes.xml.gz";

	private final List<Double> distanceClasses = new ArrayList<>(Arrays.asList( 2000., 4000., 6000., 8000., 10000., Double.MAX_VALUE ));
	private final Map<Double,String> distanceClass2Labels = new HashMap<>();

	public static void main(String[] args) {
		new BeelineDistanceDistributionGenerator().run();
	}

	private void run(){

		distanceClass2Labels.put(2000.0, "0-2");
		distanceClass2Labels.put(4000.0, "2-4");
		distanceClass2Labels.put(6000.0, "4-6");
		distanceClass2Labels.put(8000.0, "6-8");
		distanceClass2Labels.put(10000.0, "8-10");
		distanceClass2Labels.put(Double.MAX_VALUE, "10+");
		
		LegModeBeelineDistanceDistributionFromPlansAnalyzer beelineCalculator = new LegModeBeelineDistanceDistributionFromPlansAnalyzer();

		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(plansFile);
		config.plans().setInputPersonAttributeFile(personAttributeFile);

		Scenario sc = ScenarioUtils.loadScenario(config);

		beelineCalculator.init(sc);
		beelineCalculator.preProcessData();
		beelineCalculator.postProcessData();

		SortedMap<String, Map<Id<Person>, List<Double>>>mode2id2dists = beelineCalculator.getMode2PersonId2RouteDistances();
		SortedMap<String,SortedMap<Double,Integer>> mode2class2count = new TreeMap<>();

		// initialize
		for(String mode : PatnaUtils.URBAN_ALL_MODES) {
			SortedMap<Double, Integer> clas2count = new TreeMap<>();
			for (Double d : distanceClasses) {
				clas2count.put(d, 0);
			}
			mode2class2count.put(mode, clas2count);
		}

		// get the data
		for(String mode : PatnaUtils.URBAN_ALL_MODES) {
			for (Id<Person> pId : mode2id2dists.get(mode).keySet()){
				if ( ! PatnaPersonFilter.isPersonBelongsToUrban(pId) ) continue;
				for (Double d : mode2id2dists.get(mode).get(pId)){
					SortedMap<Double, Integer> clas2count = mode2class2count.get(mode);
					for(Double dClass : distanceClasses) {
						if (d <= dClass ) {
							clas2count.put(dClass, clas2count.get(dClass)+1);
							mode2class2count.put(mode, clas2count);
							break;
						}
					}
				}
			}
		}

		// write it
		try (BufferedWriter writer = IOUtils.getBufferedWriter(dir+"/analysis/modalDistanceDistribution.it."+iterationNumber+".txt")) {
			writer.write("mode\tdistanceClass\tcount\tshare\n");
			for(String mode : mode2class2count.keySet()){
				for(Double d  : mode2class2count.get(mode).keySet()){
					double sum = getDistClassSum(mode2class2count, d);
					Integer count = mode2class2count.get(mode).get(d);
					writer.write(mode+"\t"+distanceClass2Labels.get(d)+"\t"+count+"\t"+NumberUtils.round(count*100/sum, 2)+"\n");					
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason :"+e);
		}

		// income dependent
		Map<Id<Person>, Double> person2income = new HashMap<>();
		for (Person p : sc.getPopulation().getPersons().values()){
			if ( ! PatnaPersonFilter.isPersonBelongsToUrban(p.getId()) ) continue;
			double inc = (double) sc.getPopulation().getPersonAttributes().getAttribute(p.getId().toString(), PatnaUtils.INCOME_ATTRIBUTE);
			person2income.put(p.getId(), inc);
		}

		SortedMap<String, SortedMap<Double, SortedMap<Double, Integer>>> mode2inc2distcounter = new TreeMap<>();
		// initialize
		for(String mode : PatnaUtils.URBAN_ALL_MODES) {
			SortedMap<Double, SortedMap<Double,Integer>> inc2dist2count = new TreeMap<>();
			for( Double inc : person2income.values()) {
				SortedMap<Double, Integer> dist2count = new TreeMap<>();
				for (Double dist : distanceClasses) {
					dist2count.put(dist, 0);
				}
				inc2dist2count.put(inc, dist2count);
			}
			mode2inc2distcounter.put(mode, inc2dist2count);
		}

		// get the data
		for(String mode : PatnaUtils.URBAN_ALL_MODES) {
			for (Id<Person> pId : mode2id2dists.get(mode).keySet()){
				if ( ! PatnaPersonFilter.isPersonBelongsToUrban(pId) ) continue;
				double inc = person2income.get(pId);
				for (Double d : mode2id2dists.get(mode).get(pId)){
					SortedMap<Double, Integer> clas2count = mode2inc2distcounter.get(mode).get(inc);
					for(Double dClass : distanceClasses) {
						if (d <= dClass ) {
							clas2count.put(dClass, clas2count.get(dClass)+1);
							break;
						}
					}
				}
			}
		}

		// write it
		try (BufferedWriter writer = IOUtils.getBufferedWriter(dir+"/analysis/modalIncomeDistanceDistribution.it."+iterationNumber+".txt")) {
			writer.write("mode\tincomeClass\tdistanceClass\tcount\n");
			for(String mode : mode2inc2distcounter.keySet()){
				for (Double inc : mode2inc2distcounter.get(mode).keySet()) {
					for(Double d  : mode2inc2distcounter.get(mode).get(inc).keySet()){
						writer.write(mode+"\t"+Math.round(inc/PatnaUtils.INR_USD_RATE)+"\t"+distanceClass2Labels.get(d)+"\t"+mode2inc2distcounter.get(mode).get(inc).get(d)+"\n");					
					}
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason :"+e);
		}
	}
	
	private double getDistClassSum(SortedMap<String,SortedMap<Double,Integer>> mode2class2count, Double distanceClass){
		SortedMap<String,Integer> mode2counter = new TreeMap<>();
		for (String mode : mode2class2count.keySet()) {
			int count = mode2class2count.get(mode).get(distanceClass);
			 if (mode2counter.containsKey(mode) ) mode2counter.put(mode, mode2counter.get(mode)+count);
			 else mode2counter.put(mode, count);
		}
		return (double) MapUtils.intValueSum(mode2counter);
	}
}
