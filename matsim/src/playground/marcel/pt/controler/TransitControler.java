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
import java.util.EnumSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.matsim.vehicles.BasicVehicleReaderV1;
import org.xml.sax.SAXException;

import playground.marcel.pt.config.TransitConfigGroup;
import playground.marcel.pt.queuesim.TransitQueueSimulation;
import playground.marcel.pt.router.PlansCalcTransitRoute;
import playground.marcel.pt.routes.ExperimentalTransitRouteFactory;

/**
 * @author mrieser
 */
public class TransitControler extends Controler {

	private final TransitConfigGroup transitConfig;

	public TransitControler(final String[] args) {
		super(args);

		setOverwriteFiles(true); // FIXME [MR] debug only

		this.transitConfig = new TransitConfigGroup();
		this.config.addModule(TransitConfigGroup.GROUP_NAME, this.transitConfig);
		this.config.scenario().setUseTransit(true);
		this.config.scenario().setUseVehicles(true);
		Set<EventsFileFormat> formats = EnumSet.copyOf(this.config.controler().getEventsFileFormats());
		formats.add(EventsFileFormat.xml);
		this.config.controler().setEventsFileFormats(formats);

		ActivityParams params = new ActivityParams(PlansCalcTransitRoute.TRANSIT_ACTIVITY_TYPE);
		params.setTypicalDuration(120.0);
		config.charyparNagelScoring().addActivityParams(params);
		
		this.getNetworkFactory().setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());
		
		TransitControlerListener cl = new TransitControlerListener(this.transitConfig);
		addControlerListener(cl);
	}

	@Override
	protected void runMobSim() {
		new TransitQueueSimulation(this.scenarioData, this.events).run();
//		new QueueSimulation(this.scenarioData, this.events).run();
	}

	@Override
	public PlanAlgorithm getRoutingAlgorithm(final TravelCost travelCosts, final TravelTime travelTimes) {
		return new PlansCalcTransitRoute(this.config.plansCalcRoute(), this.network, travelCosts, travelTimes,
				this.getLeastCostPathCalculatorFactory(), this.scenarioData.getTransitSchedule(), this.transitConfig);
	}

	public static class TransitControlerListener implements StartupListener {

		private final TransitConfigGroup config;

		public TransitControlerListener(final TransitConfigGroup config) {
			this.config = config;
		}

		public void notifyStartup(final StartupEvent event) {
			try {
				new TransitScheduleReaderV1(event.getControler().getScenarioData().getTransitSchedule(), event.getControler().getScenarioData().getNetwork()).readFile(this.config.getTransitScheduleFile());
			} catch (SAXException e) {
				throw new RuntimeException("could not read transit schedule.", e);
			} catch (ParserConfigurationException e) {
				throw new RuntimeException("could not read transit schedule.", e);
			} catch (IOException e) {
				throw new RuntimeException("could not read transit schedule.", e);
			}
			try {
				new BasicVehicleReaderV1(event.getControler().getScenarioData().getVehicles()).parse(this.config.getVehiclesFile());
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
