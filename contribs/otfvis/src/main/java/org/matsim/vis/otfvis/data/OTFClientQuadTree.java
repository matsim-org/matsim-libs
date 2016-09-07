/* *********************************************************************** *
 * project: org.matsim.*
 * OTFClientQuad.java
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;
import org.matsim.vis.otfvis.interfaces.OTFServer;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;


/**
 * The OTFClientQuad is a QuadTree holding OTFDataReads objects.
 * It mirrors the OTFServerQuad on the server side of the OTFVis exactly.
 * It has several Executor classes defined for invalidating reading and creading the Quad.
 *
 * @author dstrippgen
 *
 */
public class OTFClientQuadTree extends QuadTree<OTFDataReader> {

	private static final long serialVersionUID = 1L;

	private static final Logger log = Logger.getLogger(OTFClientQuadTree.class);

	private double minEasting;
	private double maxEasting;
	private double minNorthing;
	private double maxNorthing;
	private SceneGraph lastGraph = null;

	public double offsetEast;
	public double offsetNorth;

	private final OTFServer host;

	private final List<OTFDataReader> additionalElements = new LinkedList<OTFDataReader>();

	private static class ReadDataExecutor implements Executor<OTFDataReader> {
		final ByteBuffer in;
		boolean readConst;
		SceneGraph graph;

		public ReadDataExecutor(final ByteBuffer in, final boolean readConst, final SceneGraph graph) {
			this.in = in;
			this.readConst = readConst;
			this.graph = graph;
		}

