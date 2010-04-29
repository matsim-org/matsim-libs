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

package playground.mzilske.vis;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.matsim.api.core.v01.network.Link;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataQuadReceiver;
import org.matsim.vis.otfvis.data.OTFDataReceiver;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;

public class LinkHandler extends OTFDataReader {

	public static final double LINK_SCALE  = 1.00; 
	
	protected OTFDataQuadReceiver quadReceiver = null;

	public OTFDataQuadReceiver getQuadReceiver() {
		return quadReceiver;
	}

	static public class Writer extends OTFDataWriter<Link> {

		private static final long serialVersionUID = 2827811927720044709L;

		@Override
		public void writeConstData(ByteBuffer out) throws IOException {
		//subtract minEasting/Northing somehow!
			Point2D.Double.Double linkStart = new Point2D.Double.Double(this.src.getFromNode().getCoord().getX() - OTFServerQuad2.offsetEast, 
					this.src.getFromNode().getCoord().getY() - OTFServerQuad2.offsetNorth);
			Point2D.Double.Double linkEnd = new Point2D.Double.Double(this.src.getToNode().getCoord().getX() - OTFServerQuad2.offsetEast,
					this.src.getToNode().getCoord().getY() - OTFServerQuad2.offsetNorth);
			
			out.putFloat((float) linkStart.x); 
			out.putFloat((float) linkStart.y);
			out.putFloat((float) linkEnd.x); 
			out.putFloat((float) linkEnd.y);
		}

		@Override
		public void writeDynData(ByteBuffer out) throws IOException {
			
		}

	}

	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
		
	}

	@Override
	public void readConstData(ByteBuffer in) throws IOException {
		this.quadReceiver.setQuad(in.getFloat(), in.getFloat(),in.getFloat(), in.getFloat());
	}

	@Override
	public void connect(OTFDataReceiver receiver) {
		if (receiver instanceof OTFDataQuadReceiver) {
			this.quadReceiver = (OTFDataQuadReceiver)receiver;
		}
	}

	@Override
	public void invalidate(SceneGraph graph) {
		this.quadReceiver.invalidate(graph);
	}
	
}

