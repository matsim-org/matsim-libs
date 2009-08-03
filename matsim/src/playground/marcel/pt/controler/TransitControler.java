/* *********************************************************************** *
 * project: org.matsim.*
 * PtControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.marcel.pt.controler;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.xml.sax.SAXException;

import playground.marcel.pt.config.TransitConfigGroup;
import playground.marcel.pt.queuesim.TransitQueueSimulation;
import playground.marcel.pt.router.PlansCalcTransitRoute;


public class TransitControler extends Controler {

	private final TransitConfigGroup transitConfig;
	private final TransitSchedule schedule;

	public TransitControler(final String[] args) {
		super(args);

		this.transitConfig = new TransitConfigGroup();
		this.config.addModule(TransitConfigGroup.GROUP_NAME, this.transitConfig);
		this.config.scenario().setUseTransit(true);
		this.config.scenario().setUseVehicles(true);
		this.schedule = this.scenarioData.getTransitSchedule();

		TransitControlerListener cl = new TransitControlerListener(this.schedule, this.scenarioData.getNetwork(), this.transitConfig);
		addControlerListener(cl);
	}

	@Override
	protected void runMobSim() {
		new TransitQueueSimulation(this.scenarioData, this.events);
	}

	@Override
	public PlanAlgorithm getRoutingAlgorithm(final TravelCost travelCosts, final TravelTime travelTimes) {
		return new PlansCalcTransitRoute(this.config.plansCalcRoute(), this.network, travelCosts, travelTimes,
				this.getLeastCostPathCalculatorFactory(), this.schedule, this.transitConfig);
	}

	public static class TransitControlerListener implements StartupListener {

		private final TransitSchedule schedule;
		private final NetworkLayer network;
		private final TransitConfigGroup config;

		public TransitControlerListener(final TransitSchedule schedule, final NetworkLayer network, final TransitConfigGroup config) {
			this.schedule = schedule;
			this.network = network;
			this.config = config;
		}

		public void notifyStartup(final StartupEvent event) {
			try {
				new TransitScheduleReaderV1(this.schedule, this.network).readFile(this.config.getTransitScheduleFile());
			} catch (SAXException e) {
				throw new RuntimeException("could not read transit schedule.", e);
			} catch (ParserConfigurationException e) {
				throw new RuntimeException("could not read transit schedule.", e);
			} catch (IOException e) {
				throw new RuntimeException("could not read transit schedule.", e);
			}
		}

	}

	public static void main(final String[] args) {
		if (args.length > 0) {
			new TransitControler(args).run();
		} else {
			new TransitControler(new String[] {"src/playground/marcel/pt/controler/transitConfig.xml"}).run();
		}
	}

}
