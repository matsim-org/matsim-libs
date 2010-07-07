/* *********************************************************************** *
 * project: org.matsim.*
 * AdaptiveController
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
package playground.dgrether.daganzosignal;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.Module;
import org.matsim.core.events.LaneEnterEvent;
import org.matsim.core.events.LaneLeaveEvent;
import org.matsim.core.events.SignalGroupStateChangedEventImpl;
import org.matsim.core.events.handler.LaneEnterEventHandler;
import org.matsim.core.events.handler.LaneLeaveEventHandler;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.signalsystems.config.AdaptiveSignalSystemControlInfo;
import org.matsim.signalsystems.control.AdaptiveSignalSystemControlerImpl;
import org.matsim.signalsystems.control.SignalGroupState;
import org.matsim.signalsystems.mobsim.SignalEngine;
import org.matsim.signalsystems.systems.SignalGroupDefinition;


/**
 * @author dgrether
 *
 */
public class AdaptiveController3 extends
		AdaptiveSignalSystemControlerImpl implements LaneEnterEventHandler, 
		LaneLeaveEventHandler, SimulationBeforeSimStepListener, SimulationInitializedListener {
	
	private static final Logger log = Logger.getLogger(AdaptiveController3.class);

	private final Id id1 = new IdImpl("1");
	private final Id id2 = new IdImpl("2");
//	private final Id id4 = new IdImpl("4");
	private final Id id5 = new IdImpl("5");
	private int vehOnLink5Lane1 = 0;
	
	/**
	 * Initialize split with a default, is not used if value is set in config
	 */
	private double splitSg1Link4 = 0.50;
	
	private double cycle = 100.0;

	private double greenTimeSg1Link4;
	private double greenTimeSg2Link5;
	
//	private double pSignal;
	
	SignalGroupDefinition sg1, sg2;

	private double lastSwitch;
	
	public AdaptiveController3(AdaptiveSignalSystemControlInfo controlInfo) {
		super(controlInfo);
		this.calculateGreenTimes();
	}

	private void calculateGreenTimes() {
		this.greenTimeSg1Link4 = this.splitSg1Link4 * this.cycle;
		this.greenTimeSg2Link5 = this.cycle - this.greenTimeSg1Link4;
	}

	@Override
	public void setSignalEngine(SignalEngine signalEngine) {
		super.setSignalEngine(signalEngine);
	}
	
	private void initParametersFromConfig(){
		Config config = this.getSignalEngine().getQSim().getScenario().getConfig();
	  Module m = config.getModule(DaganzoScenarioGenerator.CONFIG_MODULE);
//		String pSignalString = m.getValue(DaganzoScenarioGenerator.PSIGNAL_CONFIG_PARAMETER);
//		if (pSignalString != null) {
//			this.pSignal = Double.parseDouble(pSignalString);
//			log.info("Using pSignal: " + pSignal);
//		}
		String splitSg1String = m.getValue(DaganzoScenarioGenerator.SPLITSG1LINK4_CONFIG_PARAMETER);
		if (splitSg1String != null){
			this.splitSg1Link4 = Double.parseDouble(splitSg1String);
			this.calculateGreenTimes();
			log.info("Using splitSg1Link4: " + this.splitSg1Link4 + " and thus a green time for sg1 of " + this.greenTimeSg1Link4 + " in a cycle of " + this.cycle);
		}
	}
	

	public void handleEvent(LaneEnterEvent e) {
		if (e.getLinkId().equals(id5) && e.getLaneId().equals(id1)) {
			this.vehOnLink5Lane1++;
		}
	}
	public void handleEvent(LaneLeaveEvent e) {
		if (e.getLinkId().equals(id5) && e.getLaneId().equals(id1)) {
			this.vehOnLink5Lane1--;
		}
	}

	public void reset(int iteration) {
	}

  @Override
  public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent e) {
		SignalGroupState currentsg1state = this.getSignalGroupStates().get(this.sg1);
  	double currentTime = e.getSimulationTime();
		if (currentsg1state.equals(SignalGroupState.RED)) {
  		if (this.vehOnLink5Lane1 == 0 && (this.lastSwitch + this.greenTimeSg2Link5 <= currentTime)){
    		this.getSignalGroupStates().put(sg1, SignalGroupState.GREEN);
    		this.getSignalGroupStates().put(sg2, SignalGroupState.RED);
    		fireStateChanged(e.getSimulationTime(), sg1, SignalGroupState.GREEN);
    		fireStateChanged(e.getSimulationTime(), sg2, SignalGroupState.RED);
    		this.lastSwitch = currentTime;
  		}
  	}
  	else {
  		if ((this.lastSwitch + this.greenTimeSg1Link4) <= currentTime){
    		this.getSignalGroupStates().put(sg1, SignalGroupState.RED);
    		this.getSignalGroupStates().put(sg2, SignalGroupState.GREEN);
    		fireStateChanged(e.getSimulationTime(), sg1, SignalGroupState.RED);
    		fireStateChanged(e.getSimulationTime(), sg2, SignalGroupState.GREEN);
    		this.lastSwitch = currentTime;
  		}
  	}	
  }

  private void fireStateChanged(double simulationTime, SignalGroupDefinition sg, SignalGroupState state) {
    this.getSignalEngine().getEvents().processEvent(
        new SignalGroupStateChangedEventImpl(simulationTime, sg.getSignalSystemDefinitionId(), 
            sg.getId(), state));
	}

	@Override
  public SignalGroupState getSignalGroupState(double seconds,
      SignalGroupDefinition signalGroup) {
    return this.getSignalGroupStates().get(signalGroup);
  }


  @Override
  public void notifySimulationInitialized(SimulationInitializedEvent e) {
		this.initParametersFromConfig();
		this.sg1 = this.getSignalGroups().get(id1);
		this.sg2 = this.getSignalGroups().get(id2);
    this.getSignalGroupStates().put(this.sg1, SignalGroupState.GREEN);
    this.getSignalGroupStates().put(this.sg2, SignalGroupState.RED);
    this.lastSwitch = ((QSim)e.getQueueSimulation()).getSimTimer().getSimStartTime();
		fireStateChanged(this.lastSwitch, sg1, SignalGroupState.GREEN);
		fireStateChanged(this.lastSwitch, sg2, SignalGroupState.RED);
  }

}
