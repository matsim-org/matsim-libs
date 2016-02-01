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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import matsimConnector.agents.Pedestrian;
import matsimConnector.environment.TransitionArea;
import matsimConnector.events.CAAgentChangeLinkEvent;
import matsimConnector.events.CAAgentConstructEvent;
import matsimConnector.events.CAAgentEnterEnvironmentEvent;
import matsimConnector.events.CAAgentExitEvent;
import matsimConnector.events.CAAgentLeaveEnvironmentEvent;
import matsimConnector.events.CAAgentMoveEvent;
import matsimConnector.events.CAAgentMoveToOrigin;
import matsimConnector.events.CAEngineStepPerformedEvent;
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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import pedCA.environment.grid.EnvironmentGrid;
import pedCA.environment.grid.GridPoint;
import pedCA.environment.grid.PedestrianGrid;
import pedCA.environment.network.Coordinates;
import pedCA.utility.FileUtility;

public class EventBasedVisDebuggerEngine implements CAEventHandler, LineEventHandler, ForceReDrawEventHandler, RectEventHandler{

	double time;
	private EventsBasedVisDebugger vis;

	@SuppressWarnings("rawtypes")
	private final Map<Id,CircleProperty> circleProperties = new HashMap<Id,CircleProperty>();
	private final CircleProperty defaultCp = new CircleProperty();


	private final Scenario sc;

	private long lastUpdate = -1;
	private final double dT;
	private  Control keyControl;

	private final List<ClockedVisDebuggerAdditionalDrawer> drawers = new ArrayList<ClockedVisDebuggerAdditionalDrawer>();
	private int nrAgents;
	
	FrameSaver fs = null;
	boolean environmentInit= false;

	public EventBasedVisDebuggerEngine() {
		this.sc = null;
		this.dT = Constants.CA_STEP_DURATION;
		this.vis = new EventsBasedVisDebugger(this.fs);

		this.keyControl = new Control(this.vis.zoomer,90,this.fs);
		this.vis.addKeyControl(this.keyControl);
	}
	
	public EventBasedVisDebuggerEngine(Scenario sc) {
		this.sc = sc;
		this.dT = Constants.CA_STEP_DURATION;
		this.vis = new EventsBasedVisDebugger(sc,this.fs);

		this.keyControl = new Control(this.vis.zoomer,90,this.fs);
		this.vis.addKeyControl(this.keyControl);
	}
	
	
	public void startIteration(int iteration){
		fs = null;
		if((iteration%2==0) && Constants.SAVE_FRAMES){
			String pathName = Constants.PATH+"/videos/frames/it"+iteration;
			FileUtility.deleteDirectory(new File(pathName));
			fs = new FrameSaver(pathName, "png", 300);
		}
		this.vis.fs = fs;
		this.keyControl.fs = fs;
	}

	public void addAdditionalDrawer(VisDebuggerAdditionalDrawer drawer) {
		this.vis.addAdditionalDrawer(drawer);
		if (drawer instanceof ClockedVisDebuggerAdditionalDrawer) {
			this.drawers.add((ClockedVisDebuggerAdditionalDrawer) drawer);
		}
	}
	
	private void drawNodesAndLinks() {
		Map<String, Node> nodes = new HashMap<>();
		for (Node n : sc.getNetwork().getNodes().values()) {
			this.vis.addCircleStatic(n.getCoord().getX(),n.getCoord().getY(),.2f,0,0,0,255,0);
		}
		for (Link l : sc.getNetwork().getLinks().values()) {
			
			Node from = l.getFromNode();
			Node to = l.getToNode();
			
			if (from!= null && to != null){
				boolean isStairs = false;
				for (String stairId : Constants.stairsLinks){
					if (stairId.equals(l.getId().toString()))
						isStairs = true;
				}
				if (isStairs)
					this.vis.addLineStatic(from.getCoord().getX(), from.getCoord().getY(), to.getCoord().getX(),
							to.getCoord().getY(), 255, 255, 0, 255, 0);
				else
					this.vis.addLineStatic(from.getCoord().getX(), from.getCoord().getY(), to.getCoord().getX(),
						to.getCoord().getY(), 0, 0, 0, 255, 0);
			}
		}
	}

	private void drawCAEnvironments() {
		CAScenario scenarioCA = (CAScenario) this.sc.getScenarioElement(Constants.CASCENARIO_NAME);

		for (CAEnvironment environmentCA : scenarioCA.getEnvironments().values()) {
			drawCAEnvironment(environmentCA);	
		}
		
	}

	public void drawCAEnvironment(CAEnvironment environmentCA) {
		drawObjects(environmentCA.getContext().getEnvironmentGrid());
		for (PedestrianGrid pedestrianGrid : environmentCA.getContext().getPedestrianGrids())
			drawPedestrianGridBorders(pedestrianGrid);
	}

