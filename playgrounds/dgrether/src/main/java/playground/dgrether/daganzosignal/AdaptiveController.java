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

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.LaneEnterEvent;
import org.matsim.core.events.LaneLeaveEvent;
import org.matsim.core.events.SignalGroupStateChangedEventImpl;
import org.matsim.core.events.handler.LaneEnterEventHandler;
import org.matsim.core.events.handler.LaneLeaveEventHandler;
import org.matsim.core.gbl.MatsimRandom;
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
public class AdaptiveController extends
		AdaptiveSignalSystemControlerImpl implements LaneEnterEventHandler, 
		LaneLeaveEventHandler, SimulationBeforeSimStepListener, SimulationInitializedListener {
	
	private static final Logger log = Logger.getLogger(AdaptiveController.class);

	private final Id id1 = new IdImpl("1");
	private final Id id4 = new IdImpl("4");
	private final Id id5 = new IdImpl("5");
	private int vehOnLink5Lane1 = 0;

	private Random random;
	
	private double pSignal;
	
	public AdaptiveController(AdaptiveSignalSystemControlInfo controlInfo) {
		super(controlInfo);
		this.random = MatsimRandom.getLocalInstance();
	}

	@Override
	public void setSignalEngine(SignalEngine signalEngine) {
		super.setSignalEngine(signalEngine);
		Config config = this.getSignalEngine().getMobsim().getScenario().getConfig();
	  pSignal = Double.parseDouble(config.getModule(DaganzoScenarioGenerator.CONFIG_MODULE).getValue(DaganzoScenarioGenerator.PSIGNAL_CONFIG_PARAMETER));
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
  	double rand = random.nextDouble();
  	for (SignalGroupDefinition signalGroup : this.getSignalGroups().values()){
      SignalGroupState oldState = this.getSignalGroupStates().get(signalGroup);
      SignalGroupState newState = null;
      if (signalGroup.getLinkRefId().equals(id5)){
        if (vehOnLink5Lane1 > 0 && (rand >= pSignal)/**/) {
          newState = SignalGroupState.GREEN;
        }
        else {
          newState = SignalGroupState.RED;
        }
      }
      else if (signalGroup.getLinkRefId().equals(id4)){
        if (vehOnLink5Lane1 > 0 && (rand >= pSignal)/**/){
          newState = SignalGroupState.RED;
        }
        else {
          newState = SignalGroupState.GREEN;
        }
      }
      //save the new state
      this.getSignalGroupStates().put(signalGroup, newState);

      //fire the event
      if (!newState.equals(oldState)){
        this.getSignalEngine().getEvents().processEvent(
            new SignalGroupStateChangedEventImpl(e.getSimulationTime(), signalGroup.getSignalSystemDefinitionId(), 
                signalGroup.getId(), newState));
      }
    }
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
