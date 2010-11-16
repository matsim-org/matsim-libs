/* *********************************************************************** *
 * project: org.matsim.*
 * JbSignalController
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
package playground.jbischoff.BAsignals;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
//import org.matsim.signalsystems.control.SignalGroupState;
import org.matsim.signalsystems.model.SignalController;
import org.matsim.signalsystems.model.SignalGroup;
import org.matsim.signalsystems.model.SignalGroupState;
import org.matsim.signalsystems.model.SignalPlan;
import org.matsim.signalsystems.model.SignalSystem;

/**
 * @author dgrether
 * @author jbischoff
 */

public class JbSignalController implements SignalController {
	public static final String IDENTIFIER = "JBSignalController";
	
	private static final Logger log = Logger
			.getLogger(JbSignalController.class);

	private SignalSystem system;
	private Map<Id, SignalPlan> plans;
	private JbSignalPlan activePlan;
	private Map<Id, Integer> adaptiveOnsets;
	private Map<Id, Integer> adaptiveDroppings;
	private AdaptiveControllHead adaptiveControllHead;
	private Map<Double, List<Id>> gapsAtSecond;
	private int availableStretchTime;
	private Map<Id, Integer> maxDrop;
	private Map<Id, Integer> minDrop;
	private Map<Id, Integer> minOn;
	private Map<Id, Integer> originalGreenTimes;

	public JbSignalController(AdaptiveControllHead ach) {
		this.adaptiveDroppings = new HashMap<Id, Integer>();
		this.adaptiveOnsets = new HashMap<Id, Integer>();
		this.maxDrop = new HashMap<Id, Integer>();
		this.minDrop = new HashMap<Id, Integer>();
		this.minOn = new HashMap<Id, Integer>();
		this.adaptiveControllHead = ach;
		this.gapsAtSecond = new HashMap<Double, List<Id>>();

	}

	private void fillOriginalGreenTimes(){
		for (Entry<Id,Integer> e : this.maxDrop.entrySet()){
			int ogt = e.getValue() - this.adaptiveControllHead.getMaxOnset().get(e.getKey());
			this.originalGreenTimes.put(e.getKey(),ogt);
			
		}
	}
	
	@Override
	public void addPlan(SignalPlan plan) {
		if (this.plans == null) {
			this.plans = new HashMap<Id, SignalPlan>();
			// TODO remove when checkActive is implemented
			this.activePlan = (JbSignalPlan) plan;
		}
		this.plans.put(plan.getId(), plan);
	}

	private void checkActivePlan() {
		// TODO implement active plan logic
	}

	@Override
	public void setSignalSystem(SignalSystem system) {
		this.system = system;
		if (this.adaptiveControllHead.signalSystemIsAdaptive(this.system)) {
			for (Id sgid : this.system.getSignalGroups().keySet()) {
				this.maxDrop.put(sgid, this.adaptiveControllHead
						.getMaxDropping().get(sgid));
				this.minDrop.put(sgid, this.adaptiveControllHead
						.getMinDropping().get(sgid));
				this.minOn.put(sgid, this.adaptiveControllHead.getMinOnset()
						.get(sgid));
				// log.info("m add "+sgid+" ; "+this.minOn.get(sgid));
			}
		}

	}

	@Override
	public void updateState(double timeSeconds) {
		
		this.checkActivePlan();
		int currentSecondinPlan = ((int) (timeSeconds) % this.activePlan
				.getCylce());
		if (currentSecondinPlan == 0)
			this.resetAdaptiveSignals();
		boolean artlong = true;
		for (Id sgId : this.getGapListatSecond(timeSeconds)) {
			if (this.adaptiveDroppings.get(sgId) < this.maxDrop.get(sgId)) {
				this.postPoneAdaptiveOffSet(sgId);
				artlong = false;
			}
		}

		if (this.adaptiveControllHead.signalSystemIsAdaptive(this.system)) {
			for (Id gId : this.system.getSignalGroups().keySet()) {
				if (artlong
						&& this.availableStretchTime > 0
						&& this.system.getSignalGroups().get(gId).getState() == SignalGroupState.GREEN
						&& this.originalGreenTimes.get(gId)>(this.adaptiveDroppings.get(gId)-this.adaptiveOnsets.get(gId))
							) {
					this.postPoneOffSet(gId, 1);
					artlong = false;
				}
				
//				log.error(currentSecondinPlan+" on "+this.adaptiveOnsets.get(gId)+"for gid"+gId);
				if (this.adaptiveOnsets.get(gId) == currentSecondinPlan) {
					this.system.scheduleOnset(timeSeconds, gId);
//					log.info("scheduling onset at " + currentSecondinPlan	+  ", sg " + gId);
				}
				if (this.adaptiveDroppings.get(gId) == currentSecondinPlan)
					{this.system.scheduleDropping(timeSeconds, gId);
//					log.info("scheduling drop at " + currentSecondinPlan	+  ", sg " + gId);
			}}

		} else
			this.updateNonAdaptiveStates(timeSeconds);

		if (artlong && this.availableStretchTime > 0)
			this.availableStretchTime--;

	}

