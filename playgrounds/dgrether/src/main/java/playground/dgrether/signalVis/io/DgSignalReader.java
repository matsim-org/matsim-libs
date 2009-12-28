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
package playground.dgrether.signalVis.io;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.vis.otfvis.caching.SceneGraph;


/**
 * @author dgrether
 *
 */
public class DgSignalReader extends DgOtfLaneReader {

	/**
	 * 
	 */
	public DgSignalReader() {
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

	@Override
	public void invalidate(SceneGraph graph) {
		super.invalidate(graph);
		// invalidate agent receivers
	}
	
//	@Override
//	public void invalidate(SceneGraph graph) {
////		super.invalidate(graph);
////		graph.addItem(this.drawer);
//	}
	
}
