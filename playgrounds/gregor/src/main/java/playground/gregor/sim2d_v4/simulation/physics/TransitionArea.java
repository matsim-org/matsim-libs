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
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.Tuple;

import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.scenario.Section;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import be.humphreys.simplevoronoi.GraphEdge;
import be.humphreys.simplevoronoi.Voronoi;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

public class TransitionArea extends PhysicalSim2DSection {


	//experimental
	private static TreeMap<Double, Double> densSiteDistMapping = new TreeMap<Double,Double>();
	static {
		densSiteDistMapping.put(0.19104999999999994,4.0);
		densSiteDistMapping.put(0.21184999999999976,3.6);
		densSiteDistMapping.put(0.26750000000000007,3.24);
		densSiteDistMapping.put(0.3321499999999997,2.9160000000000004);
		densSiteDistMapping.put(0.3980500000000004,2.6244000000000005);
		densSiteDistMapping.put(0.4811500000000005,2.3619600000000007);
		densSiteDistMapping.put(0.6136499999999958,2.1257640000000007);
		densSiteDistMapping.put(0.7493999999999987,1.9131876000000005);
		densSiteDistMapping.put(0.9349499999999996,1.7218688400000006);
		densSiteDistMapping.put(1.1291999999999947,1.5496819560000006);
		densSiteDistMapping.put(1.3175999999999966,1.3947137604000006);
		densSiteDistMapping.put(1.5383500000000019,1.2552423843600007);
		densSiteDistMapping.put(1.82015,1.1297181459240007);
		densSiteDistMapping.put(2.1683000000000003,1.0167463313316005);
		densSiteDistMapping.put(2.6483500000000095,0.9150716981984405);
		densSiteDistMapping.put(3.2106999999999997,0.8235645283785964);
		densSiteDistMapping.put(3.74675,0.7412080755407368);
		densSiteDistMapping.put(4.578200000000003,0.6670872679866631);
		densSiteDistMapping.put(6.1715000000000035,0.6003785411879968);
	}
	

	//transition stuff
	private final int transitionBufferSize;
	private final List<Tuple<Sim2DAgent,Double>> transitionBuffer = new ArrayList<Tuple<Sim2DAgent,Double>>();

	private double minX = Double.POSITIVE_INFINITY;
	private double minY = Double.POSITIVE_INFINITY;
	private double maxX = Double.NEGATIVE_INFINITY;
	private double maxY = Double.NEGATIVE_INFINITY;
	private final double x0;
	private final double x1;
	private final double x2;
	private final double x3;
	private final double y0;
	private final double y1;
	private final double y2;
	private final double y3;
	private final double dx;
	private final double dy;


//	//DEBUG
//	private double loop = 1;
//	private double msaDens = 0;
//	private final List<String> res = new ArrayList<String>();
//	private double minSiteDist = 4;
	
	public TransitionArea(Section sec, Sim2DScenario sim2dsc, PhysicalSim2DEnvironment penv, int transitionBufferSize) {
		super(sec,sim2dsc,penv);
		Polygon p = sec.getPolygon();
		
		Coordinate[] coords = p.getExteriorRing().getCoordinates();
		if (coords.length != 5) {
			throw new RuntimeException("number of coords must be 5");
		}
		
		this.x0 = coords[0].x;
		this.x1 = coords[1].x;
		this.x2 = coords[2].x;
		this.x3 = coords[3].x;
		this.y0 = coords[0].y;
		this.y1 = coords[1].y;
		this.y2 = coords[2].y;
		this.y3 = coords[3].y;
		double dx = this.x3 - this.x0;
		double dy = this.y3 - this.y0;
		double l = Math.sqrt(dx*dx+dy*dy);
		dx /= l;
		dy /= l;
		this.dx = dx;
		this.dy = dy;
		
		
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

	}

	public boolean hasBufferSpace() {
		return this.transitionBuffer.size() < this.transitionBufferSize;
	}

	public void addAgentTransitionBuffer(Sim2DAgent agent, double linkDensity) {
		if (this.transitionBuffer.size() >= this.transitionBufferSize){
			throw new RuntimeException("Buffer size exceeded max size");
		}
		
		Entry<Double, Double> ceil = densSiteDistMapping.ceilingEntry(linkDensity);
		double vDist;
		if (ceil != null) {
			vDist = ceil.getValue();
		} else  {
			Entry<Double, Double> floor = densSiteDistMapping.floorEntry(linkDensity);
			vDist = floor.getValue();
		}
		
		this.transitionBuffer.add(new Tuple<Sim2DAgent,Double>(agent,vDist));
		agent.setPSec(this);
	}



