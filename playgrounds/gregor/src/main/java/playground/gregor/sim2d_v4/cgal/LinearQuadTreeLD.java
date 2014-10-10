/* *********************************************************************** *
 * project: org.matsim.*
 * LinearQuadTreeLD.java
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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.gregor.sim2d_v4.events.debug.LineEvent;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Implementation of a linear quadtree for constant-time neighbors searches similar to
 * Aizawa, K. and Tanaka, S: "A Constant-Time Algorithm for Finding Neighbors in Quadtrees", IEEE
 * Transactions on Pattern Analysis and Machine Intelligence, Vol. 31, No. 7, 2009
 * @author laemmel
 *
 */
public class LinearQuadTreeLD {

	private static final Logger log = Logger.getLogger(LinearQuadTreeLD.class);

	private static final int MAX_DEPTH = 15; //for int MAX_DEPTH must be <= 15; for long <= 31
	public static final int EAST = 0x1;
	private static final int NORTH_EAST = 0x3;
	public static final int NORTH = 0x2;
	private static final int NORTH_WEST = 0x15555557;
	public static final int WEST = 0x15555555;
	private static final int SOUTH_WEST = 0x3fffffff;
	public static final int SOUTH = 0x2aaaaaaa;
	private static final int SOUTH_EAST = 0x2aaaaaab;

	private static final int TX = 0x15555555;
	private static final int TY = 0x2aaaaaaa;

	public static final int BLACK = 1;
	public static final int WHITE = 0;

	private static final int SW_QUADRANT = 0x0;
	private static final int SE_QUADRANT = 0x1;
	private static final int NW_QUADRANT = 0x2;
	private static final int NE_QUADRANT = 0x3;

	public static final int D_WEST = 0;
	public static final int D_SOUTH = 1;
	public static final int D_EAST = 2;
	public static final int D_NORTH = 3;

	private final Quad root;

	private final Map<Integer,Quad> quads = new HashMap<Integer,Quad>();
	private final EventsManager em;//DEBUG

	private static final double MIN_QUAD_SIZE = 2; 
	
	private final int [] occ = new int [MAX_DEPTH];
	private int quadsCnt = 0;
	
	//	public List<>
	public LinearQuadTreeLD(List<TwoDObject> obj, Envelope e, EventsManager em) {
//		addObjs(e.getMinX(),e.getMinY(),obj);
//		addObjs(e.getMaxX(),e.getMinY(),obj);
//		addObjs(e.getMaxX(),e.getMaxY(),obj);
//		addObjs(e.getMinX(),e.getMaxY(),obj);
//		for (double y = e.getMinY(); y < e.getMaxY(); y+=1) {
//			addObjs(e.getMaxX()-.1,y,obj);
//			addObjs(e.getMinX()+.1,y,obj);
//		}
		this.em = em;
		
		this.root = new Quad(obj,e,0x0,0,Integer.MIN_VALUE,Integer.MIN_VALUE,Integer.MIN_VALUE,Integer.MIN_VALUE);
		this.quads.put(0, this.root);
		
		Queue<Quad> qq = new LinkedList<Quad>();
		qq.add(this.root);
		while (qq.peek() != null) {
			Quad quad = qq.poll();
			if (quad.getColor() <= 1 || quad.getLevel() == MAX_DEPTH){ //leaf (== BLACK or WHITE)
				this.quadsCnt++;
				this.occ[quad.getLevel()]++;
				continue;
			}
			
			int key = computeKey(quad.getLocationCode(), quad.getLevel());
			//			this.quads.remove(key);
			incrementNeighborsLevelDifferences(quad);

			split(quad,qq);

		}


//		//DEBUG
		for (Quad q : this.quads.values()) {
			q.debug();
		}
		double entropy = 0;
		for (int i = 0; i < MAX_DEPTH; i++) {
			if (this.occ[i] == 0) {
				continue;
			}
			double pi = (double)this.occ[i]/this.quadsCnt;
			
			entropy -= this.occ[i]*pi*Math.log(pi)/Math.log(2);
			
		}
		System.out.println(entropy/this.quadsCnt);
		
	}

	private void addObjs(double minX, double minY, List<TwoDObject> obj) {
		DummyObj o = new DummyObj();
		o.x = minX;
		o.y = minY;
		obj.add(o);
	}

	public Collection<Quad> getQuads() {
		return this.quads.values();
	}
	
	public List<Quad> query(Envelope e) {
		List<Quad> ret = new ArrayList<Quad>();
		ret.addAll(this.root.query(e));
		return ret;
	}

