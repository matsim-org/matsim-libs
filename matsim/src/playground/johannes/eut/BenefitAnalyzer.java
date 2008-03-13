/* *********************************************************************** *
 * project: org.matsim.*
 * BenefitAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.eut;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.controler.Controler;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.events.ShutdownEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.controler.listener.ShutdownListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.network.Link;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Route;
import org.matsim.router.util.TravelTimeI;
import org.matsim.utils.io.IOUtils;

/**
 * @author illenberger
 *
 */
public class BenefitAnalyzer implements IterationEndsListener, ShutdownListener, IterationStartsListener, StartupListener {

	private TripAndScoreStats tripStats;
	
//	private EUTRouterAnalyzer routerAnalyzer;
	
	private TravelTimeMemory ttKnowledge;
	
	private ArrowPrattRiskAversionI utilFunc;
	
	private Map<Person, Double> ceMap;
	
	private List<Double> samples;
	
	private BufferedWriter writer;
	

	public BenefitAnalyzer(TripAndScoreStats tripStats, EUTRouterAnalyzer routerAnalyzer, TravelTimeMemory ttKnowledge, ArrowPrattRiskAversionI utilFunc) {
		this.tripStats = tripStats;
//		this.routerAnalyzer = routerAnalyzer;
		this.ttKnowledge = ttKnowledge;
		this.utilFunc = utilFunc;
	}
	/*
	 * This is ugly, but there is no event between initialization and actual run
	 * of the queue sim.
	 */
	public void addGuidedPerson(Person p) {
		
			Plan plan = p.getSelectedPlan();
			double cesum = 0;
			int tripcounts = 0;
			for(Iterator it = plan.getIteratorLeg(); it.hasNext();) {
				tripcounts++;
				Leg leg = ((Leg)it.next());
				Route route = leg.getRoute();
				double totaltravelcosts = 0;
				
				for (TravelTimeI traveltimes : ttKnowledge.getTravelTimes()) {
					double traveltime = calcTravTime(traveltimes, route, leg.getDepTime());
					double travelcosts = utilFunc.evaluate(traveltime);
					totaltravelcosts += travelcosts;
				}

				double avrcosts = totaltravelcosts
						/ (double) ttKnowledge.getTravelTimes().size();

				cesum += utilFunc.getTravelTime(avrcosts);
			}
			
			double ceavr = cesum/(double)tripcounts;
			ceMap.put(p, ceavr);
	}
	
	public void notifyIterationEnds(IterationEndsEvent event) {
		/*
		 * Get the experienced trip duration from the tripstats
		 */
		double benefitsum = 0;
		for(Person p : ceMap.keySet()) {
			Double triptime = tripStats.getTripDurations().get(p);
			if (triptime != null) { // unfortunately this can happen -> withinday bug
				double ce = ceMap.get(p);
				double benefit = ce - triptime;
				benefitsum += benefit;
				samples.add(benefit);
			}
		}

		try {
			writer.write(String.valueOf(event.getIteration()));
			writer.write("\t");
			writer.write(String.valueOf(benefitsum/(double)ceMap.size()));
			writer.newLine();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void notifyShutdown(ShutdownEvent event) {
		try {
			writer.write("avr\t");
			double sum = 0;
			for(Double d : samples)
				sum += d;
			writer.write(String.valueOf(sum/(double)samples.size()));
			writer.newLine();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private double calcTravTime(TravelTimeI traveltimes, Route route,
			double starttime) {
		double totaltt = 0;
		for (Link link : route.getLinkRoute()) {
			totaltt += traveltimes.getLinkTravelTime(link, starttime + totaltt);
		}
		return totaltt;
	}

	public void notifyIterationStarts(IterationStartsEvent event) {
		ceMap = new HashMap<Person, Double>();
	}

	public void notifyStartup(StartupEvent event) {
		try {
			writer = IOUtils.getBufferedWriter(Controler.getOutputFilename("benefits.txt"));
			writer.write("Iteration\tbenefits");
			writer.newLine();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		samples = new LinkedList<Double>();
	}

}
