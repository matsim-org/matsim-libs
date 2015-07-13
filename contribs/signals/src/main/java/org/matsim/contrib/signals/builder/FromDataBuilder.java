/* *********************************************************************** *
 * project: org.matsim.*
 * FromDataBuilder
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.signals.builder;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.contrib.signals.model.AmberLogicImpl;
import org.matsim.contrib.signals.model.DatabasedSignal;
import org.matsim.contrib.signals.model.IntergreensLogicImpl;
import org.matsim.contrib.signals.model.SignalGroupImpl;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.model.AmberLogic;
import org.matsim.contrib.signals.model.IntergreensLogic;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalController;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.model.SignalSystemsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;

/**
 * @author dgrether
 *
 */
public class FromDataBuilder implements SignalSystemsModelBuilder{

	private SignalsData signalsData;
	private SignalModelFactory factory = new DefaultSignalModelFactory();
	private EventsManager events;
	private Scenario scenario;

	public FromDataBuilder(Scenario scenario, SignalModelFactory factory, EventsManager events){
		this.signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		this.scenario = scenario;
		this.factory = factory;
		this.events = events;
	}
	
	public FromDataBuilder(Scenario scenario, EventsManager events){
		this(scenario, new DefaultSignalModelFactory(), events);
	}
	
	public void createAndAddSignals(SignalSystem system){
		SignalSystemData ssData = signalsData.getSignalSystemsData().getSignalSystemData().get(system.getId());
		for (SignalData signalData : ssData.getSignalData().values()){
			Signal signal = new DatabasedSignal(signalData);
			system.addSignal(signal);
		}
	}
	
	public void createAndAddSignalSystemsFromData(SignalSystemsManager manager){
		//process information of SignalSystemsData object
		for (SignalSystemData ssData : this.signalsData.getSignalSystemsData().getSignalSystemData().values()){
			SignalSystem system = this.factory.createSignalSystem(ssData.getId());
			manager.addSignalSystem(system);
			system.setSignalSystemsManager(manager);
		}
	}
	

	public void createAndAddSignalGroupsFromData(SignalSystem system){
		//process information of  SignalGroupsData object and create the signal groups
		Map<Id<SignalGroup>, SignalGroupData> signalGroupDataMap = this.signalsData.getSignalGroupsData().getSignalGroupDataBySystemId(system.getId());
		for (SignalGroupData signalGroupData : signalGroupDataMap.values()){
			SignalGroup group = new SignalGroupImpl(signalGroupData.getId());
			for (Id<Signal> signalId : signalGroupData.getSignalIds()){
				Signal signal = system.getSignals().get(signalId);
				group.addSignal(signal);
			}
			system.addSignalGroup(group);
		}
	}
	
	public void createAndAddSignalSystemControllerFromData(SignalSystem system){
		//process information of SignalControlData
		SignalSystemControllerData systemControlData = signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().get(system.getId());
		SignalController controller = this.factory.createSignalSystemController(systemControlData.getControllerIdentifier());
		controller.setSignalSystem(system);
		system.setSignalSystemController(controller);
		if (systemControlData.getSignalPlanData() != null) { 
			for (SignalPlanData planData : systemControlData.getSignalPlanData().values()){
				SignalPlan plan = this.factory.createSignalPlan(planData);
				controller.addPlan(plan);
			}
		}
	}
	
	public void createAndAddAmberLogic(SignalSystemsManager manager){
		//process information of AmberTimesData object
		if (ConfigUtils.addOrGetModule(this.scenario.getConfig(), SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).isUseAmbertimes()){
			AmberLogic amberLogic = new AmberLogicImpl(this.signalsData.getAmberTimesData());
			manager.setAmberLogic(amberLogic);
		}
	}
	
	public SignalSystemsManager createSignalSystemManager(){
		SignalSystemsManager manager = this.factory.createSignalSystemsManager();
		manager.setSignalsData(this.signalsData);
		manager.setEventsManager(events);
		return manager;
	}
	
	public void createAndAddIntergreenTimesLogic(SignalSystemsManager manager){
		if (ConfigUtils.addOrGetModule(this.scenario.getConfig(), SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).isUseIntergreenTimes()){
			IntergreensLogic intergreensLogic = new IntergreensLogicImpl(this.signalsData.getIntergreenTimesData(), ConfigUtils.addOrGetModule(this.scenario.getConfig(), SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class));
			this.events.addHandler(intergreensLogic);
		}
	}
	
	@Override
	public SignalSystemsManager createAndInitializeSignalSystemsManager() {
		//1.) SignalSystemsManager
		SignalSystemsManager manager = this.createSignalSystemManager();
		//2.) SignalSystems
		this.createAndAddSignalSystemsFromData(manager);
		//3.) Signals then SignalGroups then SignalController
		for (SignalSystem system : manager.getSignalSystems().values()){
			this.createAndAddSignals(system);
			this.createAndAddSignalGroupsFromData(system);
			this.createAndAddSignalSystemControllerFromData(system);
		}
		//4.) AmberLogic
		this.createAndAddAmberLogic(manager);
		//5.) IntergreenTimesLogic 
		this.createAndAddIntergreenTimesLogic(manager);
		return manager;
	}

	@Override
	public SignalModelFactory getSignalModelFactory() {
		return this.factory;
	}

	@Override
	public void setSignalModelFactory(SignalModelFactory factory) {
		this.factory = factory;
	}
	
	
	
}
