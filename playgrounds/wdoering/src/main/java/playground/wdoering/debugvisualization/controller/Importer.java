/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.wdoering.debugvisualization.controller;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import playground.wdoering.debugvisualization.model.Agent;
import playground.wdoering.debugvisualization.model.DataPoint;
import playground.wdoering.oldstufffromgregor.XYVxVyEvent;
import playground.wdoering.oldstufffromgregor.XYVxVyEventsHandler;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;


public class Importer implements XYVxVyEventsHandler, LinkEnterEventHandler, Runnable {

	private HashMap<String, Agent> agents = null;
	private HashMap<Integer, DataPoint> networkNodes = null;
	private HashMap<Integer, int[]> networkLinks = null;
	private ArrayList<Geometry> geometries = null;

	private Double maxPosX,maxPosY,maxPosZ,maxTimeStep=Double.MIN_VALUE;
	private Double minPosX,minPosY,minPosZ,minTimeStep=Double.MAX_VALUE;

	private Controller controller = null;

	private Double[] timeStepsAsDoubleValues;

	private LinkedList<Double> timeSteps;

	private Thread readerThread;

	private ShapeFileReader shapeFileReader;
	private double lastEventsTime = 0;
	private double lastTime;
	private final double 	step = 0.066745068285285;
	
	private ArrayList<Double> times;


	public Importer(Controller controller)
	{
		this.controller = controller;

	}



	public Importer(Controller controller, Scenario sc, Thread readerThread)
	{

		this.controller = controller;
		this.readerThread = readerThread;

		Network network = sc.getNetwork();

		//		private HashMap<Integer, DataPoint> nodes = null;
		//		private HashMap<Integer, int[]> links = null;
		//sc.getNetwork().get -> nur über Links (hat from / to nodes (getcoord (get x y)))

		Map<Id, ? extends org.matsim.api.core.v01.network.Node> networkNodes = network.getNodes();

		//int k = 0;

		this.minPosX = this.minPosY = this.minPosZ = this.minTimeStep = Double.MAX_VALUE;
		this.maxPosX = this.maxPosY = this.maxPosZ = this.maxTimeStep = Double.MIN_VALUE;


		this.networkNodes = new HashMap<Integer, DataPoint>();
		this.networkLinks = new HashMap<Integer, int[]>();

		for (Map.Entry<Id, ? extends org.matsim.api.core.v01.network.Node> node : networkNodes.entrySet())
		{
			//k++;
			//			System.out.println("");
			//			System.out.println("---------------------------------");
			//
			//		    System.out.println(node.getKey() + "|" + node.getValue());

			//store current node in variable
			org.matsim.api.core.v01.network.Node currentNode;
			currentNode = node.getValue();


			//get coordinates of the current node
			double posX = currentNode.getCoord().getX();
			double posY = currentNode.getCoord().getY();

			//recalculate min and max values
			this.maxPosX = Math.max(this.maxPosX, posX); this.minPosX = Math.min(this.minPosX, posX);
			this.maxPosY = Math.max(this.maxPosY, posY); this.minPosY = Math.min(this.minPosY, posY);

			//store node as DataPoint
			DataPoint nodeDataPoint = new DataPoint(posX, posY);

			//transform id to string (@TODO: use IdImpl)
			int nodeID = Integer.valueOf(currentNode.getId().toString());

			//store node datapoint + id to nodes hashmap
			this.networkNodes.put(nodeID, nodeDataPoint);

			//get in and out links
			//Map<Id, ? extends Link> inLinks = currentNode.getInLinks();
			Map<Id, ? extends Link> outLinks = currentNode.getOutLinks();

			//		    System.out.println("IN LINKS");
			//		    for (Map.Entry<Id, ? extends Link> inLink : inLinks.entrySet())
			//		    {
			//		    	System.out.println(inLink.getKey() + "|" + inLink.getValue());
			//
			//		    }

			//		    System.out.println("OUT LINKS");
			for (Map.Entry<Id, ? extends Link> outLink : outLinks.entrySet())
			{
				int from = Integer.valueOf(outLink.getValue().getFromNode().getId().toString());
				int to = Integer.valueOf(outLink.getValue().getToNode().getId().toString());

				System.out.println(Integer.valueOf(outLink.getKey().toString()) + ": from: " + from + "| to: "  + to );

				//internal structure
				int[] fromTo = {from, to};

				//store to links hashmap
				this.networkLinks.put(Integer.valueOf(outLink.getKey().toString()),fromTo);
			}

			//if (k>100) System.exit(0);

			//@TODO: matsim nodes auf datapoints übertragen
			//@TODO: thread suspend ? (stepper implementieren)
		}



		//		for (Iterator iterator = networkNodes.iterator(); iterator.hasNext();) {
		//			type type = (type) iterator.next();
		//
		//		}




	}

