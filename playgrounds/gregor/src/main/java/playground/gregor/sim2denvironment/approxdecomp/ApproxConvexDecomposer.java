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

package playground.gregor.sim2denvironment.approxdecomp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;

import playground.gregor.sim2denvironment.Algorithms;
import playground.gregor.sim2denvironment.GisDebugger;
import playground.gregor.sim2denvironment.approxdecomp.Graph.Node;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Approximate convex decomposition algorithm as proposed by:
 * Lien, J.-M. & Amato, N. Approximate convex decomposition of polygons Computational Geometry, 2006, 35, 100-123
 * This is an extended version that allows Steiner points insertion 
 * @author laemmel
 *
 */
public class ApproxConvexDecomposer {

	private static final Logger log = Logger.getLogger(ApproxConvexDecomposer.class);

	private static final double S_D = 0.1;
	private static final double S_C = .1;
	private static final double TAU =.5;
	private static final boolean ALLOW_STEINER_VERTICES = true;
	private static final double epsilon = 0.0001;

	private final GeometryFactory geofac = new GeometryFactory();

	private final List<LineString> initOpeningsIntersections;

	private final MedialAxisApproximator maa = new MedialAxisApproximator();
	private final ShortestPath sp = new ShortestPath();


	//HACK
	private final Map<LinearRing,List<Opening>> holeOpenings = new HashMap<LinearRing,List<Opening>>();

	public ApproxConvexDecomposer(List<LineString> openings) {
		this.initOpeningsIntersections = openings;
	}

	public ApproxConvexDecomposer() {
		this(null);
	}

	public List<PolygonInfo> decompose(Geometry geo) {
		if (geo instanceof Polygon) {
			log.info("decomposing polygon consisting of:" + ((Polygon)geo).getExteriorRing().getCoordinates().length + " vertices");
			PolygonInfo pi = new PolygonInfo();
			pi.p = (Polygon) geo;
			return decomposePolygon(pi);
		} else if (geo instanceof MultiPolygon) {
			return deomposeMultiPolygon((MultiPolygon)geo);
		} else {
			throw new RuntimeException("Algorithm is not applicable to geometries of type" + geo.getGeometryType() + "!");
		}
	}

	private void createIntialOpenings(PolygonInfo pi) {

		Coordinate[] coords = pi.p.getExteriorRing().getCoordinates();
		for (int i = 0; i < coords.length-1; i++) {
			Coordinate c0 = coords[i];
			Coordinate c1 = coords[i+1];
			for (LineString ls : this.initOpeningsIntersections) {
				if (intersects(ls,c0,c1)) {
					Opening op = new Opening();
					op.edge = i;
					pi.openings.add(op);
					break;
				}
			}
		}
	}

	private boolean intersects(LineString ls, Coordinate c0, Coordinate c1) {
		for (int i = 0; i < ls.getCoordinates().length-1; i++) {
			Coordinate d0 = ls.getCoordinates()[i];
			Coordinate d1 = ls.getCoordinates()[i+1];
			double l0 = Algorithms.isLeftOfLine(c0, d0, d1);
			double l1 = Algorithms.isLeftOfLine(c1, d0, d1);
			if (l0 * l1 < 0) {
				double m0 = Algorithms.isLeftOfLine(d0, c0, c1);
				double m1 = Algorithms.isLeftOfLine(d1, c0, c1);
				if (m0 * m1 < 0) {
					return true;
				}
			}
		}

		return false;
	}

	private List<PolygonInfo> deomposeMultiPolygon(MultiPolygon geo) {
		List<PolygonInfo> ret = new ArrayList<PolygonInfo>();
		for (int i = 0; i < geo.getNumGeometries(); i++) {
			Polygon p = (Polygon) geo.getGeometryN(i);
			ret.addAll(decompose(p));
		}


		return ret;
	}

