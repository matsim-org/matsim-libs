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

package playground.jjoubert.roadpricing.senozon;

import org.apache.log4j.Logger;
import org.matsim.analysis.CalcAverageTolledTripLength;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;

/**
 * Integrates the RoadPricing functionality into the MATSim Controler.
 *
 * @author mrieser
 */
public class SanralRoadPricing implements StartupListener, AfterMobsimListener, IterationEndsListener {

	private RoadPricingScheme scheme = null;
	private SanralCalcPaidToll tollCalc = null;
	private CalcAverageTolledTripLength cattl = null;

	final static private Logger log = Logger.getLogger(SanralRoadPricing.class);

	@Override
	public void notifyStartup(final StartupEvent event) {
		final Controler controler = event.getControler();
		if (controler.getConfig().scenario().isUseRoadpricing()) {
			throw new RuntimeException("roadpricing must not be enabled in config.scenario in order to use special road pricing features!");
		}
		// read the road pricing scheme from file
		this.scheme = new RoadPricingScheme();
		RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(this.scheme);
		try {
			rpReader.parse(controler.getConfig().roadpricing().getTollLinksFile());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

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
		this.tollCalc = new SanralCalcPaidToll(controler.getNetwork(), this.scheme);
		controler.getEvents().addHandler(this.tollCalc);

		// replace the travelCostCalculator with a toll-dependent one if required
		if (RoadPricingScheme.TOLL_TYPE_DISTANCE.equals(this.scheme.getType()) || RoadPricingScheme.TOLL_TYPE_CORDON.equals(this.scheme.getType())) {
			final TravelCostCalculatorFactory previousTravelCostCalculatorFactory = controler.getTravelCostCalculatorFactory();
			// area-toll requires a regular TravelCost, no toll-specific one.
			TravelCostCalculatorFactory travelCostCalculatorFactory = new TravelCostCalculatorFactory() {

				@Override
				public PersonalizableTravelCost createTravelCostCalculator(
						PersonalizableTravelTime timeCalculator,
						CharyparNagelScoringConfigGroup cnScoringGroup) {
					return new SanralTollTravelCostCalculator(previousTravelCostCalculatorFactory.createTravelCostCalculator(timeCalculator, cnScoringGroup), SanralRoadPricing.this.scheme);
				}

			};
			controler.setTravelCostCalculatorFactory(travelCostCalculatorFactory);
		}

		this.cattl = new CalcAverageTolledTripLength(controler.getNetwork(), this.scheme);
		controler.getEvents().addHandler(this.cattl);
	}

	@Override
	public void notifyAfterMobsim(final AfterMobsimEvent event) {
		// evaluate the final tolls paid by the agents and add them to their scores
		this.tollCalc.sendUtilityEvents(Time.MIDNIGHT, event.getControler().getEvents());
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		log.info("The sum of all paid tolls : " + this.tollCalc.getAllAgentsToll() + " Euro.");
		log.info("The number of people, who paid toll : " + this.tollCalc.getDraweesNr());
		log.info("The average paid trip length : " + this.cattl.getAverageTripLength() + " m.");
	}

	public RoadPricingScheme getRoadPricingScheme() {
		return this.scheme;
	}

	public SanralCalcPaidToll getPaidTolls() {
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
