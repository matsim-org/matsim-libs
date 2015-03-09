/* *********************************************************************** *
 * project: org.matsim.*
 * AdaptPlans.java
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

package playground.staheale.preprocess;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.MatsimFacilitiesReader;


public class AdaptPlans {
	private static Logger log = Logger.getLogger(AdaptPlans.class);
	private ScenarioImpl scenario;
	private ScenarioImpl scenarioNew;
	private QuadTree<ActivityFacility> sportsFunQuadTree;
	private QuadTree<ActivityFacility> workQuadTree;
	private QuadTree<ActivityFacility> gastroCultureQuadTree;
	private QuadTree<ActivityFacility> educationQuadTree;
	private QuadTree<ActivityFacility> shopRetailQuadTree;
	private QuadTree<ActivityFacility> shopServiceQuadTree;
	boolean tta = false;
	Random random = new Random(37835409);
	int homeAct = 0;
	int workAct = 0;
	int shopAct = 0;
	int leisureAct = 0;
	int educationAct = 0;
	int ptLeg = 0;
	int walkLeg = 0;
	int carLeg = 0;
	int bikeLeg = 0;
	double latestStarttime = 0;
	Id id = null;
	int isWorking = 0;

//public AdaptPlans() {
//}
	
public static void main(String[] args) throws IOException {
	AdaptPlans adaptPlans = new AdaptPlans();
	adaptPlans.run();
	}

public void run() throws IOException {
	scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

	MatsimPopulationReader PlansReader = new MatsimPopulationReader(scenario); 
	PlansReader.readFile("./input/population2010baseline.xml.gz");
	Population pop = scenario.getPopulation();
	log.info("Initial population size is " +pop.getPersons().size());
	
	//////////////////////////////////////////////////////////////////////
	// preparing output file for commuting distances

	final String header="Person_id;Distance";
	final BufferedWriter out =
			IOUtils.getBufferedWriter("./output/distances.txt");
	out.write(header);
	out.newLine();
	
	for (Person p : pop.getPersons().values()) {
		List<PlanElement> pes = p.getSelectedPlan().getPlanElements();
		Iterator<PlanElement> iter = pes.iterator();
		Coord homeCoords = null;
		Coord workCoords = null;
		int start = 0;
		while (iter.hasNext()) {
			PlanElement pe = iter.next();
			if (pe instanceof Activity) {
				ActivityImpl a = (ActivityImpl) pe;
				if (a.getType().equals("home") != true && a.getType().equals("work") != true
						&& a.getType().equals("shopping") != true && a.getType().equals("leisure") != true 
						&& a.getType().equals("education") != true) {
					log.warn("act type: " +a.getType()+ ", person: " +p.getId());
				}
				if (a.getType().equals("home") && start < 1 && iter.hasNext()) { homeAct += 1; start += 1; homeCoords = a.getCoord();}
				else if (a.getType().equals("work")) { workAct += 1; workCoords = a.getCoord();}
				else if (a.getType().equals("shopping")) { shopAct += 1; }
				else if (a.getType().equals("leisure")) { leisureAct += 1; }
				else if (a.getType().equals("education")) { educationAct += 1; }
				if (a.getStartTime() > latestStarttime) {
					latestStarttime = a.getStartTime();
					id = p.getId();
				}
			}
			if (pe instanceof Leg) {
				LegImpl l = (LegImpl) pe;
				if (l.getMode() != "walk" && l.getMode() != "bike" && l.getMode() != "car" && l.getMode() != "pt") {
					log.warn("leg type: " +l.getMode()+ ", person: " +p.getId());
				}
				if (l.getMode().equals("car")) { carLeg += 1; }
				else if (l.getMode().equals("pt")) { ptLeg += 1; }
				else if (l.getMode().equals("walk")) { walkLeg += 1; }
				else if (l.getMode().equals("bike")) { bikeLeg += 1; }				
			}
		}
		// check commuter distances
		if (workCoords != null) {
			
			double dx = workCoords.getX() - homeCoords.getX();
			double dy = workCoords.getY() - homeCoords.getY();
			double dist = Math.sqrt(dx*dx+dy*dy);
			out.write(p.getId().toString()+";"+dist);
			out.newLine();
			isWorking += 1;
		}
		workCoords = null;
		homeCoords = null;
	}
	out.flush();
	out.close();
	log.info("number of home acts: " +homeAct);
	log.info("number of work acts: " +workAct);
	log.info("number of shopping acts: " +shopAct);
	log.info("number of leisure acts: " +leisureAct);
	log.info("number of education acts: " +educationAct);
	log.info("number of car legs: " +carLeg);
	log.info("number of pt legs: " +ptLeg);
	log.info("number of walk legs: " +walkLeg);
	log.info("number of bike legs: " +bikeLeg);
	log.info("latestStarttime: " +latestStarttime+ " for person " +id.toString());
	log.info("number of people working: " +isWorking);
    
	scenarioNew = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	int prob = 0;
	int popCount = 0;
	int popSize = pop.getPersons().size();
	
	for (Person p : pop.getPersons().values()) {
		prob = random.nextInt(2);
		//log.info("prob = " +prob);
		if (prob == 1 && popCount < 2*(popSize/10)) { //(popSize/10)
			scenarioNew.getPopulation().addPerson(p);
			popCount += 1;
		}
		
	}
	log.info("pop size is: " +scenarioNew.getPopulation().getPersons().size());
	new PopulationWriter(scenarioNew.getPopulation(), null).write("./output/population2010baseline_2percent.xml.gz");	
	}
}