	private List<PolygonInfo> decomposePolygon(PolygonInfo geo) {
		if (this.initOpeningsIntersections != null) {
			createIntialOpenings(geo);
		}


		List<PolygonInfo> ret = new ArrayList<PolygonInfo>();
		Queue<PolygonInfo> open = new ConcurrentLinkedQueue<PolygonInfo>();
		//		PolygonInfo ppi = new PolygonInfo();
		//		ppi.p = geo;
		open.add(geo);

		while (open.size() > 0) {
			PolygonInfo pi = open.poll();

			double[] c = computeShellConcavity(pi.p); 
			if (pi.p.getNumInteriorRing() > 0) {
				PolygonInfo resolved = resolveDeepestHole(pi,c);
				open.add(resolved);
				continue;
			}

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
				ret.add(pi); //convex enough
			} else if (mostConcaveVertex == 0) {
				rotateRing(pi);
				open.add(pi);
			} else {		
				//				mostConcaveVertex += c.length-1; //since in Java "x % y" for x < 0 is not the same as "x mod y", we add this offset here to x so that later on we can calculate "(x-1) % y" safely

				Collection<PolygonInfo> resolved = splitPolygon(pi,mostConcaveVertex, c);
				if (resolved.size() > 0) {
					open.addAll(resolved);
				} else {
					ret.add(pi);//could not split or add Steiner points
				}

			}
		}