	private void split(Quad quad, Queue<Quad> qq) {
		Envelope e = quad.getEnvelope();
		Envelope eSE = new Envelope(e.getMaxX()-e.getWidth()/2,e.getMaxX(),e.getMinY()+e.getHeight()/2,e.getMinY());
		Envelope eNE = new Envelope(e.getMaxX()-e.getWidth()/2,e.getMaxX(),e.getMaxY()-e.getHeight()/2,e.getMaxY());
		Envelope eNW = new Envelope(e.getMinX()+e.getWidth()/2,e.getMinX(),e.getMaxY()-e.getHeight()/2,e.getMaxY());
		Envelope eSW = new Envelope(e.getMinX()+e.getWidth()/2,e.getMinX(),e.getMinY()+e.getHeight()/2,e.getMinY());
		List<TwoDObject> oSE = new ArrayList<TwoDObject>();
		List<TwoDObject> oNE = new ArrayList<TwoDObject>();
		List<TwoDObject> oNW = new ArrayList<TwoDObject>();
		List<TwoDObject> oSW = new ArrayList<TwoDObject>();
		for (TwoDObject o : quad.getObjects()) {
			if (eSE.contains(o.getX(), o.getY())){
				oSE.add(o);
			} else if (eNE.contains(o.getX(), o.getY())){
				oNE.add(o);
			} else if (eNW.contains(o.getX(), o.getY())){
				oNW.add(o);
			} else if (eSW.contains(o.getX(), o.getY())){
				oSW.add(o);
			} else {
				//				log.warn("Object: " + o + " seems to be out of boundary of quad:" + quad);
			}
		}
		int lcSE = quad.getLocationCode() | (SE_QUADRANT << (2*(MAX_DEPTH - (quad.getLevel()+1))));
		int lcNE = quad.getLocationCode() | (NE_QUADRANT << (2*(MAX_DEPTH - (quad.getLevel()+1))));
		int lcNW = quad.getLocationCode() | (NW_QUADRANT << (2*(MAX_DEPTH - (quad.getLevel()+1))));
		int lcSW = quad.getLocationCode() | (SW_QUADRANT << (2*(MAX_DEPTH - (quad.getLevel()+1))));
		int pDW = quad.getDWest();
		if (pDW != Integer.MIN_VALUE) {
			pDW--;
		}
		int pDE = quad.getDEast();
		if (pDE != Integer.MIN_VALUE) {
			pDE--;
		}
		int pDN = quad.getDNorth();
		if (pDN != Integer.MIN_VALUE) {
			pDN--;
		}
		int pDS = quad.getDSouth();
		if (pDS != Integer.MIN_VALUE) {
			pDS--;
		}
		Quad childSE = new Quad(oSE,eSE,lcSE,quad.getLevel()+1,pDE,0,0,pDS);
		Quad childNE = new Quad(oNE,eNE,lcNE,quad.getLevel()+1,pDE,pDN,0,0);
		Quad childNW = new Quad(oNW,eNW,lcNW,quad.getLevel()+1,0,pDN,pDW,0);
		Quad childSW = new Quad(oSW,eSW,lcSW,quad.getLevel()+1,0,0,pDW,pDS);
		quad.setChildren(childSE,childNE,childNW,childSW);

		if (childSE.getDSouth() != Integer.MIN_VALUE){
			incrementNeighorsLevelDifference(childSE, SOUTH, D_NORTH);
		}if (childSE.getDEast() != Integer.MIN_VALUE){
			incrementNeighorsLevelDifference(childSE, EAST, D_WEST);
		}

		if (childNE.getDNorth() != Integer.MIN_VALUE){
			incrementNeighorsLevelDifference(childNE, NORTH, D_SOUTH);
		}if (childNE.getDEast() != Integer.MIN_VALUE){
			incrementNeighorsLevelDifference(childNE, EAST, D_WEST);
		}

		if (childNW.getDNorth() != Integer.MIN_VALUE){
			incrementNeighorsLevelDifference(childNW, NORTH, D_SOUTH);
		}if (childNW.getDWest() != Integer.MIN_VALUE){
			incrementNeighorsLevelDifference(childNW, WEST, D_EAST);
		}

		if (childSW.getDSouth() != Integer.MIN_VALUE){
			incrementNeighorsLevelDifference(childSW, SOUTH, D_WEST);
		}if (childSW.getDWest() != Integer.MIN_VALUE){
			incrementNeighorsLevelDifference(childSW, WEST, D_EAST);
		}

		this.quads.put(computeKey(childSW.getLocationCode(),quad.getLevel()+1), childSW);
		this.quads.put(computeKey(childSE.getLocationCode(),quad.getLevel()+1), childSE);
		this.quads.put(computeKey(childNW.getLocationCode(),quad.getLevel()+1), childNW);
		this.quads.put(computeKey(childNE.getLocationCode(),quad.getLevel()+1), childNE);

		qq.add(childNE);
		qq.add(childNW);
		qq.add(childSE);
		qq.add(childSW);
		//		System.out.println(childSW);
		//		System.out.println(childSE);
		//		System.out.println(childNE);
		//		System.out.println(childNW);

	}


