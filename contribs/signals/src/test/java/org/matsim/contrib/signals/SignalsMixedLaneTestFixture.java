/* *********************************************************************** *
 * project: org.matsim.*
 * SignalsMixedLaneTestFixture
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.signals;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.lanes.MixedLaneTestFixture;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalSystem;


/**
 * @author dgrether
 *
 */
public class SignalsMixedLaneTestFixture {

	public final Scenario sc;
	public final Id<Link> id1, id2, id3;
	public final Id<Person> pid1, pid2;
	private MixedLaneTestFixture delegate;

	public SignalsMixedLaneTestFixture(){
		delegate = new MixedLaneTestFixture();
		this.sc = delegate.sc;
		id1 = delegate.id1;
		id2 = delegate.id2;
		id3 = delegate.id3;
		pid1 = delegate.pid1;
		pid2 = delegate.pid2;
		this.sc.getConfig().scenario().setUseSignalSystems(true);
	
		//create signalsystems
		SignalSystemsData signals = new SignalSystemsDataImpl();
		this.sc.addScenarioElement( SignalsData.ELEMENT_NAME , signals);
		SignalSystemsDataFactory signalsFactory = signals.getFactory();
		SignalSystemData system = signalsFactory.createSignalSystemData(Id.create(delegate.id1, SignalSystem.class));
		signals.addSignalSystemData(system);
		SignalData signal = signalsFactory.createSignalData(Id.create(delegate.id2, Signal.class));
		system.addSignalData(signal);
		signal.addLaneId(Id.create(1, Lane.class));
		signal.addTurningMoveRestriction(delegate.id2);
		signal = signalsFactory.createSignalData(Id.create(delegate.id3, Signal.class));
		system.addSignalData(signal);
		signal.addLaneId(Id.create(1, Lane.class));
		signal.addTurningMoveRestriction(delegate.id3);
		
		//TODO continue here
		
//
//		//create signal system config
//		SignalSystemConfigurations signalConf = this.sc.getSignalSystemConfigurations();
//		SignalSystemConfigurationsFactory signalConfb = signalConf.getFactory();
//		SignalSystemConfiguration systemConf = signalConfb.createSignalSystemConfiguration(id1);
//		PlanBasedSignalSystemControlInfo signalPlanControl = signalConfb.createPlanBasedSignalSystemControlInfo();
//		SignalSystemPlan signalPlan = signalConfb.createSignalSystemPlan(id1);
//		signalPlan.setCycleTime(60);
//		SignalGroupSettings group2Settings = signalConfb.createSignalGroupSettings(id2);
//		group2Settings.setRoughCast(0);
//		group2Settings.setDropping(0);
//		group2Settings.setInterGreenTimeDropping(0);
//		group2Settings.setInterGreenTimeRoughcast(0);
//		SignalGroupSettings group3Settings = signalConfb.createSignalGroupSettings(id3);
//		group3Settings.setRoughCast(0);
//		group3Settings.setDropping(1);
//		group3Settings.setInterGreenTimeDropping(0);
//		group3Settings.setInterGreenTimeRoughcast(0);
//		//plug it together
//		signalPlan.addLightSignalGroupConfiguration(group2Settings);
//		signalPlan.addLightSignalGroupConfiguration(group3Settings);
//		signalPlanControl.addPlan(signalPlan);
//		systemConf.setSignalSystemControlInfo(signalPlanControl);
//		signalConf.addSignalSystemConfiguration(systemConf);
		
	}

	public void create2PersonPopulation() {
		this.delegate.create2PersonPopulation();
	}
	
}
