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

package playground.mrieser.pt.controler;

import java.util.EnumSet;
import java.util.Set;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.PtConstants;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.vehicles.VehicleReaderV1;


/**
 * @author mrieser
 */
public class TransitControler extends Controler {

	private final TransitConfigGroup transitConfig;

	/**
	 * @deprecated to not use, does not work properly!
	 * @param args
	 */
	@Deprecated
	public TransitControler(final String[] args) {
		super(args);
		this.transitConfig = new TransitConfigGroup();
		init();
	}

	/**
	 * @deprecated do not use, does not work properly!
	 * @param configFile
	 */
	@Deprecated
	public TransitControler(final String configFile) {
		super(configFile);
		this.transitConfig = new TransitConfigGroup();
		init();
	}

	public TransitControler(final ScenarioImpl scenario) {
		super(scenario);
		this.transitConfig = new TransitConfigGroup();
		init();
	}

	private final void init() {
		if (this.config.getModule(TransitConfigGroup.GROUP_NAME) == null) {
			this.config.addModule(TransitConfigGroup.GROUP_NAME, this.transitConfig);
		} else {
			// this would not be necessary if TransitConfigGroup is part of core config
			Module oldModule = this.config.getModule(TransitConfigGroup.GROUP_NAME);
			this.config.removeModule(TransitConfigGroup.GROUP_NAME);
			this.transitConfig.addParam("transitScheduleFile", oldModule.getValue("transitScheduleFile"));
			this.transitConfig.addParam("vehiclesFile", oldModule.getValue("vehiclesFile"));
			this.transitConfig.addParam("transitModes", oldModule.getValue("transitModes"));
		}
		if (this.config.getQSimConfigGroup() == null) {
		  this.config.addQSimConfigGroup(new QSimConfigGroup());
		}
		this.config.scenario().setUseTransit(true);
		this.config.scenario().setUseVehicles(true);
		Set<EventsFileFormat> formats = EnumSet.copyOf(this.config.controler().getEventsFileFormats());
		formats.add(EventsFileFormat.xml);
		this.config.controler().setEventsFileFormats(formats);

		ActivityParams params = new ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
		params.setTypicalDuration(120.0);
		this.config.planCalcScore().addActivityParams(params);

		((PopulationFactoryImpl) this.getPopulation().getFactory()).setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());

	}

	public static class TransitControlerListener implements StartupListener {

		private final TransitConfigGroup config;

		public TransitControlerListener(final TransitConfigGroup config) {
			this.config = config;
		}

		@Override
		public void notifyStartup(final StartupEvent event) {
			if (this.config.getTransitScheduleFile() != null) {
				new TransitScheduleReaderV1(event.getControler().getScenario().getTransitSchedule(), event.getControler().getScenario().getNetwork(), event.getControler().getScenario()).readFile(this.config.getTransitScheduleFile());
			}
			if (this.config.getVehiclesFile() != null) {
				new VehicleReaderV1(event.getControler().getScenario().getVehicles()).parse(this.config.getVehiclesFile());
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
