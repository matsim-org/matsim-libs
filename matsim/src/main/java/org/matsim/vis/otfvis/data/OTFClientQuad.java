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
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.interfaces.OTFServerRemote;


/**
 * The OTFClientQuad is a QuadTree holding OTFDataReads objects.
 * It mirrors the OTFServerQuad on the server side of the OTFVis exactly.
 * It has several Executor classes defined for invalidating reading and creading the Quad.
 * 
 * @author dstrippgen
 *
 */
public class OTFClientQuad extends QuadTree<OTFDataReader> {
	
	private static final long serialVersionUID = 1L;

	private static final Logger log = Logger.getLogger(OTFClientQuad.class);
	
	private final double minEasting;
	private final double maxEasting;
	private final double minNorthing;
	private final double maxNorthing;
	private SceneGraph lastGraph = null;

	public double offsetEast;
	public double offsetNorth;

	private final String id;
	private final OTFServerRemote host;
	private OTFConnectionManager connect;

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

		public void execute(final double x, final double y, final OTFDataReader reader) {
			try {
				if (this.readConst)
					reader.readConstData(this.in);
				else
					reader.readDynData(this.in, this.graph);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	private static class InvalidateExecutor implements Executor<OTFDataReader> {
		private final SceneGraph sceneGraph;
		
		public InvalidateExecutor(final SceneGraph aSceneGraph) {
			this.sceneGraph = aSceneGraph;
		}

		public void execute(final double x, final double y, final OTFDataReader reader) {
			reader.invalidate(this.sceneGraph);
		}
	}

	public OTFClientQuad(final String id, final OTFServerRemote host, final double minX, final double minY, final double maxX, final double maxY) {
		super(minX, minY, maxX, maxY);
		this.minEasting = minX;
		this.maxEasting = maxX;
		this.minNorthing = minY;
		this.maxNorthing = maxY;
		this.id = id;
		this.host = host;
	}

	public String getId() {
		return id;
	}

	public void addAdditionalElement(final OTFDataReader element) {
		if (additionalElements.contains(element)) {
			// Some movies do this.
			log.warn("Trying to add a reader twice. Ignoring: " + element);
		} else {
			this.additionalElements.add(element);
		}
	}

	public synchronized void createReceiver(final OTFConnectionManager c) {
		this.connect = c;
		SceneGraph graph = new SceneGraph(null, -1, connect, null);
		for (OTFDataReader reader : this.values()) {
			Collection<OTFDataReceiver> drawers = this.connect.getReceiversForReader(reader.getClass(), graph);
			for (OTFDataReceiver drawer : drawers) {
				reader.connect(drawer);
			}
		}
		
		log.info("Connecting additional elements...");
		for(OTFDataReader element : this.additionalElements) {
			Collection<OTFDataReceiver> drawers = connect.getReceiversForReader(element.getClass(), graph);
			for (OTFDataReceiver drawer : drawers) {
				element.connect(drawer);
				log.info("  Connected " + element.getClass().getName() + " to " + drawer.getClass().getName());
			}
		}
	}

	public synchronized void getConstData() throws RemoteException {
		byte[] bbyte = this.host.getQuadConstStateBuffer(this.id);
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

	public void clearCache() {
		this.cachedTimes.clear();
	}

	public synchronized SceneGraph getSceneGraphNoCache(final int time, Rect rect, final OTFDrawer drawer) throws RemoteException {
		List<Rect> rects = new LinkedList<Rect>();

		SceneGraph cachedResult = this.cachedTimes.get(time);
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
		if(this.host.isLive() == false) {
			rect = null;
			cachedResult = null;
		}

		SceneGraph result;
		if ( cachedResult == null) {
			SceneGraph result1 = new SceneGraph(rect, time, this.connect, drawer);
			QuadTree.Rect bound2 = this.host.isLive() ? rect : this.top.getBounds();
			byte[] bbyte;
			bbyte = this.host.getQuadDynStateBuffer(this.id, bound2);
			ByteBuffer in = ByteBuffer.wrap(bbyte);
			this.execute(bound2, new ReadDataExecutor(in, false, result1));
			for(OTFDataReader element : this.additionalElements) {
				try {
					element.readDynData(in,result1);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// fill with elements
			invalidate(rect, result1);
			result = result1;
		} else {
			result = cachedResult;
			result.setRect(rect);
			for(Rect rectPart : rects) {
				QuadTree.Rect bound2 = this.host.isLive() ? rectPart : this.top.getBounds();
				byte[] bbyte;
				bbyte = this.host.getQuadDynStateBuffer(this.id, bound2);
				ByteBuffer in = ByteBuffer.wrap(bbyte);
				this.execute(bound2, new ReadDataExecutor(in, false, result));
				// fill with elements
				invalidate(rectPart, result);
			}
		}

		result.finish();

		if (OTFClientControl.getInstance().getOTFVisConfig().isCachingAllowed()) {
		  this.cachedTimes.put(time, result);
		}
		return result;
	}

	public synchronized SceneGraph getSceneGraph(final int time, final Rect rect, final OTFDrawer drawer) throws RemoteException {
		if ((time == -1) && (this.lastGraph != null)) return this.lastGraph;
		this.lastGraph = getSceneGraphNoCache(time, rect, drawer);
		return this.lastGraph;
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

	synchronized public void invalidateAll(final SceneGraph result) {
		invalidate(null, result);
	}
	
	@Override
	public double getMinEasting() {
		return this.minEasting;
	}

	@Override
	public double getMaxEasting() {
		return this.maxEasting;
	}

	@Override
	public double getMinNorthing() {
		return this.minNorthing;
	}

	@Override
	public double getMaxNorthing() {
		return this.maxNorthing;
	}

}
