/* *********************************************************************** *
 * project: org.matsim.*
 * DgPrognose2025Verschmierer
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.prognose2025;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.population.MatsimPopulationReader;

import playground.dgrether.DgPaths;
import playground.mzilske.prognose2025.Verschmierer;


/**
 * @author dgrether
 *
 */
public class DgPrognose2025Verschmierer {

	public static final String LANDKREISE = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/osm_zellen/landkreise.shp"; 
	
	public static final String GV_POPULATION = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/demand/population_gv_10pct_raw.xml";
	
	public static final String GV_POPULATION_VERSCHMIERT = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/demand/population_gv_10pct_verschmiert.xml";

	private static final Logger log = Logger.getLogger(DgPrognose2025Verschmierer.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Verschmierer verschmierer = new Verschmierer();
		verschmierer.setFilename(LANDKREISE);
		verschmierer.prepare();
		
		Scenario scenario = new ScenarioImpl();
		MatsimPopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(GV_POPULATION);
		
		Scenario newScenario = new ScenarioImpl();
		Population newPopulation = newScenario.getPopulation();
		PopulationFactory popFac = newPopulation.getFactory();
		for (Person person : scenario.getPopulation().getPersons().values()){
			Person newPerson = popFac.createPerson(person.getId());
			newPopulation.addPerson(newPerson);
			Plan newPlan = popFac.createPlan();
			newPerson.addPlan(newPlan);
			for (PlanElement pe : person.getPlans().get(0).getPlanElements()){
				if (pe instanceof Activity){
					Activity act = (Activity) pe;
					Coord coord = act.getCoord();
					Coord newCoord = verschmierer.shootIntoSameZoneOrLeaveInPlace(coord);
					log.info("Old coord: " + coord + " new coord:  " + newCoord);
					Activity newAct = popFac.createActivityFromCoord(act.getType(), newCoord);
					newPlan.addActivity(newAct);
				}
				else if (pe instanceof Leg){
					Leg newLeg = popFac.createLeg(((Leg)pe).getMode());
					newPlan.addLeg(newLeg);
				}
			}
		}
		PopulationWriter popWriter = new PopulationWriter(newPopulation, null);
		popWriter.write(GV_POPULATION_VERSCHMIERT);
		
	}
}
