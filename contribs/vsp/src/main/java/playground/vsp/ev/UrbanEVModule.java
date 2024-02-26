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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.charging.ChargingModule;
import org.matsim.contrib.ev.discharging.DischargingModule;
import org.matsim.contrib.ev.fleet.ElectricFleetModule;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureModule;
import org.matsim.contrib.ev.stats.EvStatsModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;

public class UrbanEVModule extends AbstractModule {
	static final String PLUGIN_IDENTIFIER = " plugin";
	public static final String PLUGIN_INTERACTION = ScoringConfigGroup.createStageActivityType( PLUGIN_IDENTIFIER );
	static final String PLUGOUT_IDENTIFIER = " plugout";
	public static final String PLUGOUT_INTERACTION = ScoringConfigGroup.createStageActivityType( PLUGOUT_IDENTIFIER );
	@Inject private Config config;

	@Override public void install() {
		QSimComponentsConfigGroup qsimComponentsConfig = ConfigUtils.addOrGetModule( config, QSimComponentsConfigGroup.class );
		qsimComponentsConfig.addActiveComponent( EvModule.EV_COMPONENT );

		//standard EV stuff
		install(new ChargingInfrastructureModule());
		install(new ChargingModule());
		install(new DischargingModule());
		install(new EvStatsModule());
		install(new ElectricFleetModule());

		//bind custom EVFleet stuff
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
				// yes it does, just like the regular VehicleChargingHandler in the ev contrib. schlenther, feb'24.
			}
		});

		//bind urban ev planning stuff
		addMobsimListenerBinding().to(UrbanEVTripsPlanner.class);
		// (I think that this inserts the charging activities just before the mobsim starts (i.e. it is not in the plans).  kai, apr'23)

		bind( ActivityWhileChargingFinder.class ).in( Singleton.class );

		//bind custom analysis:
		addEventHandlerBinding().to(ChargerToXY.class).in(Singleton.class);
		addMobsimListenerBinding().to(ChargerToXY.class);
		addEventHandlerBinding().to(ActsWhileChargingAnalyzer.class).in(Singleton.class);
		addControlerListenerBinding().to(ActsWhileChargingAnalyzer.class);
	}

}
