/* *********************************************************************** *
 * project: org.matsim.*
 * ApproxConvexDecomposer.java
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

package playground.gregor.approxdecomp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

import playground.gregor.approxdecomp.DecompGuiDebugger.GuiDebugger;
import playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.velocityobstacle.Algorithms;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Approximate convex decomposition algorithm as proposed by:
 * Lien, J.-M. & Amato, N. Approximate convex decomposition of polygons Computational Geometry, 2006, 35, 100-123
 * This is am extended version that allows Steiner points insertion 
 * @author laemmel
 *
 */
public class ApproxConvexDecomposer {
	
	private static final Logger log = Logger.getLogger(ApproxConvexDecomposer.class);

	private static final double S_D = 0.1;
	private static final double S_C = .1;
	private static final double TAU = 2;
	private static final boolean ALLOW_STEINER_VERTICES = false;
	private static final double epsilon = 0.0001;

	private final GeometryFactory geofac = new GeometryFactory();
	
	public List<Polygon> decompose(Geometry geo) {
		if (geo instanceof Polygon) {
			return decomposePolygon((Polygon)geo);
		} else if (geo instanceof MultiPolygon) {
			return deomposeMultiPolygon((MultiPolygon)geo);
		} else {
			throw new RuntimeException("Algorithm is not applicable to geometries of type" + geo.getGeometryType() + "!");
		}
	}

	private List<Polygon> deomposeMultiPolygon(MultiPolygon geo) {
		List<Polygon> ret = new ArrayList<Polygon>();
		for (int i = 0; i < geo.getNumGeometries(); i++) {
			Polygon p = (Polygon) geo.getGeometryN(i);
			ret.addAll(decomposePolygon(p));
		}
		return ret;
	}

	private List<Polygon> decomposePolygon(Polygon geo) {


		List<Polygon> ret = new ArrayList<Polygon>();
		Queue<Polygon> open = new ConcurrentLinkedQueue<Polygon>();
		open.add(geo);

		while (open.size() > 0) {
			Polygon p = open.poll();
			System.out.println("ps:" + open.size() + " vertices current:"+p.getExteriorRing().getNumPoints());
			GuiDebugger.addObject(p);
			
			//TODO holes
			
			double[] c = computeShellConcavity(p); 

			double maxConcave = 0;
			int mostConcaveVertex = 0;

			for (int i = 0; i < c.length; i++) {
					double val = c[i];
					if (val > maxConcave) {
						maxConcave = val;
						mostConcaveVertex = i;
					}
				}
			if (maxConcave <= TAU) {
				ret.add(p); //convex enough
			} else {		
				mostConcaveVertex += c.length-1; //since in Java "x % y" for x < 0 is not the same as "x mod y", we add this offset here to x so that later on we can calculate "(x-1) % y" safely  
				Collection<Polygon> resolved = splitPolygon(p,mostConcaveVertex, c);
				if (resolved.size() > 0) {
					open.addAll(resolved);
				} else {
					ret.add(p);//could not split or add Steiner points
				}
					
			}
		}


		return ret;
	}



	private Collection<Polygon> splitPolygon(Polygon p, int vIdx, double[] c) {
		
		Collection<Polygon> ret = new ArrayList<Polygon>();
		
		Coordinate [] ring = p.getExteriorRing().getCoordinates();
		Coordinate split = ring[vIdx%(ring.length-1)];
		
		Coordinate pred = ring[(vIdx-1)%(ring.length-1)];
		Coordinate succ = ring[(vIdx+1)%(ring.length-1)];
		
		
		double bestScore = -1;
		int wIdx = 0;
		
		double oldLeftPred = 0;
		double oldLeftSucc = 0;
		
		int steiner2Idx = 0;
		
		for (int i = 0; i < p.getExteriorRing().getCoordinates().length-1; i++) {
			if (i >= vIdx -1 && i <= vIdx + 1) {
				continue;
			}
			Coordinate test = ring[i];
			double leftPred = Algorithms.isLeftOfLine(test, pred, split);
			double leftSucc = Algorithms.isLeftOfLine(test, split,succ);
			
			
			
			
			if (leftPred < 0 && leftSucc < 0 && notIntersecting(split,test,ring)) {
				double score = (1+S_C*c[i])/(S_D*split.distance(test));
				if (score > bestScore) {
					bestScore = score;
					wIdx = i;
				}
			} else {
				if (oldLeftPred > 0 && oldLeftSucc < 0 && leftPred < 0 && leftSucc > 0) {
					steiner2Idx = i;
				}
				oldLeftPred = leftPred;
				oldLeftSucc = leftSucc;	
			}
		}
		
		if (bestScore > -1) {
			splitPolygonAt(ring,vIdx,wIdx,ret);
		} else if (ALLOW_STEINER_VERTICES){
			insertSteinerPointAndReturnPolygon(ring,vIdx,(steiner2Idx-1)%(ring.length-1),steiner2Idx,ret);
		} else {
			log.warn("Can not split here and Steiner points are not allowed so leaving current polygon as it is!");
		}
		
		
		return ret;
		
		
	}

