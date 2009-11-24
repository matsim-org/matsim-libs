/* *********************************************************************** *
 * project: org.matsim.*
 * BusOTFVis.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.test;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.media.opengl.GL;

import org.matsim.core.gbl.MatsimResource;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataReceiver;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentArrayDrawer;

/**
 * @author yu
 * 
 */
public class VehOTFVis {
	public static class VehDrawer extends AgentArrayDrawer {

		public VehDrawer(String filename) {
			this.texture = OTFOGLDrawer.createTexture(MatsimResource
					.getAsInputStream(filename));
		}

		public void onDraw(GL gl) {

		}

	}

	public static class VehReader extends OTFDataReader {

		@Override
		public void connect(OTFDataReceiver receiver) {
		}

		@Override
		public void invalidate(SceneGraph graph) {

		}

		@Override
		public void readConstData(ByteBuffer in) throws IOException {

		}

		@Override
		public void readDynData(ByteBuffer in, SceneGraph graph)
				throws IOException {
			VehDrawer cars = new VehDrawer("");
			VehDrawer buses=new VehDrawer("");
		}

	}

	public static class VehWriter extends OTFDataWriter {

		@Override
		public void writeConstData(ByteBuffer out) throws IOException {

		}

		@Override
		public void writeDynData(ByteBuffer out) throws IOException {

		}

	}
}
