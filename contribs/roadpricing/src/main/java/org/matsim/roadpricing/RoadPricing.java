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

package org.matsim.roadpricing;

import org.apache.log4j.Logger;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.misc.Time;

/**
 * Integrates the RoadPricing functionality into the MATSim Controler.  Does the following:
 * <p/>
 * Initialization:<ul>
 * <li> Reads the road pricing scheme and adds it as a scenario element.
 * <li> Adds the CalcPaidToll events listener (to calculate the toll per agent).
 * <li> Adds the toll to the TravelDisutility for the router (by wrapping the pre-existing Travel Disutility object).
 * </ul>
 * After mobsim:<ul>
 * <li> Send toll as money events to agents.
 * </ul>
 * Will also generate and output some statistics ...
 *
 * @author mrieser
 */
public class RoadPricing implements StartupListener, AfterMobsimListener, 
IterationEndsListener, ShutdownListener {
	// public is needed (for obvious reasons: this is the entry point). kai, sep'14

	private RoadPricingScheme scheme = null ;
	private CalcPaidToll calcPaidToll = null;
	private CalcAverageTolledTripLength cattl = null;

	final static private Logger log = Logger.getLogger(RoadPricing.class);
	private RoadPricingConfigGroup rpConfig;

	/**
	 * This constructor will pull the road pricing scheme from the file in the config.
	 */
	public RoadPricing() {
		// public is needed (for obvious reasons: this is the entry point). kai, sep'14
		Gbl.printBuildInfo("RoadPricing", "/org.matsim.contrib/roadpricing/revision.txt");
	}

	/**
	 * This constructor will take the road pricing scheme given to it.
	 */
	public RoadPricing( RoadPricingScheme scheme ) {
		this() ;
		this.scheme = scheme ;
	}

	@Override
	public void notifyStartup(final StartupEvent event) {
		final Controler controler = event.getControler();
		rpConfig = ConfigUtils.addOrGetModule(controler.getConfig(), RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class);
		if ( this.scheme == null ) {
			String tollLinksFile = rpConfig.getTollLinksFile();
			if ( tollLinksFile == null ) {
				throw new RuntimeException("Road pricing inserted but neither toll links file nor RoadPricingScheme given.  "
						+ "Such an execution path is not allowed.  If you want a base case without toll, "
						+ "construct a zero toll file and insert that. ") ;
			}
			RoadPricingSchemeImpl rpsImpl = new RoadPricingSchemeImpl() ;
			new RoadPricingReaderXMLv1(rpsImpl).parse(tollLinksFile);
			this.scheme = rpsImpl ;
		}

		// add scheme as top level container into scenario: 
		event.getControler().getScenario().addScenarioElement( RoadPricingScheme.ELEMENT_NAME, scheme);

		// add the events handler to calculate the tolls paid by agents
        this.calcPaidToll = new CalcPaidToll(controler.getScenario().getNetwork(), this.scheme);
		controler.getEvents().addHandler(this.calcPaidToll);

		// replace the travelCostCalculator with a toll-dependent one if required
		if (RoadPricingScheme.TOLL_TYPE_DISTANCE.equals(this.scheme.getType()) 
				|| RoadPricingScheme.TOLL_TYPE_CORDON.equals(this.scheme.getType())
				|| RoadPricingScheme.TOLL_TYPE_LINK.equals(this.scheme.getType()) )
			// yy this is historically without area toll but it might be better to do it also with area toll
			// when the randomizing router is used.  I do think, however, that the current specification
			// of the area toll disutility will not work in that way.  kai, sep'14
		{
			TravelDisutilityIncludingToll.Builder travelDisutilityFactory = new TravelDisutilityIncludingToll.Builder(
					controler.getTravelDisutilityFactory(), scheme, controler.getConfig().planCalcScore().getMarginalUtilityOfMoney()
					) ;
			travelDisutilityFactory.setSigma( rpConfig.getRoutingRandomness() );
			controler.setTravelDisutilityFactory(travelDisutilityFactory);
		}

        this.cattl = new CalcAverageTolledTripLength(controler.getScenario().getNetwork(), this.scheme);
		controler.getEvents().addHandler(this.cattl);
	}

	@Override
	public void notifyAfterMobsim(final AfterMobsimEvent event) {
		// evaluate the final tolls paid by the agents and add them to their scores
		this.calcPaidToll.sendMoneyEvents(Time.MIDNIGHT, event.getControler().getEvents());
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		log.info("The sum of all paid tolls : " + this.calcPaidToll.getAllAgentsToll() + " Euro.");
		log.info("The number of people who paid toll : " + this.calcPaidToll.getDraweesNr());
		log.info("The average paid trip length : " + this.cattl.getAverageTripLength() + " m.");
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		String filename = event.getControler().getControlerIO().getOutputFilename("output_toll.xml.gz") ;
		new RoadPricingWriterXMLv1(this.scheme).writeFile(filename);
	}

}
