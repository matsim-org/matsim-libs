/* *********************************************************************** *
 * project: org.matsim.*
 * DgOtfSignalWriter
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
package org.matsim.signalsystems.otfvis.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.SignalGroupStateChangedEvent;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.otfvis.io.OTFLaneWriter;
import org.matsim.signalsystems.control.SignalGroupState;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.matsim.vis.snapshots.writers.VisNetwork;

/**
 * @author dgrether
 */
public class OTFSignalWriter extends OTFLaneWriter {

	private transient SignalGroupStateChangeTracker signalTracker;
	private transient SignalGroupsData signalGroups;
	private transient SignalSystemsData signalSystems;

	public OTFSignalWriter(VisNetwork visNetwork, LaneDefinitions lanes, SignalSystemsData signalSystemsData, SignalGroupsData signalGroupsData, SignalGroupStateChangeTracker signalTracker) {
		super(visNetwork, lanes);
		this.signalTracker = signalTracker;
		this.signalSystems = signalSystemsData;
		this.signalGroups = signalGroupsData;
	}

	@Override
	public void writeConstData(ByteBuffer out) throws IOException {
		super.writeConstData(out);
		this.writeSignalSystems(out);
	}
	
	private void writeSignalSystems(ByteBuffer out){
		out.putInt(this.signalGroups.getSignalGroupDataBySignalSystemId().size());
		for (Id systemId : this.signalGroups.getSignalGroupDataBySignalSystemId().keySet()){
			ByteBufferUtils.putString(out, systemId.toString());
			Map<Id, SignalGroupData> groups = this.signalGroups.getSignalGroupDataBySystemId(systemId);
			out.putInt(groups.size());
			for (SignalGroupData group : groups.values()){
				ByteBufferUtils.putString(out, group.getId().toString());
				out.putInt(group.getSignalIds().size());
				for (Id signalId : group.getSignalIds()){
					ByteBufferUtils.putString(out, signalId.toString());
					SignalData signal = signalSystems.getSignalSystemData().get(systemId).getSignalData().get(signalId);
					ByteBufferUtils.putString(out, signal.getLinkId().toString());
					if (signal.getLaneIds() == null || signal.getLaneIds().isEmpty()){
						out.putInt(0);
					}
					else {
						out.putInt(signal.getLaneIds().size());
						for (Id laneId : signal.getLaneIds()){
							ByteBufferUtils.putString(out, laneId.toString());
						}
					}
					
					if (signal.getTurningMoveRestrictions() == null || signal.getTurningMoveRestrictions().isEmpty()){
						out.putInt(0);
					}
					else {
						out.putInt(signal.getTurningMoveRestrictions().size());
						for (Id outLinkId : signal.getTurningMoveRestrictions()){
							ByteBufferUtils.putString(out, outLinkId.toString());
						}
					}
				}
			}
		}
	}

	
	private void writeSignalGroupStates(ByteBuffer out){
		out.putInt(this.signalTracker.getSignalGroupEvents().size());
		for (SignalGroupStateChangedEvent e : this.signalTracker.getSignalGroupEvents()){
			ByteBufferUtils.putString(out, e.getSignalSystemId().toString());
			ByteBufferUtils.putString(out, e.getSignalGroupId().toString());
			SignalGroupState state = e.getNewState();
			if (state.equals(SignalGroupState.GREEN)){
				out.putInt(1);
			}
			else if (state.equals(SignalGroupState.RED)){
				out.putInt(0);
			}
			else if (state.equals(SignalGroupState.REDYELLOW)){
				out.putInt(2);
			}
			else if (state.equals(SignalGroupState.YELLOW)){
				out.putInt(3);
			}
			else if (state.equals(SignalGroupState.OFF)){
				out.putInt(4);
			}
			
		}
		this.signalTracker.getSignalGroupEvents().clear();
	}
	
	@Override
	public void writeDynData(ByteBuffer out) throws IOException {
		this.writeSignalGroupStates(out);
	}

}
