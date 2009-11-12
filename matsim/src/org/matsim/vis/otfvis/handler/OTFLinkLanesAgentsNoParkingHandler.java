/* *********************************************************************** *
 * project: org.matsim.*
 * OTFLinkAgentsNoParkingHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.vis.otfvis.handler;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;


/**
 * OTFLinkAgentsNoParkingHandler transmits the number of lanes as const data
 * I do not think this is used.
 * 
 * @author david
 *
 */
public class OTFLinkLanesAgentsNoParkingHandler extends OTFLinkAgentsHandler {
	static {
		OTFDataReader.setPreviousVersion(OTFLinkLanesAgentsNoParkingHandler.class.getCanonicalName() + "V1.3", ReaderV1_3.class);
	}
	

	static public class Writer extends  OTFLinkAgentsHandler.Writer {

		private static final long serialVersionUID = 6541770536927233851L;

		@Override
		public void writeConstData(ByteBuffer out) throws IOException {
			String id = this.src.getLink().getId().toString();
			ByteBufferUtils.putString(out, id);
			super.writeConstData(out);
			out.putInt(NetworkUtils.getNumberOfLanesAsInt(0, this.src.getLink()));
		}

		@Override
		public OTFDataWriter<QueueLink> getWriter() {
			return new Writer();
		}

	}
	
	@Override
	public void readConstData(ByteBuffer in) throws IOException {
		String id = ByteBufferUtils.getString(in);

		this.quadReceiver.setQuad(in.getFloat(), in.getFloat(),in.getFloat(), in.getFloat(), in.getInt());
		this.quadReceiver.setId(id.toCharArray());
	}
	/***
	 * PREVIOUS VERSION of the reader
	 * 
	 * @author dstrippgen
	 */
	public static final class ReaderV1_3 extends OTFLinkAgentsHandler {
		@Override
		public void readConstData(ByteBuffer in) throws IOException {
			this.quadReceiver.setQuad(in.getFloat(), in.getFloat(),in.getFloat(), in.getFloat(), in.getInt());
		}
	}
}
