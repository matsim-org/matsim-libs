/* *********************************************************************** *
 * project: org.matsim.*
 * DgSignalReader
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
package org.matsim.contrib.signals.otfvis;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.lanes.vis.VisLane;
import org.matsim.lanes.vis.VisLinkWLanes;
import org.matsim.lanes.vis.VisSignal;
import org.matsim.vis.otfvis.caching.SceneGraph;

/**
 * @author dgrether
 */
public class OTFSignalReader extends OTFLaneReader {

	public OTFSignalReader() {
		super();
	}

	@Override
	public void readConstData(ByteBuffer in) throws IOException {
		super.readConstData(in);
		this.readSignalSystems(in);
	}

	private void readSignalSystems(ByteBuffer in){
		int noSignalSystems = in.getInt();
		for (int i = 0; i < noSignalSystems; i++){
			String systemId = ByteBufferUtils.getString(in);
			VisSignalSystem otfsystem = new VisSignalSystem(systemId);
			this.drawer.addOTFSignalSystem(otfsystem);
			
			int noGroups = in.getInt();
			for (int j = 0; j < noGroups; j++){
				String groupId = ByteBufferUtils.getString(in);
				VisSignalGroup otfgroup = new VisSignalGroup(systemId, groupId);
				otfsystem.addOTFSignalGroup(otfgroup);
				int noSignals = in.getInt();
				for (int k = 0; k < noSignals; k++){
					String signalId = ByteBufferUtils.getString(in);
					String linkId = ByteBufferUtils.getString(in);
					VisLinkWLanes link = this.drawer.getLanesLinkData().get(linkId);
					VisSignal signal = new VisSignal(systemId, signalId);
					otfgroup.addSignal(signal);
					int noLanes = in.getInt();
					if (noLanes == 0){
						link.addSignal(signal);
					}
					else {
						for (int l = 0; l < noLanes; l++){
							String laneId = ByteBufferUtils.getString(in);
							VisLane laneData = link.getLaneData().get(laneId);
							laneData.addSignal(signal);
						}
					}
					int noTurningMoveRestrictions = in.getInt();
					for (int l = 0; l < noTurningMoveRestrictions; l++){
						String toLinkId = ByteBufferUtils.getString(in);
						VisLinkWLanes toLink = this.drawer.getLanesLinkData().get(toLinkId);
						signal.addTurningMoveRestriction(toLink);
					}
				}
			}
		}
	}
	

	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
		int noEvents = in.getInt();
		for (int i = 0; i < noEvents; i++){
			String systemId = ByteBufferUtils.getString(in);
			String groupId = ByteBufferUtils.getString(in);
			int stateInt = in.getInt();
			SignalGroupState state = null;
			if (stateInt == 1){
				state = SignalGroupState.GREEN;
			}
			else if (stateInt == 0){
				state = SignalGroupState.RED;
			}
			else if (stateInt == 2){
				state = SignalGroupState.REDYELLOW;
			}
			else if (stateInt == 3){
				state = SignalGroupState.YELLOW;
			}
			else if (stateInt == 4){
				state = SignalGroupState.OFF;
			}
			this.drawer.updateGreenState(systemId, groupId, state);
		}
	}
}
