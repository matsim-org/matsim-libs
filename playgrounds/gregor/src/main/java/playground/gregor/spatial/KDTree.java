/* *********************************************************************** *
 * project: org.matsim.*
 * KDTree.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.spatial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.QuadTree;

import playground.gregor.sim2d_v3.helper.gisdebug.GisDebugger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

//import playground.gregor.sim2d_v2.helper.gisdebug.GisDebugger;
//
//import com.vividsolutions.jts.geom.Coordinate;
//import com.vividsolutions.jts.geom.Envelope;
//import com.vividsolutions.jts.geom.GeometryFactory;
//import com.vividsolutions.jts.geom.LineString;
//import com.vividsolutions.jts.geom.Point;


/**
 * 2D-KDTree (2DTree) implementation
 * @author laemmel
 *
 */
public class KDTree {

	private final KDNode root;
	private final List<double[]> points;

	public KDTree(List<double[]> points) {

				Envelope e = new Envelope();

		List<Integer> x = new ArrayList<Integer>();
		List<Integer> y = new ArrayList<Integer>();
		for (int i = 0; i < points.size(); i++) {
			x.add(i);
			y.add(i);

						Coordinate c = new Coordinate(points.get(i)[0],points.get(i)[1]);
						e.expandToInclude(c);
		}

		Collections.sort(x, new XComp(points));
		Collections.sort(y, new YComp(points));
		this.points = points;
				P p = new P(x,y,e);
//		P p = new P(x,y);

		int splitPoint = x.size()/2;
		double splitVal = this.points.get(x.get(splitPoint))[0];
		this.root = new KDNode(p,0,splitVal,null);

		buildKDTree(this.root,0);
		//		System.out.println(this.root.left.p.x.size()+ "  " + this.root.right.p.x.size());
				GisDebugger.dump("/Users/laemmel/tmp/!KDP.shp");
	}

	public KDNode getRoot() {
		return this.root;
	}

	public double[] getNearestNeighbor(double x, double y) {
		KDNode n = getNearestNeighbor(x, y,this.root);
		//		GisDebugger.dump("/Users/laemmel/tmp/!KDTreeNN.shp");
		return this.points.get(n.p.getXs().get(0));
	}

	private KDNode getNearestNeighbor(double x, double y, KDNode root) {
		KDNode leaf = goDownToLeaf(x,y,0,root);

		KDNode best = goUpToRoot(x,y,leaf,leaf, root.depth);
		return best;
	}

	private KDNode goUpToRoot(double x, double y, KDNode current, KDNode currentBest, int rootDepth) {
		if (current.depth == rootDepth) {
			return currentBest;
		}

		KDNode parent = current.parent;

		KDNode nodeToCheck;
		if (parent.left == current) {
			nodeToCheck = parent.right;
		} else {
			nodeToCheck = parent.left;
		}
		if (needToCheck(parent,x,y,currentBest)) {
			KDNode tmpBest = getNearestNeighbor(x,y,nodeToCheck);
			if (!tmpBest.p.isLeaf()) {
				throw new RuntimeException("Should not happen! Remove this check if it does not happen!");
			}
			double [] tmpVal = this.points.get(tmpBest.p.getXs().get(0));
			double sqrDistTmp = (x-tmpVal[0]) * (x-tmpVal[0]) + (y-tmpVal[1]) * (y-tmpVal[1]);

			//TODO this value needs to be calculated only once!!
			double [] val = this.points.get(currentBest.p.getXs().get(0));
			double bestSqrDist = (x- val[0]) * ( x - val[0]) + (y - val[1]) * (y - val[1]);

			if (sqrDistTmp < bestSqrDist) {
				currentBest  = tmpBest;
			}
		}

		return goUpToRoot(x,y,parent,currentBest,rootDepth);
	}

	private boolean needToCheck(KDNode node, double x, double y,
			KDNode currentBest) {

		//TODO this value needs to be calculated only once!!
		double [] val = this.points.get(currentBest.p.getXs().get(0));
		double bestDist = Math.sqrt((x- val[0]) * ( x - val[0]) + (y - val[1]) * (y - val[1]));

		if (node.depth % 2 == 0) {
			if (Math.abs(node.splitVal - x) < bestDist) {
				return true;
			}
		} else {
			if (Math.abs(node.splitVal - y) < bestDist) {
				return true;
			}			
		}
		return false;
	}

