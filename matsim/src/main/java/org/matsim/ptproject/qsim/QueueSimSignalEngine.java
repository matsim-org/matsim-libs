/* *********************************************************************** *
 * project: org.matsim.*
 * QueueSimLaneEngine
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
package org.matsim.ptproject.qsim;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.queuesim.SignalEngine;
import org.matsim.core.mobsim.queuesim.listener.QueueSimulationListener;
import org.matsim.evacuation.shelters.signalsystems.SheltersDoorBlockerController;
import org.matsim.signalsystems.config.BasicAdaptivePlanBasedSignalSystemControlInfo;
import org.matsim.signalsystems.config.BasicAdaptiveSignalSystemControlInfo;
import org.matsim.signalsystems.config.BasicPlanBasedSignalSystemControlInfo;
import org.matsim.signalsystems.config.BasicSignalSystemConfiguration;
import org.matsim.signalsystems.config.BasicSignalSystemConfigurations;
import org.matsim.signalsystems.control.AdaptivePlanBasedSignalSystemControler;
import org.matsim.signalsystems.control.AdaptiveSignalSystemControler;
import org.matsim.signalsystems.control.DefaultPlanBasedSignalSystemController;
import org.matsim.signalsystems.control.PlanBasedSignalSystemController;
import org.matsim.signalsystems.control.SignalSystemController;
import org.matsim.signalsystems.systems.SignalGroupDefinition;
import org.matsim.signalsystems.systems.SignalSystemDefinition;
import org.matsim.signalsystems.systems.SignalSystems;


public class QueueSimSignalEngine implements SignalEngine {
	
	
	private static final Logger log = Logger.getLogger(QueueSimSignalEngine.class);

	/**
	 * The SignalSystemDefinitions accessible by their Id
	 */
	private SortedMap<Id, SignalSystemDefinition> signalSystemDefinitions;
	/**
	 * The SignalGroupDefinitions accessible by the Id of the SignalSystem they belong
	 * to.
	 */
	private SortedMap<Id, List<SignalGroupDefinition>> signalGroupDefinitionsBySystemId;
	/**
	 * Contains the SignalSystemControler instances which can be accessed by the
	 * Id of the SignalSystemDefinition
	 */
	private SortedMap<Id, SignalSystemController> signalSystemControlerBySystemId;

	private SignalSystems signalSystems;

	private BasicSignalSystemConfigurations signalSystemsConfig;

	private QueueNetwork network;

	private QueueSimulation simulation;

	private EventsManager events;

	public QueueSimSignalEngine(QueueSimulation sim) {
		this.simulation = sim;
		this.network = sim.getQueueNetwork();
		this.events = QueueSimulation.getEvents();
	}

	public void setSignalSystems(final SignalSystems signalSystems, final BasicSignalSystemConfigurations basicSignalSystemConfigurations){
		this.signalSystems = signalSystems;
		this.signalSystemsConfig = basicSignalSystemConfigurations;
	}
	
	/**
	 * Initialize the signal systems
	 */
	protected void prepareSignalSystems() {
		if (this.signalSystems != null) {
			initSignalSystems(this.signalSystems);
		}
		if (this.signalSystemsConfig != null) {
			initSignalSystemController(this.signalSystemsConfig);
		}
	}
	
	/**
	 * @see org.matsim.ptproject.qsim.SignalEngine#getSignalSystemControlerBySystemId()
	 */
	public SortedMap<Id, SignalSystemController> getSignalSystemControlerBySystemId() {
		return this.signalSystemControlerBySystemId;
	}


	/**
	 * @see org.matsim.ptproject.qsim.SignalEngine#getSignalSystemDefinitions()
	 */
	public SortedMap<Id, SignalSystemDefinition> getSignalSystemDefinitions() {
		return this.signalSystemDefinitions;
	}
	

	private void initSignalSystems(final SignalSystems signalSystems) {
		//store the signalSystemDefinitions in a Map
		this.signalSystemDefinitions = new TreeMap<Id, SignalSystemDefinition>();
		this.signalSystemDefinitions.putAll(signalSystems.getSignalSystemDefinitions());
		//init the signalGroupDefinitions
		this.signalGroupDefinitionsBySystemId= new TreeMap<Id, List<SignalGroupDefinition>>();
		for (SignalGroupDefinition signalGroupDefinition : signalSystems.getSignalGroupDefinitions().values()) {
			QueueLink queueLink = this.network.getQueueLink(signalGroupDefinition.getLinkRefId());
			if (queueLink == null) {
				throw new IllegalStateException("SignalGroupDefinition Id: " + signalGroupDefinition.getId() + " of SignalSystem Id:  " + signalGroupDefinition.getSignalSystemDefinitionId() + " is set to non existing Link with Id: " + signalGroupDefinition.getLinkRefId());
			}
			if (signalGroupDefinition.getSignalSystemDefinitionId() == null) {
				log.warn("SignalGroupDefinition Id: " + signalGroupDefinition.getId() + " is not attached to a SignalSystem (SignalSystemDefinitionId not set). SignalGroup will not be used!");
				continue;
			}
			if (signalGroupDefinition.getLaneIds() == null) {
				log.warn("SignalGroupDefinition Id: " + signalGroupDefinition.getId() + " of SignalSystem Id:  " + signalGroupDefinition.getSignalSystemDefinitionId() + " is not attached to a lane. SignalGroup will not be used!");
				continue;
			}
			List<SignalGroupDefinition> list = this.signalGroupDefinitionsBySystemId.get(signalGroupDefinition.getSignalSystemDefinitionId());
			if (list == null) {
				list = new ArrayList<SignalGroupDefinition>();
				this.signalGroupDefinitionsBySystemId.put(signalGroupDefinition.getSignalSystemDefinitionId(), list);
			}
			list.add(signalGroupDefinition);
			queueLink.addSignalGroupDefinition(signalGroupDefinition);
			this.network.getNodes().get(queueLink.getLink().getToNode().getId()).setSignalized(true);
		}
	}

	private void initSignalSystemController(final BasicSignalSystemConfigurations basicSignalSystemConfigurations) {
		this.signalSystemControlerBySystemId = new TreeMap<Id, SignalSystemController>();
		for (BasicSignalSystemConfiguration config :
			basicSignalSystemConfigurations.getSignalSystemConfigurations().values()) {
			SignalSystemController systemControler = null;
			if (this.signalSystemControlerBySystemId.containsKey(config.getSignalSystemId())){
				throw new IllegalStateException("SignalSystemControler for SignalSystem with id: " + config.getSignalSystemId() +
						" already exists. Cannot add second SignalSystemControler for same system. Check your" +
						" signal system's configuration file.");
			}
			if (config.getControlInfo() instanceof BasicAdaptivePlanBasedSignalSystemControlInfo) {
				AdaptiveSignalSystemControler c = createAdaptiveControler((BasicAdaptiveSignalSystemControlInfo)config.getControlInfo());
				if (!(c instanceof PlanBasedSignalSystemController)){
					throw new IllegalArgumentException("Class " + c.getClass().getName() + "is no PlanBasedSignalSystemController instance. Check your configuration of the signal system control!");
				}
				AdaptivePlanBasedSignalSystemControler controler = (AdaptivePlanBasedSignalSystemControler) c;
				systemControler = controler;
			}
			else if (config.getControlInfo() instanceof BasicAdaptiveSignalSystemControlInfo) {
				AdaptiveSignalSystemControler controler = createAdaptiveControler((BasicAdaptiveSignalSystemControlInfo)config.getControlInfo());
				systemControler = controler;
			}
			else if (config.getControlInfo() instanceof BasicPlanBasedSignalSystemControlInfo){
				DefaultPlanBasedSignalSystemController controler = new DefaultPlanBasedSignalSystemController(config);
				systemControler = controler;
			}
			if (systemControler != null){
				this.initSignalSystemControlerDefaults(systemControler, config);
				this.signalSystemControlerBySystemId.put(config.getSignalSystemId(), systemControler);
				systemControler.setSignalEngine(this);
				if (systemControler instanceof QueueSimulationListener) {
					this.simulation.addQueueSimulationListeners((QueueSimulationListener)systemControler);
				}
				//add controller to signal groups
				List<SignalGroupDefinition> groups = this.signalGroupDefinitionsBySystemId.get(config.getSignalSystemId());
				if ((groups == null) || groups.isEmpty()) {
					String message = "SignalSystemControler for SignalSystem Id: " + config.getSignalSystemId() + " without any SignalGroups defined in SignalSystemConfiguration!";
					log.warn(message);
				}
				else {
					for (SignalGroupDefinition group : groups){
						systemControler.getSignalGroups().put(group.getId(), group);
						group.setResponsibleLSAControler(systemControler);
					}
				}
				
				
			}
			else {
				log.error("Could not initialize signal system controler for signal system with id: " + config.getSignalSystemId() + " " +
						"Check stacktrace for details.");
			}
		}
	}

	private AdaptiveSignalSystemControler createAdaptiveControler(
			final BasicAdaptiveSignalSystemControlInfo config) {
		String controllerName = config.getAdaptiveControlerClass();
		AdaptiveSignalSystemControler controler = null;
		if (controllerName == null){
			throw new IllegalArgumentException("controler class must be given");
		}
		if (controllerName.startsWith("org.matsim")){
			//when we have standardized code for adaptive control
			//within org.matsim here is the point to create those controlers
			throw new IllegalArgumentException("Loading classes by name within the org.matsim packages is not allowed!");
		}
		else if (controllerName.equalsIgnoreCase("SheltersDoorBlockerController")){
			controler = new SheltersDoorBlockerController(config);
		}
		else {
			try {
				Class<? extends AdaptiveSignalSystemControler> klas = (Class<? extends AdaptiveSignalSystemControler>) Class.forName(config.getAdaptiveControlerClass());
				Class[] args = new Class[1];
				args[0] = BasicAdaptiveSignalSystemControlInfo.class;
				Constructor<? extends AdaptiveSignalSystemControler> c = klas.getConstructor(args);
				controler = c.newInstance(config);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		if (controler == null){
			throw new IllegalStateException("Cannot create AdaptiveSignalSystemControler for class name: " + config.getAdaptiveControlerClass());
		}
		return controler;
	}



	private void initSignalSystemControlerDefaults(final SignalSystemController controler, final BasicSignalSystemConfiguration config){
		SignalSystemDefinition systemDef = this.signalSystemDefinitions.get(config.getSignalSystemId());
		controler.setDefaultCycleTime(systemDef.getDefaultCycleTime());
		controler.setDefaultInterGreenTime(systemDef.getDefaultInterGreenTime());
		controler.setDefaultSynchronizationOffset(systemDef.getDefaultSynchronizationOffset());
	}

	public EventsManager getEvents() {
		return this.events;
	}
	
}
