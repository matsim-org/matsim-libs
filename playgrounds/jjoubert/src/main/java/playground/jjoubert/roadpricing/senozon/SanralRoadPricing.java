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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.roadpricing.*;
import playground.jjoubert.roadpricing.senozon.routing.SanralTravelDisutilityIncludingToll;
import playground.jjoubert.roadpricing.senozon.scoring.SanralCalcPaidToll;

/**
 * Integrates the RoadPricing functionality into the MATSim Controler.
 *
 * @author mrieser
 */
public class SanralRoadPricing implements StartupListener, AfterMobsimListener, IterationEndsListener {

	private RoadPricingSchemeImpl scheme = null;
	private SanralCalcPaidToll tollCalc = null;
	private CalcAverageTolledTripLength cattl = null;

	final static private Logger log = Logger.getLogger(SanralRoadPricing.class);

	@Override
	public void notifyStartup(final StartupEvent event) {
		final MatsimServices controler = event.getServices();
		// read the road pricing scheme from file
		this.scheme = new RoadPricingSchemeImpl();
		RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(this.scheme);
		try {
            rpReader.parse(ConfigUtils.addOrGetModule(controler.getConfig(), RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).getTollLinksFile());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		if (RoadPricingScheme.TOLL_TYPE_AREA.equals(this.scheme.getType())) {
			// checks that the replanning strategies don't specify a certain router, as we need a special router ourselves.
			final StrategyConfigGroup config = controler.getConfig().strategy();
			for (StrategySettings settings : config.getStrategySettings()) {
				if (settings.getStrategyName().startsWith("ReRoute_")) {
					throw new RuntimeException("The replanning module " + settings.getStrategyName() + " is not supported together with an area toll. Please use the normal \"ReRoute\" instead.");
				}
			}
		}

		// add the events handler to calculate the tolls paid by agents
        this.tollCalc = new SanralCalcPaidToll(controler.getScenario().getNetwork(), this.scheme);
		controler.getEvents().addHandler(this.tollCalc);

		// replace the travelCostCalculator with a toll-dependent one if required
		if (RoadPricingScheme.TOLL_TYPE_DISTANCE.equals(this.scheme.getType()) || RoadPricingScheme.TOLL_TYPE_CORDON.equals(this.scheme.getType())) {
			final TravelDisutilityFactory previousTravelDisutilityFactory = controler.getTravelDisutilityFactory();
			// area-toll requires a regular TravelCost, no toll-specific one.
			final TravelDisutilityFactory travelCostCalculatorFactory = new TravelDisutilityFactory() {

				@Override
				public TravelDisutility createTravelDisutility(
						TravelTime timeCalculator) {
					return new SanralTravelDisutilityIncludingToll(previousTravelDisutilityFactory.createTravelDisutility(timeCalculator), SanralRoadPricing.this.scheme);
				}

			};
			throw new RuntimeException();
//			services.addOverridingModule(new AbstractModule() {
//				@Override
//				public void install() {
//					bindCarTravelDisutilityFactory().toInstance(travelCostCalculatorFactory);
//				}
//			});
		}

        this.cattl = new CalcAverageTolledTripLength(controler.getScenario().getNetwork(), this.scheme);
		controler.getEvents().addHandler(this.cattl);
	}

	@Override
	public void notifyAfterMobsim(final AfterMobsimEvent event) {
		// evaluate the final tolls paid by the agents and add them to their scores
		this.tollCalc.sendUtilityEvents(
				Time.MIDNIGHT, 
				event.getServices().getEvents()
				);
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
