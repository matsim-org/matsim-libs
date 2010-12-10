/* *********************************************************************** *
 * project: org.matsim.*
 * PlanToShapefile.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.Utilities;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.utils.gis.matsim2esri.plans.SelectedPlans2ESRIShape;
import org.xml.sax.SAXException;

public class PlanToShapefile {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String plansFilename;
		String networkFilename;
		String outputDir;
		String crs;
		if(args.length == 4){
			plansFilename = args[0];
			networkFilename = args[1];
			outputDir = args[2];
			crs = args[3];
		} else{
			throw new IllegalArgumentException("Need four arguments.");
		}
		Scenario s = new ScenarioImpl();
		MatsimNetworkReader nr = new MatsimNetworkReader(s);
		MatsimPopulationReader pr = new MatsimPopulationReader(s);
		try {
			nr.parse(networkFilename);
			pr.parse(plansFilename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		SelectedPlans2ESRIShape sp = new SelectedPlans2ESRIShape(
				s.getPopulation(), 
				s.getNetwork(), 
				MGC.getCRS(crs), 
				outputDir);
		sp.setWriteActs(true);
		sp.setWriteLegs(false);
		try {
			sp.write();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
