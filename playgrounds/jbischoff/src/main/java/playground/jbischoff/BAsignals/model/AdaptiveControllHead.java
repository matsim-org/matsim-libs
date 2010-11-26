/* *********************************************************************** *
 * project: org.matsim.*
 * AdaptiveControllHead.java
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

package playground.jbischoff.BAsignals.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalSystemControllerData;
import org.matsim.signalsystems.model.Signal;
import org.matsim.signalsystems.model.SignalGroup;
import org.matsim.signalsystems.model.SignalSystem;

import playground.jbischoff.BAsignals.JBBaParams;

public class AdaptiveControllHead {

	private static final Logger log = Logger.getLogger(AdaptiveControllHead.class);
	/**
	 * All Maps of this class, except signalSystemMap, map a unique Id of a SignalGroup to something.
	 */
	private Map<Id, SignalSystemControllerData> sscmap;
	private Map<Id, SignalSystem> signalSystemsMap;
	private Map<Id, Integer> minOnset;
	private Map<Id, Integer> maxOnset;
	private Map<Id, Integer> minDropping;
	private Map<Id, Integer> maxDropping;
	private List<Id> adaptiveSignalGroups;
	private Map<Id, Integer> intergreentimes;

	public AdaptiveControllHead() {
		sscmap = new HashMap<Id, SignalSystemControllerData>();
		this.adaptiveSignalGroups = new LinkedList<Id>();
		this.signalSystemsMap = new HashMap<Id, SignalSystem>();
		this.maxDropping = new HashMap<Id, Integer>();
		this.maxOnset = new HashMap<Id, Integer>();
		this.minDropping = new HashMap<Id, Integer>();
		this.minOnset = new HashMap<Id, Integer>();
		this.intergreentimes = new HashMap<Id, Integer>();

	}

	public void addAdaptiveSignalSystem(SignalSystem system, SignalSystemControllerData asigs) {

		this.sscmap.put(asigs.getSignalSystemId(), asigs);
		this.signalSystemsMap.put(system.getId(), system);

		for (Id sgroupId : system.getSignalGroups().keySet()) {
			this.adaptiveSignalGroups.add(sgroupId);
		}
		this.sizeDownPlans();

	}

	public Id getSignalSystemforLaneId(Id laneId) {
		for (SignalSystem system : this.signalSystemsMap.values()) {
			for (SignalGroup sg : system.getSignalGroups().values()) {
				for (Signal sig : sg.getSignals().values()) {
					if (sig.getLaneIds().contains(laneId)) {
						return system.getId();
					}
				}
			}

		}
		return null;
	}

	/**
	 * In the case that not adaptive systems are controlled by not adaptive DefaultControllers
	 * this method and the calling if statements should not be needed.
	 */
	public boolean signalSystemIsAdaptive(SignalSystem system) {
		if (this.signalSystemsMap.containsValue(system))
			return true;
		else
			return false;
	}

	public Id getSignalSystemforGroupId(Id groupId) {
		for (SignalSystem system : this.signalSystemsMap.values()) {
			if (system.getSignalGroups().containsKey(groupId)) {
				return system.getId();
			}

		}
		return null;
	}

	public Id getSignalGroupforLaneId(Id laneId) {
		for (SignalSystem system : this.signalSystemsMap.values()) {
			for (SignalGroup sg : system.getSignalGroups().values()) {
				for (Signal sig : sg.getSignals().values()) {
					if (sig.getLaneIds().contains(laneId)) {
						return sg.getId();
					}
				}
			}

		}
		return null;
	}


	public void sizeDownPlans() {
		double ratio = JBBaParams.PLANSDOWNSIZER;
		for (SignalSystemControllerData sscd : this.sscmap.values()) {

			for (SignalPlanData spd : sscd.getSignalPlanData().values()) {
				
				for (SignalGroupSettingsData sgsd : spd.getSignalGroupSettingsDataByGroupId().values()) {
					this.maxDropping.put(sgsd.getSignalGroupId(), sgsd.getDropping());
					this.maxOnset.put(sgsd.getSignalGroupId(), sgsd.getOnset());
					Long nos = Math.round(sgsd.getOnset() * ratio);
					Long ndr = Math.round(sgsd.getDropping() * ratio);
					this.minDropping.put(sgsd.getSignalGroupId(), ndr.intValue());
					this.minOnset.put(sgsd.getSignalGroupId(), nos.intValue());
					this.checkInterGreenTimes(spd.getCycleTime());
				}

			}
		}

	}

	// checks that original intergreentimes are kept after sizing down plans
	private void checkInterGreenTimes(int cycleTime) {

		for (Entry<Id, Integer> dropen : this.maxDropping.entrySet()) {
			Id ssid = this.getSignalSystemforGroupId(dropen.getKey());
			int intergreentime = 0;
			for (Entry<Id, Integer> onsen : this.maxOnset.entrySet()) {
				if (this.getSignalSystemforGroupId(onsen.getKey()).equals(ssid)) {
					if (onsen.getValue() > dropen.getValue()) {
						if (intergreentime == 0 || onsen.getValue() - dropen.getValue() < intergreentime)
							intergreentime = onsen.getValue() - dropen.getValue();

					}
				}

			}

			if (intergreentime == 0)
				intergreentime = cycleTime - dropen.getValue();
			this.intergreentimes.put(dropen.getKey(), intergreentime);
		}

		for (Entry<Id, Integer> mined : this.minDropping.entrySet()) {
			Map<Id, Integer> newons = new HashMap<Id, Integer>();
			Id ssid = this.getSignalSystemforGroupId(mined.getKey());
			int intergreentime = this.intergreentimes.get(mined.getKey());
			for (Entry<Id, Integer> minon : this.minOnset.entrySet()) {
				if (this.getSignalSystemforGroupId(minon.getKey()).equals(ssid)) {
					if (minon.getValue() > mined.getValue()
							& minon.getValue() < mined.getValue() + intergreentime) {
						newons.put(minon.getKey(), mined.getValue() + intergreentime);
						// log.info("changed onset of "+minon.getKey()+" from "+minon.getValue()+" to "+newons.get(minon.getKey()));
					}
				}
			}
			this.minOnset.putAll(newons);

		}

	}

	public List<Id> getAdaptiveSignalGroups() {
		return adaptiveSignalGroups;
	}

	public Map<Id, SignalSystem> getSignalSystemsMap() {
		return signalSystemsMap;
	}

	public Map<Id, Integer> getMinOnset() {
		return minOnset;
	}

	public Map<Id, Integer> getMaxOnset() {
		return maxOnset;
	}

	public Map<Id, Integer> getMinDropping() {
		return minDropping;
	}

	public Map<Id, Integer> getMaxDropping() {
		return maxDropping;
	}

}
