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

package org.matsim.utils.vis.otfvis.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.gbl.Gbl;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.vis.otfvis.caching.SceneGraph;
import org.matsim.utils.vis.otfvis.gui.PoolFactory;
import org.matsim.utils.vis.otfvis.interfaces.OTFDataReader;
import org.matsim.utils.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.utils.vis.otfvis.interfaces.OTFLiveServerRemote;
import org.matsim.utils.vis.otfvis.interfaces.OTFQuery;
import org.matsim.utils.vis.otfvis.interfaces.OTFServerRemote;


public class OTFClientQuad extends QuadTree<OTFDataReader> {
	private final double minEasting;
	private final double maxEasting;
	private final double minNorthing;
	private final double maxNorthing;
	
	public double offsetEast;
	public double offsetNorth;

	private final String id;
	private final OTFServerRemote host;
	private OTFConnectionManager connect;
	private static boolean cachingAllowed = true;
	
	private final List<OTFDataReader> additionalElements= new LinkedList<OTFDataReader>();

	static class CreateReceiverExecutor extends Executor<OTFDataReader> {
		final OTFConnectionManager connect;
		final SceneGraph graph;

		public CreateReceiverExecutor(OTFConnectionManager connect2, SceneGraph graph) {
			this.connect = connect2;
			this.graph = graph;
		}

		@Override
		public void execute(double x, double y, OTFDataReader reader) {
			Collection<OTFData.Receiver> drawers = connect.getReceivers(reader.getClass(), graph);
			for (OTFData.Receiver drawer : drawers) reader.connect(drawer);
		}
	}

	class ReadDataExecutor extends Executor<OTFDataReader> {
		final ByteBuffer in;
		boolean readConst;
		SceneGraph graph;

		public ReadDataExecutor(ByteBuffer in, boolean readConst, SceneGraph graph) {
			this.in = in;
			this.readConst = readConst;
			this.graph = graph;
		}