	private KDNode goDownToLeaf(double x, double y, int i, KDNode current) {

		//		Envelope e = current.p.getEnvelope();
		//		Coordinate [] coords = {new Coordinate(e.getMinX(),e.getMinY()),new Coordinate(e.getMaxX(),e.getMinY()),new Coordinate(e.getMaxX(),e.getMaxY()), new Coordinate(e.getMinX(),e.getMaxY()),new Coordinate(e.getMinX(),e.getMinY())};
		//		GeometryFactory geofac = new GeometryFactory();
		//		LineString ls = geofac.createLineString(coords);
		//		GisDebugger.addGeometry(ls,i+"");


		if (current.p.isLeaf()) {
			return current;
		}

		boolean goLeft;
		if (current.depth % 2 == 0) {
			if (x <= current.splitVal) {
				goLeft = true;
			} else {
				goLeft = false;
			}
		} else {
			if (y <= current.splitVal) {
				goLeft = true;
			} else {
				goLeft = false;
			}
		}

		KDNode child = goLeft ? current.left : current.right;
		return goDownToLeaf(x,y,++i,child);
	}

	private KDNode buildKDTree(KDNode current, int i) {

				Envelope e = current.p.getEnvelope();
				Coordinate [] coords = {new Coordinate(e.getMinX(),e.getMinY()),new Coordinate(e.getMaxX(),e.getMinY()),new Coordinate(e.getMaxX(),e.getMaxY()), new Coordinate(e.getMinX(),e.getMaxY()),new Coordinate(e.getMinX(),e.getMinY())};
				GeometryFactory geofac = new GeometryFactory();
				LineString ls = geofac.createLineString(coords);
				GisDebugger.addGeometry(ls,i+"");


		if (current.p.isLeaf()) {
			return current;
		}

		List<Integer> x = current.p.getXs();
		List<Integer> y = current.p.getYs();

		int splitPoint = x.size()/2;
		double splitVal = current.splitVal;

		double splitR, splitL;
		P p1;
		P p2;

		if (i % 2 == 0) { // x-split

			List<Integer> xLeft = x.subList(0, splitPoint);
			List<Integer> xRight = x.subList(splitPoint, x.size());


			boolean [] lr = new boolean[this.points.size()];


			for (int j = 0; j < splitPoint; j++) {
				int idx = x.get(j);
				lr[idx] = false;
			}
			for (int j = splitPoint; j < x.size(); j++) {
				int idx = x.get(j);
				lr[idx] = true;
			}

			List<Integer> yLeft = new ArrayList<Integer>();
			List<Integer> yRight = new ArrayList<Integer>();
			for (int j = 0; j < x.size(); j++) {//BUG
				int idx0 = y.get(j);
				if (lr[idx0]) {
					yRight.add(idx0);
				} else {
					yLeft.add(idx0);
				}
			}


			splitVal = this.points.get(x.get(splitPoint))[0];
						Envelope eLeft = new Envelope(e.getMinX(),splitVal,e.getMinY(),e.getMaxY());
						Envelope eRight = new Envelope(splitVal,e.getMaxX(),e.getMinY(),e.getMaxY());
			//			p1 = new P(xLeft,yLeft,eLeft);
			//			p2 = new P(xRight,yRight,eRight);

			int splitPointR = xRight.size()/2;
			int splitPointL = xLeft.size()/2;
			splitR = this.points.get(yRight.get(splitPointR))[1];
			splitL = this.points.get(yLeft.get(splitPointL))[1];

			p1 = new P(xLeft,yLeft,eLeft);
			p2 = new P(xRight,yRight,eRight);

		} else { // y-split

			List<Integer> yLeft = y.subList(0, splitPoint);
			List<Integer> yRight = y.subList(splitPoint, y.size());


			boolean [] lr = new boolean[this.points.size()];


			for (int j = 0; j < splitPoint; j++) {
				int idx = y.get(j);
				lr[idx] = false;
			}
			for (int j = splitPoint; j < x.size(); j++) {
				int idx = y.get(j);
				lr[idx] = true;
			}

			List<Integer> xLeft = new ArrayList<Integer>();
			List<Integer> xRight = new ArrayList<Integer>();
			for (int j = 0; j < x.size(); j++) { //BUG
				int idx0 = x.get(j);
				if (lr[idx0]) {
					xRight.add(idx0);
				} else {
					xLeft.add(idx0);
				}
			}


			splitVal = this.points.get(y.get(splitPoint))[1];
						Envelope eLeft = new Envelope(e.getMinX(),e.getMaxX(),e.getMinY(),splitVal);
						Envelope eRight = new Envelope(e.getMinX(),e.getMaxX(),splitVal,e.getMaxY());
			//			p1 = new P(xLeft,yLeft,eLeft);
			//			p2 = new P(xRight,yRight,eRight);


			int splitPointR = xRight.size()/2;
			int splitPointL = (int) 0.5+xLeft.size()/2;
			splitR = this.points.get(xRight.get(splitPointR))[0];
			splitL = this.points.get(xLeft.get(splitPointL))[0];

			p1 = new P(xLeft,yLeft,eLeft);
			p2 = new P(xRight,yRight,eRight);
		}
		i++;

		KDNode right = new KDNode(p2, i, splitR, current); 

		buildKDTree(right,i);
		KDNode left = new KDNode(p1,i,splitL,current);
		buildKDTree(left,i);

		KDNode ret = current;
		ret.left = left;
		ret.right = right;
		return ret;
	}

