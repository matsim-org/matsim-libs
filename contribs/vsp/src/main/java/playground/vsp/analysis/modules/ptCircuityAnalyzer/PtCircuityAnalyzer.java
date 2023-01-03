/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.vsp.analysis.modules.ptCircuityAnalyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.vehicles.Vehicles;

import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * 
 * This analysis module calculates the beeline distances and the distances
 * really travelled on the pt network. It differentiates pt trips including
 * pt transit lines and trips which only include transit_walk.
 * 
 * @author gleich
 *
 */
public class PtCircuityAnalyzer extends AbstractAnalysisModule {
	
	private Scenario scenario;
	private Vehicles vehicles;
	private PtDistanceHandler ptDistanceHandler;
	private Map<Id, List<Double>> ptBeelineDistances = new HashMap<Id, List<Double>>(); // Person Id to List of Distances per leg
	private Map<Id, List<Double>> transitWalkBeelineDistances = new HashMap<Id, List<Double>>();
	private Map<Id, List<Double>> ptActualDistances = new HashMap<Id, List<Double>>(); // Person Id to List of Distances per leg
	private Map<Id, List<Double>> transitWalkActualDistances = new HashMap<Id, List<Double>>();
	private Map<Id, Queue<Coord>> actCoords = new HashMap<Id, Queue<Coord>>(); // Person Id to List of activity coords
	private Map<Id, Double> totalPtBeelineDistancesPerAgent = new HashMap<Id, Double>();
	private Map<Id, Double> totalPtActualDistancesPerAgent = new HashMap<Id, Double>();
	private Map<Id, Double> totalTransitWalkBeelineDistancesPerAgent = new HashMap<Id, Double>();
	private Map<Id, Double> totalTransitWalkActualDistancesPerAgent = new HashMap<Id, Double>();