	private void incrementNeighborsLevelDifferences(Quad quad) {
		if (quad.getDEast() != Integer.MIN_VALUE){
			incrementNeighorsLevelDifference(quad,EAST,D_WEST);
		}
		if (quad.getDSouth() != Integer.MIN_VALUE){
			incrementNeighorsLevelDifference(quad,SOUTH,D_NORTH);
		}
		if (quad.getDWest() != Integer.MIN_VALUE){
			incrementNeighorsLevelDifference(quad,WEST,D_EAST);
		}
		if (quad.getDNorth() != Integer.MIN_VALUE){
			incrementNeighorsLevelDifference(quad,NORTH,D_SOUTH);
		}

	}


	private void incrementNeighorsLevelDifference(Quad quad, int direction,int oppDirection) {
		int eLc = quadLocationAdd(quad.getLocationCode(), direction<<(2*(MAX_DEPTH-quad.getLevel())));
		Quad eQ = this.quads.get(computeKey(eLc,quad.getLevel()));
		if (eQ != null && eQ.getLevel() == quad.getLevel()) {
			eQ.incrementD(oppDirection);
		}

	}

	private int quadLocationAdd(int nQ, int dNQ) {
		return (((nQ|TY)+(dNQ&TX))&TX)|(((nQ|TX)+(dNQ&TY))&TY);
	}

	private int computeKey(int loc, int level) {
		//pairing function from
		//Stephen Wolfram, A new kind of science, Wolfram Media, 2002.
		return loc < level ? level*level + loc : loc*loc + loc + level;
	}

	public final class Quad {
		private final int locationCode;
		private final int level;

		//TODO array?
		private int dEast;
		private int dNorth;
		private int dWest;
		private int dSouth;

