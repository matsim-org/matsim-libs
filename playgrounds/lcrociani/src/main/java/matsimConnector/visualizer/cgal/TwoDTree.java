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


package matsimConnector.visualizer.cgal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.QuadTree;

import com.vividsolutions.jts.geom.Envelope;



/**
 * implementation of 2-dimensional kd-tree (== 2d-tree)
 * similar to de Berg et al (2000), Computational Geometry: Algorithms and Applications. Springer
 * The main differences are:
 * 	1. for small data sets (<100) data is stored in a list, since for small data sets a linear search is usually faster
 * 	2. the tree has a max depth to avoid stack overflow exceptions. If the max depth has been reached for a region all 
 * data subsequently inserted into this region is stored in a list. Thus, the algorithm retrieves data by linear search for those regions.  
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
		this.root.cache.clear();
//		this.root.cache = new ArrayList<T>(10000);
		this.root.left = null;
		this.root.right = null;
		this.root.maxDepth = 0;
		for (T val : values) {
			insert(val);
		}
		this.root.maxDepth = 128;
		if (values.size() > TwoDNode.cacheSize) {
			this.root.split();
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
		private static final Logger log = Logger.getLogger(TwoDNode.class);
		private static final int cacheSize = 128;
		private static final double EPSILON = 0.0001f; 
		private int maxDepth = 128; 

		private final int depth;
		private final Envelope envelope;
		private TwoDNode<T> left = null;
		private TwoDNode<T> right = null;
		private final List<T> cache = new ArrayList<T>(cacheSize);

		private boolean internalNode = false;

		public TwoDNode(Envelope e, int i) {
			this.depth = i;
			this.envelope = e;
			if (this.depth >= this.maxDepth) {
				log.warn("Maximum recursion depth reached! The region is: " + this.envelope + " Data inserted into this region will be stored in a linear list.");

			}
		}

		private boolean insert(T val) {

			if (this.envelope.intersects(val.getX(), val.getY())) {
				if (!this.internalNode) {
					this.cache.add(val);
					if (this.cache.size() >= TwoDNode.cacheSize && this.depth < this.maxDepth) {
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
			if (this.envelope.intersects(val.getX(), val.getY())) {

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
					if (e.intersects(val.getX(), val.getY())) {
						ret.add(val);
					}
				}				
			}
			return ret;
		}

		void split() {
			
//			if (this.cache.size() < 128) {
//				throw new RuntimeException("size was:" + this.cache.size());
//			}
			
			final double minX = this.envelope.getMinX();
			final double minY = this.envelope.getMinY();
			final double maxX = this.envelope.getMaxX();
			final double maxY = this.envelope.getMaxY();

			if (this.depth % 2 == 0) { //vertical split
				final double [] xs = new double[this.cache.size()];
				int idx = 0;
				Iterator<T> it = this.cache.iterator();
				while (it.hasNext()) {
					T next = it.next();
					double x = next.getX();
					xs[idx++] = x;
				}
				Arrays.sort(xs);
				final double median = xs[this.cache.size()/2];
				if ((xs[this.cache.size()-1]-xs[0]) < EPSILON) {
					log.warn("region to small can not split it!  The region is: " + this.envelope + " Data inserted into this region will be stored in a linear list.");
					this.maxDepth = this.depth;
				}

				//				final double width = median - this.envelope.getMinX();
				Envelope e1 = new Envelope(minX, median,  minY, maxY);
				this.left = new TwoDNode<T>(e1, this.depth+1);
				Envelope e2 = new Envelope(median,maxX, minY,maxY);
				this.right = new TwoDNode<T>(e2, this.depth+1);

			} else {//horizontal split
				final double [] ys = new double[this.cache.size()];
				int idx = 0;
				Iterator<T> it = this.cache.iterator();
				while (it.hasNext()) {
					T next = it.next();
					double y = next.getY();
					ys[idx++] = y;
				}
				Arrays.sort(ys);
				double median = ys[this.cache.size()/2];
				if ((ys[this.cache.size()-1]-ys[0]) < EPSILON) {
					log.warn("region to small can not split it!  The region is: " + this.envelope + " Data inserted into this region will be stored in a linear list.");
					this.maxDepth = this.depth;
				}


				//				final double height = this.envelope.getHeight();
				Envelope e1 = new Envelope(minX, maxX,  minY, median);
				this.left = new TwoDNode<T>(e1, this.depth+1);
				Envelope e2 = new Envelope(minX ,maxX, median,maxY);
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

	/*
	public static void main(String [] args) {
		for (int i = 10000; i <= 10000; i++) {
			System.out.println(i);
			run(10000);
		}
	}
	
	//testing only!!
	private static void run (int test) {
		int TEST = test;
		Envelope e = new Envelope(-100,100,-100,100);
		List<TwoDObj> os = new ArrayList<TwoDObj>();
		for (int i = 0; i < TEST; i++) {
			double x = (MatsimRandom.getRandom().nextFloat() - .5f) * 200;
			double y = (MatsimRandom.getRandom().nextFloat() - .5f) * 200;
			TwoDObj o = new TwoDObj(x,y);
			os.add(o);
		}


		//1. build
		long start = System.nanoTime();
		for (int i = 0; i < 1000; i ++) {
			QuadTree<TwoDObj> quadTree = new QuadTree<TwoDObj>(-100,-100,100,100);
			for (TwoDObj o : os) {
				quadTree.put(o.getX(), o.getY(), o);
			}
		}
		long stop = System.nanoTime();
		System.out.println("quad-tree build took: " + ((stop-start)/1000/1000.) + " ms");


		long start2 = System.nanoTime();
		for (int i = 0; i < 1000; i ++) {
			TwoDTree<TwoDObj> twoDTree = new TwoDTree<TwoDObj>(e);
			twoDTree.buildTwoDTree(os);
		}
		long stop2 = System.nanoTime();
		System.out.println("2D-tree build took:   " + ((stop2-start2)/1000/1000.) + " ms");

		//2. range search
		QuadTree<TwoDObj> quadTree = new QuadTree<TwoDObj>(-100,-100,100,100);
		TwoDTree<TwoDObj> twoDTree = new TwoDTree<TwoDObj>(e);
		for (TwoDObj o : os) {
			quadTree.put(o.getX(), o.getY(), o);
		}
		twoDTree.buildTwoDTree(os);

		List<Envelope> ranges = new ArrayList<Envelope>();
		for (int i = 0; i < TEST; i++) {
			double minX = (MatsimRandom.getRandom().nextDouble() - .5)*180; 
			double minY= (MatsimRandom.getRandom().nextDouble() - .5)*180;
			double rangeX = (1+2*MatsimRandom.getRandom().nextDouble())*10;
			double rangeY = (1+2*MatsimRandom.getRandom().nextDouble())*10;
			Envelope ee = new Envelope(minX, minX+rangeX, minY, minY+rangeY);
			ranges.add(ee);
		}

		long start3 = System.nanoTime();
		for (int i = 0; i < 100; i++) {
			for (Envelope ee : ranges) {
				quadTree.get(ee.getMinX(), ee.getMinY(), ee.getMaxX(), ee.getMaxY(), new ArrayList<TwoDObj>());
			}}
		long stop3 = System.nanoTime();
		System.out.println("quad-tree range search took: " + ((stop3-start3)/1000/1000.) + " ms");

		long start4 = System.nanoTime();
		for (int i = 0; i < 100; i++) {
			for (Envelope ee : ranges) {
				twoDTree.get(ee);
			}}
		long stop4 = System.nanoTime();
		System.out.println("2D-tree range search took: " + ((stop4-start4)/1000/1000.) + " ms");

		for (Envelope ee: ranges) {
			Set<TwoDObj> q = new HashSet<TwoDTree.TwoDObj>();
			quadTree.get(ee.getMinX(), ee.getMinY(), ee.getMaxX(), ee.getMaxY(), q);
			List<TwoDObj> k = twoDTree.get(ee);
			if (q.size() != k.size()) {
				System.err.println("should not happen");
			}
			for (TwoDObj t : k) {
				if (!q.contains(t)){
					System.err.println("should not happen");	
				}
			}
		}
	}

	private static final class TwoDObj implements TwoDObject {

		private final double x;
		private final double y;

		public TwoDObj(double x, double y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public double getX() {
			return this.x;
		}

		@Override
		public double getY() {
			return this.y;
		}

	}*/
}
