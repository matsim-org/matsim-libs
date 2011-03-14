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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.dgrether.DgPaths;
import playground.gregor.gis.coordinatetransform.ApproximatelyCoordianteTransformation;


/**
 * @author dgrether
 *
 */
public class DgPrognose2025GvVerschmierer {

	public static final String LANDKREISE = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/osm_zellen/landkreise.shp"; 
	
	public static final String GV_POPULATION = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/demand/population_gv_1pct_raw.xml";
	
	public static final String GV_POPULATION_VERSCHMIERT = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/demand/population_gv_1pct_verschmiert.xml";

	private static final Logger log = Logger.getLogger(DgPrognose2025GvVerschmierer.class);
	
	private String f = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/orig/netze/coordinateTransformationLookupTable.csv";
	private ApproximatelyCoordianteTransformation transform = new ApproximatelyCoordianteTransformation(f);

//	private CoordinateTransformation wgs84ToWgs84Utm35S = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM35S); 
//	private CoordinateTransformation wgs84Utm35SToWgs84 = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_UTM35S, TransformationFactory.WGS84);
	private CoordinateTransformation wgs84ToDhdnGk4 = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4); 
	private CoordinateTransformation dhdnGk4ToWgs84 = TransformationFactory.getCoordinateTransformation(TransformationFactory.DHDN_GK4, TransformationFactory.WGS84);
	
	public DgPrognose2025GvVerschmierer(){
		
	}
	
	public void verschmierePopulation(){
		Verschmierer verschmierer = new Verschmierer(LANDKREISE);
		
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimPopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(GV_POPULATION);
		
		Scenario newScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population newPopulation = newScenario.getPopulation();
		PopulationFactory popFac = newPopulation.getFactory();
		for (Person person : scenario.getPopulation().getPersons().values()){
			Person newPerson = popFac.createPerson(person.getId());
			newPopulation.addPerson(newPerson);
			Plan newPlan = popFac.createPlan();
			newPerson.addPlan(newPlan);
			for (PlanElement pe : person.getPlans().get(0).getPlanElements()){
				if (pe instanceof Activity){
					ActivityImpl act = (ActivityImpl) pe;
					Coord coord = act.getCoord();
					Coord wgs84Coord = transform.getTransformed(coord);
					act.setCoord(wgs84Coord);
					Coord projectedCoord = wgs84ToDhdnGk4.transform(wgs84Coord);
					Coord newWgs84Coord = verschmierer.shootIntoSameZoneOrLeaveInPlace(projectedCoord);
					newWgs84Coord = dhdnGk4ToWgs84.transform(newWgs84Coord);
//					log.info("Old coord: " + wgs84Coord + " new coord:  " + newWgs84Coord);
					Activity newAct = popFac.createActivityFromCoord(act.getType(), newWgs84Coord); 
					newAct.setEndTime(act.getEndTime());
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
		
		DgActivities2KmlWriter kmlWriter = new DgActivities2KmlWriter();
		kmlWriter.writeKml(GV_POPULATION + ".kml", scenario.getPopulation());
		kmlWriter.writeKml(GV_POPULATION_VERSCHMIERT + ".kml", newPopulation);
		
	}
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new DgPrognose2025GvVerschmierer().verschmierePopulation();
	}
}