	private void postPoneAdaptiveOffSet(Id sgId) {
		if ((this.adaptiveDroppings.get(sgId) - this.adaptiveOnsets.get(sgId)) <= JBBaParams.MAXPHASELENGTH) {
			int oldmd = this.maxDrop.get(sgId);
			oldmd = oldmd + 2;
			if (oldmd < this.activePlan.getCylce()-2)
				this.maxDrop.put(sgId, oldmd);
			
		}
		int ps;
		if (this.adaptiveDroppings.get(sgId) + JBBaParams.PHASESTEPPROLONGER < this.maxDrop
				.get(sgId)) {
			// log.info("got it 3 ast" + this.availableStretchTime);
			ps = JBBaParams.PHASESTEPPROLONGER;
		} else {
			ps = this.maxDrop.get(sgId) - this.adaptiveDroppings.get(sgId);
			// log.info("got it 4 ast" + this.availableStretchTime);

		}
		this.postPoneOffSet(sgId, ps);
	}

	private void postPoneOffSet(Id sgId, int step) {
		if (this.availableStretchTime > 0) {
			int currentdrop = this.adaptiveDroppings.get(sgId);
			int newdrop = currentdrop + step;
			this.adaptiveDroppings.put(sgId, newdrop);
			// if (step>1)
			// log.info("Drop of Sg "+sgId+" shifted from "+currentdrop+" to "+newdrop);
			this.updateOtherSignalGroups(sgId, step);
		}
	}

	private void updateOtherSignalGroups(Id sgId, int step) {
		for (SignalGroup otherSg : this.system.getSignalGroups().values()) {
			if (!otherSg.getId().equals(sgId)) {
				if (otherSg.getState() == SignalGroupState.GREEN
						| otherSg.getState() == SignalGroupState.REDYELLOW) {
					int currentdrop = this.adaptiveDroppings.get(otherSg
							.getId());
					int newdrop = currentdrop + step;
					this.adaptiveDroppings.put(otherSg.getId(), newdrop);
					// if (step>1)
					// log.info("Drop of Sg "+otherSg.getId()+" shifted from "+currentdrop+" to "+newdrop);
				} else {
					int currentonset = this.adaptiveOnsets.get(otherSg.getId());
					int currentdrop = this.adaptiveDroppings.get(otherSg
							.getId());
					int newdrop = currentdrop + step;
					if (newdrop > this.activePlan.getCylce())
						newdrop = this.activePlan.getCylce() - 1;
					this.adaptiveDroppings.put(otherSg.getId(), newdrop);
					int newonset = currentonset + step;
					this.adaptiveOnsets.put(otherSg.getId(), newonset);
					// if (step>1)
					// log.info("Onset of Sg "+otherSg.getId()+" shifted from "+currentonset+" to "+newonset);
				}
			}
		}
	}

	private void resetAdaptiveSignals() {

		this.adaptiveDroppings.clear();
		this.adaptiveOnsets.clear();
		for (Entry<Id, Integer> e : this.minDrop.entrySet()) {
			this.adaptiveDroppings.put(e.getKey(), e.getValue());
		}
		for (Entry<Id, Integer> e : this.minOn.entrySet()) {
			this.adaptiveOnsets.put(e.getKey(), e.getValue());
		}

		if (this.originalGreenTimes == null){
			this.originalGreenTimes = new HashMap<Id, Integer>();
			this.fillOriginalGreenTimes();
			log.info("prepared Original Green Times");
		}

		this.availableStretchTime = JBBaParams.STRETCHTIME;
	}

	private void updateNonAdaptiveStates(double timeSeconds) {
		List<Id> droppingGroupIds = this.activePlan.getDroppings(timeSeconds);
		if (droppingGroupIds != null) {
			for (Id id : droppingGroupIds) {
				this.system.scheduleDropping(timeSeconds, id);
			}
		}

		List<Id> onsetGroupIds = this.activePlan.getOnsets(timeSeconds);
		if (onsetGroupIds != null) {
			for (Id id : onsetGroupIds) {
				this.system.scheduleOnset(timeSeconds, id);
			}
		}

	}

	public void addGapAtSecond(double second, Id sgId) {
		if (!this.gapsAtSecond.containsKey(second)) {
			this.gapsAtSecond.put(second, new LinkedList<Id>());
		}
		this.gapsAtSecond.get(second).add(sgId);
	}

	public List<Id> getGapListatSecond(double second) {
		if (!this.gapsAtSecond.containsKey(second)) {
			this.gapsAtSecond.put(second, new LinkedList<Id>());
		}
		return this.gapsAtSecond.get(second);

	}

	@Override
	public void reset(Integer iterationNumber) {
		// TODO Auto-generated method stub

	}

	@Override
	public void simulationInitialized(double simStartTimeSeconds) {
		// TODO Auto-generated method stub
		
	}

}
