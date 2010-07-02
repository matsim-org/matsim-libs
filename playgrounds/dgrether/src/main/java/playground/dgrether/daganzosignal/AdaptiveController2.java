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
import org.matsim.signalsystems.config.AdaptiveSignalSystemControlInfo;
import org.matsim.signalsystems.control.AdaptiveSignalSystemControlerImpl;
import org.matsim.signalsystems.control.SignalGroupState;
import org.matsim.signalsystems.mobsim.SignalEngine;
import org.matsim.signalsystems.systems.SignalGroupDefinition;


/**
 * @author dgrether
 *
 */
public class AdaptiveController2 extends
		AdaptiveSignalSystemControlerImpl implements LaneEnterEventHandler, 
		LaneLeaveEventHandler, SimulationBeforeSimStepListener, SimulationInitializedListener {
	
	private static final Logger log = Logger.getLogger(AdaptiveController2.class);

	private final Id id1 = new IdImpl("1");
	private final Id id2 = new IdImpl("2");
	private final Id id4 = new IdImpl("4");
	private final Id id5 = new IdImpl("5");
	private int vehOnLink5Lane1 = 0;
	
	private double splitSg1 = 0.50;
	
	private double cycle = 60.0;

	private double greenTimeSg1 = calculateGreenTimeSg1();
	
	private double pSignal;
	
	public AdaptiveController2(AdaptiveSignalSystemControlInfo controlInfo) {
		super(controlInfo);
	}

	private double calculateGreenTimeSg1() {
		return this.splitSg1 * this.cycle;
	}

	@Override
	public void setSignalEngine(SignalEngine signalEngine) {
		super.setSignalEngine(signalEngine);
		this.initParametersFromConfig();
	}
	
	private void initParametersFromConfig(){
		Config config = this.getSignalEngine().getQSim().getScenario().getConfig();
	  Module m = config.getModule(DaganzoScenarioGenerator.CONFIG_MODULE);
		String pSignalString = m.getValue(DaganzoScenarioGenerator.PSIGNAL_CONFIG_PARAMETER);
		if (pSignalString != null) {
			this.pSignal = Double.parseDouble(pSignalString);
			log.info("Using pSignal: " + pSignal);
		}
		String splitSg1String = m.getValue(DaganzoScenarioGenerator.SPLITSG1LINK4_CONFIG_PARAMETER);
		if (splitSg1String != null){
			this.splitSg1 = Double.parseDouble(splitSg1String);
			this.greenTimeSg1 = this.calculateGreenTimeSg1();
			log.info("Using splitSg1Link4: " + this.splitSg1 + " and thus a green time for sg1 of " + this.greenTimeSg1 + " in a cycle of " + this.cycle);
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
  	double rest =  e.getSimulationTime() % this.cycle;
		SignalGroupDefinition sg1 = this.getSignalGroups().get(id1);
		SignalGroupState currentsg1state = this.getSignalGroupStates().get(sg1);
  	SignalGroupDefinition sg2 = this.getSignalGroups().get(id2);
  	if (currentsg1state.equals(SignalGroupState.RED) && rest < this.greenTimeSg1){
  		this.getSignalGroupStates().put(sg1, SignalGroupState.GREEN);
  		this.getSignalGroupStates().put(sg2, SignalGroupState.RED);
  		fireStateChanged(e.getSimulationTime(), sg1, SignalGroupState.GREEN);
  		fireStateChanged(e.getSimulationTime(), sg2, SignalGroupState.RED);
  	}
  	else if (currentsg1state.equals(SignalGroupState.GREEN) && rest >= this.greenTimeSg1) {
  		this.getSignalGroupStates().put(sg1, SignalGroupState.RED);
  		this.getSignalGroupStates().put(sg2, SignalGroupState.GREEN);
  		fireStateChanged(e.getSimulationTime(), sg1, SignalGroupState.RED);
  		fireStateChanged(e.getSimulationTime(), sg2, SignalGroupState.GREEN);
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
    for (SignalGroupDefinition signalGroup : this.getSignalGroups().values()){
      if (signalGroup.getLinkRefId().equals(id5)){
        this.getSignalGroupStates().put(signalGroup, SignalGroupState.RED);
      }
      else {
        this.getSignalGroupStates().put(signalGroup, SignalGroupState.GREEN);
      }
    }  
  }

}
