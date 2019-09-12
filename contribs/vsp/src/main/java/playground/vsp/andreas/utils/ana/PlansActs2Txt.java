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

package playground.vsp.andreas.utils.ana;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlWriter;

import playground.vsp.andreas.utils.ana.plans2gexf.GridNode;

/**
 * 
 * @author aneumann
 *
 */
public class PlansActs2Txt extends MatsimJaxbXmlWriter{
	
	private static final Logger log = Logger.getLogger(PlansActs2Txt.class);
	
	private final double gridSize;
	
	private HashMap<String, HashMap<String, GridNode>> actType2gridNodeId2GridNode = new HashMap<String, HashMap<String, GridNode>>();

	public static void main(String[] args) {
		Gbl.startMeasurement();
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	
		String networkFile = "f:/p/output/corr_t_2/corr_t_2.output_network.xml.gz";
		String inPlansFile = "f:/p/output/corr_t_2/ITERS/it.0/corr_t_2.0.plans.xml.gz";
		String outputDir = "f:/p/output/corr_t_2/ITERS/it.0/";
		
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFile);
	
		Population inPop = sc.getPopulation();
		MatsimReader popReader = new PopulationReader(sc);
		popReader.readFile(inPlansFile);
		
		PlansActs2Txt p2g = new PlansActs2Txt(100.0);
		p2g.parsePopulation(inPop);
		p2g.write(outputDir);
		
		Gbl.printElapsedTime();
		Gbl.printMemoryUsage();
	}

	public PlansActs2Txt(double gridSize){
		this.gridSize = gridSize;
	}
	
	private void parsePopulation(Population pop) {
		for (Person person : pop.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			
			for (PlanElement pE : plan.getPlanElements()) {
				if (pE instanceof Activity) {
					Activity act = (Activity) pE;
					GridNode currentNode = getNodeFromAct(act);
					currentNode.addPoint(act.getType(), act.getCoord());
				}
			}
		}
	}

	public void write(String outputDir) {
		for (String actType : this.actType2gridNodeId2GridNode.keySet()) {
			try {
				String filename = outputDir + actType + ".csv";
				BufferedWriter writer = IOUtils.getBufferedWriter(filename);
				writer.write("x;y;Acts " + actType); writer.newLine();
				
				
				for (GridNode gridNode : this.actType2gridNodeId2GridNode.get(actType).values()) {
					writer.write(gridNode.getX() + ";" + gridNode.getY() + ";" + gridNode.getCountForType(actType)); writer.newLine();
				}
				
				
				writer.flush();
				writer.close();
				log.info("Output written to " + filename);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}		
			
		}
	}

	private GridNode getNodeFromAct(Activity act) {
		String gridNodeId = GridNode.getGridNodeIdForCoord(act.getCoord(), this.gridSize);
		
		if (this.actType2gridNodeId2GridNode.get(act.getType()) == null) {
			this.actType2gridNodeId2GridNode.put(act.getType(), new HashMap<String, GridNode>());
		}
		
		if (this.actType2gridNodeId2GridNode.get(act.getType()).get(gridNodeId) == null) {
			this.actType2gridNodeId2GridNode.get(act.getType()).put(gridNodeId, new GridNode(gridNodeId));
		}
		
		return this.actType2gridNodeId2GridNode.get(act.getType()).get(gridNodeId);
	}
}