	private boolean notIntersecting(Coordinate c0, Coordinate c1,
			Coordinate[] ring) {
		for (int i = 1  ; i < ring.length; i++) {
			Coordinate t0 = ring[i-1];
			Coordinate t1 = ring[i];
			
			double t0LeftOfLinec0c1 = Algorithms.isLeftOfLine(t0, c0, c1);
			double t1LeftOfLinec0c1 = Algorithms.isLeftOfLine(t1, c0, c1);
			if (t0LeftOfLinec0c1 * t1LeftOfLinec0c1 < 0) {
				double c0LeftOfLinet0t1 = Algorithms.isLeftOfLine(c0, t0, t1);
				double c1LeftOfLinet0t1 = Algorithms.isLeftOfLine(c1, t0, t1);
				if (c0LeftOfLinet0t1 * c1LeftOfLinet0t1 < 0) {
					return false;
				}
			}
			

			
		}
		
		return true;
	}

	private void insertSteinerPointAndReturnPolygon(Coordinate[] ring,
			int vIdx, int steiner1Idx, int steiner2Idx, Collection<Polygon> ret) {
		Coordinate steiner = computeSteinerCoordinate(ring,vIdx,steiner1Idx,steiner2Idx);
		
		
		Coordinate [] coords = new Coordinate[ring.length+1];
		
		int j = 0;
		for (int i = 0; i < ring.length; i ++) {
			coords[j] = ring[i];
			if (i == steiner1Idx) {
				coords[++j] = steiner;
			}
			j++;
		}
		LinearRing lr = this.geofac.createLinearRing(coords);
		Polygon p = this.geofac.createPolygon(lr, null);
		ret.add(p);
	}

	private Coordinate computeSteinerCoordinate(Coordinate[] ring, int vIdx,
			int steiner1Idx, int steiner2Idx) {
		
		Coordinate pred = ring[(vIdx-1)%(ring.length-1)];
		Coordinate v = ring[vIdx%(ring.length-1)];
		Coordinate succ = ring[(vIdx+1)%(ring.length-1)];
		Coordinate s1 = ring[steiner1Idx];
		Coordinate s2 = ring[steiner2Idx];
		
		
		Coordinate c1 = new Coordinate(); 
		computeLineIntersection(pred, v, s1, s2, c1);
		
		
		Coordinate c2 = new Coordinate();
		computeLineIntersection(v,succ, s1, s2, c2);

		return new Coordinate((c1.x+c2.x)/2,(c1.y+c2.y)/2);
	}