	/* (non-Javadoc)
	 * @see playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DSection#updateAgents(double)
	 */
	@Override
	public void updateAgents(double time) {
		this.agentTwoDTree.clear();
		this.agents.addAll(this.inBuffer);
		this.inBuffer.clear();

		handleTransitionBuffer(time);
////		System.out.println("dens: " + this.agents.size()/16.);
//		int cnt =0;
//		for (Sim2DAgent a : this.agents) {
//			double[] pos = a.getPos();
//			if (CGAL.isLeftOfLine(pos[0], pos[1], this.x3-this.dy, this.y3+this.dx, this.x0-this.dy, this.y0+this.dx) > 0) {
//				cnt++;
//			}
//			
//		}
//		this.msaDens = this.loop/(this.loop+1) *this.msaDens + 1/(this.loop+1)*(cnt/1.5);
//		this.loop++;
////		System.out.println("dens: " + cnt/4. + " msaA:" + this.msaDens);
//		if (this.loop == 2500) {
//			this.res.add(new String( this.msaDens + "\t" +this.minSiteDist));
//			this.msaDens = 0;
//			this.loop = 1;
//			this.minSiteDist *= .9;
//			
//			for (String s : this.res) {
//				System.out.println(s);
//			}
//		}


		this.agentTwoDTree.buildTwoDTree(this.agents);
		Iterator<Sim2DAgent> it = this.agents.iterator();
		while (it.hasNext()) {
			Sim2DAgent agent = it.next();
			updateAgent(agent, time);
		}
	}



	private void handleTransitionBuffer(double time) {
		Iterator<Tuple<Sim2DAgent, Double>> it = this.transitionBuffer.iterator();
		int count = 0;
		while (it.hasNext()) {
			Tuple<Sim2DAgent, Double> tup = it.next();
			Sim2DAgent a = tup.getFirst();
			if (this.agents.size() < 1) {
				this.agents.add(a);//HACK
				it.remove();
				continue;
			}
			// build Voronoi diagram
			
			double [] x = new double[this.agents.size()+4];
			double [] y = new double[this.agents.size()+4];
			int i = 0;
			for (; i < this.agents.size(); i++) {
				x[i] = this.agents.get(i).getPos()[0];
				y[i] = this.agents.get(i).getPos()[1];
			}
			x[i] = this.x0;
			y[i++] = this.y0;
			x[i] = this.x1;
			y[i++] = this.y1;
			x[i] = this.x2;
			y[i++] = this.y2;
			x[i] = this.x3;
			y[i++] = this.y3;

			Voronoi v = new Voronoi(tup.getSecond());
			LinkedList<GraphEdge> vd = (LinkedList<GraphEdge>) v.generateVoronoi(x, y, this.minX, this.maxX, this.minY, this.maxY);//TODO

			if (vd.size() == 0) { //no space left
//				System.out.println("full");
				return;
			}

//			//DEBUG
//			for ( GraphEdge ge : vd) {
//				Segment s = new Segment();
//				s.x0 = ge.x1;
//				s.x1 = ge.x2;
//				s.y0 = ge.y1;
//				s.y1 = ge.y2;
//				
//				int val = count % (3*255);
//				int r,g,b;
//				if (val <= 255) {
//					r=val;
//					g=0;
//					b=255-r;
//				} else if (val <= (2*255)) {
//					b=val-255;
//					g=255-b;
//					r=0;
//				} else {
//					g=val;
//					r=255-g;
//					b=0;
//				}
//				
//				this.penv.getEventsManager().processEvent(new LineEvent(0, s, false,r,g,b,255,0));
//				count += 32;
//			}
			
//			vd.get(i)
//			a.updateVelocity(time);
			ArrayList<double[]> cands = new ArrayList<double[]>();
			for ( GraphEdge ge : vd) {
				boolean gotOne = false;
				if (isInside(ge.x1,ge.y1)){
					cands.add(new double[]{ge.x1,ge.y1});
					gotOne = true;
				}
				if (isInside(ge.x2,ge.y2)){
					cands.add(new double[]{ge.x2,ge.y2});
					gotOne = true;
				}
				if (!gotOne) {
					double candX = (ge.x1+ge.x2)/2;
					double candY = (ge.y1+ge.y2)/2;
					if (isInside(candX,candY)){
						cands.add(new double[]{candX,candY});	
					}
				}
			}
			
			if (cands.size() == 0) {
//				System.out.println("full");
				return;
			}
			int rnd = MatsimRandom.getRandom().nextInt(cands.size());
			double[] p = a.getPos();
			p[0] = cands.get(rnd)[0];
			p[1] = cands.get(rnd)[1];
			
			
			this.agents.add(a);
			it.remove();
		
		}


	}

	private boolean isInside(double x12, double y12) {
		
		double coeff = .19;
		if (CGAL.isLeftOfLine(x12, y12, this.x0+this.dx*coeff, this.y0+this.dy*coeff, this.x1+this.dx*coeff, this.y1+this.dy*coeff) > 0) {
			return false;
		}
		if (CGAL.isLeftOfLine(x12, y12, this.x1+this.dy, this.y1-this.dx, this.x2+this.dy, this.y2-this.dx) > 0) {
			return false;
		}
		if (CGAL.isLeftOfLine(x12, y12, this.x2-+this.dx*coeff, this.y2-this.dy*coeff, this.x3-this.dx*coeff, this.y3-this.dy*coeff) > 0) {
			return false;
		}
		if (CGAL.isLeftOfLine(x12, y12, this.x3-this.dy, this.y3+this.dx, this.x0-this.dy, this.y0+this.dx) > 0) {
			return false;
		}
		return true;
	}


}
