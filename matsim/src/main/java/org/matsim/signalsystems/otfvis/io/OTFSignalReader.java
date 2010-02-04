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

import org.apache.log4j.Logger;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.lanes.otfvis.io.OTFLaneReader;
import org.matsim.vis.otfvis.caching.SceneGraph;

/**
 *
 * @author dgrether
 */
public class OTFSignalReader extends OTFLaneReader {

  private static final Logger log = Logger.getLogger(OTFSignalReader.class);

  public OTFSignalReader() {
	  log.error("new SignalReader instance!!!");
	}

	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
		int numberOfLanes = in.getInt();
		if (numberOfLanes > 1) {
			String id;
			boolean green;
			for (int i = 0; i < numberOfLanes; i++) {
				id = ByteBufferUtils.getString(in);
				green = (in.getInt() == 1 ? true : false);
				this.drawer.updateGreenState(id, green);
			}
		}
	}

//	@Override
//	public void invalidate(SceneGraph graph) {
////		super.invalidate(graph);
////		graph.addItem(this.drawer);
//	}

}