		return ret;
	}



	private PolygonInfo resolveDeepestHole(PolygonInfo pi, double[] c) {

		Tuple<CoordinateInfo,CoordinateInfo> resolve = getResolvePair(pi);
		if (resolve.getFirst().cIdx == 0 || resolve.getSecond().cIdx == 0) {
			if (resolve.getFirst().cIdx == 0) {
				rotate(pi,resolve.getFirst());
			}
			if (resolve.getSecond().cIdx == 0) {
				rotate(pi,resolve.getSecond());
			}
		}

		LinearRing lr2 = (LinearRing) pi.p.getInteriorRingN(resolve.getFirst().pIdx);
		LinearRing lr1 = null;
		List<Opening> openings01;
		LinearRing shell = null;LinearRing ring = null;
		LinearRing [] holes = new LinearRing[pi.p.getNumInteriorRing()-1];int holesIdx = 0;
		if (resolve.getSecond().pIdx == -1) {
			lr1 = (LinearRing) pi.p.getExteriorRing();
			openings01 = pi.openings;
			shell = merge(lr1,lr2,resolve.getSecond().cIdx,resolve.getFirst().cIdx);
		} else {
			lr1 = (LinearRing) pi.p.getInteriorRingN(resolve.getSecond().pIdx);
			openings01 = this.holeOpenings.remove(lr1);
			shell = (LinearRing) pi.p.getExteriorRing();
			ring = merge(lr1,lr2,resolve.getSecond().cIdx,resolve.getFirst().cIdx);
			holes[holesIdx++] = ring;
		}
		List<Opening> openings02 = this.holeOpenings.remove(lr2);
		
		if (openings01 == null) {
			openings01 = new ArrayList<Opening>();
		}
		if (openings02 == null) {
			openings02 = new ArrayList<Opening>();
		}
		
		
		
		List<Opening> newOpenings = new ArrayList<Opening>();
		for (Opening o : openings01) {
			Opening oo = new Opening();
			if (o.edge < resolve.getSecond().cIdx) {
				oo.edge = o.edge;
			} else {
				oo.edge = o.edge + 2 + lr2.getCoordinates().length-1;
			}
			newOpenings.add(oo);
		}
		Opening o1 = new Opening();
		o1.edge = resolve.getSecond().cIdx;
		newOpenings.add(o1);
		Opening o2 = new Opening();
		o2.edge = resolve.getSecond().cIdx+lr2.getCoordinates().length;
		newOpenings.add(o2);
		
		for (Opening o : openings02) {
			Opening oo = new Opening();
			int edge = o.edge;
			edge -= resolve.getFirst().cIdx;
			if (edge < 0) {
				edge += lr2.getCoordinates().length-1;
			}
			edge += resolve.getSecond().cIdx;
			edge++;
			oo.edge = edge;
			newOpenings.add(oo);	
		}
			
		
		



		for (int j = 0; j < pi.p.getNumInteriorRing(); j++) {
			if (j == resolve.getFirst().pIdx || j == resolve.getSecond().pIdx) {
				continue;
			}
			holes[holesIdx++] = (LinearRing) pi.p.getInteriorRingN(j);
		}


		Polygon newp = this.geofac.createPolygon(shell, holes);
		PolygonInfo ret = new PolygonInfo();
		if (resolve.getSecond().pIdx == -1) {
			ret.openings = newOpenings;
		} else {
			ret.openings = pi.openings;
			this.holeOpenings.put(ring, newOpenings);
		}
		ret.p = newp;


		return ret;
	}

	private void rotate(PolygonInfo pi, CoordinateInfo ci) {
		
		LineString ls = null;
		if (ci.pIdx == -1) {
			ls = pi.p.getExteriorRing();
			//rotate openings
			for (Opening o : pi.openings) {
				o.edge--;
				if (o.edge == -1) {
					o.edge = ls.getCoordinates().length-2;
				}
			}
		} else {
			ls = pi.p.getInteriorRingN(ci.pIdx);
		}
		Coordinate[] coords = ls.getCoordinates();
		for (int i = 0; i < coords.length-1; i++) {
			coords[i] = coords[i+1];
		}
		coords[coords.length-1] = coords[0];
		
	
		
	}

	private LinearRing merge(LinearRing lr1, LinearRing lr2, int idx1, int idx2) {
		Coordinate[] c1 = lr1.getCoordinates();
		Coordinate[] c2 = lr2.getCoordinates();
		Coordinate [] coords = new Coordinate[c1.length+c2.length+1];
		//		boolean isCCW1 = CGAlgorithms.isCCW(c1);
		//		boolean isCCW2 = CGAlgorithms.isCCW(c2);
		int idx = 0;
		for (int i = 0; i < c1.length; i++) {
			if (i <= idx1 || idx > (idx1+c2.length)) {
				coords[idx++] = c1[i];

			} else {
				i--; i--;
				//				if (isCCW1 == isCCW2) {
				while (idx1+c2.length >= idx) {
					int mod = idx2 % c2.length;
					if (mod != 0) {
						coords[idx++] = c2[mod];
					}
					idx2++;
				}
				//				} else {
				//					idx2 += c2.length;
				//					while (idx1+c2.length >= idx) {
				//						int mod = idx2 % c2.length;
				//						if (mod != 0) {
				//							coords[idx++] = c2[mod];
				//						}
				//						idx2--;
				//					}
				//				}
			}
		}

		LinearRing lr = this.geofac.createLinearRing(coords);

		return lr;
	}

	private Tuple<CoordinateInfo, CoordinateInfo> getResolvePair(PolygonInfo pi) {
		QuadTree<CoordinateInfo> quadTree = buildQuadTree(pi.p);
		LinkedList<AntipodalPair> open = new LinkedList<AntipodalPair>();
		for (int i = 0; i < pi.p.getNumInteriorRing(); i++) {
			LinearRing ring = (LinearRing) pi.p.getInteriorRingN(i);
			Polygon pring = this.geofac.createPolygon(ring, null);
			Node n = this.maa.run(pring);
			Tuple<Node, Double> pInfo = this.sp.getFarestNode(n);
			Tuple<Node, Double> cwpInfo = this.sp.getFarestNode(pInfo.getFirst());
			AntipodalPair ap = new AntipodalPair();
			ap.dist = cwpInfo.getSecond();
			ap.pIdx = i;
			double distP = Double.POSITIVE_INFINITY;
			double distCWP = Double.POSITIVE_INFINITY;
			for (int j = 0; j < ring.getCoordinates().length; j ++) {
				Coordinate coord = ring.getCoordinateN(j);
				double dist = coord.distance(pInfo.getFirst().c);
				if (dist < distP) {
					ap.pi = j;
					distP = dist; 
				}
				dist = coord.distance(cwpInfo.getFirst().c); 
				if (dist < distCWP) {
					ap.cwpi = j;
					distCWP = dist;
				}
			}
			open.add(ap);
		}

		Map<Integer, Double> concavity = new HashMap<Integer,Double>();
		double mostConcave = 0;
		Tuple<CoordinateInfo,CoordinateInfo> resolve = null;
		boolean reverse =false;
		while (!open.isEmpty()) {
			Iterator<AntipodalPair> it = open.iterator();
			while (it.hasNext()) {
				AntipodalPair element = it.next();
				Coordinate pc = getCoord(element.pIdx, element.pi, pi.p);
				Coordinate cwpc = getCoord(element.pIdx, element.cwpi, pi.p);

				CoordinateInfo pci = getNearestNoneIntersectingCoordinate(element.pi,element.pIdx,quadTree,pi.p, element.dist);
				Coordinate pcic = getCoord(pci.pIdx, pci.cIdx, pi.p);

				CoordinateInfo cwpci = getNearestNoneIntersectingCoordinate(element.cwpi,element.pIdx,quadTree,pi.p, element.dist);
				Coordinate cwpcic = getCoord(cwpci.pIdx, cwpci.cIdx, pi.p);
				double cwdist = cwpcic.distance(cwpc);
				double dist = pc.distance(pcic);
				if (cwdist < dist && !reverse || cwdist > dist && reverse) {
					double conc  = 0;
					if (cwpci.pIdx == -1) {
						conc = cwdist + element.dist;
						concavity.put(element.pIdx, conc);
						it.remove();
					} else {
						Double oConc = concavity.get(cwpci.pIdx);
						if (oConc != null) {
							conc = oConc + cwdist + element.dist;
							concavity.put(element.pIdx, conc);
							it.remove();
						}
					}

					if (conc > mostConcave) {
						mostConcave = conc;
						CoordinateInfo tmp = new CoordinateInfo();
						tmp.pIdx = element.pIdx;
						tmp.cIdx = element.cwpi;
						resolve = new Tuple<CoordinateInfo,CoordinateInfo>(tmp,cwpci);
					}
				} else {
					double conc  = 0;
					if (pci.pIdx == -1) {
						conc = dist + element.dist;
						concavity.put(element.pIdx, conc);
						it.remove();
					} else {
						Double oConc = concavity.get(pci.pIdx);
						if (oConc != null) {
							conc = oConc + dist + element.dist;
							concavity.put(element.pIdx, conc);
							it.remove();
						}
					}

					if (conc > mostConcave) {
						mostConcave = conc;
						CoordinateInfo tmp = new CoordinateInfo();
						tmp.pIdx = element.pIdx;
						tmp.cIdx = element.pi;
						resolve = new Tuple<CoordinateInfo,CoordinateInfo>(tmp,pci);
					}				
				}

			}
			if (resolve == null) { //could not resolve any hole to the shell in the first iteration
				for (AntipodalPair ap : open) {
					concavity.put(ap.pIdx, 0.);
				}
			}
//			reverse = !reverse;

		}
		return resolve;
	}

	private CoordinateInfo getNearestNoneIntersectingCoordinate(int pi,
			int pIdx, QuadTree<CoordinateInfo> quadTree, Polygon p, double range) {

		Coordinate c = getCoord(pIdx,pi,p); 


		boolean found = false;
		CoordinateInfo ret = null;
		while (!found) {
			Collection<CoordinateInfo> coll = quadTree.getDisk(c.x, c.y, range);
			double minDist = Double.POSITIVE_INFINITY;
			for (CoordinateInfo ci : coll) {
				if (ci.pIdx != pIdx) {
					Coordinate tmp = getCoord(ci.pIdx,ci.cIdx,p);

					if (intersects(p, c, tmp)) {
						continue;
					}
					double dist = tmp.distance(c);
					if (dist < minDist) {
						minDist = dist;
						ret = ci;
						found = true;
					}
				}
			}
			range *= 2.;
		}


		return ret;
	}

	private boolean intersects(Polygon p, Coordinate c, Coordinate tmp) {

		if (intersects(p.getExteriorRing(),c,tmp)) {
			return true;
		}
		for (int i = 0; i < p.getNumInteriorRing(); i++) {
			if (intersects(p.getInteriorRingN(i),c,tmp)) {
				return true;
			}
		}

		return false;
	}

	private Coordinate getCoord(int pIdx, int pi, Polygon p) {
		if (pIdx == -1) {
			return p.getExteriorRing().getCoordinateN(pi);
		} else {
			return p.getInteriorRingN(pIdx).getCoordinateN(pi);
		}
	}

	private QuadTree<CoordinateInfo> buildQuadTree(Polygon p) {
		Geometry bounds = p.getBoundary();
		Envelope e = new Envelope();
		for (Coordinate cc : bounds.getCoordinates()) {
			e.expandToInclude(cc);
		}
		QuadTree<CoordinateInfo> ret = new QuadTree<CoordinateInfo>(e.getMinX(), e.getMinY(),e.getMaxX(),e.getMaxY());
		Coordinate[] coords = p.getExteriorRing().getCoordinates();
		for (int i = 0; i < coords.length; i++) {
			Coordinate cc = coords[i];
			CoordinateInfo ci = new CoordinateInfo();
			ci.cIdx = i;
			ci.pIdx = -1;
			ret.put(cc.x, cc.y, ci);
		}

		for (int i = 0; i < p.getNumInteriorRing(); i++) {
			coords = p.getInteriorRingN(i).getCoordinates();
			for (int j = 0; j < coords.length; j++) {
				Coordinate cc = coords[j];
				CoordinateInfo ci = new CoordinateInfo();
				ci.cIdx = j;
				ci.pIdx = i;
				ret.put(cc.x, cc.y, ci);
			}
		}

		return ret;
	}

	private void rotateRing(PolygonInfo pi) {
		Coordinate[] ring = pi.p.getExteriorRing().getCoordinates();
		Coordinate [] coords = new Coordinate[ring.length];
		int pos = 0;
		coords[pos++] = ring[ring.length-2];
		for (int j = 0; j < ring.length-1; j++) {
			coords[pos++] = ring[j];
		}
		LinearRing lr = this.geofac.createLinearRing(coords);
		Polygon p = this.geofac.createPolygon(lr, null);
		pi.p = p;

		//revise openings
		for (Opening o : pi.openings) {
			if (o.edge != 0){
				o.edge--;
			}else {
				o.edge = coords.length-2;
			}
		}

	}

	private Collection<PolygonInfo> splitPolygon(PolygonInfo pi, int vIdx, double[] c) {

		Collection<PolygonInfo> ret = new ArrayList<PolygonInfo>();

		Coordinate [] ring = pi.p.getExteriorRing().getCoordinates();
		Coordinate split = ring[vIdx%(ring.length-1)];

		Coordinate pred = ring[(vIdx-1)%(ring.length-1)];
		Coordinate succ = ring[(vIdx+1)%(ring.length-1)];


		double bestScore = Double.NEGATIVE_INFINITY;
		boolean splitPointFound = false;
		int wIdx = 0;

		double oldLeftPred = 0;
		double oldLeftSucc = 0;

		int steiner2Idx = -1;


		for (int i = 0; i < pi.p.getExteriorRing().getCoordinates().length; i++) {
			Coordinate test = ring[i];

			if (i >= vIdx -1 && i <= vIdx + 1) {
				continue;
			}

			double leftPred = Algorithms.isLeftOfLine(test, pred, split);
			double leftSucc = Algorithms.isLeftOfLine(test, split,succ);




			if (leftPred < 0 && leftSucc < 0 && notIntersecting(split,test,ring)) {
				splitPointFound = true;
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

		if (splitPointFound) {
			splitPolygonAt(pi,vIdx,wIdx,ret);
		} else if (ALLOW_STEINER_VERTICES){
			int steiner1Idx;
			if (steiner2Idx == 0) {
				steiner2Idx = ring.length-1;
				steiner1Idx = ring.length-2;
			} else {
				steiner1Idx = steiner2Idx-1;
			}
			insertSteinerPointAndReturnPolygon(pi,vIdx,steiner1Idx,steiner2Idx,ret);
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

	private void insertSteinerPointAndReturnPolygon(PolygonInfo pi,
			int vIdx, int steiner1Idx, int steiner2Idx, Collection<PolygonInfo> ret) {

		Coordinate [] ring = pi.p.getExteriorRing().getCoordinates();
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
		pi.p = p;

		//revise openings
		Opening steinerOpening = null;
		for (Opening open : pi.openings) {
			if (open.edge == steiner1Idx) {
				steinerOpening = new Opening();
				steinerOpening.edge = open.edge+1;
			} else if (open.edge > steiner1Idx){
				open.edge++;
			}
		}
		if (steinerOpening != null) {
			pi.openings.add(steinerOpening);
		}

		ret.add(pi);
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

	private void splitPolygonAt(PolygonInfo pi, int vIdx, int wIdx,
			Collection<PolygonInfo> ret) {

		Coordinate [] ring = pi.p.getExteriorRing().getCoordinates();
		List<Coordinate> left = new ArrayList<Coordinate>();
		List<Coordinate> right = new ArrayList<Coordinate>();

		if (vIdx > wIdx) {
			int tmp = vIdx;
			vIdx = wIdx;
			wIdx = tmp;
		}

		for (int i = 0; i <= vIdx; i++) {
			left.add(ring[i]);
		}
		for (int i = wIdx; i < ring.length; i++) {
			left.add(ring[i]);
		}

		for (int i = vIdx; i <= wIdx; i++) {
			right.add(ring[i]);
		}
		right.add(right.get(0));



		Coordinate[] lCoords =  left.toArray(new Coordinate[0]);
		Coordinate[] rCoords = right.toArray(new Coordinate[0]);

		LinearRing llr = this.geofac.createLinearRing(lCoords);
		LinearRing rlr = this.geofac.createLinearRing(rCoords);

		Polygon lp = this.geofac.createPolygon(llr, null);
		Polygon rp = this.geofac.createPolygon(rlr, null);

		PolygonInfo lpi = new PolygonInfo();
		PolygonInfo rpi = new PolygonInfo();
		lpi.p = lp;
		rpi.p = rp;

		//add new Openings
		Opening ol = new Opening();
		ol.edge = vIdx;
		lpi.openings.add(ol);
		Opening or = new Opening();
		or.edge = wIdx - vIdx;
		rpi.openings.add(or);

		//revise old openings
		for (Opening open : pi.openings) {
			if (open.edge < vIdx) {
				lpi.openings.add(open);
			}else if (open.edge < wIdx) {
				open.edge -= vIdx;
				rpi.openings.add(open);
			} else {
				open.edge += -(wIdx - vIdx) + 1;
				lpi.openings.add(open);

			}
		}

		ret.add(lpi);
		ret.add(rpi);
	}


	private double[] computeShellConcavity(Polygon p) {

		LineString ring = p.getExteriorRing();
		Coordinate[] shell = ring.getCoordinates();		
		double [] ret = new double[shell.length];
		boolean ccw = CGAlgorithms.isCCW(shell);
		if (ccw) {
			GisDebugger.addGeometry(p);
			GisDebugger.dump("/Users/laemmel/devel/burgdorf2d2/tmp/dump.shp");
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

	/*package*/ static final class PolygonInfo {
		Polygon p;
		List<Opening> openings = new ArrayList<Opening>();
	}

	/*package*/ static final class Opening {
		int edge;
	}

	private static final class AntipodalPair {
		int pi = -1;
		int cwpi = -1; 
		double dist;
		int pIdx;
	}

	private static final class CoordinateInfo {
		int cIdx;
		int pIdx;
	}

}
