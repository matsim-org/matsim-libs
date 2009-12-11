/* *********************************************************************** *
 * project: org.matsim.*
 * OTFDefaultNodeHandler.java
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

import org.matsim.ptproject.qsim.QueueNode;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuad;
import org.matsim.vis.otfvis.data.OTFWriterFactory;
import org.matsim.vis.otfvis.data.OTFDataReceiver;
import org.matsim.vis.otfvis.data.OTFDataXYCoordReceiver;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;

/**
 * OTFDefaultNodeHandler IOs the coordinates of a node. 
 * It is not used yet.
 * 
 * @author david
 *
 */
public class OTFDefaultNodeHandler extends OTFDataReader   {
	private OTFDataXYCoordReceiver xyReceiver = null;

	static public class Writer extends  OTFDataWriter<QueueNode> implements OTFWriterFactory<QueueNode> {

		private static final long serialVersionUID = 8011757932341886429L;

		@Override
		public void writeConstData(ByteBuffer out) throws IOException {
			out.putFloat((float)(this.src.getNode().getCoord().getX() - OTFServerQuad.offsetEast)); //subtract minEasting/Northing somehow!
			out.putFloat((float)(this.src.getNode().getCoord().getY() - OTFServerQuad.offsetNorth));
		}

		@Override
		public void writeDynData(ByteBuffer out) throws IOException {
		}

		public OTFDataWriter<QueueNode> getWriter() {
			return new Writer();
		}
	}

	@Override
	public void readConstData(ByteBuffer in) throws IOException {
		if (this.xyReceiver != null) {
			this.xyReceiver.setXYCoord(in.getFloat(), in.getFloat());
		} else {
			in.getFloat();
			in.getFloat();
		}
	}

	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
	}

	@Override
	public void connect(OTFDataReceiver receiver) {
		if (receiver instanceof OTFDataXYCoordReceiver) {
			this.xyReceiver = (OTFDataXYCoordReceiver) receiver;
		}
	}

	@Override
	public void invalidate(SceneGraph graph) {
		if (this.xyReceiver != null) this.xyReceiver.invalidate(graph);
	}

}
