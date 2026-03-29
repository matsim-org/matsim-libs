/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.accidents;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.accidents.AccidentsConfigGroup.AccidentsComputationMethod;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;

import com.google.inject.Inject;

/**
* @author ikaddoura, mmayobre
*/

 class AccidentControlerListener implements StartupListener, IterationEndsListener, AfterMobsimListener {
	private static final Logger log = LogManager.getLogger(AccidentControlerListener.class);

	@Inject
	private AnalysisEventHandler analzyer;

	@Inject
	private Scenario scenario;

	@Inject
	private AccidentsContext accidentsContext;

	@Inject AccidentCostComputation accidentCostComputation;

	@Inject AccidentControlerListener(){}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		AccidentWriter accidentWriter = new AccidentWriter();
		accidentWriter.write(this.scenario, event, this.accidentsContext.getLinkId2info(), analzyer);
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {

		log.info("Computing accident costs per link and time bin...");
		AccidentsConfigGroup accidentsCfg = (AccidentsConfigGroup) scenario.getConfig().getModules().get(AccidentsConfigGroup.GROUP_NAME);

		double totalAccidentCostsPerDay = 0.;

		final double timeBinSize = this.scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize();

		for (AccidentLinkInfo linkInfo : this.accidentsContext.getLinkId2info().values()) {

			Link link = this.scenario.getNetwork().getLinks().get(linkInfo.getLinkId());

			for (double endTime = timeBinSize ; endTime <= this.scenario.getConfig().travelTimeCalculator().getMaxTime(); endTime = endTime + timeBinSize ) {

				final double time = (endTime - timeBinSize/2.);
				final int timeBinNr = (int) (time / timeBinSize);

				final AccidentsConfigGroup accidentsConfig = ConfigUtils.addOrGetModule( scenario.getConfig(), AccidentsConfigGroup.class );
				final double demand = accidentsConfig.getScaleFactor() * analzyer.getDemand(linkInfo.getLinkId(), timeBinNr);

				double accidentCosts = 0.;

				String linkAccidentsComputationMethod = (String) this.scenario.getNetwork().getLinks().get(linkInfo.getLinkId()).getAttributes().getAttribute(
					"accidentsComputationMethod" );

				if (linkAccidentsComputationMethod == null) {
					throw new RuntimeException("Required link attribute " + "accidentsComputationMethod" + " is null."
							+ " Please pre-process your network and specify the link attributes that are required to compute accident costs. Aborting...");
				}

				if (linkAccidentsComputationMethod.equals( AccidentsComputationMethod.BVWP.toString() )) {
					AccidentCostComputationBVWP.RoadType roadType = AccidentUtils.getRoadTypeForAccidents( link );

					accidentCosts = accidentCostComputation.computeAccidentCosts( demand, link, roadType );

				} else {
					throw new RuntimeException("Unknown accident computation approach or value not set. Aborting...");
				}

				TimeBinInfo timeBinInfo = new TimeBinInfo(timeBinNr);
				timeBinInfo.setAccidentCosts(accidentCosts);

				linkInfo.getTimeSpecificInfo().put(timeBinNr, timeBinInfo);

				totalAccidentCostsPerDay += accidentCosts;
			}
		}
		log.info("Computing accident costs per link and time bin... Done.");

		log.info("+++ Total accident costs per day [EUR] (upscaled to full population size): " + totalAccidentCostsPerDay);
	}

	@Override
	public void notifyStartup(StartupEvent event) {

		for (Link link : this.scenario.getNetwork().getLinks().values()) {
			AccidentLinkInfo info = new AccidentLinkInfo(link.getId());
			this.accidentsContext.getLinkId2info().put(link.getId(), info);
		}
		log.info("Initializing all link-specific information... Done.");

	}
}
