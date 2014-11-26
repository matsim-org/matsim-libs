/* *********************************************************************** *
 * project: org.matsim.*
 * EventDecoderEngine.java
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

package matsimConnector.visualizer.debugger.eventsbaseddebugger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import matsimConnector.agents.Pedestrian;
import matsimConnector.events.CAAgentConstructEvent;
import matsimConnector.events.CAAgentEnterEnvironmentEvent;
import matsimConnector.events.CAAgentExitEvent;
import matsimConnector.events.CAAgentLeaveEnvironmentEvent;
import matsimConnector.events.CAAgentMoveEvent;
import matsimConnector.events.CAAgentMoveToOrigin;
import matsimConnector.events.CAEventHandler;
import matsimConnector.events.debug.ForceReDrawEvent;
import matsimConnector.events.debug.ForceReDrawEventHandler;
import matsimConnector.events.debug.LineEvent;
import matsimConnector.events.debug.LineEventHandler;
import matsimConnector.events.debug.RectEvent;
import matsimConnector.events.debug.RectEventHandler;
import matsimConnector.scenario.CAEnvironment;
import matsimConnector.scenario.CAScenario;
import matsimConnector.utility.Constants;
import matsimConnector.utility.MathUtility;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;

import pedCA.environment.network.Coordinates;



public class EventBasedVisDebuggerEngine implements CAEventHandler, LineEventHandler, ForceReDrawEventHandler, RectEventHandler{

	double time;
	private final EventsBasedVisDebugger vis;

	@SuppressWarnings("rawtypes")
	private final Map<Id,CircleProperty> circleProperties = new HashMap<Id,CircleProperty>();
	private final CircleProperty defaultCp = new CircleProperty();


	private final Scenario sc;

	private long lastUpdate = -1;
	private final double dT;
	private final  Control keyControl;

	private final List<ClockedVisDebuggerAdditionalDrawer> drawers = new ArrayList<ClockedVisDebuggerAdditionalDrawer>();
	private int nrAgents;

	FrameSaver fs = null;

	public EventBasedVisDebuggerEngine() {
		this.sc = null;
		this.dT = Constants.CA_STEP_DURATION;
		this.vis = new EventsBasedVisDebugger(this.fs);

		this.keyControl = new Control(this.vis.zoomer,90,this.fs);
		this.vis.addKeyControl(this.keyControl);
		init();
	}
	
	public EventBasedVisDebuggerEngine(Scenario sc) {
		this.sc = sc;
		this.dT = Constants.CA_STEP_DURATION;
		this.vis = new EventsBasedVisDebugger(sc,this.fs);

		this.keyControl = new Control(this.vis.zoomer,90,this.fs);
		this.vis.addKeyControl(this.keyControl);
		init();
	}

	public void addAdditionalDrawer(VisDebuggerAdditionalDrawer drawer) {
		this.vis.addAdditionalDrawer(drawer);
		if (drawer instanceof ClockedVisDebuggerAdditionalDrawer) {
			this.drawers.add((ClockedVisDebuggerAdditionalDrawer) drawer);
		}
	}

	private void init() {

		this.defaultCp.a = 255;
		this.defaultCp.minScale = 0;
		this.defaultCp.rr = .19f;

		//Links
		
		//lp.minScale = 10;

		CAScenario scenarioCA = (CAScenario) this.sc.getScenarioElement(Constants.CASCENARIO_NAME);

		for (CAEnvironment environmentCA : scenarioCA.getEnvironments().values()) {
			drawCAEnvironment(environmentCA);	
		}
		
	}

	public void drawCAEnvironment(CAEnvironment environmentCA) {
		LineProperty lp = new LineProperty();
		lp.r = 0; lp.g = 0; lp.b = 0; lp.a = 192;
		int rows = environmentCA.getContext().getEnvironmentGrid().getRows();
		int columns = environmentCA.getContext().getEnvironmentGrid().getColumns();
		Coordinates c0 = new Coordinates(0, 0);
		Coordinates c1 = new Coordinates(columns*Constants.CA_CELL_SIDE, 0);
		Coordinates c2 = new Coordinates(0, rows*Constants.CA_CELL_SIDE);
		Coordinates c3 = new Coordinates(columns*Constants.CA_CELL_SIDE, rows*Constants.CA_CELL_SIDE);
		
		this.vis.addLineStatic(c0.getX(), c0.getY(), c1.getX(), c1.getY(), lp.r,lp.g,lp.b,lp.a, 0);
		this.vis.addLineStatic(c2.getX(), c2.getY(), c3.getX(), c3.getY(), lp.r,lp.g,lp.b,lp.a, 0);
		this.vis.addLineStatic(c0.getX(), c0.getY(), c2.getX(), c2.getY(), lp.r,lp.g,lp.b,lp.a, 0);
		this.vis.addLineStatic(c1.getX(), c1.getY(), c3.getX(), c3.getY(), lp.r,lp.g,lp.b,lp.a, 0);
	}

	@Override
	public void reset(int iteration) {
		this.time = -1;
		this.vis.reset(iteration);

	}

	public void handleEvent(CAAgentMoveEvent event) {
		if (event.getRealTime() >= this.time+Constants.CA_STEP_DURATION) {
			update(this.time);
			this.time = event.getRealTime();
		}

		this.nrAgents++;
		
		double from_x = MathUtility.convertGridCoordinate(event.getFrom_x());
		double from_y = MathUtility.convertGridCoordinate(event.getFrom_y());
		double to_x = MathUtility.convertGridCoordinate(event.getTo_x());
		double to_y = MathUtility.convertGridCoordinate(event.getTo_y());

		/*
		GridPoint deltaPos = DirectionUtility.convertHeadingToGridPoint(event.getDirection());
		double to_x_triangle = MathUtility.convertGridCoordinate(event.getFrom_x()+deltaPos.getX());
		double to_y_triangle = MathUtility.convertGridCoordinate(event.getFrom_y()+deltaPos.getY());
		double dx = (to_y_triangle - from_y);
		double dy = -(to_x_triangle - from_x);
		double length = Math.sqrt(dx*dx+dy*dy);
		dx /= length;
		dy /= length;
		
		double x0 = to_x_triangle;
		double y0 = to_y_triangle;
		double al = .20;
		double x1 = x0 + dy*al -dx*al/4;
		double y1 = y0 - dx*al -dy*al/4;
		double x2 = x0 + dy*al +dx*al/4;
		double y2 = y0 - dx*al +dy*al/4;
		*/
		
		double z = this.vis.zoomer.getZoomScale();
		int a = 255;
		if (z >= 48 && z < 80){
			z -= 48;
			a = (int)(255./32*z+.5);
		}
		this.vis.addLine(to_x, to_y, from_x, from_y, 0, 0, 0, a, 50);
		//this.vis.addTriangle(x0, y0, x1, y1, x2, y2, 0, 0, 0, a, 50, true);

		CircleProperty cp = this.circleProperties.get(event.getPedestrian().getId());
		if (cp == null) {
			cp = this.defaultCp;
		}

		this.vis.addCircle(to_x,to_y,cp.rr,cp.r,cp.g,cp.b,cp.a,cp.minScale,cp.fill);
		this.vis.addText(to_x,to_y, ""+event.getDensity(), 200);
	}

	private void update(double time2) {
		this.keyControl.awaitPause();
		this.keyControl.awaitScreenshot();
		this.keyControl.update(time2);
		long timel = System.currentTimeMillis();
	
		long last = this.lastUpdate ;
		long diff = timel - last;
		if (diff < this.dT*1000/this.keyControl.getSpeedup()) {
			long wait = (long) (this.dT *1000/this.keyControl.getSpeedup()-diff);
			try {
				Thread.sleep(wait);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		this.vis.update(this.time);
		this.lastUpdate = System.currentTimeMillis();
		for (ClockedVisDebuggerAdditionalDrawer drawer : this.drawers){
			drawer.update(this.lastUpdate);
			if (drawer instanceof InfoBox) {
				((InfoBox)drawer).setNrAgents(this.nrAgents);
			}
		}
		this.nrAgents = 0;
	}

	private static final class CircleProperty {
		boolean fill = true;
		float rr;
		int r,g,b,a, minScale = 0;
	}

	private static final class LineProperty {
		public int r,g,b,a = 0;
	}
	
	
	public void handleEvent(CAAgentConstructEvent event) {
		
		Pedestrian pedestrian = event.getPedestrian();
		CircleProperty cp = new CircleProperty();
		cp.rr = (float) (0.4/5.091);
		//int nr = pedestrian.getId().toString().hashCode()%100;
		int nr = pedestrian.getDestination().getLevel();
		int color = nr;//(nr/10)%3;
//		if (Integer.parseInt(a.getId().toString()) < 0) {
//			color = 1;
//		} else {
//			color = 2;
//		}
		if (color == 1){
			cp.r = 255;
			cp.g = 255-nr;
			cp.b = 0;
			cp.a = 255;
		} else if (color == 2) { 
			cp.r = nr-nr;
			cp.g = 0;
			cp.b = 255;
			cp.a = 255;
		}else {
			cp.r = 0;
			cp.g = 255;
			cp.b = 255-nr;
			cp.a = 255;
		}
		
		if (pedestrian.getId().toString().startsWith("g")) {
			cp.r=0;
			cp.g=255-nr;
			cp.b=0;
		} else if (pedestrian.getId().toString().startsWith("b")) {
			cp.r=0;
			cp.g=0;
			cp.b=255-nr;			
		}else if (pedestrian.getId().toString().startsWith("r")) {
			cp.r=255-nr;
			cp.g=0;
			cp.b=0;			
		}
		
		this.circleProperties.put(pedestrian.getId(), cp);
	}
		
	@Override
	public void handleEvent(CAAgentExitEvent event) {
		this.circleProperties.remove(event.getPedestrian().getId());
	}

	@Override
	public void handleEvent(LineEvent e) {

		if (e.isStatic()) {
			if (e.getGap() == 0) {
				this.vis.addLineStatic(e.getSegment().x0, e.getSegment().y0, e.getSegment().x1, e.getSegment().y1, e.getR(), e.getG(), e.getB(), e.getA(), e.getMinScale());
			} else {
				this.vis.addDashedLineStatic(e.getSegment().x0, e.getSegment().y0, e.getSegment().x1, e.getSegment().y1, e.getR(), e.getG(), e.getB(), e.getA(), e.getMinScale(),e.getDash(),e.getGap());
			}
		} else {
			if (e.getGap() == 0) {
				this.vis.addLine(e.getSegment().x0, e.getSegment().y0, e.getSegment().x1, e.getSegment().y1, e.getR(), e.getG(), e.getB(), e.getA(), e.getMinScale());
			} else {
				this.vis.addDashedLine(e.getSegment().x0, e.getSegment().y0, e.getSegment().x1, e.getSegment().y1, e.getR(), e.getG(), e.getB(), e.getA(), e.getMinScale(),e.getDash(),e.getGap());
			}
		}

	}

	@Override
	public void handleEvent(ForceReDrawEvent event) {
		this.keyControl.requestScreenshot();
		update(event.getTime());
	}

	@Override
	public void handleEvent(RectEvent e) {
		this.vis.addRect(e.getTx(),e.getTy(),e.getSx(),e.getSy(),255,255,255,255,0,e.getFill());
	}

	public int getNrAgents() {
		return this.nrAgents;
	}

	@Override
	public void handleEvent(CAAgentMoveToOrigin event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(CAAgentLeaveEnvironmentEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(CAAgentEnterEnvironmentEvent event) {
		// TODO Auto-generated method stub
		
	}
}