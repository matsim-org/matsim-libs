/* *********************************************************************** *
 * project: org.matsim.*
 * JbSignalBuilder
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
package playground.jbischoff.BAsignals.builder;

import org.apache.log4j.Logger;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.builder.SignalSystemsModelBuilder;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalSystemControllerData;
import org.matsim.signalsystems.model.DatabasedSignalPlan;
import org.matsim.signalsystems.model.SignalController;
import org.matsim.signalsystems.model.SignalPlan;
import org.matsim.signalsystems.model.SignalSystem;
import org.matsim.signalsystems.model.SignalSystemsManager;

import playground.jbischoff.BAsignals.model.AdaptiveControllHead;
import playground.jbischoff.BAsignals.model.CarsOnLaneHandler;
import playground.jbischoff.BAsignals.model.JbSignalController;

/**
 * @author dgrether
 *
 */
public class JbSignalBuilder implements SignalSystemsModelBuilder {
	private static final Logger log = Logger.getLogger(JbSignalBuilder.class);

	private FromDataBuilder dataBuilder;
	private SignalsData signalsData;
	private AdaptiveControllHead adaptiveControllHead;
	private CarsOnLaneHandler collh;
	
	public JbSignalBuilder(SignalsData signalsData, FromDataBuilder dataBuilder, CarsOnLaneHandler carsOnLaneHandler, AdaptiveControllHead ach){
		this.dataBuilder = dataBuilder;
		this.signalsData = signalsData;
		this.adaptiveControllHead = ach;
		this.collh = carsOnLaneHandler;
	}
	
	@Override
	public SignalSystemsManager createAndInitializeSignalSystemsManager() {
//		collh = new CarsOnLaneHandler();
		collh.setAdaptiveControllHead(this.adaptiveControllHead);
		//1.) SignalSystemsManager
		SignalSystemsManager manager = dataBuilder.createSignalSystemManager();
		//2.) SignalSystems
		dataBuilder.createAndAddSignalSystemsFromData(manager);
		manager.getEventsManager().addHandler(collh);
		
		//3.) Signals then SignalGroups then SignalController
		for (SignalSystem system : manager.getSignalSystems().values()){
			dataBuilder.createAndAddSignals(system);
			dataBuilder.createAndAddSignalGroupsFromData(system);
			this.createAndAddSignalSystemControllerFromData(system);
		}
//		adaptiveControllHead.sizeDownPlans(45);
		//4.) AmberLogic
		dataBuilder.createAndAddAmberLogic(manager);
		return manager;
	}
	

	public void createAndAddSignalSystemControllerFromData(SignalSystem system){
		//process information of SignalControlData
		SignalSystemControllerData systemControlData = signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().get(system.getId());
		if (systemControlData.getControllerIdentifier().equals("JBSignalController")){
			this.adaptiveControllHead.addAdaptiveSignalSystem(system, systemControlData);
			log.info("Treating sigsy: "+system.getId() +" as adaptive");
			this.collh.addSystem(system);
			SignalController controller = new JbSignalController(this.adaptiveControllHead);
			controller.setSignalSystem(system);
			system.setSignalSystemController(controller);
			for (SignalPlanData planData : systemControlData.getSignalPlanData().values()){
				SignalPlan plan = new DatabasedSignalPlan(planData);
				controller.addPlan(plan);
			}
		}
		else {
			this.dataBuilder.createAndAddSignalSystemControllerFromData(system);
		}
		
	}

	public CarsOnLaneHandler getCollh() {
		return collh;
	}




}
