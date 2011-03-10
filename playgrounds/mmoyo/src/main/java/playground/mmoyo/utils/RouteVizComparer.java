/* *********************************************************************** *
 * project: org.matsim.*
 * RouteVizComparer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mmoyo.utils;

import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.run.OTFVis;

public class RouteVizComparer {
	Config config = null;
	
	public RouteVizComparer (String configFile){
		
		try {
			this.config = ConfigUtils.loadConfig(configFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void run(final String netFile, String[] arraPopPaths){
		//Create a new population where persons will be stored 
		Population[] arrayPops = new Population[arraPopPaths.length];
		String[] arrayFrags = new String[arraPopPaths.length];
		
		PlanFragmenter fragmenter =  new PlanFragmenter();
		DataLoader loader = new DataLoader();

		NetworkImpl net = loader.readNetwork(netFile);
		
		final String strFrag = "/Frag_";
		for (int i=0; i< arraPopPaths.length;i++){
			arrayPops[i]= fragmenter.run(loader.readPopulation(arraPopPaths[i]));

			File file = new File(arraPopPaths[i]);
			arrayFrags[i]=file.getParentFile().getAbsolutePath() + strFrag + file.getName();
			
			PopulationWriter popwriter = new PopulationWriter(arrayPops[i], net);
			popwriter.write(arrayFrags[i]);
		}

		//merge fragmented plans
		Population mergedPop = new PlansMerger().plansAggregator(arrayFrags);
		PopulationWriter popwriter = new PopulationWriter(mergedPop, net);
		popwriter.write(config.controler().getOutputDirectory() + "/merged.xml.gz");

		//convert each plan into new persons
		mergedPop =  new PlanFragmenter().plans2Persons(mergedPop);
		popwriter = new PopulationWriter(mergedPop, net);
		String convertedPopPath = config.controler().getOutputDirectory() + "/converted.xml.gz";
		popwriter.write(convertedPopPath);
		
		//prepare new config for otfviz
		this.config.setParam("plans", "inputPlansFile", convertedPopPath);
		String outConfig = this.config.controler().getOutputDirectory() + "/config_convertedPopulation.xml";
		new ConfigWriter(this.config).write(outConfig);

		//set data containers to null, the OTFVis may need that memory space
		arrayPops= null;
		net= null;
		popwriter= null;
		mergedPop= null;
		this.config = null;
		
		OTFVis.playConfig(outConfig);
	}
	
	public static void main(String[] args) {
		String netPath = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		String configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
		
		String[] arraPopPaths = new String[3];
		arraPopPaths[0]= "../playgrounds/mmoyo/output/cadyts/Around812550_walk6.0_dist0.0_tran1200.0.gz";
		arraPopPaths[1]= "../playgrounds/mmoyo/output/cadyts/Around812550_walk8.0_dist0.5_tran720.0.xml.gz";
		arraPopPaths[2]= "../playgrounds/mmoyo/output/cadyts/Around812550_walk10.0_dist0.0_tran240.0.xml.gz";
		
		new RouteVizComparer(configFile).run(netPath, arraPopPaths);
	}

}
