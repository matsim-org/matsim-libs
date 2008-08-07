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
import org.matsim.config.groups.StrategyConfigGroup;
import org.matsim.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.controler.Controler;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.roadpricing.CalcPaidToll;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingScoringFunctionFactory;
import org.matsim.roadpricing.TollTravelCostCalculator;

/**
 * Integrates the RoadPricing functionality into the MATSim Controler.
 *
 * @author mrieser
 */
public class RoadPricing implements StartupListener, IterationEndsListener {

	private RoadPricingScheme scheme = null;
	private CalcPaidToll tollCalc = null;
	private CalcAverageTolledTripLength cattl = null;

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

		if (RoadPricingScheme.TOLL_TYPE_AREA.equals(this.scheme.getType())) {
			// checks that the replanning strategies don't specify a certain router, as we need a special router ourselves.
			final StrategyConfigGroup config = controler.getConfig().strategy();
			for (StrategySettings settings : config.getStrategySettings()) {
				if (settings.getModuleName().startsWith("ReRoute_")) {
					throw new RuntimeException("The replanning module " + settings.getModuleName() + " is not supported together with an area toll. Please use the normal \"ReRoute\" instead.");
				}
			}
		}

		// add the events handler to calculate the tolls paid by agents
		this.tollCalc = new CalcPaidToll(controler.getNetwork(), this.scheme);
		controler.getEvents().addHandler(this.tollCalc);

		// add the toll-score to the existing scoring function
		controler.setScoringFunctionFactory(new RoadPricingScoringFunctionFactory(this.tollCalc, controler.getScoringFunctionFactory()));
		log.debug("Loaded RoadPricingScoringFunctionFactory and set in controler");
		// replace the travelCostCalculator with a toll-dependent one if required
		if (RoadPricingScheme.TOLL_TYPE_DISTANCE.equals(this.scheme.getType()) || RoadPricingScheme.TOLL_TYPE_CORDON.equals(this.scheme.getType())) {
			// area-toll requires a regular TravelCost, no toll-specific one.
			controler.setTravelCostCalculator(new TollTravelCostCalculator(controler.getTravelCostCalculator(), this.scheme));
		}

		this.cattl = new CalcAverageTolledTripLength(controler.getNetwork(), this.scheme);
		controler.getEvents().addHandler(this.cattl);
	}

	public void notifyIterationEnds(final IterationEndsEvent event) {
		log.info("The sum of all paid tolls : " + this.tollCalc.getAllAgentsToll() + " Euro.");
		log.info("The number of people, who paid toll : " + this.tollCalc.getDraweesNr());
		log.info("The average paid trip length : " + this.cattl.getAverageTripLength() + " m.");
	}

	public RoadPricingScheme getRoadPricingScheme() {
		return this.scheme;
	}

	public CalcPaidToll getPaidTolls() {
		return this.tollCalc;
	}

	public double getAllAgentsToll() {
		return this.tollCalc.getAllAgentsToll();
	}

	public int getDraweesNr() {
		return this.tollCalc.getDraweesNr();
	}

	public double getAvgPaidTripLength() {
		return this.cattl.getAverageTripLength();
	}
}
