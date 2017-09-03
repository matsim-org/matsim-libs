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

package org.matsim.contrib.minibus.stats;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.genericUtils.GridNode;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;

/**
 * Accumulates the number of activities right before and after a paratransit trip. Ignores pt interactions.
 * Accumulation is done with respect to given grid size. Grid nodes' coordinates are the weighted average 
 * of all activities relevant for that grid node.
 * 
 * @author aneumann
 *
 */
final class ActivityLocationsParatransitUser implements IterationEndsListener {
	private final static Logger log = Logger.getLogger(ActivityLocationsParatransitUser.class);

    private final String pIdentifier;
	private final double gridSize;
	private boolean firstIteration = true;
	
	private Set<String> actTypes = new TreeSet<>();
	private HashMap<String, GridNode> gridNodeId2GridNode = new HashMap<>();

	@Inject ActivityLocationsParatransitUser(PConfigGroup pConfig) {
		this(pConfig.getPIdentifier(), pConfig.getGridSize());
	}
	
	private ActivityLocationsParatransitUser(String pIdentifier, double gridSize) {
		log.info("enabled");
		this.pIdentifier = pIdentifier;
		this.gridSize = gridSize;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

        parsePopulation(event.getServices().getScenario().getPopulation());

        String outNameIdentifier = "actsFromParatransitUsers.txt";
        if (this.firstIteration) {
			// write it to main output
			writeResults(event.getServices().getControlerIO().getOutputFilename("0." + outNameIdentifier));
			this.firstIteration = false;
		} else {
			// write it somewhere
			writeResults(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), outNameIdentifier));
		}
	}

	private void parsePopulation(Population population) {

		this.gridNodeId2GridNode = new HashMap<>();
		this.actTypes = new TreeSet<>();
		
		for (Person person : population.getPersons().values()) {
			Activity lastAct = null;
			boolean lastLegUsesParatransit = false;
			Activity lastActAdded = null;
			
			
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()) {
				if (pE instanceof Activity) {
					Activity act = ((Activity) pE);
					
					if (!act.getType().equalsIgnoreCase(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
						this.actTypes.add(act.getType());
						if (lastLegUsesParatransit) {
							if (lastActAdded == null || !lastActAdded.equals(lastAct)) {
								getNodeFromAct(lastAct).addPoint(lastAct.getType(), lastAct.getCoord());
								lastActAdded = lastAct;
							}
							if (!lastActAdded.equals(act)) {
								getNodeFromAct(act).addPoint(act.getType(), act.getCoord());
								lastActAdded = act;
							}
						}
						lastAct = act;
						lastLegUsesParatransit = false;
					}
				}

				if (pE instanceof Leg) {
					// check, if it is a paratransit user
					Leg leg = (Leg) pE;
					
//					if (leg.getRoute() instanceof GenericRouteImpl) {
						Route route = leg.getRoute();
						
						if (route.getRouteDescription() != null) {
							if (route.getRouteDescription().contains(this.pIdentifier)) {
								// it's a paratransit user
								lastLegUsesParatransit = true;
							}
						}
//					}
				}
			}
		}
	}
	
	private void writeResults(String filename) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filename)));
			writer.write("x\ty\ttotal");
			for (String actType : this.actTypes) {
				writer.write("\t" + actType);
			}
			writer.newLine();
			
			for (GridNode gridNode : this.gridNodeId2GridNode.values()) {
				int totalNActs = 0;
				for (String actType : this.actTypes) {
					totalNActs += gridNode.getCountForType(actType);
				}
				
				writer.write(gridNode.getX() + "\t" + gridNode.getY() + "\t" + totalNActs);
				
				for (String actType : this.actTypes) {
					writer.write("\t" + gridNode.getCountForType(actType));
				}
				
				writer.newLine();
			}
			
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private GridNode getNodeFromAct(Activity act) {
		String gridNodeId = GridNode.getGridNodeIdForCoord(act.getCoord(), this.gridSize);
		
		if (this.gridNodeId2GridNode.get(gridNodeId.toString()) == null) {
			this.gridNodeId2GridNode.put(gridNodeId.toString(), new GridNode(gridNodeId));
		}
		
		return this.gridNodeId2GridNode.get(gridNodeId.toString());
	}

	public static void main(String[] args) {
		
		Gbl.startMeasurement();
		
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		String networkFile = "f:/p_runs/txl/network.final.xml.gz";
		String inPlansFile = "f:/p_runs/txl/run71/it.380/run71.380.plans.xml.gz";
		String outFilename = "f:/p_runs/txl/run71/it.380/actsFromParatransitUsers.txt";
		
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFile);
		
		Population inPop = sc.getPopulation();
		MatsimReader popReader = new PopulationReader(sc);
		popReader.readFile(inPlansFile);
		
		ActivityLocationsParatransitUser ana = new ActivityLocationsParatransitUser("para_", 100.0);
		ana.parsePopulation(inPop);
		ana.writeResults(outFilename);
		
		Gbl.printElapsedTime();
	}
}
