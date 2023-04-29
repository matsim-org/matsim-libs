/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
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

package playground.vsp.ev;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.charging.ChargingModule;
import org.matsim.contrib.ev.discharging.DischargingModule;
import org.matsim.contrib.ev.fleet.ElectricFleetModule;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureModule;
import org.matsim.contrib.ev.stats.EvStatsModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;
import org.matsim.core.modal.AbstractModalQSimModule;

public class UrbanEVModule extends AbstractModule {
	static final String PLUGIN_IDENTIFIER = " plugin";
	public static final String PLUGIN_INTERACTION = PlanCalcScoreConfigGroup.createStageActivityType(
			PLUGIN_IDENTIFIER );
	static final String PLUGOUT_IDENTIFIER = " plugout";
	public static final String PLUGOUT_INTERACTION = PlanCalcScoreConfigGroup.createStageActivityType(
			PLUGOUT_IDENTIFIER );
	@Inject
	private Config config;

	@Override
	public void install() {
		QSimComponentsConfigGroup qsimComponentsConfig = ConfigUtils.addOrGetModule( config, QSimComponentsConfigGroup.class );
		qsimComponentsConfig.addActiveComponent( EvModule.EV_COMPONENT );

//		UrbanEVConfigGroup urbanEVConfig = ConfigUtils.addOrGetModule( config, UrbanEVConfigGroup.class );

//		if (urbanEVConfig == null)
//			throw new IllegalArgumentException(
//					"no config group of type " + UrbanEVConfigGroup.GROUP_NAME + " was specified in the config");
		// was this meaningful?  I.e. do we want the code to fail if there is no such config group?  kai, apr'23

		//standard EV stuff
		install(new ChargingInfrastructureModule());
		install(new ChargingModule());
		install(new DischargingModule());
		install(new EvStatsModule());
		install(new ElectricFleetModule());

		//bind custom EVFleet stuff
//		bind(ElectricFleetUpdater.class).in(Singleton.class);
		addControlerListenerBinding().to(ElectricFleetUpdater.class).in( Singleton.class );
		// (this takes the matsim modal vehicles for each leg and gives them to the ElectricFleetSpecification.  Don't know why it has to be in
		// this ad-hoc way.  kai, apr'23)

		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				//this is responsible for charging vehicles according to person activity start and end events..
//				bind(UrbanVehicleChargingHandler.class);
				addMobsimScopeEventHandlerBinding().to(UrbanVehicleChargingHandler.class);
				// (I think that this takes the plugin/plugout activities, and actually initiates the charging.  kai, apr'23)
			}
		});

		//bind urban ev planning stuff
		addMobsimListenerBinding().to(UrbanEVTripsPlanner.class);
		// (I think that this inserts the charging activities just before the mobsim starts (i.e. it is not in the plans).  kai, apr'23)

		//TODO find a better solution for this yyyy yes.  We do not want automagic.  kai, apr'23
//		Collection<String> whileChargingActTypes = urbanEVConfig.getWhileChargingActivityTypes().isEmpty() ?
//				config.planCalcScore().getActivityTypes() :
//				urbanEVConfig.getWhileChargingActivityTypes();

//		bind(ActivityWhileChargingFinder.class).toInstance(new ActivityWhileChargingFinder(whileChargingActTypes,
//				urbanEVConfig.getMinWhileChargingActivityDuration_s()));

		bind( ActivityWhileChargingFinder.class ).in( Singleton.class );

		//bind custom analysis:
		addEventHandlerBinding().to(ChargerToXY.class).in(Singleton.class);
		addMobsimListenerBinding().to(ChargerToXY.class);
		addEventHandlerBinding().to(ActsWhileChargingAnalyzer.class).in(Singleton.class);
		addControlerListenerBinding().to(ActsWhileChargingAnalyzer.class);
	}

	private Set<String> getOpenBerlinActivityTypes() {
		Set<String> activityTypes = new HashSet<>();
		for (long ii = 600; ii <= 97200; ii += 600) {
			activityTypes.add("home_" + ii + ".0");
			activityTypes.add("work_" + ii + ".0");
			activityTypes.add("leisure_" + ii + ".0");
			activityTypes.add("shopping_" + ii + ".0");
			activityTypes.add("other_" + ii + ".0");
		}
		return activityTypes;
	}

}
