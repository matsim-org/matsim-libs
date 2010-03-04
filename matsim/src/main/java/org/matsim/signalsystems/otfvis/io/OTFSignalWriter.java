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

import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.lanes.otfvis.io.OTFLaneWriter;
import org.matsim.ptproject.qsim.QLane;
import org.matsim.ptproject.qsim.QLinkLanesImpl;
import org.matsim.vis.otfvis.data.OTFDataWriter;

/**
 * @author dgrether
 */
public class OTFSignalWriter extends OTFLaneWriter {

	public OTFSignalWriter() {
	}

	@Override
	public void writeDynData(ByteBuffer out) throws IOException {
		int numberOfToNodeQueueLanes = this.src.getToNodeQueueLanes().size();
		out.putInt(numberOfToNodeQueueLanes);
		if (numberOfToNodeQueueLanes > 1) {
			for (QLane ql : this.src.getToNodeQueueLanes()){
				ByteBufferUtils.putString(out, ql.getLaneId().toString());
				if (ql.isThisTimeStepGreen()) {
					out.putInt(1);
				}
				else {
					out.putInt(0);
				}
			}
		}
	}

	@Override
	public OTFDataWriter<QLinkLanesImpl> getWriter() {
		return new OTFSignalWriter();
	}

}