	private static class KDNode {
		private final int depth;
		private final double splitVal;
		private final KDNode parent;
		public KDNode(P p, int i, double splitVal, KDNode parent) {
			this.p = p;
			this.depth = i;
			this.splitVal = splitVal;
			this.parent = parent;
		}

		private KDNode left;
		private KDNode right;
		private final P p;
	}

	private static class P {

		private final List<Integer> x;
		private final List<Integer> y;
				private final Envelope e;
		
				public P(List<Integer> x2, List<Integer> y2, Envelope e) {
					this.x = x2;
					this.y = y2;
					this.e = e;
				}
//		public P(List<Integer> x2, List<Integer> y2) {
//			this.x = x2;
//			this.y = y2;
//		}
				public Envelope getEnvelope() {
					return this.e;
				}

		public List<Integer> getXs() {
			return this.x;
		}
		public List<Integer> getYs() {
			return this.y;
		}

		public boolean isLeaf() {
			return this.x.size() == 1;
		}


	}

	private static class XComp implements Comparator<Integer> {

		private final List<double[]> points;

		public XComp(List<double[]> points) {
			this.points = points;
		}

		@Override
		public int compare(Integer a0, Integer a1) {
			double p0 = this.points.get(a0)[0];
			double p1 = this.points.get(a1)[0];
			if (p0 < p1) {
				return -1;
			} else if (p0 > p1) {
				return 1;
			}
			throw new RuntimeException("Not implemented yet");
		}

	}

	private static class YComp implements Comparator<Integer> {

		private final List<double[]> points;

		public YComp(List<double[]> points) {
			this.points = points;
		}

		@Override
		public int compare(Integer a0, Integer a1) {
			double p0 = this.points.get(a0)[1];
			double p1 = this.points.get(a1)[1];
			if (p0 < p1) {
				return -1;
			} else if (p0 > p1) {
				return 1;
			}
			throw new RuntimeException("Not implemented yet");
		}

	}


