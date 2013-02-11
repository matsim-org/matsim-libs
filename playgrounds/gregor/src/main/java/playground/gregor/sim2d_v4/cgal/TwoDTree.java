/* *********************************************************************** *
 * project: org.matsim.*
 * KDTree.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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


package playground.gregor.sim2d_v4.cgal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.vividsolutions.jts.geom.Envelope;



/**
 * implementation of 2-dimensional kd-tree (== 2d-tree)
 * similar to de Berg et al (2000), Computational Geometry: Algorithms and Applications. Springer
 * @author laemmel
 *
 * @param <T>
 */
public class TwoDTree<T extends TwoDObject> {


	private TwoDNode<T> root;

	public TwoDTree(Envelope e) {
		this.root = new TwoDNode<T>(e,0);
	}

	public void buildTwoDTree(List<T> values) {
		for (T val : values) {
			insert(val);
		}
	}

	public List<T> get(Envelope e) {
		return this.root.get(e);
	}


	public List<T> get(double minX, double minY, double maxX, double maxY) {
		Envelope e = new Envelope(minX,maxX,minY,maxY);
		return get(e);
	}

	public void insert(T val) {
		this.root.insert(val);
	}

	public void remove(T val) {
		this.root.remove(val);
	}

	public void clear() {
		this.root = new TwoDNode<T>(this.root.getEnvelope(),0);
	}

	private static class TwoDNode<T extends TwoDObject> {
		private final static int cacheSize = 16; 

		private final int depth;
		private final Envelope envelope;
		private TwoDNode<T> left = null;
		private TwoDNode<T> right = null;
		private final List<T> cache = new ArrayList<T>(cacheSize);

		private boolean internalNode = false;

		public TwoDNode(Envelope e, int i) {
			this.depth = i;
			this.envelope = e;
		}

		private boolean insert(T val) {

			if (this.envelope.intersects(val.getXLocation(), val.getYLocation())) {
				if (!this.internalNode) {
					this.cache.add(val);
					if (this.cache.size() >= cacheSize) {
						split();
					}

				} else {
					this.left.insert(val);
					this.right.insert(val);
				}
				return true;
			}

			return false;
		}

		public void remove(T val) {
			if (this.envelope.intersects(val.getXLocation(), val.getYLocation())) {

				if (this.internalNode) {
					this.left.remove(val);
					this.right.remove(val);
				} else {
					if (this.cache.size() > 0) {
						Iterator<T> it = this.cache.iterator();
						while (it.hasNext()) {
							T next = it.next();
							if (next == val) {
								it.remove();
								return;
							}
						}
					}				
				}
			}


		}

		public List<T> get(Envelope e) {
			List<T> ret = new ArrayList<T>();

			if (!this.envelope.intersects(e)) {
				return ret;
			}

			if (this.internalNode) {
				ret.addAll(this.left.get(e));
				ret.addAll(this.right.get(e));
			} else {
				for (T val : this.cache) {
					if (e.intersects(val.getXLocation(), val.getYLocation())) {
						ret.add(val);
					}
				}				
			}
			return ret;
		}

		private void split() {
			final double minX = this.envelope.getMinX();
			final double minY = this.envelope.getMinY();
			final double maxX = this.envelope.getMaxX();
			final double maxY = this.envelope.getMaxY();

			if (this.depth % 2 == 0) { //vertical split
				final double width = this.envelope.getWidth();
				Envelope e1 = new Envelope(minX, minX +width/2,  minY, maxY);
				this.left = new TwoDNode<T>(e1, this.depth+1);
				Envelope e2 = new Envelope(minX +width/2,maxX, minY,maxY);
				this.right = new TwoDNode<T>(e2, this.depth+1);

			} else {//horizontal split
				final double height = this.envelope.getHeight();
				Envelope e1 = new Envelope(minX, maxX,  minY, minY+height/2);
				this.left = new TwoDNode<T>(e1, this.depth+1);
				Envelope e2 = new Envelope(minX ,maxX, minY+height/2,maxY);
				this.right = new TwoDNode<T>(e2, this.depth+1);
			}

			Iterator<T> it = this.cache.iterator();
			while (it.hasNext()) {
				T next = it.next();
				if (this.left.insert(next)) {
					it.remove();
					continue;
				}
				if (this.right.insert(next)) {
					it.remove();
				}
			}

			this.internalNode = true;

		}
		