		private final Envelope e;
		private final List<TwoDObject> objs;
		private Quad se = null;
		private Quad ne = null;
		private Quad nw = null;
		private Quad sw = null;
		private boolean isLeaf = true;
		public Quad(List<TwoDObject> objs, Envelope e, int locationCode, int level, int dEast, int dNorth, int dWest, int dSouth) {
//			if (level > MAX_DEPTH) {
////				throw new RuntimeException("max depth exceeded!");
////				this.objs.addAll(objs);
//				
//			}
			this.e = e;
			if (e.getArea() == 0) {
				throw new RuntimeException("Error: empty envelope!");
			}
			this.objs = objs;
			this.locationCode = locationCode;
			this.level = level;
			this.dEast = dEast;
			this.dNorth = dNorth;
			this.dWest = dWest;
			this.dSouth = dSouth;
		}
		public List<Quad> query(Envelope q) {
			List<Quad> ret = new ArrayList<Quad>();
			if (!q.intersects(this.e)) {
				return ret;
			}
			if (this.isLeaf) {
				ret.add(this);
			} else if (q.intersects(this.e)) {
				ret.addAll(this.ne.query(q));
				ret.addAll(this.nw.query(q));
				ret.addAll(this.sw.query(q));
				ret.addAll(this.se.query(q));
			}
			return ret;
		}
		public void debug() {
			if (LinearQuadTreeLD.this.em == null){
				System.out.println(this);
				return;
			}
			LineSegment s0 = new LineSegment();
			s0.x0 = this.e.getMinX();
			s0.y0 = this.e.getMinY();
			s0.x1 = this.e.getMaxX();
			s0.y1 = this.e.getMinY();
			LineSegment s1 = new LineSegment();
			s1.x0 = this.e.getMaxX();
			s1.y0 = this.e.getMinY();
			s1.x1 = this.e.getMaxX();
			s1.y1 = this.e.getMaxY();
			LineSegment s2 = new LineSegment();
			s2.x0 = this.e.getMaxX();
			s2.y0 = this.e.getMaxY();
			s2.x1 = this.e.getMinX();
			s2.y1 = this.e.getMaxY();
			LineSegment s3 = new LineSegment();
			s3.x0 = this.e.getMinX();
			s3.y0 = this.e.getMaxY();
			s3.x1 = this.e.getMinX();
			s3.y1 = this.e.getMinY();
			LinearQuadTreeLD.this.em.processEvent(new LineEvent(0, s0, false, 128,128,128,64,0));
			LinearQuadTreeLD.this.em.processEvent(new LineEvent(0, s1, false, 128,128,128,64,0));
			LinearQuadTreeLD.this.em.processEvent(new LineEvent(0, s2, false, 128,128,128,64,0));
			LinearQuadTreeLD.this.em.processEvent(new LineEvent(0, s3, false, 128,128,128,64,0));

			//neighbors
//			Quad west = getNeighbor(D_WEST);
//			if (west != null && west.getColor() < 2) {
//				LineSegment link = new LineSegment();
//				link.x0 = this.e.getMinX()+this.e.getWidth()/2;
//				link.y0 = this.e.getMinY()+this.e.getHeight()/2;
//				link.x1 = west.e.getMinX()+west.e.getWidth()/2;
//				link.y1 = west.e.getMinY()+west.e.getHeight()/2;
//				LinearQuadTreeLD.this.em.processEvent(new LineEvent(0, link, false, 128,0,0,255,0));
//			}
//			Quad south = getNeighbor(D_SOUTH);
//			if (south != null && south.getColor() < 2) {
//				LineSegment link = new LineSegment();
//				link.x0 = this.e.getMinX()+this.e.getWidth()/2;
//				link.y0 = this.e.getMinY()+this.e.getHeight()/2;
//				link.x1 = south.e.getMinX()+south.e.getWidth()/2;
//				link.y1 = south.e.getMinY()+south.e.getHeight()/2;
//				LinearQuadTreeLD.this.em.processEvent(new LineEvent(0, link, false, 0,128,0,255,0));
//			}
//			Quad east = getNeighbor(D_EAST);
//			if (east != null && east.getColor() < 2) {
//				LineSegment link = new LineSegment();
//				link.x0 = this.e.getMinX()+this.e.getWidth()/2;
//				link.y0 = this.e.getMinY()+this.e.getHeight()/2;
//				link.x1 = east.e.getMinX()+east.e.getWidth()/2;
//				link.y1 = east.e.getMinY()+east.e.getHeight()/2;
//				LinearQuadTreeLD.this.em.processEvent(new LineEvent(0, link, false, 0,0,128,255,0));
//			}
//			Quad north = getNeighbor(D_NORTH);
//			if (north != null && north.getColor() < 2) {
//				LineSegment link = new LineSegment();
//				link.x0 = this.e.getMinX()+this.e.getWidth()/2;
//				link.y0 = this.e.getMinY()+this.e.getHeight()/2;
//				link.x1 = north.e.getMinX()+north.e.getWidth()/2;
//				link.y1 = north.e.getMinY()+north.e.getHeight()/2;
//				LinearQuadTreeLD.this.em.processEvent(new LineEvent(0, link, false, 0,64,64,255,0));
//			}

		}

		public Quad getNeighbor(int direction) {
			int dd = Integer.MIN_VALUE;
			int dNq = 0;
			if (direction == LinearQuadTreeLD.D_WEST) {
				dd = this.dWest;
				dNq = LinearQuadTreeLD.WEST;
			}else if (direction == LinearQuadTreeLD.D_SOUTH) {
				dd = this.dSouth;
				dNq = LinearQuadTreeLD.SOUTH;
			}else if (direction == LinearQuadTreeLD.D_EAST) {
				dd = this.dEast;
				dNq = LinearQuadTreeLD.EAST;
			}else if (direction == LinearQuadTreeLD.D_NORTH) {
				dd = this.dNorth;
				dNq = LinearQuadTreeLD.NORTH;
			}
			if (dd == Integer.MIN_VALUE) {
				return null;
			}
			int nq = this.locationCode;

			if (dd < 0) {
				int shift = 2*(MAX_DEPTH-this.level-dd);
				int shiftedNq = (nq >> shift) << shift;
				int shiftedDNq = dNq << shift;
				int mq = LinearQuadTreeLD.this.quadLocationAdd(shiftedNq, shiftedDNq);
				return LinearQuadTreeLD.this.quads.get(computeKey(mq,this.level+dd));
			} else {
				int shift = 2*(MAX_DEPTH-this.level);
				int shiftedDNq = dNq << shift;
				int mq = LinearQuadTreeLD.this.quadLocationAdd(nq, shiftedDNq);
				return LinearQuadTreeLD.this.quads.get(computeKey(mq, this.level));
			}


		}