		@Override
		public void execute(double x, double y, OTFDataReader reader) {
			try {
				if (readConst)
					reader.readConstData(in);
				else
					reader.readDynData(in, graph);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	class InvalidateExecutor extends Executor<OTFDataReader> {
		private final SceneGraph result;
		public InvalidateExecutor(SceneGraph result) {
			this.result = result;
		}
 
		@Override
		public void execute(double x, double y, OTFDataReader reader) {
			reader.invalidate(result);
		}
	}

	public class ClassCountExecutor extends Executor<OTFDataReader> {
		private final Class targetClass;
		private int count = 0;
		
		public ClassCountExecutor(Class clazz) {
			targetClass = clazz;
		}
		
		public int getCount() {
			return count;
		}
		
		@Override
		public void execute(double x, double y, OTFDataReader reader) {
			if (targetClass.isAssignableFrom(reader.getClass())) count++;
		}
	}

	public OTFClientQuad(String id, OTFServerRemote host, double minX, double minY, double maxX, double maxY) {
		super(minX, minY, maxX, maxY);
		this.minEasting = minX;
		this.maxEasting = maxX;
		this.minNorthing = minY;
		this.maxNorthing = maxY;
		this.id = id;
		this.host = host;
	}

	public void addAdditionalElement(OTFDataReader element) {
		additionalElements.add(element);
	}

	public synchronized void createReceiver(final OTFConnectionManager connect) {

		this.connect = connect;

		SceneGraph graph = new SceneGraph(null, -1, connect, null);
		this.execute(this.top.getBounds(),
				new CreateReceiverExecutor(connect, graph));
		for(OTFDataReader element : additionalElements) {
			Collection<OTFData.Receiver> drawers = connect.getReceivers(element.getClass(), graph);
			for (OTFData.Receiver drawer : drawers) element.connect(drawer);
		}
	}

	private void getAdditionalData(ByteBuffer in, boolean readConst, SceneGraph graph) {
		for(OTFDataReader element : additionalElements) {
			try {
				if (readConst) element.readConstData(in);
				else element.readDynData(in,graph);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
	private void getData(QuadTree.Rect bound, boolean readConst, SceneGraph result, boolean readAdd)
			throws RemoteException {
		bound = host.isLive() ? bound : this.top.getBounds();
		byte[] bbyte = readConst ? host.getQuadConstStateBuffer(id):host.getQuadDynStateBuffer(id, bound);
		ByteBuffer in = ByteBuffer.wrap(bbyte);
		Gbl.startMeasurement();
		//int colls = 
		this.execute(bound, this.new ReadDataExecutor(in, readConst, result));
		if (readAdd) getAdditionalData(in, readConst, result);
		//System.out.println("# readData: " + colls);

		PoolFactory.resetAll();
		
	}

	public synchronized void getConstData() throws RemoteException {
		getData(null, true, null, true);
	}

	synchronized protected void getDynData(QuadTree.Rect bound, SceneGraph result, boolean readAdd) throws RemoteException {
		getData(bound, false, result, readAdd);
	}
	 
	private final Map<Integer,SceneGraph> cachedTimes = new HashMap<Integer,SceneGraph>();
	
	public void clearCache() {
		cachedTimes.clear();
	}
	
	synchronized public SceneGraph preloadTime(double time, Rect rect, OTFDrawer drawer) throws RemoteException {
		SceneGraph result = new SceneGraph(rect, time, connect, drawer);
		getDynData(rect, result, true);
		// fill with elements
		invalidate(rect, result);
		return result;
	}
	

	/*
	 * new getScenegraph(time, rect) called from either OGLDrawer or (for caching) from builderThread
	 * if cahchedversion is in rect and time -> OK
	 * else 
	 * build scnegraph wth new layers set according to connect
	 * new scenegraph(rect);
	 * invalidate quad and additional elements
	 * in invalidate of elements :
	 *  scenegraph.add2layer(this);
	 *  
	 *  store scenegraph according to time
	 *  
	 */
	
	private SceneGraph lastGraph = null;
	
	public synchronized SceneGraph getSceneGraphNoCache(int time, Rect rect, OTFDrawer drawer) throws RemoteException {
		List<Rect> rects = new LinkedList<Rect>();		
		
		SceneGraph cachedResult = cachedTimes.get(time);
		if(cachedResult != null) {
			Rect cachedRect = cachedResult.getRect();
			if(cachedRect == null || cachedRect.containsOrEquals(rect)) return cachedResult;
			
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
		if(host.isLive() == false) {
			rect = null;
			cachedResult = null;
		}
		
		SceneGraph result;
		if ( cachedResult == null) {
			result = preloadTime(time, rect, drawer);
		} else {
			result = cachedResult;
			result.setRect(rect);
			for(Rect rectPart : rects) {
				getDynData(rectPart, result, false);
				// fill with elements
				invalidate(rectPart, result);
			}
		}


		result.finish();
		if (isCachingAllowed()) cachedTimes.put(time, result);
		return result;
	}
	
	public synchronized SceneGraph getSceneGraph(int time, Rect rect, OTFDrawer drawer) throws RemoteException {
		if (time == -1 && lastGraph != null) return lastGraph;
		lastGraph = getSceneGraphNoCache(time, rect, drawer);
		return lastGraph;
	}
	
	synchronized private void invalidate(Rect rect, SceneGraph result) {
		if (rect == null) {
			rect = this.top.getBounds();
		}

		//int colls = 
		this.execute(rect, this.new InvalidateExecutor(result));
		for(OTFDataReader element : additionalElements) {
			element.invalidate(result);
		}
	}

	@Override
	public double getMinEasting() {
		return minEasting;
	}

	@Override
	public double getMaxEasting() {
		return maxEasting;
	}

	@Override
	public double getMinNorthing() {
		return minNorthing;
	}

	@Override
	public double getMaxNorthing() {
		return maxNorthing;
	}

	public OTFQuery doQuery(OTFQuery query) {
		OTFQuery result = null;
		try {
			if(host.isLive()) {
				result = ((OTFLiveServerRemote)host).answerQuery(query);
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * @return the chachingAllowed
	 */
	public static boolean isCachingAllowed() {
		return cachingAllowed;
	}

	/**
	 * @param chachingAllowed the chachingAllowed to set
	 */
	public static void setCachingAllowed(boolean cachingAllowed) {
		OTFClientQuad.cachingAllowed = cachingAllowed;
	}
}