	private void splitPolygonAt(Coordinate[] ring, int vIdx, int wIdx,
			Collection<Polygon> ret) {
		List<Coordinate> left = new ArrayList<Coordinate>();
		List<Coordinate> right = new ArrayList<Coordinate>();
		int idx = vIdx%(ring.length-1);
		do {
			left.add(ring[idx%(ring.length-1)]);
			idx++;
		}while((idx%(ring.length-1)) != (wIdx+1)%(ring.length-1));
		idx = wIdx;
		do {
			right.add(ring[idx%(ring.length-1)]);
			idx++;
		} while ((idx%(ring.length-1)) != (vIdx+1)%(ring.length-1));
		left.add(left.get(0));
		right.add(right.get(0));
		Coordinate[] lCoords =  left.toArray(new Coordinate[0]);
		Coordinate[] rCoords = right.toArray(new Coordinate[0]);
		
		LinearRing llr = this.geofac.createLinearRing(lCoords);
		LinearRing rlr = this.geofac.createLinearRing(rCoords);
		
		Polygon lp = this.geofac.createPolygon(llr, null);
		Polygon rp = this.geofac.createPolygon(rlr, null);

		ret.add(lp);
		ret.add(rp);
	}
	
	
	private double[] computeShellConcavity(Polygon p) {

		LineString ring = p.getExteriorRing();
		Coordinate[] shell = ring.getCoordinates();		
		double [] ret = new double[shell.length];
		boolean ccw = CGAlgorithms.isCCW(shell);
		if (ccw) {
			log.warn("wrong orientation. leaving polygon as it is!");
			return ret;
		}
		shell = Arrays.copyOf(shell, shell.length-1);
		
		LineString cHullRing = ((Polygon) ring.convexHull()).getExteriorRing();
		
		Coordinate[] cHull = cHullRing.getCoordinates();
		cHull = Arrays.copyOf(cHull, cHull.length-1);

		if (cHull.length != shell.length) { // polygon is not convex
			List<PocketBridge> pocketBridges = computePocketBridges(shell,cHull);
			for (PocketBridge pb : pocketBridges) {
				computePocketBridgeConcavity(pb,shell,ret);
			}
		}	
		return ret;
	}


	private void computePocketBridgeConcavity(PocketBridge pb,
			Coordinate[] shell, double [] concavity) {

		SPConcavity spConcavity = new SPConcavity();
		spConcavity.computeSPConcavity(shell, pb,concavity);
	}

	private List<PocketBridge> computePocketBridges(Coordinate[] shell,
			Coordinate[] cHull) {
		List<PocketBridge> ret = new ArrayList<PocketBridge>();


		int i = 0; //cHull idx;
		int j = 0; //shell idx;
		for (;shell[j] != cHull[i]; j++);

		while (i < cHull.length) {
			int betaMinus = j;
			int betaPlus = (j+1);
			int nextCHullPos = i + 1;
			Coordinate c = cHull[nextCHullPos%cHull.length];
			while (shell[betaPlus%shell.length] != c) {
				betaPlus++;
			}
			if (betaPlus-betaMinus > 1) {
				PocketBridge pb = new PocketBridge();
				pb.betaMinus = betaMinus;
				pb.betaPlus = betaPlus;
				ret.add(pb);
			}
			j = betaPlus;
			i++;
		}


		return ret;
	}

	
	private boolean computeLineIntersection(Coordinate a0, Coordinate a1, Coordinate b0, Coordinate b1, Coordinate intersectionCoordinate) {

		double a = (b1.x - b0.x) * (a0.y - b0.y) - (b1.y - b0.y) * (a0.x - b0.x);
		double b = (a1.x - a0.x) * (a0.y - b0.y) - (a1.y - a0.y) * (a0.x - b0.x);
		double denom = (b1.y - b0.y) * (a1.x - a0.x) - (b1.x - b0.x) * (a1.y - a0.y);

		//coincident
		if (Math.abs(a) < epsilon && Math.abs(b) < epsilon && Math.abs(denom) < epsilon) {
			intersectionCoordinate.x = (a0.x+a1.x) /2;
			intersectionCoordinate.y = (a0.y+a1.y) /2;
			return true;
		}

		//parallel
		if (Math.abs(denom) < epsilon) {
			return false;
		}

		double ua = a / denom;

		double x = a0.x + ua * (a1.x - a0.x);
		double y = a0.y + ua * (a1.y - a0.y);
		intersectionCoordinate.x = x;
		intersectionCoordinate.y = y;

		return true;
	}
	

	/*package*/ static final class PocketBridge {
		int betaMinus;
		int betaPlus;
	}

}
