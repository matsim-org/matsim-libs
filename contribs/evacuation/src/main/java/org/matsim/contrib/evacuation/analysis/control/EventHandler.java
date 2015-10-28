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

package org.matsim.contrib.evacuation.analysis.control;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.evacuation.analysis.control.vis.ClearingTimeVisualizer;
import org.matsim.contrib.evacuation.analysis.control.vis.EvacuationTimeVisualizer;
import org.matsim.contrib.evacuation.analysis.control.vis.UtilizationVisualizer;
import org.matsim.contrib.evacuation.analysis.data.Cell;
import org.matsim.contrib.evacuation.analysis.data.ColorationMode;
import org.matsim.contrib.evacuation.analysis.data.EventData;
import org.matsim.core.events.handler.Vehicle2DriverEventHandler;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

public class EventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonArrivalEventHandler, PersonDepartureEventHandler, 
Runnable, Wait2LinkEventHandler, VehicleLeavesTrafficEventHandler {

	private HashMap<Integer, int[]> networkLinks = null;

	private Double[] timeStepsAsDoubleValues;

	private LinkedList<Double> timeSteps;

	private double lastTime = Double.NaN;

	private Network network;

	private double cellSize;
	private QuadTree<Cell> cellTree;

	private final Map<Id<Person>, Event> events = new HashMap<>();
	private double timeSum;
	private double maxCellTimeSum;
	private int maxUtilization;
	private int arrivals;
	private List<Tuple<Double, Integer>> arrivalTimes;

	private ArrayList<Link> links;

	private Rect boundingBox;
	private String eventName;

	private HashMap<Id<Link>, List<Tuple<Id<Person>, Double>>> linkEnterTimes;
	private HashMap<Id<Link>, List<Tuple<Id<Person>, Double>>> linkLeaveTimes;
	private double maxClearingTime;

	private ColorationMode colorationMode = ColorationMode.GREEN_YELLOW_RED;
	private float cellTransparency;

	private int k;
	private int cellCount;
	private boolean ignoreExitLink = true;
	
	private boolean useCellCount = true;
	private double sampleSize = 0.1;

	private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();

	public EventHandler(boolean useCellCount, String eventFilename, Scenario sc, double cellSize, Thread readerThread) {
		this.useCellCount = useCellCount;
		
		this.sampleSize = Double.valueOf(sc.getConfig().getModule("evacuation").getValue("sampleSize"));
		
		if (useCellCount)
			this.cellCount = (int)cellSize;
		else
			this.cellSize = cellSize;
		
		this.eventName = eventFilename;
		this.network = sc.getNetwork();
		this.arrivalTimes = new ArrayList<Tuple<Double, Integer>>();
		init();
	}

	public void setK(int k) {
		this.k = k;
	}

	public void setIgnoreExitLink(boolean ignoreExitLink) {
		this.ignoreExitLink = ignoreExitLink;
	}

	public boolean isIgnoreExitLink() {
		return ignoreExitLink;
	}

	private void init() {

		this.arrivals = 0;
		this.timeSum = 0;
		this.maxUtilization = 0;
		this.maxClearingTime = Double.NEGATIVE_INFINITY;
		this.maxCellTimeSum = Double.NEGATIVE_INFINITY;
		this.linkEnterTimes = new HashMap<>();
		this.linkLeaveTimes = new HashMap<>();

		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		this.links = new ArrayList<Link>();

		for (Link link : this.network.getLinks().values()) {
			if ((link.getId().toString().contains("el")) || (link.getId().toString().contains("en")))
				continue;

			minX = Math.min(minX, Math.min(link.getFromNode().getCoord().getX(), link.getToNode().getCoord().getX()));
			minY = Math.min(minY, Math.min(link.getFromNode().getCoord().getY(), link.getToNode().getCoord().getY()));
			maxX = Math.max(maxX, Math.max(link.getFromNode().getCoord().getX(), link.getToNode().getCoord().getX()));
			maxY = Math.max(maxY, Math.max(link.getFromNode().getCoord().getY(), link.getToNode().getCoord().getY()));

			this.links.add(link);

		}

		this.boundingBox = new Rect(minX, minY, maxX, maxY);

		this.cellTree = new QuadTree<Cell>(minX, minY, maxX, maxY);

		if ((useCellCount) && (cellCount>0))
			cellSize = (maxX-minX)/cellCount;
		
		cellCount = 0;
		
		for (double x = minX; x <= maxX; x += cellSize) {
			for (double y = minY; y <= maxY; y += cellSize) {
				Cell cell = new Cell(new LinkedList<Event>());
				cell.setCoord(new Coord(x, y));
				this.cellTree.put(x, y, cell);
				cellCount++;
			}

		}

	}

	public LinkedList<Double> getTimeSteps() {
		this.timeStepsAsDoubleValues = this.timeSteps.toArray(new Double[this.timeSteps.size()]);
		Arrays.sort(this.timeStepsAsDoubleValues);

		this.timeSteps = new LinkedList<Double>();
		for (Double timeStepValue : this.timeStepsAsDoubleValues)
			this.timeSteps.addLast(timeStepValue);

		return this.timeSteps;
		// return timeStepsAsDoubleValues;
	}

	public HashMap<Integer, int[]> getLinks() {
		return this.networkLinks;
	}

	@Override
	public void reset(int iteration) {
		delegate.reset(iteration);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getPersonId().toString().contains("veh"))
			return;

		// just save departure event
		this.events.put(event.getPersonId(), event);

		// get cell from person id
		PersonDepartureEvent departure = (PersonDepartureEvent) this.events.get(event.getPersonId());
		Link link = this.network.getLinks().get(departure.getLinkId());
		Coord c = link.getCoord();
		Cell cell = this.cellTree.getClosest(c.getX(), c.getY());

		// get the cell data, store event to it
		List<Event> cellEvents = cell.getData();
		cellEvents.add(event);

	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {

		if (event.getPersonId().toString().contains("veh"))
			return;

		// get cell from person id
		PersonDepartureEvent departure = (PersonDepartureEvent) this.events.get(event.getPersonId());
		Link link = this.network.getLinks().get(departure.getLinkId());
		Coord c = link.getCoord();
		Cell cell = this.cellTree.getClosest(c.getX(), c.getY());

		// get the cell data, store event to it
		List<Event> cellEvents = cell.getData();
		cellEvents.add(event);

		// do not consider the exit link
		if ((ignoreExitLink) && (cell.getId().toString().equals("" + Cell.getCurrentId())))
			return;

		double time = event.getTime() - departure.getTime();

		if (!cell.getId().toString().equals(cellCount)) {
			cell.setTimeSum(cell.getTimeSum() + time);

			// update max timesum of all cells
			this.maxCellTimeSum = Math.max(cell.getTimeSum(), this.maxCellTimeSum);
		}

		cell.incrementCount();
		this.timeSum += time;
		this.arrivals++;

		if (lastTime != event.getTime()) {
			this.arrivalTimes.add(new Tuple<Double, Integer>(event.getTime(), this.arrivals));
			cell.addArrivalTime(event.getTime());
			lastTime = event.getTime();
		}

	}

	@Override
	public void run() {

	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> personId = delegate.getDriverOfVehicle(event.getVehicleId());
		
		if (personId.toString().contains("veh"))
			return;

		// get link id
		Id<Link> linkId = event.getLinkId();
		
		// get cell from person id
		Link link = this.network.getLinks().get(linkId);
		Coord c = link.getCoord();
		Cell cell = this.cellTree.getClosest(c.getX(), c.getY());

		// do not consider the exit link
		if ((ignoreExitLink) && (cell.getId().toString().equals("" + Cell.getCurrentId())))
			return;

		// update cell link enter time
		cell.addLinkEnterTime(linkId, personId, event.getTime());

		// check for highest global utilization of a single link
		int enterCount = cell.getLinkEnterTimes().size();
		maxUtilization = Math.max(maxUtilization, enterCount);

		// update global link enter times
		List<Tuple<Id<Person>, Double>> times;
		if (linkEnterTimes.containsKey(linkId))
			times = linkEnterTimes.get(linkId);
		else
			times = new LinkedList<Tuple<Id<Person>, Double>>();
		times.add(new Tuple<Id<Person>, Double>(personId, event.getTime()));

		linkEnterTimes.put(linkId, times);

	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {

		// get link id
		Id<Link> linkId = event.getLinkId();
		Id<Person> personId = delegate.getDriverOfVehicle(event.getVehicleId());

		// get cell from person id
		Link link = this.network.getLinks().get(linkId);
		Coord c = link.getCoord();
		Cell cell = this.cellTree.getClosest(c.getX(), c.getY());

		// do not consider the exit link
		if ((ignoreExitLink) && (cell.getId().toString().equals("" + Cell.getCurrentId())))
			return;

		// update cell link leave time
		cell.addLinkLeaveTime(linkId, personId, event.getTime());

		// update global link leave times
		List<Tuple<Id<Person>, Double>> times;
		if (linkLeaveTimes.containsKey(linkId))
			times = linkLeaveTimes.get(linkId);
		else
			times = new LinkedList<>();
		times.add(new Tuple<Id<Person>, Double>(personId, event.getTime()));

		linkLeaveTimes.put(linkId, times);

	}

	public QuadTree<Cell> getCellTree() {
		return cellTree;
	}

	public EventData getData() {

		getClearingTimes();

		EventData eventData = new EventData(eventName);

		eventData.setCellTree(cellTree);
		eventData.setCellSize(cellSize);
		eventData.setTimeSum(timeSum);
		eventData.setMaxCellTimeSum(maxCellTimeSum);
		eventData.setArrivals(arrivals);
		eventData.setArrivalTimes(arrivalTimes);
		eventData.setBoundingBox(boundingBox);
		eventData.setLinkEnterTimes(linkEnterTimes);
		eventData.setLinkLeaveTimes(linkLeaveTimes);
		eventData.setMaxUtilization(maxUtilization);
		eventData.setMaxClearingTime(maxClearingTime);
		eventData.setSampleSize(sampleSize);

		// set visualization attributes
		setVisualData(eventData);

		return eventData;
	}

	private void setVisualData(EventData eventData) {
		Clusterizer clusterizer = new Clusterizer();

		EvacuationTimeVisualizer eVis = new EvacuationTimeVisualizer(eventData, clusterizer, k, this.colorationMode, this.cellTransparency);
		ClearingTimeVisualizer cVis = new ClearingTimeVisualizer(eventData, clusterizer, k, this.colorationMode, this.cellTransparency);
		UtilizationVisualizer uVis = new UtilizationVisualizer(links, eventData, clusterizer, k, this.colorationMode, this.cellTransparency);

		eventData.setEvacuationTimeVisData(eVis.getColoration());
		eventData.setClearingTimeVisData(cVis.getColoration());
		eventData.setLinkUtilizationVisData(uVis.getColoration());

	}

	private void getClearingTimes() {

		for (Link link : this.links) {
			Coord fromNodeCoord = link.getFromNode().getCoord();
			Coord toNodeCoord = link.getToNode().getCoord();

			double minX = Math.min(fromNodeCoord.getX(), toNodeCoord.getX()) - cellSize / 2d;
			double maxX = Math.max(fromNodeCoord.getX(), toNodeCoord.getX()) + cellSize / 2d;
			double minY = Math.min(fromNodeCoord.getY(), toNodeCoord.getY()) - cellSize / 2d;
			double maxY = Math.max(fromNodeCoord.getY(), toNodeCoord.getY()) + cellSize / 2d;

			Rect boundary = new Rect(minX, minY, maxX, maxY);

			// get all cells that are within the boundary from celltree
			LinkedList<Cell> cells = new LinkedList<Cell>();
			List<Tuple<Id<Person>, Double>> currentLinkLeaveTimes = linkLeaveTimes.get(link.getId());

			if ((currentLinkLeaveTimes != null) && (currentLinkLeaveTimes.size() > 0)) {
				// cut 5%
				int confidentElementNo = Math.max(0, (int) (currentLinkLeaveTimes.size() * 0.95d - 1));

				double latestTime = currentLinkLeaveTimes.get(confidentElementNo).getSecond();
				maxClearingTime = Math.max(latestTime, maxClearingTime);

				cellTree.getRectangle(boundary, cells);

				for (Cell cell : cells)
					cell.updateClearanceTime(latestTime);
			}

		}
	}

	public void setColorationMode(ColorationMode colorationMode) {
		this.colorationMode = colorationMode;
	}

	public void setTransparency(float cellTransparency) {
		this.cellTransparency = cellTransparency;

	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(Wait2LinkEvent event) {
		delegate.handleEvent(event);
	}

}
