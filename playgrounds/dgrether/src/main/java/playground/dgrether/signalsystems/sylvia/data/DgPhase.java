/* *********************************************************************** *
 * project: org.matsim.*
 * DgPhase
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.sylvia.data;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalGroupSettingsData;

final class DgPhase {
		
		private static final Logger log = Logger.getLogger(DgPhase.class);
		private Integer on = null;
		private Integer off = null;
		private Map<Id, SignalGroupSettingsData> signalGroupSettingsByGroupId = new HashMap<Id, SignalGroupSettingsData>();
		
//		private boolean mixedPhase = false;
		
		public DgPhase(Integer phaseOn, Integer phaseDrop) {
			log.debug("created phase from " + phaseOn + " to " + phaseDrop);
			this.on = phaseOn;
			this.off = phaseDrop;
		}

		public void setPhaseStartSecond(Integer on){
			this.on = on;
		}
		
		public void setPhaseEndSecond(Integer off){
			this.off = off;
		}
		
		public Integer getPhaseStartSecond(){
			return this.on;
		}
		
		public Integer getPhaseEndSecond(){
			return this.off;
		}
		
		public void addSignalGroupSettingsData(SignalGroupSettingsData settings) {
			log.debug("  adding settings to phase: " + settings.getSignalGroupId() + " on: " + settings.getOnset() + " drop " + settings.getDropping());
			if (settings.getOnset() < this.on || settings.getDropping() > off){
				throw new IllegalStateException("SignalGroupSettings longer than phase length!");
			}
			this.signalGroupSettingsByGroupId.put(settings.getSignalGroupId(), settings);
		}
		
		public Map<Id, SignalGroupSettingsData> getSignalGroupSettingsByGroupId(){
			return this.signalGroupSettingsByGroupId;
		}
		
	}