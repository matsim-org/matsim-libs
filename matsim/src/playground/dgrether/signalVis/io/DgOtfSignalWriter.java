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
package playground.dgrether.signalVis.io;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.matsim.core.mobsim.queuesim.QueueLane;
import org.matsim.core.utils.misc.ByteBufferUtils;


/**
 * @author dgrether
 *
 */
public class DgOtfSignalWriter extends DgOtfLaneWriter {

	/**
	 * 
	 */
	public DgOtfSignalWriter() {
	}

	@Override
	public void writeDynData(ByteBuffer out) throws IOException {
		out.putInt(this.src.getToNodeQueueLanes().size());
		for (QueueLane ql : this.src.getToNodeQueueLanes()){
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
