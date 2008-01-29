/* *********************************************************************** *
 * project: org.matsim.*
 * RoadPricing.java
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

package org.matsim.controler.corelisteners;

import org.apache.log4j.Logger;
import org.matsim.analysis.CalcAverageTolledTripLength;
import org.matsim.analysis.CalcAverageTripLength;
import org.matsim.controler.Controler;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.roadpricing.CalcPaidToll;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingScoringFunctionFactory;
import org.matsim.roadpricing.TollTravelCostCalculator;

import playground.yu.analysis.CalcAvgSpeed;
import playground.yu.analysis.CalcTrafficPerformance;

/**
 * Integrates the RoadPricing functionality into the MATSim Controler.
 *
 * @author mrieser
 */
public class RoadPricing implements StartupListener, IterationEndsListener, IterationStartsListener {

	private RoadPricingScheme scheme = null;
	private CalcPaidToll tollCalc = null;
	private CalcAverageTolledTripLength cattl = null;
	private CalcTrafficPerformance ctpf = null;
	private CalcAvgSpeed cas = null;
	
	final static private Logger log = Logger.getLogger(RoadPricing.class);

	public void notifyStartup(final StartupEvent event) {
		Controler controler = event.getControler();
		// read the road pricing scheme from file
		RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(controler.getNetwork());
		try {
			rpReader.parse(controler.getConfig().roadpricing().getTollLinksFile());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		this.scheme = rpReader.getScheme();

		// add the events handler to calculate the tolls paid by agents
		this.tollCalc = new CalcPaidToll(controler.getNetwork(), this.scheme);
		controler.getEvents().addHandler(this.tollCalc);

		// add the toll-score to the existing scoring function
		controler.setScoringFunctionFactory(
				new RoadPricingScoringFunctionFactory(this.tollCalc, controler.getScoringFunctionFactory()));

		// replace the travelCostCalculator with a toll-dependent one if required
		if ("distance".equals(this.scheme.getType()) || "cordon".equals(this.scheme.getType())) {
			controler.setTravelCostCalculator(new TollTravelCostCalculator(controler.getTravelCostCalculator(), this.scheme));
		}
		
		NetworkLayer network = controler.getNetwork();
		
		cas = new CalcAvgSpeed(network);
		ctpf = new CalcTrafficPerformance(network);
		Events events = controler.getEvents();
		events.addHandler(cas);
		events.addHandler(ctpf);
		cattl = new CalcAverageTolledTripLength(network, controler
				.getRoadPricing().getRoadPricingScheme());
		events.addHandler(cattl);
		
		// TODO [MR] I think that the Area-Router is not yet loaded (never was, neither in this nor in the old controler)

	}
	public void notifyIterationStarts(IterationStartsEvent event) {
		int it = event.getIteration();
		cas.reset(it);
		ctpf.reset(it);
		cattl.reset(it);
	}

	public void notifyIterationEnds(IterationEndsEvent event) {
		int it = event.getIteration();
		if (it % 10 == 0) {
			log.info("The sum of all paid tolls : "
					+ tollCalc.getAllAgentsToll() + " €.");
			log.info("The number of people, who paid toll : "
					+ tollCalc.getDraweesNr());
			CalcAverageTripLength catl = new CalcAverageTripLength();
			catl.run(event.getControler().getPopulation());
			log.info("The average trip length : " + catl.getAverageTripLength()
					+ " m.");
			log.info("The average paid trip length : "
					+ cattl.getAverageTripLength() + " m.");
			log.info("The traffic performance of the whole network : "
					+ ctpf.getTrafficPerformance() + " Pkm.");
			log.info("The average travel speed : " + cas.getAvgSpeed()
					+ " km/h.");
		}
	}

	public RoadPricingScheme getRoadPricingScheme() {
		return this.scheme;
	}

	public CalcPaidToll getPaidTolls() {
		return this.tollCalc;
	}

}
