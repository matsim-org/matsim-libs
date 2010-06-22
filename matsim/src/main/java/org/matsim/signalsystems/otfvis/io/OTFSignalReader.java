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
package org.matsim.signalsystems.otfvis.io;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.lanes.otfvis.io.OTFLaneReader;
import org.matsim.signalsystems.control.SignalGroupState;
import org.matsim.vis.otfvis.caching.SceneGraph;

/**
 * @author dgrether
 */
public class OTFSignalReader extends OTFLaneReader {

  public OTFSignalReader() {
	}

	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
		if (this.isQLinkLanesReader){
			int numberOfLanes = in.getInt();
			if (numberOfLanes > 1) {
				String id;
				boolean green;
				for (int i = 0; i < numberOfLanes; i++) {
					id = ByteBufferUtils.getString(in);
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
					this.drawer.updateGreenState(id, state);
				}
			}
		}
	}
}
