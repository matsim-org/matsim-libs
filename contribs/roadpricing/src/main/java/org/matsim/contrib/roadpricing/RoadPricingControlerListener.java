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

package org.matsim.contrib.roadpricing;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.util.TravelDisutility;

/**
 * Integrates the RoadPricing functionality into the MATSim Controler.  Does the 
 * following:
 * <p></p>
 * <strike>Initialization:
 * <ul>
 * 		<li> Adds the {@link RoadPricingTollCalculator} events listener (to calculate the
 * 			 toll per agent).
 * 		<li> Adds the toll to the {@link TravelDisutility} for the router (by 
 * 			 wrapping the pre-existing {@link TravelDisutility} object).
 * </ul></strike>
 * After mobsim:
 * <ul>
 * 		<li> Send toll as money events to agents.
 * </ul>
 * Will also generate and output some statistics ...
 *
 * @author mrieser
 */
class RoadPricingControlerListener implements StartupListener, IterationEndsListener, ShutdownListener {

	final static private Logger log = Logger.getLogger(RoadPricingControlerListener.class);

	private final RoadPricingScheme scheme;
	private final RoadPricingTollCalculator calcPaidToll;
	private final CalcAverageTolledTripLength cattl;
	private OutputDirectoryHierarchy controlerIO;

	@Inject
	RoadPricingControlerListener(RoadPricingScheme scheme, RoadPricingTollCalculator calcPaidToll, CalcAverageTolledTripLength cattl, OutputDirectoryHierarchy controlerIO) {
		this.scheme = scheme;
		this.calcPaidToll = calcPaidToll;
		this.cattl = cattl;
		this.controlerIO = controlerIO;
		Gbl.printBuildInfo("RoadPricing", "/org.matsim.contrib/roadpricing/revision.txt");
	}

	@Override
	public void notifyStartup(final StartupEvent event) {}



	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		log.info("The sum of all paid tolls : " + this.calcPaidToll.getAllAgentsToll() + " monetary units.");
		log.info("The number of people who paid toll : " + this.calcPaidToll.getDraweesNr());
		log.info("The average paid trip length : " + this.cattl.getAverageTripLength() + " m.");
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		String filename = this.controlerIO.getOutputFilename("output_toll.xml.gz") ;
		new RoadPricingWriterXMLv1(this.scheme).writeFile(filename);
	}

}