	public PtCircuityAnalyzer(Scenario sc, Vehicles vehicles) {
		super(PtCircuityAnalyzer.class.getSimpleName());
		this.scenario =  sc;
		this.vehicles = vehicles;
	}

	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new ArrayList<EventHandler>();
		handler.add(ptDistanceHandler);
		return handler;
	}

	@Override
	public void preProcessData() {
		calculateBeelineDistancesAndListActivityCoords();
		this.ptDistanceHandler = new PtDistanceHandler(scenario, this.vehicles, this.actCoords);
	}

	@Override
	public void postProcessData() {
		getDistancesFromEvents();
		compareBeeline2Pt();
	}


	private void calculateBeelineDistancesAndListActivityCoords() {
		for(Person person: scenario.getPopulation().getPersons().values()) {
			ptBeelineDistances.put(person.getId(), new ArrayList<Double>());
			transitWalkBeelineDistances.put(person.getId(), new ArrayList<Double>());
			actCoords.put(person.getId(), new LinkedList<Coord>());
			Coord currentActivityCoord;
			Coord lastActivityCoord = null;
			String mode = null; // transport mode
			
			for(PlanElement planElement: person.getSelectedPlan().getPlanElements()) {
				if(planElement instanceof Activity) {
					Activity currentActivity = (Activity) planElement;
					/* Exclude "pt interaction" pseudo-activities */
					if(currentActivity.getType().equalsIgnoreCase("pt interaction")) {
						continue;
					} else {
						currentActivityCoord = currentActivity.getCoord();
						actCoords.get(person.getId()).add(currentActivityCoord);
						/* Check if this is the first activity (no lastActivityCoord set) */
						if(lastActivityCoord == null) {
							lastActivityCoord = currentActivityCoord;
							continue;
						/* Calculate beeline (euclidian) distance */
						} else {
							double euclidianDistance = Math.sqrt(
							Math.pow((currentActivityCoord.getX() - lastActivityCoord.getX()), 2)
							+ Math.pow((currentActivityCoord.getY() - lastActivityCoord.getY()), 2));
							if(mode.equalsIgnoreCase("pt")) {
								ptBeelineDistances.get(person.getId()).add(euclidianDistance);
							} else if (mode.equalsIgnoreCase("transit_walk")) {
								//Test
								//System.out.println("Found transit_walk only leg for agent "+person.getId()+" distances "+transitWalkBeelineDistances.get(person.getId()));
								transitWalkBeelineDistances.get(person.getId()).add(euclidianDistance);
								//System.out.println("Found transit_walk only leg for agent "+person.getId()+" distances "+transitWalkBeelineDistances.get(person.getId()));
							} // other modes excluded
							// Reset transport mode for the next activity to activity leg
							mode = null;
						}
						lastActivityCoord = currentActivityCoord;
					}
				} else if (planElement instanceof Leg) {
					Leg leg = (Leg) planElement;
					if(leg.getMode().equalsIgnoreCase("car")) {
						mode = "car";
					} else if (leg.getMode().equalsIgnoreCase("transit_walk")) {
						if (mode == null) {
							mode = "transit_walk";						
						} else if (!mode.equalsIgnoreCase("pt")) { // if a leg included pt, it is no transit_walk only leg
							mode = "transit_walk";
						}
					} else if (leg.getMode().equalsIgnoreCase("pt")) {
						mode = "pt";
					} else {
						mode = leg.getMode();
					}
				}
			}
		}
	}
	
	private void getDistancesFromEvents() {
		ptActualDistances = ptDistanceHandler.getPtDistances();
		transitWalkActualDistances = ptDistanceHandler.getTransitWalkDistances();
	}
	
	private void compareBeeline2Pt() {
		/* Omit beeline distances travelled to activities which where omitted in the events */
		for(Id ii: scenario.getPopulation().getPersons().keySet()) {
			double totalPtBeelinePerAgent = 0.0;
			double totalPtActualPerAgent = 0.0;
			/* ptDistances.get(ii).size(): Number of legs travelled as written in the events, 
			 * can be smaller than the number of legs planned to be travelled in the plans 
			 */
			if(ptActualDistances.containsKey(ii) && ptBeelineDistances.containsKey(ii)) { // Cater for agents without pt trips
				for(int jj = 0; jj < ptActualDistances.get(ii).size(); jj ++) {
					totalPtBeelinePerAgent += ptBeelineDistances.get(ii).get(jj);
					totalPtActualPerAgent += ptActualDistances.get(ii).get(jj);
				}
			}
			totalPtBeelineDistancesPerAgent.put(ii, totalPtBeelinePerAgent);
			totalPtActualDistancesPerAgent.put(ii, totalPtActualPerAgent);
		}
		
		for(Id ii: scenario.getPopulation().getPersons().keySet()) {
			double totalTransitWalkBeelinePerAgent = 0.0;
			double totalTransitWalkActualPerAgent = 0.0;
			/* TransitWalkDistances.get(ii).size(): Number of legs travelled as written in the events, 
			 * can be smaller than the number of legs planned to be travelled in the plans 
			 */
			if(transitWalkActualDistances.containsKey(ii) && transitWalkBeelineDistances.containsKey(ii)) { // Cater for agents without pt trips
				for(int jj = 0; jj < transitWalkActualDistances.get(ii).size(); jj ++) {
					totalTransitWalkBeelinePerAgent += transitWalkBeelineDistances.get(ii).get(jj);
					totalTransitWalkActualPerAgent += transitWalkActualDistances.get(ii).get(jj);
				}
			}
			totalTransitWalkBeelineDistancesPerAgent.put(ii, totalTransitWalkBeelinePerAgent);
			totalTransitWalkActualDistancesPerAgent.put(ii, totalTransitWalkActualPerAgent);
		}
		
		/*
		//Test Circuity values
		boolean plausible = true;
		for(Id ii: scenario.getPopulation().getPersons().keySet()) {
			if((totalPtBeelineDistancesPerAgent.get(ii)/totalPtActualDistancesPerAgent.get(ii) > 1.0)
					||(totalPtBeelineDistancesPerAgent.get(ii)/totalPtActualDistancesPerAgent.get(ii) < 0)){
				System.out.println("Not plausible pt circuity for agent " + ii);
				plausible = false;
			}
			if((totalTransitWalkBeelineDistancesPerAgent.get(ii)/totalTransitWalkActualDistancesPerAgent.get(ii) < 0.999)
					||(totalTransitWalkBeelineDistancesPerAgent.get(ii)/totalTransitWalkActualDistancesPerAgent.get(ii) > 1.001)){
				System.out.println("Not plausible transit_walk circuity for agent " + ii);
				plausible = false;
			}
		}
		if(plausible){
			System.out.println("Only plausible Circuity values found");
		}
		*/
		
	}

	@Override
	public void writeResults(String outputFolder) {
		String separator = "\t";
		String fileName = outputFolder + "Beeline2PtDistanceAnalysis.txt";

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fileName)));
			bw.write("Agent Id" + separator + "ptBeeline" + separator + "ptDistance" + separator + "ptCircuity" 
					+ separator + "transit_walkBeeline" + separator + "transit_walkDistance" + separator + "transit_walkCircuity");
			bw.newLine();
			bw.write("total" + separator);
			
			double totalPtBeeline = 0;
			for(double ii: totalPtBeelineDistancesPerAgent.values()) {
				totalPtBeeline += ii;
			}
			double totalPt = 0;
			for(double ii: totalPtActualDistancesPerAgent.values()) {
				totalPt += ii;
			}
			double totalTransitWalkBeeline = 0;
			for(double ii: totalTransitWalkBeelineDistancesPerAgent.values()) {
				totalTransitWalkBeeline += ii;
			}
			double totalTransitWalk = 0;
			for(double ii: totalTransitWalkActualDistancesPerAgent.values()) {
				totalTransitWalk += ii;
			}
			
			bw.write(totalPtBeeline + separator + totalPt + separator + totalPtBeeline/totalPt 
					+ separator + totalTransitWalkBeeline
					+ separator + totalTransitWalk + separator + totalTransitWalkBeeline/totalTransitWalk);
			
			for(Id ii: totalPtBeelineDistancesPerAgent.keySet()) {
				bw.newLine();
				bw.write(ii + separator + totalPtBeelineDistancesPerAgent.get(ii) 
						+ separator + totalPtActualDistancesPerAgent.get(ii)
						+ separator + totalPtBeelineDistancesPerAgent.get(ii)/totalPtActualDistancesPerAgent.get(ii)
						+ separator + totalTransitWalkBeelineDistancesPerAgent.get(ii) 
						+ separator + totalTransitWalkActualDistancesPerAgent.get(ii)
						+ separator + totalTransitWalkBeelineDistancesPerAgent.get(ii)/totalTransitWalkActualDistancesPerAgent.get(ii));
			}
			
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
		
	}

}
