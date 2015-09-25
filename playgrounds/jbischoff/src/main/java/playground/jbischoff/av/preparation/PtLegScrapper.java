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

package playground.jbischoff.av.preparation;


import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * @author  jbischoff
 *
 */
public class PtLegScrapper {
	Scenario scenario;

	public static void main(String[] args) {
		
		PtLegScrapper pm = new PtLegScrapper();
		
		String popFile = "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/population.xml.gz";;
		String newPopFile = "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scrappedpopulation.xml.gz";
		String networkFile = "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/network.xml.gz";
		pm.run(networkFile,popFile,newPopFile);
		
		
	}

	public void run(String networkFile, String popFile, String newpopFile) {
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFile);
		new MatsimPopulationReader(scenario).readFile(popFile);
		Scenario s2 = replacePTLegs(scenario);
		new PopulationWriter(s2.getPopulation()).write(newpopFile);;

	
	}

	private Scenario replacePTLegs(Scenario scen){
		Scenario newscen = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population newPopulation = newscen.getPopulation();
		for (Person p : scen.getPopulation().getPersons().values()){
			Person n = newPopulation.getFactory().createPerson(p.getId());	
			newPopulation.addPerson(n);
			Plan plan = p.getSelectedPlan();
			Plan newPlan = newPopulation.getFactory().createPlan();
			n.addPlan(newPlan);
		
			boolean ignoreNextLeg = false;
			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Activity){
										
					Activity a = (Activity) pe;
					
					if (a.getType().equals("pt interaction")){
						ignoreNextLeg = true;
					}
					else{
					
					Activity na = newPopulation.getFactory().createActivityFromCoord(a.getType(),a.getCoord());
					na.setEndTime(a.getEndTime());
					newPlan.addActivity(na);
					}
				}
				else if (pe instanceof Leg){
					
					if (ignoreNextLeg){
						ignoreNextLeg = false;
						
					}
					else{
						String mode = ((Leg) pe).getMode();
						if (mode.equals("transit_walk")) mode = "pt";
						Leg nl = newPopulation.getFactory().createLeg(mode);
						newPlan.addLeg(nl);
					}
					}
				
			}
			
		}
		
		
		
		return newscen;
	}

	
	

}