	public void readEventFile(String fileName)
	{
		try {

			File file = new File(fileName);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			doc.getDocumentElement().normalize();
			NodeList eventList = doc.getElementsByTagName("event");

			this.agents = new HashMap<String,Agent>();

			int currentAgent = 0;

			this.timeSteps = new LinkedList<Double>();
			//minPosX = minPosY = minPosZ = minTimeStep = Double.MAX_VALUE;
			//maxPosX = maxPosY = maxPosZ = maxTimeStep = Double.MIN_VALUE;

			for (int s = 0; s < eventList.getLength(); s++) {


				Node currentNode = eventList.item(s);

				NamedNodeMap attributeList = currentNode.getAttributes();

				//check all attributes
				for (int r = 0; r < attributeList.getLength(); r++)
				{

					//xml /w type node
					if (attributeList.item(r).getNodeName().equals("type"))
					{
						String nodeValue = attributeList.item(r).getNodeValue();



						if (nodeValue.equals("XYZAzimuth"))
						{

							//System.out.println("peng");
							//System.out.println("x val:" + Double.valueOf(attributeList.getNamedItem("x").getNodeValue()));
							String agentNumber = String.valueOf(attributeList.getNamedItem("person").getNodeValue());

							//Get current agent data & check if agent data has already been collected
							Agent agent = this.agents.get(agentNumber);
							if (agent==null)
								agent = new Agent();

							//Collect time and position
							Double time = Double.valueOf(attributeList.getNamedItem("time").getNodeValue());
							Double posX = Double.valueOf(attributeList.getNamedItem("x").getNodeValue());
							Double posY = Double.valueOf(attributeList.getNamedItem("y").getNodeValue());
							Double posZ = Double.valueOf(attributeList.getNamedItem("z").getNodeValue());

							//add time
							if (!this.timeSteps.contains(time))
								this.timeSteps.addLast(time);

							//Determine minimum and maximum positions
							this.maxPosX = Math.max(this.maxPosX, posX); this.minPosX = Math.min(this.minPosX, posX);
							this.maxPosY = Math.max(this.maxPosY, posY); this.minPosY = Math.min(this.minPosY, posY);
							this.maxPosZ = Math.max(this.maxPosZ, posZ); this.minPosZ = Math.min(this.minPosZ, posZ);
							this.maxTimeStep = Math.max(this.maxTimeStep, time); this.minTimeStep = Math.min(this.minTimeStep, time);

							System.out.println("px:"+posX+" | mpx: "+ this.maxPosX );

							//add dataPoint to agent
							agent.addDataPoint(time, posX, posY, posZ);

							//add agent data to agents hashMap
							this.agents.put(String.valueOf(agentNumber), agent);

						}
					}



					//System.out.println(attributeList.item(r).getNodeName() + ":" + attributeList.item(r).getNodeValue());
				}



				//System.out.println(timeLine.size());
				//System.out.println("first:" + timeLine.first() + "| last: " + timeLine.last());

				if (currentNode.getNodeType() == Node.ELEMENT_NODE) {

				}

			}


		} catch (Exception e) {



			e.printStackTrace();
		}
	}

	public void readShapeFile(String shapeFileString)
	{
		
				this.geometries  = new ArrayList<Geometry>();
				for (SimpleFeature ft : ShapeFileReader.getAllFeatures(shapeFileString))
				{
					Geometry geo = (Geometry) ft.getDefaultGeometry();
					//System.out.println(ft.getFeatureType());
					this.geometries.add(geo);
				}
				
				
				int j = 0;			
				for (Geometry geo : this.geometries)
				{
					
					System.out.println(geo.toText());
					Coordinate [] coordinates = geo.getCoordinates();
					
					System.out.println("geomobj # " + j);
					for (int i = 0; i < coordinates.length; i++)
						System.out.println(i + ":" + coordinates[i].x + "|"  + coordinates[i].y + "|" +coordinates[i].z);	
					
					j++;
					
				}
				
				System.out.print(this.maxPosX);
				System.out.println(" - " + this.minPosX);
				System.out.print(this.maxPosY);
				System.out.println(" - " + this.minPosY);
				
//				System.exit(0);
				

		
//		DenseMultiPointFromGeometries dmp = new DenseMultiPointFromGeometries();
//		MultiPoint mp = dmp.getDenseMultiPointFromGeometryCollection(geos);
//		for (int i = 0; i < mp.getNumPoints(); i++)
//		{
//			System.out.println(i + ": " + mp.getGeometryN(i));
//			//Point p = (Point) mp.getGeometryN(i).get;
//			//quad.put(p.getX(), p.getY(), p.getCoordinate());
//		}
		
		
		//this.c.setQuadTree(quad);

	}
	
	public ArrayList<Geometry> getGeometries()
	{
		return this.geometries;
	}

	public HashMap<String, Agent> importAgentData()
	{


		return this.agents;
	}

