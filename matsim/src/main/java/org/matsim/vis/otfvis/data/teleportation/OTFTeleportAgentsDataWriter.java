/* *********************************************************************** *
 * project: org.matsim.*
 * OTFTeleportAgentsDataWriter
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
package org.matsim.vis.otfvis.data.teleportation;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.vis.otfvis.data.OTFDataWriter;


/**
 * @author dgrether
 *
 */
public class OTFTeleportAgentsDataWriter extends OTFDataWriter<Map<Id, TeleportationVisData>> {
  
	private static final Logger log = Logger.getLogger(OTFTeleportAgentsDataWriter.class);
	
	private double time;

	public OTFTeleportAgentsDataWriter() {
	}

	@Override
	public void writeConstData(ByteBuffer out) throws IOException {
	}

	@Override
	public void writeDynData(ByteBuffer out) throws IOException {
//		log.error("writing agent data");
		if (this.src != null) {
			out.putInt(this.src.size());
			for (TeleportationVisData d : this.src.values()){
				ByteBufferUtils.putString(out, d.getId().toString());
				d.calculatePosition(this.time);
//				log.error("id: " + d.getId() + " x: " + d.getX() + " y: " + d.getY());
				out.putDouble(d.getX());
				out.putDouble(d.getY());
			}
		}
		else {
			out.putInt(0);
		}
	}

	public void setTime(double time) {
		this.time = time;
	}

}
