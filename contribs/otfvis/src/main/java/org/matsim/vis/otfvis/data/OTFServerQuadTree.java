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

import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;
import org.matsim.vis.otfvis.interfaces.OTFServer;



/**
 * OTFServerQuad is the quad representation of all elements of the network on the server
 * side. This QuadTree is mirrored on the client side by OTFClientQuad.
 *
 * @author dstrippgen
 *
 */
public abstract class OTFServerQuadTree extends QuadTree<OTFDataWriter> {

	private static final Logger log = Logger.getLogger(OTFServerQuadTree.class);

	private final List<OTFDataWriter> additionalElements = new LinkedList<OTFDataWriter>();

	private static final long serialVersionUID = 1L;
	private double minEasting;
	private double maxEasting;
	private double minNorthing;
	private double maxNorthing;
	private double easting;
	private double northing;

	private  static OTFTransformation otfTransformation = null;
	private static CoordinateTransformation transformation = new IdentityTransformation();

	public OTFServerQuadTree(Network network) {
		super(0,0,0,0);
		this.setBoundingBoxFromNetwork(network);
	}

	public abstract void initQuadTree(final OTFConnectionManager connect);

	protected void setBoundingBoxFromNetwork(Network n){
		this.minEasting = Double.POSITIVE_INFINITY;
		this.maxEasting = Double.NEGATIVE_INFINITY;
		this.minNorthing = Double.POSITIVE_INFINITY;
		this.maxNorthing = Double.NEGATIVE_INFINITY;

		for (org.matsim.api.core.v01.network.Node node : n.getNodes().values()) {
			Coord coord = transformation.transform(node.getCoord());
			this.minEasting = Math.min(this.minEasting, coord.getX());
			this.maxEasting = Math.max(this.maxEasting, coord.getX());
			this.minNorthing = Math.min(this.minNorthing, coord.getY());
			this.maxNorthing = Math.max(this.maxNorthing, coord.getY());
		}
		// make sure, the bounding box is bigger than the biggest element, otherwise
		// requests with null == use biggest bounding box will fail on the leftmost elements
		this.maxEasting +=1;
		this.maxNorthing +=1;

		otfTransformation = new OTFTransformation(transformation, this.minEasting, this.minNorthing);
//		log.debug("offsetEast: " + offsetEast + " offsetNorth: " + offsetNorth + " " + transformation);
		this.easting = this.maxEasting - this.minEasting;
		this.northing = this.maxNorthing - this.minNorthing;
		// set top node
		setTopNode(0, 0, easting, northing);
	}


	/**
	 * Sets a new top node in case the extremities from the c'tor are not
	 * good anymore, it also clear the QuadTree
	 * @param minX The smallest x coordinate expected
	 * @param minY The smallest y coordinate expected
	 * @param maxX The largest x coordinate expected
	 * @param maxY The largest y coordinate expected
	 */
	protected void setTopNode(final double minX, final double minY, final double maxX, final double maxY) {
		this.top = new Node<OTFDataWriter>(minX, minY, maxX, maxY);
	}

	public void addAdditionalElement(OTFDataWriter element) {
		this.additionalElements.add(element);
	}

	public OTFClientQuadTree convertToClient(final OTFServer host, final OTFConnectionManager connect) {
		final OTFClientQuadTree client = new OTFClientQuadTree(host, 0., 0.,this.easting, this.northing);
		client.offsetEast = this.minEasting;
		client.offsetNorth = this.minNorthing;

		this.execute(0.,0.,this.easting, this.northing,
				new ConvertToClientExecutor(connect,client));

		for(OTFDataWriter<?> element : this.additionalElements) {
			Collection<Class<OTFDataReader>> readerClasses = connect.getReadersForWriter(element.getClass());
			for (Class<? extends OTFDataReader> readerClass : readerClasses) {
				try {
					Object reader = readerClass.newInstance();
					client.addAdditionalElement((OTFDataReader)reader);
					log.info("Connected additional element writer " + element.getClass().getName() + "(" + element + ")  to " + reader.getClass().getName() + " (" + reader + ")");
				} catch (InstantiationException e) {
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}

		}
		return client;
	}

	public void writeConstData(ByteBuffer out) {

		for (OTFDataWriter element : this.values()) {
			try {
				element.writeConstData(out);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		for (OTFDataWriter element : this.additionalElements) {
			try {
				element.writeConstData(out);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void writeDynData(QuadTree.Rect bounds, ByteBuffer out) {

		this.execute(bounds, new WriteDataExecutor(out,false));
		// "this" is a quad tree; thus this command somehow marks the relevant leaves

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

	public static Point2D.Double transform(Coord coord) {
		Coord transformedCoord = otfTransformation.transform(coord);
		Point2D.Double result = new Point2D.Double(transformedCoord.getX(), transformedCoord.getY());
		return result;
	}
	
	public static CoordinateTransformation getOTFTransformation(){
		return otfTransformation;
	}

	public static void setTransformation(CoordinateTransformation transformation) {
		OTFServerQuadTree.transformation = transformation;
	}

	private static class ConvertToClientExecutor implements Executor<OTFDataWriter> {
		final OTFConnectionManager connect;
		final OTFClientQuadTree client;

		public ConvertToClientExecutor(OTFConnectionManager connect2, OTFClientQuadTree client) {
			this.connect = connect2;
			this.client = client;
		}
		@Override
		@SuppressWarnings("unchecked")
		public void execute(double x, double y, OTFDataWriter writer)  {
			Collection<Class<?>> readerClasses = this.connect.getToEntries(writer.getClass());
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
		@Override
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
