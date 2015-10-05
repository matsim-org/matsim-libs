/* *********************************************************************** *
 * project: org.matsim.*
 * TransitionArea.java
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

package playground.gregor.sim2d_v4.simulation.physics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.QuadTree;

import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.cgal.LineSegment;
import playground.gregor.sim2d_v4.events.debug.LineEvent;
import playground.gregor.sim2d_v4.scenario.Section;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import be.humphreys.simplevoronoi.GraphEdge;
import be.humphreys.simplevoronoi.Voronoi;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

public class TransitionAreaII extends PhysicalSim2DSection  implements TransitionAreaI{



	private static final double EPSILON = 0.0001;
	//transition stuff
	private final int transitionBufferSize;
	private final List<Sim2DAgent> transitionBuffer = new ArrayList<Sim2DAgent>();

	private double minX = Double.POSITIVE_INFINITY;
	private double minY = Double.POSITIVE_INFINITY;
	private double maxX = Double.NEGATIVE_INFINITY;
	private double maxY = Double.NEGATIVE_INFINITY;
	//	private double x0;
	//	private double x1;
	//	private  double x2;
	//	private  double x3;
	//	private double y0;
	//	private  double y1;
	//	private   double y2;
	//	private  double y3;
	//	private final   double dx;
	//	private final   double dy;

	//	private final double sqrDMin = .38 * .38;

	private double currrentDMin;
	private final LineSegment[] bounds;
	//	private final double y4;
	//	private final double x4;

	//	//DEBUG
	//	private final double loop = 1;
	//	private final double msaDens = 0;
	//	private final List<String> res = new ArrayList<String>();
	//	private final double minSiteDist = 4;

	public TransitionAreaII(Section sec, Sim2DScenario sim2dsc, PhysicalSim2DEnvironment penv, int transitionBufferSize) {
		super(sec,sim2dsc,penv);
		Polygon p = sec.getPolygon();

		Coordinate[] coords = p.getExteriorRing().getCoordinates();
		if (coords.length != 5) {
			throw new RuntimeException("number of coords must be 5");
		}

		double x0 = coords[0].x;
		double x1 = coords[1].x;
		double x2 = coords[2].x;
		double x3 = coords[3].x;
		double y0 = coords[0].y;
		double y1 = coords[1].y;
		double y2 = coords[2].y;
		double y3 = coords[3].y;
		double dx = x3 - x0;
		double dy = y3 - y0;

		double l = Math.sqrt(dx*dx+dy*dy);
		dx /= l;
		dy /= l;
		double coeff = .25;
		double coeff2 = 2;
		x0 += dx*coeff - dy*coeff;
		y0 += dy*coeff + dx*coeff;
		x1 += dx*coeff + dy*coeff2;
		y1 += dy*coeff - dx*coeff2;
		x2 -= dx*coeff - dy*coeff2;
		y2 -= dy*coeff + dx*coeff2;
		x3 -= dx*coeff + dy*coeff;
		y3 -= dy*coeff - dx*coeff;

		LineSegment s0 = new LineSegment();
		s0.x0 = x0;
		s0.y0 = y0;
		s0.x1 = x1;
		s0.y1 = y1;

		LineSegment s1 = new LineSegment();
		s1.x0 = x1;
		s1.y0 = y1;
		s1.x1 = x2;
		s1.y1 = y2;

		LineSegment s2 = new LineSegment();
		s2.x0 = x2;
		s2.y0 = y2;
		s2.x1 = x3;
		s2.y1 = y3;

		LineSegment s3 = new LineSegment();
		s3.x0 = x3;
		s3.y0 = y3;
		s3.x1 = x0;
		s3.y1 = y0;

		this.bounds = new LineSegment [] {s0,s1,s2,s3};



		for (Coordinate c : p.getExteriorRing().getCoordinates()){
			if (c.x > this.maxX){
				this.maxX = c.x;
			}
			if (c.x< this.minX){
				this.minX = c.x;
			}
			if (c.y > this.maxY){
				this.maxY = c.y;
			}
			if (c.y< this.minY){
				this.minY = c.y;
			}
		}
		this.transitionBufferSize = transitionBufferSize;


//		//debug
//		for (LineSegment s : this.bounds) {
//			this.penv.getEventsManager().processEvent(new LineEvent(0, s, true,255,0,0,255,0));
//		}
	}

	@Override
	public boolean hasBufferSpace() {
		return this.transitionBuffer.size() < this.transitionBufferSize;
	}

	@Override
	public void addAgentTransitionBuffer(Sim2DAgent agent, double linkDensity) {
		this.transitionBuffer.add(agent);
		agent.setSec(this);
	}


	@Override
	public void prepare() {
		this.agentTwoDTree.clear();
		this.agents.addAll(this.inBuffer);
		this.inBuffer.clear();

		
		if (!Sim2DConfig.EXPERIMENTAL_VD_APPROACH) {
			this.agentTwoDTree.buildTwoDTree(this.agents);
		}
	}

	/* (non-Javadoc)
	 * @see playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DSection#updateAgents(double)
	 */
	@Override
	public void updateAgents(double time) {
		handleTransitionBuffer();
//		this.densityMap.buildDensityMap();
		Iterator<Sim2DAgent> it = this.agents.iterator();
		
		int idx = 0;
		while (it.hasNext()) {
			Sim2DAgent agent = it.next();
			
			//proof of concept! needs to be revised and implemented at a different location [GL August '13]
//			Cell cell = this.densityMap.getCell(idx);
			idx++;
			
			double area = 0;//cell.area;
//			for (Integer n : cell.neighbors) {
//				area += this.densityMap.getCell(n).area;
//			}
//			area /= (cell.neighbors.size() + 1);
			
			if (area < 10 && area > 0.1) {
				double rho = 1/area;//+0.1;
				double freeSpeed = Math.max(0.001, 1.34 * (1 - Math.exp(-1.913*(1/rho-1/5.4))));

				agent.setDesiredSpeed(freeSpeed);
			} else {
				agent.setDesiredSpeed(1.34);
			}
			updateAgent(agent, time);
		}
		
		debug();

	}



	private void debug() {
		if (this.agents.size() < 2) {
			return;
		}
		double [] x;
		double [] y;
		x = new double[this.agents.size()];
		y = new double[this.agents.size()];


		int i = 0;
		for (; i < this.agents.size(); i++) {
			x[i] = this.agents.get(i).getPos()[0];
			y[i] = this.agents.get(i).getPos()[1];
		}
		Voronoi v = new Voronoi(.001);//TODO

		LinkedList<GraphEdge> vd = (LinkedList<GraphEdge>) v.generateVoronoi(x, y, this.minX, this.maxX, this.minY, this.maxY);//TODO
		if (vd.size() == 0) { //no space left
			//				System.out.println("full");
			return;
		}
//		debug(vd);
		
	}

	private void handleTransitionBuffer() {
		Iterator<Sim2DAgent> it = this.transitionBuffer.iterator();
		//		debug(time);
		
		while (it.hasNext()) {
			this.currrentDMin = Double.POSITIVE_INFINITY;
			Sim2DAgent a = it.next();
			if (this.agents.size() < 2) {
				a.updateVelocity();
				this.agents.add(a);//HACK
				it.remove();
				continue;
			}
			// build Voronoi diagram
			double [] x;
			double [] y;
			x = new double[this.agents.size()];
			y = new double[this.agents.size()];


			int i = 0;
			for (; i < this.agents.size(); i++) {
				x[i] = this.agents.get(i).getPos()[0];
				y[i] = this.agents.get(i).getPos()[1];
			}
			Voronoi v = new Voronoi(.6);//TODO

			LinkedList<GraphEdge> vd = (LinkedList<GraphEdge>) v.generateVoronoi(x, y, this.minX, this.maxX, this.minY, this.maxY);//TODO
			if (vd.size() == 0) { //no space left
				//				System.out.println("full");
				return;
			}


//			debug(vd, time);

			QuadTree<VGraphVertex> qt = new QuadTree<VGraphVertex>(this.minX,this.minY,this.maxX,this.maxY);

			double sqrDistSum = 0;
			int cnt = 0;
			//			qt.put(this.minX, this.minY, new VGraphVertex());
			for ( GraphEdge ge : vd) {

				boolean inside1 = isInside(ge.x1, ge.y1); 
				boolean inside2 = isInside(ge.x2, ge.y2);
				if (inside1 && inside2) {
					sqrDistSum += handleLeaf(ge,ge.x1,ge.y1,qt);
					cnt++;
					sqrDistSum += handleLeaf(ge,ge.x2,ge.y2,qt);
					cnt++;
				} else if (inside1) {
					sqrDistSum += handleLeaf(ge,ge.x1,ge.y1,qt);
					cnt++;
					double dxu = ge.x2 - ge.x1;
					double dyu = ge.y2 - ge.y1;
					
					//debug
//					Segment dummy2 = new Segment();
//					dummy2.x0 = ge.x1;
//					dummy2.y0 = ge.y1;
//					dummy2.x1 = ge.x2;
//					dummy2.y1 = ge.y2;
//					this.penv.getEventsManager().processEvent(new LineEvent(0, dummy2, false,0,255,0,255,0));
					
					
					for (LineSegment s : this.bounds) {
						double dxv = s.x1 - s.x0;
						double dyv = s.y1 - s.y0;
						double dxw = ge.x1 - s.x0;
						double dyw = ge.y1 - s.y0;
						double d = CGAL.perpDot(dxu, dyu, dxv, dyv);
						double c = CGAL.perpDot(dxv, dyv, dxw, dyw)/d;
						if (c < 0 || c > 1) {
							continue;
						}
						double c1 = CGAL.perpDot(dxu, dyu, dxw, dyw)/d;
						if (c1 < 0 || c1 > 1) {
							continue;
						}
						
						double xx = ge.x1 + dxu * c;
						double yy = ge.y1 + dyu * c;
						sqrDistSum += handleLeaf(ge,xx,yy,qt);
						cnt++;
//						//debug
//						Segment dummy = new Segment();
//						dummy.x0 = ge.x1;
//						dummy.y0 = ge.y1;
//						dummy.x1 = xx;
//						dummy.y1 = yy;
//						this.penv.getEventsManager().processEvent(new LineEvent(0, dummy, false,255,0,0,255,0,0.05,0.05));
					}

				} else if (inside2) {
					sqrDistSum += handleLeaf(ge,ge.x2,ge.y2,qt);
					cnt++;
					double dxu = ge.x2 - ge.x1;
					double dyu = ge.y2 - ge.y1;
					
					//debug
//					Segment dummy2 = new Segment();
//					dummy2.x0 = ge.x1;
//					dummy2.y0 = ge.y1;
//					dummy2.x1 = ge.x2;
//					dummy2.y1 = ge.y2;
//					this.penv.getEventsManager().processEvent(new LineEvent(0, dummy2, false,0,255,0,255,0));
					
					
					for (LineSegment s : this.bounds) {
						double dxv = s.x1 - s.x0;
						double dyv = s.y1 - s.y0;
						double dxw = ge.x1 - s.x0;
						double dyw = ge.y1 - s.y0;
						double d = CGAL.perpDot(dxu, dyu, dxv, dyv);
						double c = CGAL.perpDot(dxv, dyv, dxw, dyw)/d;
						if (c < 0 || c > 1) {
							continue;
						}
						double c1 = CGAL.perpDot(dxu, dyu, dxw, dyw)/d;
						if (c1 < 0 || c1 > 1) {
							continue;
						}
						double xx = ge.x1 + dxu * c;
						double yy = ge.y1 + dyu * c;
						sqrDistSum += handleLeaf(ge,xx,yy,qt);
						cnt++;
//						//debug
//						Segment dummy = new Segment();
//						dummy.x0 = ge.x1;
//						dummy.y0 = ge.y1;
//						dummy.x1 = xx;
//						dummy.y1 = yy;
//						this.penv.getEventsManager().processEvent(new LineEvent(0, dummy, false,255,0,0,255,0,0.05,0.05));
					}
				} else {
					double dxu = ge.x2 - ge.x1;
					double dyu = ge.y2 - ge.y1;
					
					//debug
//					Segment dummy2 = new Segment();
//					dummy2.x0 = ge.x1;
//					dummy2.y0 = ge.y1;
//					dummy2.x1 = ge.x2;
//					dummy2.y1 = ge.y2;
//					this.penv.getEventsManager().processEvent(new LineEvent(0, dummy2, false,0,255,0,255,0));
					
					
					for (LineSegment s : this.bounds) {
						double dxv = s.x1 - s.x0;
						double dyv = s.y1 - s.y0;
						double dxw = ge.x1 - s.x0;
						double dyw = ge.y1 - s.y0;
						double d = CGAL.perpDot(dxu, dyu, dxv, dyv);
						double c0 = CGAL.perpDot(dxv, dyv, dxw, dyw)/d;
						if (c0 < 0 || c0 > 1) {
							continue;
						}
						double c1 = CGAL.perpDot(dxu, dyu, dxw, dyw)/d;
						if (c1 < 0 || c1 > 1) {
							continue;
						}
						double xx = ge.x1 + dxu * c0;
						double yy = ge.y1 + dyu * c0;
						sqrDistSum += handleLeaf(ge,xx,yy,qt);
						cnt++;
//						//debug
//						Segment dummy = new Segment();
//						dummy.x0 = ge.x1;
//						dummy.y0 = ge.y1;
//						dummy.x1 = xx;
//						dummy.y1 = yy;
//						this.penv.getEventsManager().processEvent(new LineEvent(0, dummy, false,255,0,0,255,0,0.05,0.05));
					}
				}
			}

			double rndNum = MatsimRandom.getRandom().nextDouble() * (sqrDistSum - cnt*this.currrentDMin);
			double selNum = 0.0;
			for (VGraphVertex vg : qt.values()) {
				selNum += (vg.sqrDist-this.currrentDMin);
				if (selNum >= rndNum) {
					a.getPos()[0] = vg.x;
					a.getPos()[1] = vg.y;
					//					if (!isInside(vg.x, vg.y)) {
					//						System.out.println("got you");
					//						continue;
					//					}
					double ds = 0;
					if (vg.site0 >= this.agents.size()) {
						ds += 1.34;
					} else {
						Sim2DAgent n0 = this.agents.get(vg.site0);
						ds += n0.getActualSpeed();
					}
					if (vg.site1 >= this.agents.size()) {
						ds += 1.34;
					} else {
						Sim2DAgent n0 = this.agents.get(vg.site1);
						ds += n0.getActualSpeed();
					}
					if (vg.site2 == -1 || vg.site2 >= this.agents.size()) {
						ds += 1.34;
					} else {
						Sim2DAgent n0 = this.agents.get(vg.site2);
						ds += n0.getActualSpeed();
					}
					ds /= 3;
//					a.setDesiredSpeed(ds);
					a.updateVelocity();
//					double v0 = a.getV0();
					double[] vv = a.getVelocity();
					double mag = Math.sqrt(vv[0]*vv[0]+vv[1]*vv[1]);
					vv[0] /= mag;
					vv[1] /= mag;
					vv[0] *= ds;
					vv[1] *=ds;
					
					
					this.agents.add(a);
					//					debug(vd, x, y, time);
					it.remove();
					break;
				}
			}
//						debug(vd);
			//			System.out.println();
		}

		


	}

	private boolean isInside(double x12, double y12) {
		for ( LineSegment s : this.bounds) {
			if (CGAL.isLeftOfLine(x12, y12, s.x0,s.y0,s.x1,s.y1) > 0) {
				return false;
			}	
		}
		return true;
	}


	private double handleLeaf(GraphEdge ge, double x,
			double y, QuadTree<VGraphVertex> qt) {
		VGraphVertex leaf1 = qt.getClosest(x, y);
		if (leaf1 == null || (leaf1.x-x)*(leaf1.x-x)+(leaf1.y-y)*(leaf1.y-y) > EPSILON) {
			leaf1 = new VGraphVertex();
			leaf1.site0=ge.site1;
			leaf1.site1=ge.site2;
			leaf1.x = x;
			leaf1.y = y;

			double dx;
			double dy;
			Sim2DAgent a1 = this.agents.get(ge.site1);
			dx = a1.getPos()[0] - leaf1.x;
			dy = a1.getPos()[1] - leaf1.y;

			double sqrD = dx*dx+dy*dy;
			leaf1.sqrDist = sqrD;
			if(sqrD < this.currrentDMin) {
				this.currrentDMin = sqrD;
			}
			qt.put(leaf1.x,leaf1.y,leaf1);
			return leaf1.sqrDist;	
		} else {
			if (leaf1.site0 == ge.site1 || leaf1.site1 == ge.site1) {
				leaf1.site2 = ge.site2;
			} else {
				leaf1.site2 = ge.site1;
			}
		}
		return 0;
	}

	private void debug(LinkedList<GraphEdge> vd) {
		if (vd == null) {
			return;
		}
		////		for (Sim2DAgent a : this.agents) {
		////			a.reDrawAgent(time);
		////		}
		////
		////		for (Segment o : getOpenings()) {
		////			PhysicalSim2DSection n = getNeighbor(o);
		////			for (Sim2DAgent a : n.agents) {
		////				a.reDrawAgent(time);
		////			} 
		////		}
		////		this.penv.getEventsManager().processEvent(new ForceReDrawEvent(time));
		////		Segment constr0 = new Segment();
		////		constr0.x0 = this.x1+this.dy/2;
		////		constr0.x1 = this.x2+this.dy/2;
		////		constr0.y0 = this.y1-this.dx/2;
		////		constr0.y1 = this.y2-this.dx/2;
		////		Segment constr1 = new Segment();
		////		constr1.x0 = this.x3-this.dy/2;
		////		constr1.x1 = this.x0-this.dy/2;
		////		constr1.y0 = this.y3+this.dx/2;
		////		constr1.y1 = this.y0+this.dx/2;
		//
		//		Voronoi v = new Voronoi(.0001);//TODO
		//		LinkedList<GraphEdge> vd2 = (LinkedList<GraphEdge>) v.generateVoronoi(x, y, this.minX+2, this.maxX-2, this.minY+2, this.maxY-2);//TODO
		//		this.penv.getEventsManager().processEvent(new RectEvent(0,this.minX+2,this.maxY-2,this.maxX-this.minX-4,this.maxY-this.minY-4,true));
		for ( GraphEdge ge : vd) {
			LineSegment s = new LineSegment();
			s.x0 = ge.x1;
			s.x1 = ge.x2;
			s.y0 = ge.y1;
			s.y1 = ge.y2;
			this.penv.getEventsManager().processEvent(new LineEvent(0, s, false,255,255,255,255,50,.05,.05));
		}

		//		this.penv.getEventsManager().processEvent(new RectEvent(0,this.x1,this.y3-5,5,5,true));
		//
		//		this.penv.getEventsManager().processEvent(new LineEvent(0, constr0, false,0,0,0,255,0));
		//		this.penv.getEventsManager().processEvent(new LineEvent(0, constr1, false,0,0,0,255,0));
		//		Voronoi v2 = new Voronoi(0.001f);
		//		LinkedList<GraphEdge> vd2 = (LinkedList<GraphEdge>) v2.generateVoronoi(x, y, this.minX, this.maxX, this.minY, this.maxY);//TODO
		//		for ( GraphEdge ge : vd2) {
		//			Segment s = new Segment();
		//			s.x0 = ge.x1;
		//			s.x1 = ge.x2;
		//			s.y0 = ge.y1;
		//			s.y1 = ge.y2;
		//			this.penv.getEventsManager().processEvent(new LineEvent(0, s, false,0,0,0,255,0,.05,.05));
		//		}

//		for (Sim2DAgent a : this.agents) {
//			a.reDrawAgent(time);
//		}
//
//		for (LineSegment o : getOpenings()) {
//			PhysicalSim2DSection n = getNeighbor(o);
//			if (n == null) {
//				continue;
//			}
//			for (Sim2DAgent a : n.agents) {
//				a.reDrawAgent(time);
//			} 
//		}

//		this.penv.getEventsManager().processEvent(new ForceReDrawEvent(time));
//		System.out.println("done.");

	}


	private static final class VGraphVertex {
		public double sqrDist;
		//		public double weight = 0;
		int site0;
		int site1;
		int site2 = -1;

		double x;
		double y;
	}
}
