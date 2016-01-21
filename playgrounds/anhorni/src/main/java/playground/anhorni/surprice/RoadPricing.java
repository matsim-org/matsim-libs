/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.surprice;

import org.apache.log4j.Logger;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.misc.Time;
import org.matsim.roadpricing.*;
import org.matsim.utils.objectattributes.ObjectAttributes;


/**
 * Integrates the RoadPricing functionality into the MATSim Controler.
 *
 * @author c & p from contrib.roadpricing because ...
 * changes: LINK and AREA pricing included in notifyStartup
 * TODO: use contrib class a.s.a.p
 */
public class RoadPricing implements StartupListener, AfterMobsimListener, IterationEndsListener {

	private final RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl();
	private CalcPaidToll tollCalc = null;
	private CalcAverageTolledTripLength cattl = null;
	private ObjectAttributes preferences;

	final static private Logger log = Logger.getLogger(RoadPricing.class);
	
	public RoadPricing(ObjectAttributes preferences) {
		this.preferences = preferences;
	}
	
	@Override
	public void notifyStartup(final StartupEvent event) {
		final MatsimServices controler = event.getServices();
		// read the road pricing scheme from file
		RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(this.scheme);
		try {
            rpReader.parse(ConfigUtils.addOrGetModule(controler.getConfig(), RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).getTollLinksFile());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		event.getServices().getScenario().addScenarioElement(RoadPricingScheme.ELEMENT_NAME, scheme);

		// add the events handler to calculate the tolls paid by agents
        this.tollCalc = new CalcPaidToll(controler.getScenario().getNetwork(), this.scheme);
		controler.getEvents().addHandler(this.tollCalc);

		// replace the travelCostCalculator with a toll-dependent one if required
		// why not all?
		// TravelDisutilityIncludingToll is ready to handle all 4 schemes
		// how is link pricing handled?
		
// did somehow not work:
//		if (RoadPricingScheme.TOLL_TYPE_LINK.equals(this.scheme.getType()) ||
//				RoadPricingScheme.TOLL_TYPE_AREA.equals(this.scheme.getType()) ||
//				RoadPricingScheme.TOLL_TYPE_DISTANCE.equals(this.scheme.getType()) || 
//				RoadPricingScheme.TOLL_TYPE_CORDON.equals(this.scheme.getType())) {
//			final TravelDisutilityFactory previousTravelCostCalculatorFactory = services.getTravelDisutilityFactory();
//			// area-toll requires a regular TravelCost, no toll-specific one.
//			TravelDisutilityFactory travelCostCalculatorFactory = new TravelDisutilityFactory() {
//
//				@Override
//				public TravelDisutility createTravelDisutility(
//						TravelTime timeCalculator,
//						PlanCalcScoreConfigGroup cnScoringGroup) {
//					return new SurpriceTravelDisutilityIncludingToll(
//							previousTravelCostCalculatorFactory.createTravelDisutility(timeCalculator, cnScoringGroup), 
//							RoadPricing.this.scheme,
//							RoadPricing.this.preferences);
//				}
//				
//			};
//			services.setTravelDisutilityFactory(travelCostCalculatorFactory);
//		}

        this.cattl = new CalcAverageTolledTripLength(controler.getScenario().getNetwork(), this.scheme);
		controler.getEvents().addHandler(this.cattl);
	}

	@Override
	public void notifyAfterMobsim(final AfterMobsimEvent event) {
		// evaluate the final tolls paid by the agents and add them to their scores
		this.tollCalc.sendMoneyEvents(Time.MIDNIGHT, event.getServices().getEvents());
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
