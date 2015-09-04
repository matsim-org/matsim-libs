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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * @author  jbischoff
 *
 */
public class PopulationModeConverter {
	BoundingBox box;
	Scenario scenario;

	public static void main(String[] args) {
		
		PopulationModeConverter pm = new PopulationModeConverter();
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4); 
		Coord xymin = ct.transform(new CoordImpl(13.2794,52.4687));
		Coord xymax = ct.transform(new CoordImpl(13.4804, 52.5583));
		pm.box = BoundingBox.createBoundingBox(xymin.getX(),xymin.getY(),xymax.getX(),xymax.getY());
		pm.run("C:/Users/Joschka/Documents/runs-svn/bvg.run132.25pct/bvg.run132.25pct.output_network.xml.gz","C:/Users/Joschka/Documents/runs-svn/bvg.run132.25pct/ITERS/it.100/bvg.run132.25pct.100.plans.xml.gz","C:/Users/Joschka/Documents/runs-svn/bvg.run132.25pct/ITERS/it.100/cp.tar.gz");
		
		
	}

	public void run(String networkFile, String popFile, String newpopFile) {
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFile);
		new MatsimPopulationReader(scenario).readFile(popFile);
		int i =0;
		long l = 0;
		long e = 0;
		long s = 0;
		for (Person p : scenario.getPopulation().getPersons().values()){
//			for (Plan plan : p.getPlans()){
			Plan plan = p.getSelectedPlan();
				for (PlanElement pe : plan.getPlanElements()){
					if (pe instanceof Leg){
					Leg leg = (Leg) pe;
					if (leg.getMode().equals("car")){
						l++;	
						Route route = leg.getRoute();
						if (routeInBox(route)){
							leg.setMode("taxi");
							i++;
						}
						else if (startInBox(route)){
							s++;
						}
						else if (endInBox(route)){
							e++;
						}
					}
				}
					
				}
//			}
		} 
		new PopulationWriter(scenario.getPopulation()).write(newpopFile);
		System.out.println(i);
		System.out.println(l);
		System.out.println("s:"+s);
		System.out.println("e:"+e);
	}

	private boolean routeInBox(Route route) {
		Coord start = scenario.getNetwork().getLinks().get(route.getStartLinkId()).getCoord();
		Coord end = scenario.getNetwork().getLinks().get(route.getEndLinkId()).getCoord();
		
		return (coordIsInBox(start)&&coordIsInBox(end));
	}
	
	private boolean startInBox(Route route) {
		Coord start = scenario.getNetwork().getLinks().get(route.getStartLinkId()).getCoord();
		
		return (coordIsInBox(start));
	}
	private boolean endInBox(Route route) {
		Coord end = scenario.getNetwork().getLinks().get(route.getEndLinkId()).getCoord();
		
		return (coordIsInBox(end));
	}

	private boolean coordIsInBox(Coord coord) {
		
		return (((box.getXMin()>=coord.getX())&&(coord.getX()<=box.getXMax()))&&((box.getYMin()>=coord.getY())&&(coord.getY()<=box.getYMax())));
	}
	

}