		public void incrementD(int side) {
			if (side == D_WEST) {
				this.dWest++;
			} else if (side == D_SOUTH) {
				this.dSouth++;
			} else if (side == D_EAST) {
				this.dEast++;
			} else if (side == D_NORTH) {
				this.dNorth++;
			} else {
				throw new RuntimeException("invalide side: "+ side);
			}
		}
		public void setChildren(Quad childSE, Quad childNE, Quad childNW,
				Quad childSW) {
			this.isLeaf = false;
			this.se = childSE;
			this.ne = childNE;
			this.nw = childNW;
			this.sw = childSW;

		}
		
		public Quad getNEChild() {
			return this.ne;
		}
		public Quad getSEChild() {
			return this.se;
		}
		public Quad getNWChild() {
			return this.nw;
		}
		public Quad getSWChild() {
			return this.sw;
		}
		
		public int getDWest() {
			return this.dWest;
		}
		public int getDSouth() {
			return this.dSouth;
		}
		public int getDEast() {
			return this.dEast;
		}
		public int getDNorth() {
			return this.dNorth;
		}
		public List<? extends TwoDObject> getObjects() {
			return this.objs;
		}
		public Envelope getEnvelope() {
			return this.e;
		}
		public int getLevel() {
			return this.level;
		}
		public int getColor() {
			return this.objs.size();
		}
		public int getLocationCode() {
			return this.locationCode;
		}

		@Override
		public String toString() {
			String dE = this.dEast +"";
			if (this.dEast == Integer.MIN_VALUE){
				dE = "#";	
			}
			String dN = this.dNorth +"";
			if (this.dNorth == Integer.MIN_VALUE){
				dN = "#";	
			}
			String dW = this.dWest +"";
			if (this.dWest == Integer.MIN_VALUE){
				dW = "#";	
			}
			String dS = this.dSouth +"";
			if (this.dSouth == Integer.MIN_VALUE){
				dS = "#";	
			}
			String color = this.objs.size() == 0 ? "WHITE" : this.objs.size() == 1 ? "BLACK" : "GRAY";

			return "("+Integer.toHexString(this.locationCode)+","+this.level+"," + color + ","+dE+","+dN+","+dW+","+dS+") " + this.e;

		}

	}

	public static void main(String [] args) {

		List<int[]> pairs = new ArrayList<int[]>();
		pairs.add(new int[]{1,0});//East
		pairs.add(new int[]{1,1});//North-East
		pairs.add(new int[]{0,1});//North
		pairs.add(new int[]{-1,1});//North-West
		pairs.add(new int[]{-1,0});//West
		pairs.add(new int[]{-1,-1});//South-West
		pairs.add(new int[]{0,-1});//South
		pairs.add(new int[]{1,-1});//South-East


		System.out.println("INCREMENTS");
		int r = MAX_DEPTH;
		for (int[] pair : pairs) {
			int x = pair[0];
			int y = pair[1];
			StringBuffer buf = new StringBuffer();
			for (int rr = r; rr > 0; rr--) {
				if (y == -1) {
					buf.append("1");
				} else if (rr > 1) {
					buf.append("0");
				} else {
					buf.append(y+"");
				}
				if (x == -1) {
					buf.append("1");
				} else if (rr > 1) {
					buf.append("0");
				} else {
					buf.append(x+"");
				}
			}
			String str = buf.toString();
			long i = Long.parseLong(str, 2);
			System.out.println(Long.toHexString(i));
		}

		System.out.println();
		System.out.println("tx");
		StringBuffer bufTx = new StringBuffer();
		for (int i = 0; i < MAX_DEPTH; i++) {
			bufTx.append("01");
		}
		String strTx = bufTx.toString();
		long tx = Long.parseLong(strTx,2);
		System.out.println(Long.toHexString(tx));

		System.out.println();
		System.out.println("ty");
		StringBuffer bufTy = new StringBuffer();
		for (int i = 0; i < MAX_DEPTH; i++) {
			bufTy.append("10");
		}
		String strTy = bufTy.toString();
		long ty = Long.parseLong(strTy,2);
		System.out.println(Long.toHexString(ty));
	}


	private static final class DummyObj implements TwoDObject {
		double x;
		double y;
		
		@Override
		public double getX() {
			return this.x;
		}

		@Override
		public double getY() {
			return this.y;
		}
		
	}

}