		public Envelope getEnvelope() {
			return this.envelope;
		}

	}


//	//testing only!!
//	public static void main (String [] args) {
//		int TEST = 10000;
//		Envelope e = new Envelope(-100,100,-100,100);
//		List<TwoDObj> os = new ArrayList<TwoDObj>();
//		for (int i = 0; i < TEST; i++) {
//			float x = (MatsimRandom.getRandom().nextFloat() - .5f) * 200;
//			float y = (MatsimRandom.getRandom().nextFloat() - .5f) * 200;
//			TwoDObj o = new TwoDObj(x,y);
//			os.add(o);
//		}
//
//
//		//1. build
//		long start = System.nanoTime();
//		for (int i = 0; i < 1000; i ++) {
//			QuadTree<TwoDObj> quadTree = new QuadTree<TwoDObj>(-100,-100,100,100);
//			for (TwoDObj o : os) {
//				quadTree.put(o.getXLocation(), o.getYLocation(), o);
//			}
//		}
//		long stop = System.nanoTime();
//		System.out.println("quad-tree build took: " + ((stop-start)/1000/1000.) + " ms");
//
//
//		long start2 = System.nanoTime();
//		for (int i = 0; i < 1000; i ++) {
//			TwoDTree<TwoDObj> twoDTree = new TwoDTree<TwoDObj>(e);
//			for (TwoDObj o : os) {
//				twoDTree.insert(o);
//			}
//		}
//		long stop2 = System.nanoTime();
//		System.out.println("2D-tree build took:   " + ((stop2-start2)/1000/1000.) + " ms");
//
//		//2. range search
//		QuadTree<TwoDObj> quadTree = new QuadTree<TwoDObj>(-100,-100,100,100);
//		TwoDTree<TwoDObj> twoDTree = new TwoDTree<TwoDObj>(e);
//		for (TwoDObj o : os) {
//			quadTree.put(o.getXLocation(), o.getYLocation(), o);
//			twoDTree.insert(o);
//		}		
//
//		List<Envelope> ranges = new ArrayList<Envelope>();
//		for (int i = 0; i < TEST; i++) {
//			double minX = (MatsimRandom.getRandom().nextDouble() - .5)*180; 
//			double minY= (MatsimRandom.getRandom().nextDouble() - .5)*180;
//			double rangeX = (1+2*MatsimRandom.getRandom().nextDouble())*10;
//			double rangeY = (1+2*MatsimRandom.getRandom().nextDouble())*10;
//			Envelope ee = new Envelope(minX, minX+rangeX, minY, minY+rangeY);
//			ranges.add(ee);
//		}
//
//		long start3 = System.nanoTime();
//		for (int i = 0; i < 100; i++) {
//			for (Envelope ee : ranges) {
//				quadTree.get(ee.getMinX(), ee.getMinY(), ee.getMaxX(), ee.getMaxY(), new ArrayList<TwoDObj>());
//			}}
//		long stop3 = System.nanoTime();
//		System.out.println("quad-tree range search took: " + ((stop3-start3)/1000/1000.) + " ms");
//
//		long start4 = System.nanoTime();
//		for (int i = 0; i < 100; i++) {
//			for (Envelope ee : ranges) {
//				twoDTree.get(ee);
//			}}
//		long stop4 = System.nanoTime();
//		System.out.println("2D-tree range search took: " + ((stop4-start4)/1000/1000.) + " ms");
//
//		for (Envelope ee: ranges) {
//			Set<TwoDObj> q = new HashSet<TwoDTree.TwoDObj>();
//			quadTree.get(ee.getMinX(), ee.getMinY(), ee.getMaxX(), ee.getMaxY(), q);
//			List<TwoDObj> k = twoDTree.get(ee);
//			if (q.size() != k.size()) {
//				System.err.println("should not happen");
//			}
//			for (TwoDObj t : k) {
//				if (!q.contains(t)){
//					System.err.println("should not happen");	
//				}
//			}
//		}
//	}
//
//	private static final class TwoDObj implements TwoDObject {
//
//		private final float x;
//		private final float y;
//
//		public TwoDObj(float x, float y) {
//			this.x = x;
//			this.y = y;
//		}
//
//		@Override
//		public float getXLocation() {
//			return this.x;
//		}
//
//		@Override
//		public float getYLocation() {
//			return this.y;
//		}
//
//	}
}