	public static void main(String [] args) {
		for (int j = 0; j < 5;j++) {
			
			System.out.println("=================");
			System.out.println();
			List<double []> points = new ArrayList<double[]>();
			Envelope e = new Envelope();
			for (int i = 0; i < 50; i++) {
				double offset = 0;
				if (MatsimRandom.getRandom().nextBoolean()) {
					offset = 10*MatsimRandom.getRandom().nextDouble();
				}
				double x = 4*MatsimRandom.getRandom().nextDouble() + offset;
				double y = 4*MatsimRandom.getRandom().nextDouble() + offset;
				double [] p = {x,y};
				points.add(p);
				e.expandToInclude(x, y);
			}

			Comparator<double[]> comp = new Comparator<double []>(){

				@Override
				public int compare(double[] arg0, double[] arg1) {
					if (arg0[0] < arg1[0]) {
						return -1;
					} else if (arg0[0] > arg1[0]){
						return 1;
					}
					return 0;
				}};
				Collections.sort(points, comp);
				//		points.add(new double[]{1,2});
				//		points.add(new double[]{3,1});
				//		points.add(new double[]{0,4});
				//		points.add(new double[]{2,3});
				//		points.add(new double[]{1.5,0});
				//		points.add(new double[]{2.5,1.5});
				//		points.add(new double[]{0.5,3.5});
				//		points.add(new double[]{1.25,0.5});
				//		points.add(new double[]{0.25,0.75});

						GeometryFactory geofac = new GeometryFactory();
						for (double [] p : points) {
							Point point = geofac.createPoint(new Coordinate(p[0],p[1]));
							GisDebugger.addGeometry(point);
						}
						GisDebugger.dump("/Users/laemmel/tmp/!points.shp");


				System.gc();
				long totalMem = Runtime.getRuntime().totalMemory();
				long freeMem = Runtime.getRuntime().freeMemory();
				long usedMem = totalMem - freeMem;
				
				
				long start = System.currentTimeMillis();
				QuadTree<double[]> qtree = new QuadTree<double[]>(e.getMinX(),e.getMinY(),e.getMaxX(),e.getMaxY());
				for (double [] p : points) {
					qtree.put(p[0], p[1], p);
				}
				long stop = System.currentTimeMillis();
				System.gc();
				long totalMemQ = Runtime.getRuntime().totalMemory();
				long freeMemQ = Runtime.getRuntime().freeMemory();
				long usedMemQ = totalMemQ - freeMemQ;
				
				System.out.println("qtree build took:" + (stop-start) + " ms  qtree mem:" + (usedMemQ-usedMem)/1024/1024  + "MB");
				
				//		qtree = null;
				//		System.gc();
				
				System.gc();
				totalMem = Runtime.getRuntime().totalMemory();
				freeMem = Runtime.getRuntime().freeMemory();
				usedMem = totalMem - freeMem;
				

				start = System.currentTimeMillis();
				KDTree tree = new KDTree(points);
				stop = System.currentTimeMillis();
				
				System.gc();
				long totalMemKD = Runtime.getRuntime().totalMemory();
				long freeMemKD = Runtime.getRuntime().freeMemory();
				long usedMemKD = totalMemKD - freeMemKD;

				
				System.out.println("kdtree build took:" + (stop-start) + " ms  kdtree mem:" + (usedMemKD-usedMem)/1024/1024  + "MB");
				//		tree = null;

				System.out.println("+++++++++++");

				List<double []> qPoints = new ArrayList<double[]>();
				for (int i = 0; i < 1000000; i++) {
					double x = 4*MatsimRandom.getRandom().nextDouble();
					double y = 4*MatsimRandom.getRandom().nextDouble();
					double [] p = {x,y};
					qPoints.add(p);			
				}


				//		List<double []> resps = new ArrayList<double[]>(qPoints.size());
				start = System.currentTimeMillis();
				for (double [] p : qPoints) {
					double [] resp = qtree.get(p[0], p[1]);
					//			resps.add(resp);
				}
				stop = System.currentTimeMillis();
				System.out.println("qtree queries took:" + (stop-start) + " ms");

				//		Iterator<double[]> it = resps.iterator();
				start = System.currentTimeMillis();
				for (double [] p : qPoints) {
					double [] resp = tree.getNearestNeighbor(p[0], p[1]);
					//			double[] oldResp = it.next();
					//			if (resp != oldResp) {
					//				System.out.println("new:" + resp[0] + " " + resp[1]);
					//				System.out.println("old:" + oldResp[0] + " " + oldResp[1]);
					//			}
				}
				stop = System.currentTimeMillis();
				
				System.out.println("kdtree queries took:" + (stop-start) + " ms");		
				break;
		}
		//		double x = 1.25;
		//		double y = 2.95;
		//		double [] r = tree.getNearestNeighbor(x, y);
		//		
		//		Point point = geofac.createPoint(new Coordinate(x,y));
		//		Point point2 = geofac.createPoint(new Coordinate(r[0],r[1]));
		//		GisDebugger.addGeometry(point);
		//		GisDebugger.addGeometry(point2);
		//		GisDebugger.dump("/Users/laemmel/tmp/!kdQuery");
		//		System.out.println(r[0] + " " + r[1]);
		//		
	}

}
