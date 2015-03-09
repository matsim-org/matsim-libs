/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyseFacilities.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.staheale.analysis;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.MatsimFacilitiesReader;

public class AnalyseFacilities {
	private static Logger log = Logger.getLogger(AnalyseFacilities.class);
	double sumHomeDur = 0.0;
	double sumWorkDur = 0.0;
	double sumShopRetailDur = 0.0;
	double sumShopServiceDur = 0.0;
	double sumSportsFunDur = 0.0;
	double sumGastroCultureDur = 0.0;
	double sumTravelTime = 0.0;

	int workCount = 0;
	int shopRetailCount = 0;
	int shopServiceCount = 0;
	int sportsFunCount = 0;
	int gastroCultureCount = 0;
	int legCount = 0;
	boolean line = false;

	public AnalyseFacilities() {
		super();	
	}

	public static void main(String[] args) throws IOException {
		AnalyseFacilities analyseFacilities = new AnalyseFacilities();
		analyseFacilities.run();
	}

	public void run() {

		final ScenarioImpl scenarioPlans = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		MatsimNetworkReader NetworkReader = new MatsimNetworkReader(scenarioPlans);
		NetworkReader.readFile("./input/miniScenarioNetwork.xml");

		MatsimFacilitiesReader FacReader = new MatsimFacilitiesReader((ScenarioImpl) scenarioPlans);  
		FacReader.readFile("./input/miniScenarioFacilities.xml");

		log.info("Reading plans xml file...");
		MatsimPopulationReader PlansReader = new MatsimPopulationReader(scenarioPlans); 
		PlansReader.readFile("./input/run0.500.plans.xml.gz");
		System.out.println("Reading plans xml file...done.");

		//	log.info("number of nodes: " +scenarioPlans.getNetwork().getNodes().size());
		//	log.info("number of links: " +scenarioPlans.getNetwork().getLinks().size());
		//	
		//	log.info("number of home facilities: " +scenarioPlans.getActivityFacilities().getFacilitiesForActivityType("home").size());
		//	log.info("number of work facilities: " +scenarioPlans.getActivityFacilities().getFacilitiesForActivityType("work").size());
		//	log.info("number of education facilities: " +scenarioPlans.getActivityFacilities().getFacilitiesForActivityType("education").size());
		//	log.info("number of shop retail facilities: " +scenarioPlans.getActivityFacilities().getFacilitiesForActivityType("shop_retail").size());
		//	log.info("number of shop service facilities: " +scenarioPlans.getActivityFacilities().getFacilitiesForActivityType("shop_service").size());
		//	log.info("number of sports & fun facilities: " +scenarioPlans.getActivityFacilities().getFacilitiesForActivityType("sports_fun").size());
		//	log.info("number of gastro & culture facilities: " +scenarioPlans.getActivityFacilities().getFacilitiesForActivityType("gastro_culture").size());

		try {

			final String header="Plan_id\tact_type\tduration";
			final BufferedWriter out =
					IOUtils.getBufferedWriter("./output/duration.txt");

			out.write(header);
			out.newLine();

			for (Person p : scenarioPlans.getPopulation().getPersons().values()) {
				for (PlanElement pe : p.getSelectedPlan().getPlanElements()){
					if (pe instanceof Leg){
						LegImpl leg = (LegImpl)pe;
						out.write(p.getId().toString() + "\t"+
								"travelling\t"+
								leg.getTravelTime()
								+ "\t");
						line = true;
					}
					if (pe instanceof Activity) {
						ActivityImpl act = (ActivityImpl)pe;  
						if (act.getType().startsWith("work")) {
							out.write(p.getId().toString() + "\t"+
									"work\t"+
									act.getMaximumDuration()
									+ "\t");
							line = true;
						}
						if (act.getType().startsWith("shop_ret")) {
							out.write(p.getId().toString() + "\t"+
									"shop_retail\t"+
									act.getMaximumDuration()
									+ "\t");
							line = true;
						}
						if (act.getType().startsWith("shop_ser")) {
							out.write(p.getId().toString() + "\t"+
									"shop_service\t"+
									act.getMaximumDuration()
									+ "\t");
							line = true;
						}
						if (act.getType().startsWith("leisure_sports")) {
							out.write(p.getId().toString() + "\t"+
									"leisure_sports_fun\t"+
									act.getMaximumDuration()
									+ "\t");
							line = true;
						}
						if (act.getType().startsWith("leisure_gastro")) {
							out.write(p.getId().toString() + "\t"+
									"leisure_gastro_culture\t"+
									act.getMaximumDuration()
									+ "\t");
							line = true;
						}
					}
					if (line == true){
						out.newLine();
					}
				}


			}
			out.flush();
			out.close();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}





		for (Person p : scenarioPlans.getPopulation().getPersons().values()) {
			for (PlanElement pe : p.getSelectedPlan().getPlanElements()){
				if (pe instanceof Leg){
					LegImpl leg = (LegImpl)pe;
					sumTravelTime += leg.getTravelTime()/60;
					legCount += 1;
				}
				if (pe instanceof Activity) {
					ActivityImpl act = (ActivityImpl)pe;  
					if (act.getType().startsWith("work")) {
						double actDur = 0.0;
						actDur = (act.getEndTime()-act.getStartTime())/60;
						if (actDur < 1200){
							sumWorkDur += actDur;
							workCount += 1;
						}
					}
					if (act.getType().startsWith("shop_ret")) {
						double actDur = 0.0;
						actDur = (act.getEndTime()-act.getStartTime())/60;
						if (actDur < 540){
							sumShopRetailDur += actDur;
							shopRetailCount += 1;
						}
					}
					if (act.getType().startsWith("shop_ser")) {
						double actDur = 0.0;
						actDur = (act.getEndTime()-act.getStartTime())/60;
						if (actDur < 540){
							sumShopServiceDur += actDur;
							shopServiceCount += 1;
						}
					}
					if (act.getType().startsWith("leisure_sports")) {
						double actDur = 0.0;
						actDur = (act.getEndTime()-act.getStartTime())/60;
						if (actDur < 540){
							sumSportsFunDur += actDur;
							sportsFunCount += 1;
						}
					}
					if (act.getType().startsWith("leisure_gastro")) {
						double actDur = 0.0;
						actDur = (act.getEndTime()-act.getStartTime())/60;
						if (actDur < 540){
							sumGastroCultureDur += actDur;
							gastroCultureCount += 1;
						}
					}
				}
			}
		}

		sumHomeDur = ((24*60*3000)-(sumTravelTime+sumWorkDur+sumShopRetailDur+sumShopServiceDur+sumSportsFunDur+sumGastroCultureDur));

		log.info("----------------------------------");
		log.info("total time spent traveling = " +sumTravelTime);
		log.info("number of legs = " +legCount);
		log.info("average travel time = " +sumTravelTime/legCount);
		log.info("----------------------------------");
		log.info("total time spent in home facilities = " +sumHomeDur);
		log.info("average duration = " +sumHomeDur/3000);
		log.info("----------------------------------");
		log.info("total time spent in work facilities = " +sumWorkDur);
		log.info("number of work trips = " +workCount);
		log.info("average duration = " +sumWorkDur/workCount);
		log.info("----------------------------------");	
		log.info("total time spent in shop retail facilities = " +sumShopRetailDur);
		log.info("number of shop retail trips = " +shopRetailCount);
		log.info("average duration = " +sumShopRetailDur/shopRetailCount);
		log.info("----------------------------------");
		log.info("total time spent in shop service facilities = " +sumShopServiceDur);
		log.info("number of shop service trips = " +shopServiceCount);
		log.info("average duration = " +sumShopServiceDur/shopServiceCount);
		log.info("----------------------------------");
		log.info("total time spent in sports & fun facilities = " +sumSportsFunDur);
		log.info("number of sports & fun trips = " +sportsFunCount);
		log.info("average duration = " +sumSportsFunDur/sportsFunCount);
		log.info("----------------------------------");
		log.info("total time spent in gastro & culture facilities = " +sumGastroCultureDur);
		log.info("number of gastro & culture trips = " +gastroCultureCount);
		log.info("average duration = " +sumGastroCultureDur/gastroCultureCount);
		log.info("----------------------------------");
	}
}