		@Override
		public void execute(final double x, final double y, final OTFDataReader reader) {
			// I the end, the readers are stored in the leaves of the quad tree, and the "mechanics" will get them out and
			// feed them into this method here.  
			// The original filling happens via the quadtree "put" method.  Technically, this is achieved (I think)
			// by ConvertToClientExecutor in a ServerQuad.  I.e. the writers in the ServerQuad are mirrored by the 
			// corresponding readers in the ClientQuad.  kai, feb'11

			try {
				if (this.readConst)
					reader.readConstData(this.in);
				else
					reader.readDynData(this.in, this.graph);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static class InvalidateExecutor implements Executor<OTFDataReader> {
		private final SceneGraph sceneGraph;

		public InvalidateExecutor(final SceneGraph aSceneGraph) {
			this.sceneGraph = aSceneGraph;
		}

		@Override
		public void execute(final double x, final double y, final OTFDataReader reader) {
			reader.invalidate(this.sceneGraph);
		}
	}

	OTFClientQuadTree(final OTFServer host, final double minX, final double minY, final double maxX, final double maxY) {
		super(minX, minY, maxX, maxY);
		this.minEasting = minX;
		this.maxEasting = maxX;
		this.minNorthing = minY;
		this.maxNorthing = maxY;
		this.host = host;
	}

	public void addAdditionalElement(final OTFDataReader element) {
		if (additionalElements.contains(element)) {
			// Some movies do this.
			log.warn("Trying to add a reader twice. Ignoring: " + element);
		} else {
			this.additionalElements.add(element);
		}
	}

	public synchronized void getConstData() {
		byte[] bbyte = this.host.getQuadConstStateBuffer();
		ByteBuffer in = ByteBuffer.wrap(bbyte);
		for (OTFDataReader reader : this.values()) {
			try {
				reader.readConstData(in);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		for(OTFDataReader reader : this.additionalElements) {
			try {
				reader.readConstData(in);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		log.info("  read constant data");
	}

	private final Map<Integer,SceneGraph> cachedTimes = new HashMap<Integer,SceneGraph>();

	/**
	 * I think that this requests the scene graph for a given time step and for the rectangle that is visible.  
	 */
	private SceneGraph createSceneGraph(final int time, Rect rect) {
		List<Rect> rects = new LinkedList<>();
		/*
		 * This hack ensures that vehicles on links are drawn even if their center is not visible
		 */
		if (OTFClientControl.getInstance().getOTFVisConfig().isScaleQuadTreeRect()){
			rect = rect.scale(5.0, 5.0);
		}

		SceneGraph cachedResult = this.cachedTimes.get(time);
		// cachedTimes refers to snapshots that were already rendered at some earlier time (only mvi mode).

		if(cachedResult != null) {
			Rect cachedRect = cachedResult.getRect();
			if((cachedRect == null) || cachedRect.containsOrEquals(rect)) return cachedResult;

			Rect intersec = rect.intersection(cachedRect);
			if(intersec == null) {
				// we need to get the whole rect
				cachedResult = null;
			} else {
				// As we can only store ONE rect with our cached Drawing, we cannot simply
				// add the new portion to the old rect but have to use a rect where both
				// old and new rect fit into aka the union of both
				rect = rect.union(cachedRect);
				// Check the four possible rects, that need filling, possible rect follow this scheme
				//   1133333344
				//   11iiiiii44
				//   11iiiiii44
				//   1122222244
				double r1w = cachedRect.minX - rect.minX;
				double r2h = cachedRect.minY - rect.minY;
				double r3h = rect.maxY -cachedRect.maxY;
				double r4w = rect.maxX -cachedRect.maxX;
				if (r1w > 0) rects.add(new Rect(rect.minX,rect.minY,cachedRect.minX,rect.maxY));
				if (r4w > 0) rects.add(new Rect(cachedRect.maxX,rect.minY, rect.maxX,rect.maxY));
				if (r2h > 0) rects.add(new Rect(cachedRect.minX,rect.minY,cachedRect.maxX,cachedRect.minY));
				if (r3h > 0) rects.add(new Rect(cachedRect.minX,cachedRect.maxY,cachedRect.maxX,rect.maxY));
			}
		}

		// otherwise this Scenegraph is not useful, so we create a new one
		//
		// I don't understand the above comment.  "isLive" means is interactive.  if it is not interactive, there
		// is no cached result.
		if(this.host.isLive() == false) {
			rect = null;
			cachedResult = null;
		}

		SceneGraph result;
		if ( cachedResult == null) {
			result = new SceneGraph(rect);
			// (sets up the layers but does not put content)

			QuadTree.Rect bound2 = this.host.isLive() ? rect : this.top.getBounds();
			byte[] bbyte;
			bbyte = this.host.getQuadDynStateBuffer(bound2);
			// (seems that this contains the whole time step (in binary form))

			ByteBuffer in = ByteBuffer.wrap(bbyte);
			// (converts the byte buffer into an object)

			this.execute(bound2, new ReadDataExecutor(in, false, result));
			// (this is pretty normal, but I still keep forgetting it: The leaves of the QuadTree contain objects of type
			// OTFDataReader.  The ReadDataExecutor (defined above) uses them to read the data.

			for(OTFDataReader element : this.additionalElements) {
				try {
					element.readDynData(in,result);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// fill with elements
			//
			// I don't understand the wording.  Why does "invalidate" do a "fill with elements"?
			invalidate(rect, result);
		} else {
			result = cachedResult;
			result.setRect(rect);
			for(Rect rectPart : rects) {
				QuadTree.Rect bound2 = this.host.isLive() ? rectPart : this.top.getBounds();
				byte[] bbyte;
				bbyte = this.host.getQuadDynStateBuffer(bound2);
				ByteBuffer in = ByteBuffer.wrap(bbyte);
				this.execute(bound2, new ReadDataExecutor(in, false, result));
				// fill with elements
				invalidate(rectPart, result);
			}
		}

		result.finish();

		if (this.host.isLive() == false) {
			this.cachedTimes.put(time, result);
		}
		return result;
	}

	/**
	 * I think that this requests the scene graph for a given time step and for the rectangle that is visible.  
	 */
	public synchronized SceneGraph getSceneGraph(final int time, final Rect rect) {
		if ((time == -1) && (this.lastGraph != null)) {
			return this.lastGraph;
		} else {
			this.lastGraph = createSceneGraph(time, rect);
			return this.lastGraph;
		}
	}

	private void invalidate(Rect rect, final SceneGraph result) {
		if (rect == null) {
			rect = this.top.getBounds();
		}

		this.execute(rect, new InvalidateExecutor(result));
		for(OTFDataReader element : this.additionalElements) {
			element.invalidate(result);
		}
	}

	@Override
	public double getMinEasting() {
		return this.minEasting;
	}
	
	public void setMinEasting(double minEasting) {
		this.minEasting = minEasting;
	}

	@Override
	public double getMaxEasting() {
		return this.maxEasting;
	}
	
	public void setMaxEasting(double maxEasting) {
		this.maxEasting = maxEasting;
	}

	@Override
	public double getMinNorthing() {
		return this.minNorthing;
	}
	
	public void setMinNorthing(double minNorthing) {
		this.minNorthing = minNorthing;
	}

	@Override
	public double getMaxNorthing() {
		return this.maxNorthing;
	}
	
	public void setMaxNorthing(double maxNorthing) {
		this.maxNorthing = maxNorthing;
	}

}
