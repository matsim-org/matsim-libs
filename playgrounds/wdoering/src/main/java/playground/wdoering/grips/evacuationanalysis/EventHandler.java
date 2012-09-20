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
	private double lastTime;
	private double 	step = 0.066745068285285;
	
	private ArrayList<Double> times;
	private Network network;

	private double cellSize;
	private QuadTree<Cell> cellTree;
	
	private final Map<Id,AgentDepartureEvent> events = new HashMap<Id, AgentDepartureEvent>();
	private double timeSum;
	private double maxCellTimeSum;
	private int arrivals;

	public EventHandler(Scenario sc, double cellSize, Thread readerThread)
	{
		this.readerThread = readerThread;
		this.network = sc.getNetwork();
		this.cellSize = cellSize;
		init();
	}
	
	private void init() {
		
		this.arrivals = 0;
		this.timeSum = 0;
		this.maxCellTimeSum = Double.NEGATIVE_INFINITY;
		
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		
		for (org.matsim.api.core.v01.network.Node n : this.network.getNodes().values()) {
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
		
		
		this.cellTree = new QuadTree<Cell>(minX,minY,maxX,maxY);
		
		for (double x = minX; x <= maxX; x += cellSize) {
			for (double y = minY; y <= maxY; y += cellSize) {
				Cell<List<Event>> cell = new Cell(new LinkedList<Event>());
				this.cellTree.put(x, y, cell);
			}
			
		}
		
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
		AgentDepartureEvent departure = this.events.get(event.getPersonId());
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
		AgentDepartureEvent departure = this.events.get(event.getPersonId());
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
	}

	@Override
	public void run()
	{

	}

	@Override
	public void handleEvent(LinkEnterEvent event)
	{


	}

	@Override
	public void handleEvent(LinkLeaveEvent event)
	{
		System.out.println("link leave: " + event.getTime() + " - agent " + event.getPersonId() + " at link " + event.getLinkId());
		
	}
	
	public QuadTree<Cell> getCellTree() {
		return cellTree;
	}

	public EventData getData()
	{
		return new EventData(cellTree, cellSize, timeSum, maxCellTimeSum, arrivals);
	}




}
