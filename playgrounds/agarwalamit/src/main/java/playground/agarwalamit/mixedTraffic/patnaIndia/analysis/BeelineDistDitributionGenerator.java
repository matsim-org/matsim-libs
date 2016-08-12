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

import playground.agarwalamit.analysis.legMode.distributions.LegModeBeelineDistanceDistributionHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;

/**
 * @author amit
 */

public class BeelineDistDitributionGenerator {

	private final String dir = "../../../../repos/runs-svn/patnaIndia/run108/jointDemand/calibration/shpNetwork/multiModalCadytsAndIncome/c5/";
	private final int iterationNumber = 0;
	private final String plansFile = dir+"/ITERS/it."+iterationNumber+"/"+iterationNumber+".plans.xml.gz";
	private final String personAttributeFile = dir+ "output_personAttributes.xml.gz";

	private final List<Double> distanceClasses = new ArrayList<>(Arrays.asList( 2000., 4000., 6000., 8000., 10000., Double.MAX_VALUE ));


	public static void main(String[] args) {
		new BeelineDistDitributionGenerator().run();
	}

	private void run(){

		LegModeBeelineDistanceDistributionHandler beelineCalculator = new LegModeBeelineDistanceDistributionHandler();

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
		for(String mode : mode2id2dists.keySet()) {
			SortedMap<Double, Integer> clas2count = new TreeMap<>();
			for (Double d : distanceClasses) {
				clas2count.put(d, 0);
			}
			mode2class2count.put(mode, clas2count);
		}

		// get the data
		for(String mode : mode2id2dists.keySet()) {
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
			writer.write("mode\tdistanceClass\tcount\n");
			for(String mode : mode2class2count.keySet()){
				for(Double d  : mode2class2count.get(mode).keySet()){
					writer.write(mode+"\t"+d+"\t"+mode2class2count.get(mode).get(d)+"\n");					
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
		for(String mode : mode2id2dists.keySet()) {
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
		for(String mode : mode2id2dists.keySet()) {
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
						writer.write(mode+"\t"+inc+"\t"+d+"\t"+mode2inc2distcounter.get(mode).get(inc).get(d)+"\n");					
					}
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason :"+e);
		}

	}
}
