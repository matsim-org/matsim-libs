/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationLegDistanceDistribution.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package herbie.running.population.algorithms;

import herbie.running.pt.DistanceCalculations;
import org.apache.log4j.Logger;
import java.io.PrintStream;
import org.apache.commons.math.stat.Frequency;
import org.apache.commons.math.util.ResizableDoubleArray;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.routes.ExperimentalTransitRoute;

/**
 * Generates a crosstab of the absolute number of legs in a population, by leg mode and route distance.
 * Leg distances are classified.
 * Only selected plans are considered.
 *
 * @author meisterk
 *
 */
public class PopulationLegDistanceDistribution extends AbstractClassifiedFrequencyAnalysis implements PlanAlgorithm {

	private Network network;

	private final static Logger log = Logger.getLogger(PopulationLegDistanceDistribution.class);

	public PopulationLegDistanceDistribution(PrintStream out, Network network) {
		super(out);
		this.network = network;
	}

	@Override
	public void run(Person person) {
		this.run(person.getSelectedPlan());
	}

	@Override
	public void run(Plan plan) {
		
		boolean isPtLeg = false;
		double distForPt = 0.0;
		String ptMode = "standardPt";
		String onlyPtWalkMode = "onlyPtWalk";
		boolean containsPt = false;
		
		for (PlanElement pe : plan.getPlanElements()) {
			
			if (pe instanceof Leg) {
				
				Leg leg = (Leg) pe;
				
				String mode = leg.getMode();
				
				if(mode.equals("transit_walk") || mode.equals("pt")){
					if(mode.equals("pt")) containsPt = true;
					
					mode = ptMode;
					
					if(!isPtLeg) distForPt = 0.0;
					isPtLeg = true;
					
				}
				else{
					isPtLeg = false;
				}
				
				
				Frequency frequency = null;
				ResizableDoubleArray rawData = null;
				if (!this.frequencies.containsKey(mode)) {
					frequency = new Frequency();
					this.frequencies.put(mode, frequency);
					rawData = new ResizableDoubleArray();
					this.rawData.put(mode, rawData);
				} else {
					frequency = this.frequencies.get(mode);
					rawData = this.rawData.get(mode);
				}
				
				double distance = 0.0;
				if(leg.getMode().equals("transit_walk")){
					distance = DistanceCalculations.getTransitWalkDistance((GenericRouteImpl)leg.getRoute(), network);
				}
				else{
					if(leg instanceof LegImpl && leg.getRoute() == null && !(leg.getRoute() instanceof LinkNetworkRouteImpl) && !(leg.getRoute() instanceof ExperimentalTransitRoute)){
						log.warn("Not enough information on leg-object. Distance is set to 0.0 for this leg. Therefore no distance contribution....");
					}
					else{						
						distance = DistanceCalculations.getLegDistance(leg.getRoute(), network);
					}
				}
				
				if(isPtLeg){
					distForPt += distance;
				}
				else{
					if(distance >= 0.0){
						frequency.addValue(distance);
						rawData.addElement(distance);
					}
				}
				
//				frequency.addValue(leg.getRoute().getDistance());
//				rawData.addElement(leg.getRoute().getDistance());
			}
			else{
				if(pe instanceof Activity){
					Activity act = (Activity) pe;
					if(isPtLeg && !act.getType().equals("pt interaction")){
						String mode;
						if(!containsPt) mode = onlyPtWalkMode;
						else mode = ptMode;
						
						Frequency frequency = null;
						ResizableDoubleArray rawData = null;
						if (!this.frequencies.containsKey(mode)) {
							frequency = new Frequency();
							this.frequencies.put(mode, frequency);
							rawData = new ResizableDoubleArray();
							this.rawData.put(mode, rawData);
						} else {
							frequency = this.frequencies.get(mode);
							rawData = this.rawData.get(mode);
						}
						
						if(distForPt >= 0.0){
							frequency.addValue(distForPt);
							rawData.addElement(distForPt);
						}
						
						
						distForPt = 0.0;
						isPtLeg = false;
						containsPt = false;
					}
				}
			}
		}
	}
}
