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

package playground.jbischoff.taxibus.scenario.plans;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import playground.jbischoff.taxibus.algorithm.utils.TaxibusUtils;

/**
 * @author  jbischoff
 *
 */
public class TaxibusPlansConverter {

	public static void main(String[] args) {
		String run = "VW082PC";
		String folder = "D:/runs-svn/vw_rufbus/" + run + "/";
//		String folder = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/";
		String inputPlans= folder + run + ".output_plans.xml.gz";
//		String inputPlans= folder + "initial_plans1.0.xml.gz";
		String inputNetwork = folder + run + ".output_network.xml.gz";
//		String inputNetwork = folder + "networkpt-feb.xml";
		CoordinateTransformation tf = TransformationFactory.getCoordinateTransformation("EPSG:25832", TransformationFactory.WGS84);
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(scenario).readFile(inputPlans);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(inputNetwork);
		List<Coord> homeLocations = new ArrayList<>();
		List<String> requests = new  ArrayList<>();
		for (Person p : scenario.getPopulation().getPersons().values()){
			if (p.getId().toString().startsWith("BS_WB")){
				
				
				Activity home = (Activity) p.getSelectedPlan().getPlanElements().get(0);
				if (home.getType().startsWith("work")) continue;
				Coord coord = tf.transform(home.getCoord());
				homeLocations.add(coord);
				Leg leg1 = (Leg) p.getSelectedPlan().getPlanElements().get(1);
				if (leg1.getMode().equals(TaxibusUtils.TAXIBUS_MODE)){
					Activity work = (Activity) p.getSelectedPlan().getPlanElements().get(2);
					Coord wcoord = tf.transform(work.getCoord());
					String s = coord.getX()+";"+coord.getY()+";"+wcoord.getX()+";"+wcoord.getY()+";"+Time.writeTime(home.getEndTime());
					requests.add(s);
				}
			}
		}
		
		BufferedWriter cw = IOUtils.getBufferedWriter(folder+"homecoords.csv");
		try {
			cw.write("x;y");
			for (Coord c : homeLocations){
				cw.newLine();
				cw.write(c.getX()+";"+c.getY());
			}
			cw.flush();
			cw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		BufferedWriter sw = IOUtils.getBufferedWriter(folder+run+".requests.csv");
		try {
			sw.write("homeX;homeY;workX;workY;departureTime");
			for (String s : requests){
				sw.newLine();
				sw.write(s);
			}
			sw.flush();
			sw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
