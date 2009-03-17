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

package org.matsim.utils.vis.otfvis.handler;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.utils.vis.otfvis.caching.SceneGraph;
import org.matsim.utils.vis.otfvis.data.OTFDataWriter;
import org.matsim.utils.vis.otfvis.data.OTFServerQuad;
import org.matsim.utils.vis.otfvis.handler.OTFLinkAgentsHandler.ReaderV1_1;
import org.matsim.utils.vis.otfvis.interfaces.OTFDataReader;


public class OTFLinkLanesAgentsNoParkingHandler extends OTFLinkAgentsHandler {
	static {
		OTFDataReader.setPreviousVersion(OTFLinkLanesAgentsNoParkingHandler.class.getCanonicalName() + "V1.3", ReaderV1_3.class);
	}
	

	static public class Writer extends  OTFLinkAgentsHandler.Writer {
		/**
		 * 
		 */
		private static final long serialVersionUID = 6541770536927233851L;

		public void writeConstData(ByteBuffer out) throws IOException {
			String id = this.src.getLink().getId().toString();
			out.putInt(id.length());
			for (int i=0; i<id.length(); i++) out.putChar(id.charAt(i));
			out.putFloat((float)(this.src.getLink().getFromNode().getCoord().getX() - OTFServerQuad.offsetEast)); //subtract minEasting/Northing somehow!
			out.putFloat((float)(this.src.getLink().getFromNode().getCoord().getY() - OTFServerQuad.offsetNorth));
			out.putFloat((float)(this.src.getLink().getToNode().getCoord().getX() - OTFServerQuad.offsetEast)); //subtract minEasting/Northing somehow!
			out.putFloat((float)(this.src.getLink().getToNode().getCoord().getY() - OTFServerQuad.offsetNorth));
			out.putInt(this.src.getLink().getLanesAsInt(0));
		}

		@Override
		public OTFDataWriter<QueueLink> getWriter() {
			return new Writer();
		}

	}
	
	public void readConstData(ByteBuffer in) throws IOException {
		int length = in.getInt();
		char[] idBuffer = new char[length];
		for(int i=0;i<length;i++) idBuffer[i] = in.getChar();

		this.quadReceiver.setQuad(in.getFloat(), in.getFloat(),in.getFloat(), in.getFloat(), in.getInt());
		this.quadReceiver.setId(idBuffer);
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