	public Double[] getExtremeValues()
	{
		Double[] extremeValues = {this.maxPosX, this.maxPosY, this.maxPosZ, this.minPosX, this.minPosY, this.minPosZ, this.maxTimeStep, this.minTimeStep};
		return extremeValues;
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

	/**
	 * Read the network file. This is the file that contains
	 * all the nodes and the links between them.
	 * 
	 * @param networkFileName the String containing the network file name
	 */
	public void readNetworkFile(String networkFileName)
	{

		this.networkNodes = new HashMap<Integer, DataPoint>();
		this.networkLinks = new HashMap<Integer, int[]>();

		try
		{


			File file = new File(networkFileName);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			doc.getDocumentElement().normalize();

			//System.out.println("Root element " + doc.getDocumentElement().getNodeName());
			NodeList nodeList = doc.getElementsByTagName("node");
			//System.out.println("Information of all events");

			this.networkNodes = new HashMap<Integer,DataPoint>();

			this.minPosX = this.minPosY = this.minPosZ = this.minTimeStep = Double.MAX_VALUE;
			this.maxPosX = this.maxPosY = this.maxPosZ = this.maxTimeStep = Double.MIN_VALUE;


			for (int s = 0; s < nodeList.getLength(); s++)
			{

				//Get current node and attributes
				Node currentNode = nodeList.item(s);
				NamedNodeMap attributeList = currentNode.getAttributes();

				//check all attributes (<node id="2" x="386420.2693861949" y="5819507.022613811" />)
				//Get nodeID and coordinates
				int nodeID = Integer.valueOf(attributeList.getNamedItem("id").getNodeValue());
				Double posX = Double.valueOf(attributeList.getNamedItem("x").getNodeValue());
				Double posY = Double.valueOf(attributeList.getNamedItem("y").getNodeValue());

				this.maxPosX = Math.max(this.maxPosX, posX); this.minPosX = Math.min(this.minPosX, posX);
				this.maxPosY = Math.max(this.maxPosY, posY); this.minPosY = Math.min(this.minPosY, posY);

				//create a new dataPoint containing the coordinates
				DataPoint nodeDataPoint = new DataPoint(posX, posY);

				//add node ID and coordinates to node ArrayList
				this.networkNodes.put(nodeID, nodeDataPoint);

				System.out.println("node (" + nodeID + ") : x:" + posX + " | y: " + posY );

			}

			//max/min timestep not relevant within network data & z value handling not implemented yet
			this.maxPosZ = this.minPosZ = this.maxTimeStep = this.minTimeStep = Double.NaN;


			//Get links
			NodeList linkList = doc.getElementsByTagName("link");

			//Iterate over all links
			for (int s = 0; s < linkList.getLength(); s++)
			{

				//Get current link and attributes
				Node currentNode = linkList.item(s);
				NamedNodeMap attributeList = currentNode.getAttributes();

				//<link id="0" from="0" to="1" length="1.6245923691312782" freespeed="1.34" capacity="1.0" permlanes="1.0" oneway="1" modes="car" />

				//Get linkID and coordinates
				int linkID = Integer.valueOf(attributeList.getNamedItem("id").getNodeValue());
				int from = Integer.valueOf(attributeList.getNamedItem("from").getNodeValue());
				int to = Integer.valueOf(attributeList.getNamedItem("to").getNodeValue());

				int[] fromTo = {from,to};

				//add link ID and from/to to link ArrayList
				this.networkLinks.put(linkID, fromTo);

				System.out.println("link (" + linkID + ") : from:" + from + " | to: " + to );

			}






		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

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
		

		try {
			
//			if (lastTime != event.getTime())
			
			if (this.times==null)
				this.times = new ArrayList<Double>();
			
			if (this.times.size() == 0)
				this.times.add(event.getTime());
			else
			{
				if ((this.times.indexOf(event.getTime())) == -1)
				{
					this.times.add(event.getTime());
					Thread.sleep(250); //////////////////////////////////////////////////// SLEEP /////////////////
				}
			}

			
				
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		this.lastTime = event.getTime();
		
		//testWait(e.getTime());
		if (this.controller!=null)
		{

			this.controller.console.println("time: " + event.getTime() + " - Agent " + event.getPersonId().toString() + ": " + event.getX() + "|" + event.getY() );
			this.controller.updateAgentData(event.getPersonId().toString(), event.getX(), event.getY(), event.getVX(), event.getVY(), event.getTime());
		}
		
		this.lastTime = (long) event.getTime();

	}

	private void syncTime(double d) {

		if (d == this.lastEventsTime) {
			return;
		}
		
//		double step = ((d - this.lastEventsTime) * 1000);
		
		
		long currentTime = System.currentTimeMillis();
		long diff = (long) ((this.step*1000) - (currentTime - this.lastTime))*2;
		if (diff > 0) {
			try {
				
				Thread.sleep(diff);
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		this.lastEventsTime = d;
		this.lastTime = System.currentTimeMillis();//currentTime;
		
	}



	@Override
	public void run() {

	}

	@Override
	public void handleEvent(LinkEnterEvent event)
	{
		if (this.controller!=null)
			this.controller.updateAgentLink(event.getPersonId().toString(), event.getLinkId().toString());


	}




}
