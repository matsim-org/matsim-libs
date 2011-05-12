/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityLocations.java
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
package playground.johannes.socialnetworks.sim.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.xml.sax.SAXException;

/**
 * @author illenberger
 *
 */
public class ActivityLocations {

	public static Set<Coord> analyze(Population population) {
		Set<Coord> coords = new HashSet<Coord>();
		
		int actcount = 0;
		int plancount = 0;
		
		for(Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			plancount++;
			for(int i = 0; i < plan.getPlanElements().size(); i += 2) {
				Activity act = (Activity) plan.getPlanElements().get(i);
				if(act.getType().startsWith("l")) {
					actcount++;
					coords.add(act.getCoord());
				}
			}
		}
		
		System.out.println("Average leisure acts per plan: " + actcount/(double)plancount);
		return coords;
	}
	
	public static void drawCoords(Set<Coord> coords, String filename) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		
		MathTransform transform = null;
		try {
			transform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, CRSUtils.getCRS(21781));
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		writer.write("x\ty");
		writer.newLine();
		for(Coord coord : coords) {
			double[] point = new double[]{coord.getX(), coord.getY()};
			try {
				transform.transform(point, 0, point, 0, 1);
			} catch (TransformException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			writer.write(String.valueOf(point[0]));
			writer.write("\t");
			writer.write(String.valueOf(point[1]));
			writer.newLine();
		}
		writer.close();
	}
	
	public static void main(String args[]) throws SAXException, ParserConfigurationException, IOException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkReaderMatsimV1 netReader = new NetworkReaderMatsimV1(scenario);
		netReader.parse("/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/network/ivtch-osm.xml");
		MatsimPopulationReader reader = new MatsimPopulationReader(scenario);
		reader.readFile("/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/plans/plans_miv_zrh30km_transitincl_10pct.xml");
//		reader.readFile("/Users/jillenberger/Work/work/socialnets/data/schweiz/mz2005/rawdata/plans.wegeinland.xml");
		
		Set<Coord> coords = analyze(scenario.getPopulation());
//		drawCoords(coords, "/Users/jillenberger/Work/work/socialnets/sim/lcoords.mz2005.txt");
	}
}
