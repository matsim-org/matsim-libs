/* *********************************************************************** *
 * project: org.matsim.*
 * RoadClosuresEditor.java
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

package playground.wdoering.grips.evacuationanalysis;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import playground.gregor.sim2d_v3.events.XYVxVyEvent;
import playground.gregor.sim2d_v3.events.XYVxVyEventsHandler;
import playground.wdoering.debugvisualization.model.Agent;
import playground.wdoering.debugvisualization.model.DataPoint;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;


public class EventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, AgentArrivalEventHandler, AgentDepartureEventHandler, Runnable {

//	private HashMap<String, Agent> agents = null;
	private HashMap<Integer, DataPoint> networkNodes = null;
	private HashMap<Integer, int[]> networkLinks = null;
//	private ArrayList<Geometry> geometries = null;

//	private Double maxPosX,maxPosY,maxPosZ,maxTimeStep=Double.MIN_VALUE;
//	private Double minPosX,minPosY,minPosZ,minTimeStep=Double.MAX_VALUE;

	private Double[] timeStepsAsDoubleValues;

	private LinkedList<Double> timeSteps;

	private Thread readerThread;

//	private ShapeFileReader shapeFileReader;
	private double lastEventsTime = 0;
	private double lastTime = Double.NaN;
	private double 	step = 0.066745068285285;
	
	private ArrayList<Double> times;
	private Network network;

	private double cellSize;
	private QuadTree<Cell> cellTree;
	
	private final Map<Id,Event> events = new HashMap<Id, Event>();
	private double timeSum;
	private double maxCellTimeSum;
	private int arrivals;
	private List<Tuple<Double, Integer>> arrivalTimes;
	
	private Rect boundingBox;
	
	private String eventName;

	public EventHandler(String eventFilename, Scenario sc, double cellSize, Thread readerThread)
	{
		this.eventName = eventFilename;
		this.readerThread = readerThread;
		this.network = sc.getNetwork();
		this.cellSize = cellSize;
		this.arrivalTimes = new ArrayList<Tuple<Double, Integer>>();
		init();
	}
	
	private void init() {
		
		int putCounter = 0; //FIXME: is not necessary
		this.arrivals = 0;
		this.timeSum = 0;
		this.maxCellTimeSum = Double.NEGATIVE_INFINITY;
		
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		
		for (org.matsim.api.core.v01.network.Node n : this.network.getNodes().values()) {
			
			if ((n.getId().toString().contains("en")) || (n.getId().toString().contains("el")))
				continue;
			
			double x = n.getCoord().getX();
			double y = n.getCoord().getY();
			
			if (x < minX) {
				minX = x;
			}
			
			if (x > maxX) {
				maxX = x;
			}
			
			if (y < minY) {
				minY = y;
			}
			
			if (y > maxY) {
				maxY = y;
			}
		}
		
		this.boundingBox = new Rect(minX,minY,maxX,maxY);
		
		this.cellTree = new QuadTree<Cell>(minX,minY,maxX,maxY);
		
		for (double x = minX; x <= maxX; x += cellSize) {
			for (double y = minY; y <= maxY; y += cellSize) {
				Cell<List<Event>> cell = new Cell(new LinkedList<Event>());
				
				cell.setCentroid(new CoordImpl(x, y));
				
				this.cellTree.put(x, y, cell);
				putCounter++;
			}
			
		}
		
		System.out.println("put:" + putCounter);
//		System.exit(0);
		
		
	}
	

	public LinkedList<Double> getTimeSteps()
	{
		this.timeStepsAsDoubleValues = this.timeSteps.toArray(new Double[this.timeSteps.size()]);
		Arrays.sort(this.timeStepsAsDoubleValues);

		this.timeSteps = new LinkedList<Double>();
		for (Double timeStepValue : this.timeStepsAsDoubleValues)
			this.timeSteps.addLast(timeStepValue);

		return this.timeSteps;

		//return timeStepsAsDoubleValues;
	}



	public HashMap<Integer, int[]> getLinks()
	{
		return this.networkLinks;
	}

	public HashMap<Integer, DataPoint> getNodes()
	{
		return this.networkNodes;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void handleEvent(AgentDepartureEvent event)
	{
		//just save departure event
		this.events.put(event.getPersonId(), event);
		
		//get cell from person id
		AgentDepartureEvent departure = (AgentDepartureEvent)this.events.get(event.getPersonId());
		Link link = this.network.getLinks().get(departure.getLinkId());
		Coord c = link.getCoord();
		Cell<List<Event>> cell = this.cellTree.get(c.getX(), c.getY());
		
		//get the cell data, store event to it 
		List<Event> cellEvents = cell.getData();
		cellEvents.add(event);
	}
	
	@Override
	public void handleEvent(AgentArrivalEvent event)
	{
		//get cell from person id
		AgentDepartureEvent departure = (AgentDepartureEvent)this.events.get(event.getPersonId());
		Link link = this.network.getLinks().get(departure.getLinkId());
		Coord c = link.getCoord();
		Cell<List<Event>> cell = this.cellTree.get(c.getX(), c.getY());
		
		//get the cell data, store event to it 
		List<Event> cellEvents = cell.getData();
		cellEvents.add(event);
		
		double time = event.getTime() - departure.getTime();

		cell.setTimeSum(cell.getTimeSum() + time);
		
		//update max timesum of all cells
		this.maxCellTimeSum = Math.max(cell.getTimeSum(), this.maxCellTimeSum);
		
		cell.incrementCount();
		this.timeSum += time;
		this.arrivals++;
		
		if (lastTime!=event.getTime())
		{
			this.arrivalTimes.add(new Tuple(event.getTime(), this.arrivals));
			lastTime = event.getTime();
		}
		
	}

	@Override
	public void run()
	{

	}

	@Override
	public void handleEvent(LinkEnterEvent event)
	{
		//get cell from person id
		Link link = this.network.getLinks().get(event.getLinkId());
		Coord c = link.getCoord();
		Cell<List<Event>> cell = this.cellTree.get(c.getX(), c.getY());
		cell.addLinkEnterTime(event.getTime());

		//		
//		//get the cell data, store event to it 
//		List<Event> cellEvents = cell.getData();
//		cellEvents.add(event);
		
		
//		enterTimes.add(event.)


	}

	@Override
	public void handleEvent(LinkLeaveEvent event)
	{

		//get cell from person id
		Link link = this.network.getLinks().get(event.getLinkId());
		Coord c = link.getCoord();
		Cell<List<Event>> cell = this.cellTree.get(c.getX(), c.getY());
		cell.addLinkLeaveTime(event.getTime());
		
		
//		System.out.println("link leave: " + event.getTime() + " - agent " + event.getPersonId() + " at link " + event.getLinkId());
		
	}
	
	public QuadTree<Cell> getCellTree() {
		return cellTree;
	}

	public EventData getData()
	{
		LinkedList<Cell> cells = new LinkedList<Cell>();
		cellTree.get(new Rect(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY), cells);
		
		for (Cell cell : cells)
		{
			if (cell.getCount()>0)
				System.out.println(cell.getCount());
		}
		
		System.err.println("cell count: " + cells.size());
		
		return new EventData(eventName, cellTree, cellSize, timeSum, maxCellTimeSum, arrivals, arrivalTimes, boundingBox);
	}


}
