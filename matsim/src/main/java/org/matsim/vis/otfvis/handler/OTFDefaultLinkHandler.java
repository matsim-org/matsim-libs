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

import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataQuadReceiver;
import org.matsim.vis.otfvis.data.OTFDataReceiver;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.data.OTFWriterFactory;
import org.matsim.vis.otfvis.gui.OTFVisConfigGroup;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;
import org.matsim.vis.snapshots.writers.VisLink;

/**
 * OTFDefaultLinkHandler handles the basic IO of a link.
 * It can send the link's coords as static data.
 * <p/>
 * Sending 
 * QueueLink.getVisData().getDisplayableTimeCapValue() as dynamic data in float format
 * has been disabled some time ago.
 * 
 * @author david
 *
 */
public class OTFDefaultLinkHandler extends OTFDataReader {

	protected OTFDataQuadReceiver quadReceiver = null;
	
	public OTFDataQuadReceiver getQuadReceiver() {
		return quadReceiver;
	}

	static public class Writer extends  OTFDataWriter<VisLink> implements OTFWriterFactory<VisLink> {

		private static final long serialVersionUID = 2827811927720044709L;
		
		private static OTFVisConfigGroup otfVisConfig = null ;
		private static boolean first = true ;

		@Override
		public void writeConstData(ByteBuffer out) throws IOException {
			if ( first ) {
				// there is sometimes a null pointer exception (e.g. in testConvert in OTFVisTest), and I don't know
				// when and where this is set and when not.  Thus this hack ...  Please do better if you know better. 
				// kai, jun'11
				first = false ;
				if ( OTFClientControl.getInstance()!=null ) {
					if ( OTFClientControl.getInstance().getOTFVisConfig()!=null ) {
						otfVisConfig = OTFClientControl.getInstance().getOTFVisConfig() ;
					}
				}
			}
			String id = this.src.getLink().getId().toString();
			ByteBufferUtils.putString(out, id);
			//subtract minEasting/Northing somehow!
			Point2D.Double.Double linkStart = new Point2D.Double.Double(this.src.getLink().getFromNode().getCoord().getX() - OTFServerQuadTree.offsetEast, 
					this.src.getLink().getFromNode().getCoord().getY() - OTFServerQuadTree.offsetNorth);
			Point2D.Double.Double linkEnd = new Point2D.Double.Double(this.src.getLink().getToNode().getCoord().getX() - OTFServerQuadTree.offsetEast,
					this.src.getLink().getToNode().getCoord().getY() - OTFServerQuadTree.offsetNorth);

			out.putFloat((float) linkStart.x); 
			out.putFloat((float) linkStart.y);
			out.putFloat((float) linkEnd.x); 
			out.putFloat((float) linkEnd.y);
			if ( otfVisConfig != null ) {
				if ( OTFVisConfigGroup.NUMBER_OF_LANES.equals(otfVisConfig.getLinkWidthIsProportionalTo()) ) {
					out.putInt(NetworkUtils.getNumberOfLanesAsInt(0, this.src.getLink()));
				} else if ( OTFVisConfigGroup.CAPACITY.equals(otfVisConfig.getLinkWidthIsProportionalTo()) ) {
					out.putInt( 1 + (int)(2.*this.src.getLink().getCapacity()/3600.) ) ;
					// yyyyyy 3600. is a magic number but I cannot get to the network (where "capacityPeriod" resides).  
					// Please do better if you know better.  kai, jun'11
				} else {
					throw new RuntimeException("I do not understand.  Aborting ..." ) ;
				}
			} else {
				out.putInt(NetworkUtils.getNumberOfLanesAsInt(0, this.src.getLink()));
			}
 

		}

		@Override
		public void writeDynData(ByteBuffer out) throws IOException {
			out.putFloat((float)0.) ; // yy this should be fixed in the binary channel but I am not sure if it is worth it.  kai, apr'10
		}

		@Override
		public OTFDataWriter<VisLink> getWriter() {
			return new Writer();
		}
	}

	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
		this.quadReceiver.setColor(in.getFloat());
	}

	@Override
	public void readConstData(ByteBuffer in) throws IOException {
		String id = ByteBufferUtils.getString(in);
		this.quadReceiver.setQuad(in.getFloat(), in.getFloat(),in.getFloat(), in.getFloat(), in.getInt());
		this.quadReceiver.setId(id.toCharArray());
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

}

