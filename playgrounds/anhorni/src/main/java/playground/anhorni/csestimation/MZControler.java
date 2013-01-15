/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.csestimation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;


import playground.anhorni.analysis.microcensus.planbased.MZ2Plans;

public class MZControler {
	private Population population = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();	
	private TreeMap<Id, ShopLocation> shops = new TreeMap<Id, ShopLocation>();
	private final static Logger log = Logger.getLogger(MZControler.class);
	
	
	public static void main(String[] args) {
		MZControler c = new MZControler();
		String mzIndir = args[0];
		String universalChoiceSetFile = args[1];
		String outdir = args[2];
		c.run(universalChoiceSetFile, mzIndir, outdir);
	}
	
	public void run(String universalChoiceSetFile, String mzIndir, String outdir) {
		MZ2Plans mzCreator = new MZ2Plans();
		try {
			this.population = mzCreator.createMZ2Plans(mzIndir, outdir);
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.readUniversalCS(universalChoiceSetFile);		
		this.mergeShoppingLocations();
		this.write();
		log.info("finished .......................................");
	}
	
	private void readUniversalCS(String file) {
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String curr_line;
			while ((curr_line = br.readLine()) != null) {
				String[] entrs = curr_line.split(";", -1);
				
				Id id = new IdImpl(Integer.parseInt(entrs[0].trim()));
				ShopLocation shop = new ShopLocation(id);
				CoordImpl coord = new CoordImpl(Double.parseDouble(entrs[4]), Double.parseDouble(entrs[5]));
				shop.setCoord(coord);
				shops.put(id, shop);				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void mergeShoppingLocations() {
		// create kd-Tree of univ CS
		// coord conversion
				
		// go through shopping trips and remove all non-ZH trips and persons!
		
		// get shopping loc
	}
	
	private void write() {
		// write person and destination file for estimation
	}
}
