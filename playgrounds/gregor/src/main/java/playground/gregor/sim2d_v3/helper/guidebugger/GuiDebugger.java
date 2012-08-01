/* *********************************************************************** *
 * project: org.matsim.*
 * GuiDebugger.java
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

package playground.gregor.sim2d_v3.helper.guidebugger;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.matsim.core.gbl.MatsimRandom;

import playground.gregor.sim2d_v3.simulation.floor.Agent2D;
import playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.ORCALine;
import processing.core.PApplet;

public class GuiDebugger extends PApplet{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static ConcurrentLinkedQueue<ORCALine> orcas = new ConcurrentLinkedQueue<ORCALine>();
	private static ConcurrentLinkedQueue<Agent2D> agents = new ConcurrentLinkedQueue<Agent2D>();
	private static ConcurrentLinkedQueue<Agent2D> ignoredAgents = new ConcurrentLinkedQueue<Agent2D>();
	private static ConcurrentLinkedQueue<float[]> env = new ConcurrentLinkedQueue<float[]>();
	private static ConcurrentLinkedQueue<float[]> vecs = new ConcurrentLinkedQueue<float[]>();
	private static ConcurrentLinkedQueue<float[]> circs = new ConcurrentLinkedQueue<float[]>();
	
	public static boolean dump = false;

	public static boolean peek = false;
	
	private static float refY = 0;

	private static float refX = 0;

	private static final int W = 1000;

	private float scale = 500;

	@Override
	public void setup() {
		addMouseWheelListener(new java.awt.event.MouseWheelListener() { 
			@Override
			public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) { 
				mouseWheel(evt.getWheelRotation());
			}}); 
		size(W,W);
		background(0);
	}

	@Override
	public void draw() {
		stroke(255);
		if (dump){
			dump();
			dump = false;
		} else if (peek) {
			peek();
			vecs.clear();
			peek = false;
		}

	}

	void mouseWheel(int delta)
	{
		this.scale-=10*delta;


	}	
	
	private void peek() {
		background(0);
		 
		fill(255);
		Iterator<float[]> it = env.iterator();
		stroke(32,128,255,255);
		while (it.hasNext()){
			float[] seg = it.next();
			float x1 = offsetFloat(scaleFloat(seg[0]));
			float x2 = offsetFloat(scaleFloat(seg[2]));
			float y1 = offsetFloat(-scaleFloat(seg[1]));
			float y2 = offsetFloat(-scaleFloat(seg[3]));
			super.line(x1, y1, x2, y2);
			
		}
		stroke(255);
		
		Iterator<Agent2D> it2 = ignoredAgents.iterator();
		while (it2.hasNext()) {
			fill(200,200,200,200);
			Agent2D a = it2.next();
			agent(a);
		}
		fill(255);
		
		Iterator<ORCALine> it3 = orcas.iterator();
		while (it3.hasNext()) {
			ORCALine o = it3.next();
			drawORCALine(o);
		}
		strokeWeight(1);
		
		stroke(255);
		Iterator<Agent2D> it4 = agents.iterator();
		while (it4.hasNext()) {
			Agent2D agent = it4.next();
			agent(agent);
		}
		fill(255,255,0,255);
		ellipse(refX,refY,0.4f,0.4f);
		strokeWeight(1);
		
		Iterator<float[]> it5 = vecs.iterator();
		while (it5.hasNext()) {
			float[] vec = it5.next();
			arrow(vec[0],vec[1],vec[2],vec[3]);
		}
		
		Iterator<float[]> it6 = circs.iterator();
		while (it6.hasNext()) {
			
			float[] circ = it6.next();
			stroke(circ[3],circ[4],circ[5]);
			ellipse(circ[0], circ[1], circ[2], circ[2]);
			
		}
		
		fill(255);
	}
	
	void arrow(float x1, float y1, float x2, float y2) {
		x1 = offsetFloat(scaleFloat(x1+refX));  
		x2 = offsetFloat(scaleFloat(x2+refX));
		y1 = offsetFloat(-scaleFloat(y1+refY));
		y2 = offsetFloat(-scaleFloat(y2+refY));
		super.line(x1, y1, x2, y2);
		  pushMatrix();
		  translate(x2, y2);
		  float a = atan2(x1-x2, y2-y1);
		  rotate(a);
		  super.line(0, 0, -10, -10);
		  super.line(0, 0, 10, -10);
		  popMatrix();
		} 

	private void dump() {
		background(0);
 
		fill(255);
		Iterator<float[]> it = env.iterator();
		stroke(32,128,255,255);
		while (it.hasNext()){
			float[] seg = it.next();
			float x1 = offsetFloat(scaleFloat(seg[0]));
			float x2 = offsetFloat(scaleFloat(seg[2]));
			float y1 = offsetFloat(-scaleFloat(seg[1]));
			float y2 = offsetFloat(-scaleFloat(seg[3]));
			super.line(x1, y1, x2, y2);
			
		}
		stroke(255);
		while (ignoredAgents.size() > 0) {
			fill(200,200,200,200);
			Agent2D a = ignoredAgents.poll();
			agent(a);
		}
		fill(255);
		while (orcas.size() > 0) {
			ORCALine o = orcas.poll();
			drawORCALine(o);
		}
		strokeWeight(1);
		
		stroke(255);
		while (agents.size() > 0) {
			Agent2D agent = agents.poll();
			agent(agent);
		}
		fill(255,255,0,255);
		ellipse(refX,refY,0.4f,0.4f);
		strokeWeight(1);
		
		while (vecs.size() > 0) {
			float[] vec = vecs.poll();
			if (vec.length == 7) {
				stroke(vec[4],vec[5],vec[6]);
			}
			arrow(vec[0],vec[1],vec[2],vec[3]);
		}
		
		while (circs.size() > 0) {
			float[] circ = circs.poll();
			stroke(circ[3],circ[4],circ[5]);
			fill(circ[3],circ[4],circ[5],128);
			ellipse(circ[0]+refX, circ[1]+refY, 2*circ[2], 2*circ[2]);
			
		}

	}

	private void drawORCALine(ORCALine o) {
		stroke(255);
		strokeWeight(2);
		line(o.getPointX()-o.getDirectionX(),o.getPointY()-o.getDirectionY(),o.getPointX()+o.getDirectionX(),o.getPointY()+o.getDirectionY());
		strokeWeight(6);
		stroke(255, 200, 200, 200);
		line(o.getPointX()-o.getDirectionX()+o.getDirectionY()/15,o.getPointY()-o.getDirectionY()-o.getDirectionX()/15,o.getPointX()+o.getDirectionX()+o.getDirectionY()/15,o.getPointY()+o.getDirectionY()-o.getDirectionX()/15);
		stroke(255, 200, 200, 100);
		line(o.getPointX()-o.getDirectionX()+o.getDirectionY()/10,o.getPointY()-o.getDirectionY()-o.getDirectionX()/10,o.getPointX()+o.getDirectionX()+o.getDirectionY()/10,o.getPointY()+o.getDirectionY()-o.getDirectionX()/10);
		stroke(255, 200, 200, 100);
		strokeWeight(20);
		line(o.getPointX()-o.getDirectionX()+o.getDirectionY()/8,o.getPointY()-o.getDirectionY()-o.getDirectionX()/8,o.getPointX()+o.getDirectionX()+o.getDirectionY()/8,o.getPointY()+o.getDirectionY()-o.getDirectionX()/8);
//		stroke(255, 200, 200, 50);
		line(o.getPointX()-o.getDirectionX()+o.getDirectionY()/2,o.getPointY()-o.getDirectionY()-o.getDirectionX()/2,o.getPointX()+o.getDirectionX()+o.getDirectionY()/2,o.getPointY()+o.getDirectionY()-o.getDirectionX()/2);
		
	}

	private void agent(Agent2D agent) {

		MatsimRandom.reset(agent.getDelegate().getCurrentLinkId().hashCode());
		int b = MatsimRandom.getRandom().nextInt(255);
		fill(255,0,b,125);
		stroke(0,255,0);
		strokeWeight(2);
		
		float r = (float) (agent.getPhysicalAgentRepresentation().getAgentDiameter());

		float x0 = (float) (agent.getPosition().x);
		//		float x1 = (float) (agent.getPosition().x + r);
		float y0 = (float) (agent.getPosition().y);
		//		float y1 = (float) (agent.getPosition().y + r);

		
		ellipse(x0,y0,r,r);
		stroke(255);
		fill(255);
		double vsqr = agent.getVx()*agent.getVx()+agent.getVy()*agent.getVy();
		double v = ((int)(100*Math.sqrt(vsqr)))/100.;
		String text = agent.getDelegate().getId().toString() + "\n" + v;
		text(text, offsetFloat(scaleFloat(x0)), offsetFloat(-scaleFloat(y0)));

	}

	@Override
	public void ellipse(float a, float b, float c, float d) {

		a = offsetFloat(scaleFloat(a));
		b = offsetFloat(-scaleFloat(b));
		c = scaleFloat(c);
		d = scaleFloat(d);


		super.ellipse(a, b, c, d);
	}

	@Override
	public void line(float x1, float y1, float x2, float y2) {
		x1 += refX;
		x2 += refX;
		y1 += refY;
		y2 += refY;

		x1 = offsetFloat(scaleFloat(x1));
		x2 = offsetFloat(scaleFloat(x2));
		y1 = offsetFloat(-scaleFloat(y1));
		y2 = offsetFloat(-scaleFloat(y2));

		super.line(x1, y1, x2, y2);
	}

	private float scaleFloat(float x) {
		return this.scale/10 * x;
	}
	private float offsetFloat(float x) {
		return x + W/2;
	}

	public static void addORCA(ORCALine line) {
		orcas.add(line);
	}

	public static void setReferencePoint(float x, float y) {
		refX = x;
		refY = y;
	}

	public static void reset() {
		orcas.clear();
		agents.clear();
		ignoredAgents.clear();
		vecs.clear();
		circs.clear();
	}

	public static void addAgent(Agent2D a) {
		agents.add(a);

	}

	public static void addObstacle(float[] seg) {
		env.add(seg);
		
	}

	public static void addIgnoredAgent(Agent2D agent) {
		ignoredAgents.add(agent);
	}
	
	public static void addVector(float [] fl) {
		vecs.add(fl);
	}

	public static void addCircle(float[] fs) {
		circs.add(fs);
		
	}
}