	private void drawObjects(EnvironmentGrid environmentGrid) {
		for (int y=0; y<environmentGrid.getRows(); y++)
			for(int x=0; x<environmentGrid.getColumns();x++)
				if (environmentGrid.getCellValue(y, x)==pedCA.utility.Constants.ENV_OBSTACLE)
					drawObstacle(new GridPoint(x,y));		
				else if(environmentGrid.belongsToTacticalDestination(new GridPoint(x, y)))
					drawTacticalDestinationCell(new GridPoint(x,y));
	}

	private void drawTacticalDestinationCell(GridPoint gridPoint) {
		Coordinates bottomLeft = new Coordinates(gridPoint);
		this.vis.addRectStatic(bottomLeft.getX(), bottomLeft.getY()+Constants.CA_CELL_SIDE, Constants.CA_CELL_SIDE, Constants.CA_CELL_SIDE, 150, 150, 255, 150, 0, true);
		
	}

	private void drawObstacle(GridPoint gridPoint) {
		Coordinates bottomLeft = new Coordinates(gridPoint);
		this.vis.addRectStatic(bottomLeft.getX(), bottomLeft.getY()+Constants.CA_CELL_SIDE, Constants.CA_CELL_SIDE, Constants.CA_CELL_SIDE, 80, 80, 80, 192, 0, true);
	}

	private void drawPedestrianGridBorders(PedestrianGrid pedestrianGrid) {
		LineProperty lp = new LineProperty();
		lp.r = 0; lp.g = 0; lp.b = 0; lp.a = 192;
		
		int rows = pedestrianGrid.getRows();
		int columns = pedestrianGrid.getColumns();
		ArrayList<Coordinates> gridCoordinates = new ArrayList<Coordinates>();
		gridCoordinates.add(calculateCoordinates(0, 0, pedestrianGrid));
		gridCoordinates.add(calculateCoordinates(columns, 0, pedestrianGrid));
		gridCoordinates.add(calculateCoordinates(columns, rows, pedestrianGrid));
		gridCoordinates.add(calculateCoordinates(0, rows, pedestrianGrid));
		gridCoordinates.add(calculateCoordinates(0, 0, pedestrianGrid));
		
		Iterator <Coordinates> it = gridCoordinates.iterator();
		Coordinates c0;
		Coordinates c1 = it.next();
		
		while (it.hasNext()){
			c0 = c1;
			c1 = it.next();
			if (pedestrianGrid instanceof TransitionArea)
				this.vis.addDashedLineStatic(c0.getX(), c0.getY(), c1.getX(), c1.getY(), 0,lp.g,lp.b,lp.a, 0, .3, 0.15);
			else
				this.vis.addLineStatic(c0.getX(), c0.getY(), c1.getX(), c1.getY(), lp.r,lp.g,lp.b,lp.a, 0);
		}
	}
	
	private Coordinates calculateCoordinates(int x, int y, PedestrianGrid pedestrianGrid){
		Coordinates result;
		if (pedestrianGrid instanceof TransitionArea){
			result = new Coordinates(new GridPoint(x,y));
			result =((TransitionArea)pedestrianGrid).convertCoordinates(result);
		}
		else
			result = new Coordinates(new GridPoint(x,y));
		return result;
	}

	@Override
	public void reset(int iteration) {
		this.time = -1;
		//this.vis.reset(iteration);
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
		if (!environmentInit){
			drawNodesAndLinks();
			drawCAEnvironments();
			environmentInit = true;
		}
		
		Pedestrian pedestrian = event.getPedestrian();
		CircleProperty cp = new CircleProperty();
		cp.rr = (float) (0.8/5.091);
		this.circleProperties.put(pedestrian.getId(), cp);
		updateColor(pedestrian);
		/*
		int nr = pedestrian.getDestination().getLevel();
		int color = nr;//(nr/10)%3;

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
		*/		
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

	@Override
	public void handleEvent(CAAgentChangeLinkEvent event) {
		//Pedestrian pedestrian = event.getPedestrian();
		//updateColor(pedestrian);
	}

	private void updateColor(Pedestrian pedestrian) {
		CircleProperty cp = this.circleProperties.get(pedestrian.getId());
		//int destLevel = 0;//pedestrian.getDestination().getLevel();
		double xDest = pedestrian.getOriginMarker().getCoordinates().getX();
		//int color;
		//int origLevel = pedestrian.getOriginMarker().getLevel();
		//int color = (((destLevel+1)*origLevel)*100)%256;
		int brightness = 80;
		if (xDest<0.4){
			cp.r = 255;
			cp.g = brightness;
			cp.b = brightness;//255-color;
			cp.a = 255;
		}
		else{
			cp.r = brightness;
			cp.g = brightness;
			cp.b = 255;//255-color;
			cp.a = 255;
		}
	}

	@Override
	public void handleEvent(CAEngineStepPerformedEvent event) {
		// TODO Auto-generated method stub
		
	}
}