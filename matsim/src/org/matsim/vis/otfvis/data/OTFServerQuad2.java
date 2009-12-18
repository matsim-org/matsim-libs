/* *********************************************************************** *
 * project: org.matsim.*
 * OTFServerQuad.java
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

package org.matsim.vis.otfvis.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;
import org.matsim.vis.otfvis.interfaces.OTFServerRemote;



/**
 * OTFServerQuad is the quad representation of all elements of the network on the server
 * side. This QuadTree is mirrored on the client side by OTFClientQuad.
 * 
 * @author dstrippgen
 *
 */
public abstract class OTFServerQuad2 extends QuadTree<OTFDataWriter> implements OTFServerQuadI {
  
	private static final Logger log = Logger.getLogger(OTFServerQuad2.class);
	
	private final List<OTFDataWriter> additionalElements= new LinkedList<OTFDataWriter>();

	private static final long serialVersionUID = 1L;
	protected double minEasting;
	protected double maxEasting;
	protected double minNorthing;
	protected double maxNorthing;
	protected double easting;
	protected double northing;
	
	// Change this, find better way to transport this info into Writers
	public static double offsetEast;
	public static double offsetNorth;

	public OTFServerQuad2(Network network) {
		super(0,0,0,0);
		this.updateBoundingBox(network);
	}
	
	/**
	 * This method should be abstract as it has to be overwritten in subclasses.
	 * Due to deserialization backwards compatibility this is not possible. dg dez 09
	 */
	public abstract void initQuadTree(final OTFConnectionManager connect);

	
	protected void updateBoundingBox(Network n){
		this.minEasting = Double.POSITIVE_INFINITY;
		this.maxEasting = Double.NEGATIVE_INFINITY;
		this.minNorthing = Double.POSITIVE_INFINITY;
		this.maxNorthing = Double.NEGATIVE_INFINITY;

		for (org.matsim.api.core.v01.network.Node node : n.getNodes().values()) {
			this.minEasting = Math.min(this.minEasting, node.getCoord().getX());
			this.maxEasting = Math.max(this.maxEasting, node.getCoord().getX());
			this.minNorthing = Math.min(this.minNorthing, node.getCoord().getY());
			this.maxNorthing = Math.max(this.maxNorthing, node.getCoord().getY());
		}
		// make sure, the bounding box is bigger than the biggest element, otherwise
		// requests with null == use biggest bounding box will fail on the leftmost elements
		this.maxEasting +=1;
		this.maxNorthing +=1;

		offsetEast = this.minEasting;
		offsetNorth = this.minNorthing;
		this.easting = this.maxEasting - this.minEasting;
		this.northing = this.maxNorthing - this.minNorthing;
		// set top node
		setTopNode(0, 0, easting, northing);
	}


	public void addAdditionalElement(OTFDataWriter element) {
		this.additionalElements.add(element);
	}

	public OTFClientQuad convertToClient(String id, final OTFServerRemote host, final OTFConnectionManager connect) {
		final OTFClientQuad client = new OTFClientQuad(id, host, 0.,0., this.easting, this.northing);
		client.offsetEast = this.minEasting;
		client.offsetNorth = this.minNorthing;

		//int colls = 
		this.execute(0.,0.,this.easting, this.northing,
				new ConvertToClientExecutor(connect,client));
//		System.out.print("server executor count: " +colls );

		for(OTFDataWriter element : this.additionalElements) {
			Collection<Class> readerClasses = connect.getToEntries(element.getClass());
			for (Class readerClass : readerClasses) {
				try {
					Object reader = readerClass.newInstance();
					client.addAdditionalElement((OTFDataReader)reader);
					log.info("Connected additional element writer " + element.getClass().getName() + "(" + element + ")  to " + reader.getClass().getName() + " (" + reader + ")");
				} catch (Exception e) {
					e.printStackTrace();
					throw new RuntimeException();
				}
			}

		}
		return client;
	}

	public void writeConstData(ByteBuffer out) {
		//int colls = 
		this.execute(0.,0.,this.easting, this.northing,
				new WriteDataExecutor(out,true));

		for(OTFDataWriter element : this.additionalElements) {
			try {
				element.writeConstData(out);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void writeDynData(QuadTree.Rect bounds, ByteBuffer out) {
		//int colls = 
		this.execute(bounds, new WriteDataExecutor(out,false));
		//System.out.print("# of Writes: " + colls + " -> ");

		for(OTFDataWriter element : this.additionalElements) {
			try {
				element.writeDynData(out);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// Internally we hold the coordinates from 0,0 to max -min .. to optimize use of float in visualizer
	@Override
	public double getMaxEasting() {
		return this.maxEasting;
	}

	@Override
	public double getMaxNorthing() {
		return this.maxNorthing;
	}

	@Override
	public double getMinEasting() {
		return this.minEasting;
	}

	@Override
	public double getMinNorthing() {
		return this.minNorthing;
	}
	
//	public void replace(double x, double y, int index, Class clazz) {
//		List<OTFDataWriter> writer = getLeafValues(x,y);
//		OTFDataWriter w = writer.get(index);
//		OTFDataWriter wnew;
//		try {
//			wnew = (OTFDataWriter) clazz.newInstance();
//			wnew.setSrc(w.getSrc());
//			writer.remove(index);
//			writer.add(index, wnew);
//		} catch (InstantiationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//	}



	private static class ConvertToClientExecutor implements Executor<OTFDataWriter> {
		final OTFConnectionManager connect;
		final OTFClientQuad client;

		public ConvertToClientExecutor(OTFConnectionManager connect2, OTFClientQuad client) {
			this.connect = connect2;
			this.client = client;
		}
		@SuppressWarnings("unchecked")
		public void execute(double x, double y, OTFDataWriter writer)  {
			Collection<Class> readerClasses = this.connect.getToEntries(writer.getClass());
			for (Class readerClass : readerClasses) {
				try {
					OTFDataReader reader = (OTFDataReader)readerClass.newInstance();
					reader.setSrc(writer);
					this.client.put(x, y, reader);
				} catch (Exception e) {
					e.printStackTrace();
					throw new RuntimeException();
				}
			}
		}
	}


	private static class WriteDataExecutor implements Executor<OTFDataWriter> {
		final ByteBuffer out;
		boolean writeConst;

		public WriteDataExecutor(ByteBuffer out, boolean writeConst) {
			this.out = out;
			this.writeConst = writeConst;
		}
		public void execute(double x, double y, OTFDataWriter writer)  {
			try {
				if (this.writeConst) writer.writeConstData(this.out);
				else writer.writeDynData(this.out);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
