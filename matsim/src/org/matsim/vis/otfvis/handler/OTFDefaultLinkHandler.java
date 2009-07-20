/* *********************************************************************** *
 * project: org.matsim.*
 * OTFDefaultLinkHandler.java
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
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuad;
import org.matsim.vis.otfvis.data.OTFWriterFactory;
import org.matsim.vis.otfvis.data.OTFDataReceiver;
import org.matsim.vis.otfvis.data.OTFDataQuadReceiver;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;

public class OTFDefaultLinkHandler extends OTFDataReader {
	static {
		OTFDataReader.setPreviousVersion(OTFDefaultLinkHandler.class.getCanonicalName() + "V1.1", ReaderV1_1.class);
	}

	protected OTFDataQuadReceiver quadReceiver = null;

	public OTFDataQuadReceiver getQuadReceiver() {
		return quadReceiver;
	}


	static public class Writer extends  OTFDataWriter<QueueLink> implements OTFWriterFactory<QueueLink> {

		private static final long serialVersionUID = 2827811927720044709L;

		@Override
		public void writeConstData(ByteBuffer out) throws IOException {
			out.putFloat((float)(this.src.getLink().getFromNode().getCoord().getX() - OTFServerQuad.offsetEast)); //subtract minEasting/Northing somehow!
			out.putFloat((float)(this.src.getLink().getFromNode().getCoord().getY() - OTFServerQuad.offsetNorth));
			out.putFloat((float)(this.src.getLink().getToNode().getCoord().getX() - OTFServerQuad.offsetEast)); //subtract minEasting/Northing somehow!
			out.putFloat((float)(this.src.getLink().getToNode().getCoord().getY() - OTFServerQuad.offsetNorth));
		}

		@Override
		public void writeDynData(ByteBuffer out) throws IOException {
			out.putFloat((float)this.src.getVisData().getDisplayableTimeCapValue());
		}

		public OTFDataWriter<QueueLink> getWriter() {
			return new Writer();
		}
	}

	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
		this.quadReceiver.setColor(in.getFloat());
	}

	@Override
	public void readConstData(ByteBuffer in) throws IOException {
		this.quadReceiver.setQuad(in.getFloat(), in.getFloat(),in.getFloat(), in.getFloat());
	}

	@Override
	public void connect(OTFDataReceiver receiver) {
		if (receiver  instanceof OTFDataQuadReceiver) {
			this.quadReceiver = (OTFDataQuadReceiver)receiver;
		}
	}

	@Override
	public void invalidate(SceneGraph graph) {
		this.quadReceiver.invalidate(graph);
	}


	// Previous version of the reader

	public static final class ReaderV1_1 extends OTFDefaultLinkHandler {
		@Override
		public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
		}
	}
}

