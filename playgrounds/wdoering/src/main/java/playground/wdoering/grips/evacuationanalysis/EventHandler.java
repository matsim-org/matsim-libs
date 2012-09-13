package playground.wdoering.grips.evacuationanalysis;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
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


public class EventHandler implements XYVxVyEventsHandler, LinkEnterEventHandler, LinkLeaveEventHandler, Runnable {

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

	public EventHandler(Thread readerThread)
	{

		this.readerThread = readerThread;

//		Network network = sc.getNetwork();
//
		//		private HashMap<Integer, DataPoint> nodes = null;
		//		private HashMap<Integer, int[]> links = null;
		//sc.getNetwork().get -> nur über Links (hat from / to nodes (getcoord (get x y)))

//		Map<Id, ? extends org.matsim.api.core.v01.network.Node> networkNodes = network.getNodes();
	
		
		//TODO: clean up code
		
		//int k = 0;
//		this.minPosX = this.minPosY = this.minPosZ = this.minTimeStep = Double.MAX_VALUE;
//		this.maxPosX = this.maxPosY = this.maxPosZ = this.maxTimeStep = Double.MIN_VALUE;
//		this.networkNodes = new HashMap<Integer, DataPoint>();
//		this.networkLinks = new HashMap<Integer, int[]>();
//		for (Map.Entry<Id, ? extends org.matsim.api.core.v01.network.Node> node : networkNodes.entrySet())
//		{
//			//k++;
//			//			System.out.println("");
//			//			System.out.println("---------------------------------");
//			//
//			//		    System.out.println(node.getKey() + "|" + node.getValue());
//
//			//store current node in variable
//			org.matsim.api.core.v01.network.Node currentNode;
//			currentNode = node.getValue();
//
//
//			//get coordinates of the current node
//			double posX = currentNode.getCoord().getX();
//			double posY = currentNode.getCoord().getY();
//
//			//recalculate min and max values
//			this.maxPosX = Math.max(this.maxPosX, posX); this.minPosX = Math.min(this.minPosX, posX);
//			this.maxPosY = Math.max(this.maxPosY, posY); this.minPosY = Math.min(this.minPosY, posY);
//
//			//store node as DataPoint
//			DataPoint nodeDataPoint = new DataPoint(posX, posY);
//
//			//transform id to string (@TODO: use IdImpl)
//			int nodeID = Integer.valueOf(currentNode.getId().toString());
//
//			//store node datapoint + id to nodes hashmap
//			this.networkNodes.put(nodeID, nodeDataPoint);
//
//			//get in and out links
//			//Map<Id, ? extends Link> inLinks = currentNode.getInLinks();
//			Map<Id, ? extends Link> outLinks = currentNode.getOutLinks();
//
//			for (Map.Entry<Id, ? extends Link> outLink : outLinks.entrySet())
//			{
//				int from = Integer.valueOf(outLink.getValue().getFromNode().getId().toString());
//				int to = Integer.valueOf(outLink.getValue().getToNode().getId().toString());
//
//				System.out.println(Integer.valueOf(outLink.getKey().toString()) + ": from: " + from + "| to: "  + to );
//
//				//internal structure
//				int[] fromTo = {from, to};
//
//				//store to links hashmap
//				this.networkLinks.put(Integer.valueOf(outLink.getKey().toString()),fromTo);
//			}
//
//		}

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
	public void handleEvent(XYVxVyEvent event)
	{
		//syncTime(event.getTime());

		System.out.println("azimuth?");
		
		try {
			
//			if (lastTime != event.getTime())
			
			if (times==null)
				times = new ArrayList<Double>();
			
			if (times.size() == 0)
				times.add(event.getTime());
			else
			{
				if ((times.indexOf(event.getTime())) == -1)
				{
					times.add(event.getTime());
					Thread.sleep(250); //////////////////////////////////////////////////// SLEEP /////////////////
				}
			}

			
				
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
//		this.lastTime = event.getTime();

		
		this.lastTime = (long) event.getTime();

	}

	private void syncTime(double d) {

		if (d == this.lastEventsTime) {
			return;
		}
		
//		double step = ((d - this.lastEventsTime) * 1000);
		
		long currentTime = System.currentTimeMillis();
		long diff = (long) ((step*1000) - (currentTime - this.lastTime))*2;
		if (diff > 0)
		{
			try
			{
				Thread.sleep(diff);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		this.lastEventsTime = d;
		this.lastTime = System.currentTimeMillis();//currentTime;
		
	}



	@Override
	public void run()
	{
		System.out.println("RUN");

	}

	@Override
	public void handleEvent(LinkEnterEvent event)
	{
		//TODO link enter



	}

	@Override
	public void handleEvent(LinkLeaveEvent event)
	{
		System.out.println("link leave: " + event.getTime() + " - agent " + event.getPersonId() + " at link " + event.getLinkId());
		
	}




}
