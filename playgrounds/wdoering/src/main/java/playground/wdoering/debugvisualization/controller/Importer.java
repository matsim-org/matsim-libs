package playground.wdoering.debugvisualization.controller;
import java.awt.Point;
import java.io.File;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;

import javax.management.AttributeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import playground.wdoering.debugvisualization.model.Agent;
import playground.wdoering.debugvisualization.model.DataPoint;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class Importer {

	private HashMap<Integer, Agent> agents = null; 
	private HashMap<Integer, DataPoint> nodes = null;
	private HashMap<Integer, int[]> links = null;
	
	private Double maxPosX;
	private Double maxPosY;
	private Double maxPosZ;
	private Double minPosX;
	private Double minPosY;
	private Double minPosZ;
	
	private Double[] timeStepsAsDoubleValues;
	
	private ArrayList<Double> timeStepArray;
	
	private Double maxTimeStep;
	private Double minTimeStep;

	public void Importer()
	{
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

			agents = new HashMap<Integer,Agent>();

			int currentAgent = 0;

			timeStepArray = new ArrayList<Double>();
			minPosX = minPosY = minPosZ = minTimeStep = Double.MAX_VALUE;
			maxPosX = maxPosY = maxPosZ = maxTimeStep = Double.MIN_VALUE;

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
							int agentNumber = Integer.valueOf(attributeList.getNamedItem("person").getNodeValue());

							//Get current agent data & check if agent data has already been collected
							Agent agent = agents.get(agentNumber);
							if (agent==null)
								agent = new Agent();

							//Collect time and position
							Double time = Double.valueOf(attributeList.getNamedItem("time").getNodeValue());
							Double posX = Double.valueOf(attributeList.getNamedItem("x").getNodeValue());
							Double posY = Double.valueOf(attributeList.getNamedItem("y").getNodeValue());
							Double posZ = Double.valueOf(attributeList.getNamedItem("z").getNodeValue());

							//add time
							if (!timeStepArray.contains(time))
								timeStepArray.add(time);

							//Determine minimum and maximum positions
							maxPosX = Math.max(maxPosX, posX); minPosX = Math.min(minPosX, posX);
							maxPosY = Math.max(maxPosY, posY); minPosY = Math.min(minPosY, posY);
							maxPosZ = Math.max(maxPosZ, posZ); minPosZ = Math.min(minPosZ, posZ);
							maxTimeStep = Math.max(maxTimeStep, time); minTimeStep = Math.min(minTimeStep, time);
							
							System.out.println("px:"+posX+" | mpx: "+ maxPosX );

							//add dataPoint to agent
							agent.addDataPoint(time, posX, posY, posZ);

							//add agent data to agents hashMap
							agents.put(agentNumber, agent);

						}
					}



					//System.out.println(attributeList.item(r).getNodeName() + ":" + attributeList.item(r).getNodeValue());
				}
				
				

				//System.out.println(timeLine.size());
				//System.out.println("first:" + timeLine.first() + "| last: " + timeLine.last());

				if (currentNode.getNodeType() == Node.ELEMENT_NODE) {

					//Element fstElmnt = (Element) fstNode;


					//			           
					//			      NodeList fstNmElmntLst = fstElmnt.getElementsByTagName("firstname");
					//			      Element fstNmElmnt = (Element) fstNmElmntLst.item(0);
					//			      NodeList fstNm = fstNmElmnt.getChildNodes();
					//			      System.out.println("First Name : "  + ((Node) fstNm.item(0)).getNodeValue());
					//			      
					//			      NodeList lstNmElmntLst = fstElmnt.getElementsByTagName("lastname");
					//			      Element lstNmElmnt = (Element) lstNmElmntLst.item(0);
					//			      NodeList lstNm = lstNmElmnt.getChildNodes();
					//			      System.out.println("Last Name : " + ((Node) lstNm.item(0)).getNodeValue());
				}

			}
			
			
//			Agent testAgent = (Agent)agents.get(0);
//			System.out.println("tadps:"+testAgent.dataPoints.size());
//			Iterator it = testAgent.dataPoints.entrySet().iterator();
//			while (it.hasNext())
//			{
//				Map.Entry pairs = (Map.Entry) it.next();
//				//System.out.println(pairs.getKey() + " = " + pairs.getValue());
//				DataPoint dp = (DataPoint) pairs.getValue();
//				System.out.println(dp.getPosX());
//			}
//			System.exit(0);
//			System.out.println("timesteps:" + timeStepArray.size());
//			for (Double timeStep : timeStepArray)
//			{
//				System.out.println("timestep:" + timeStep);
//			}

		} catch (Exception e) {
			
			
			
			e.printStackTrace();
		}
	}

	public HashMap<Integer, Agent> importAgentData()
	{


		return agents;
	}

	public Double[] getExtremeValues()
	{
		Double[] extremeValues = {maxPosX, maxPosY, maxPosZ, minPosX, minPosY, minPosZ, maxTimeStep, minTimeStep}; 
		return extremeValues;
	}
	
	public Double[] getTimeSteps()
	{
		timeStepsAsDoubleValues = timeStepArray.toArray(new Double[timeStepArray.size()]);
		Arrays.sort(timeStepsAsDoubleValues);
		
		return timeStepsAsDoubleValues;
	}

	/**
	 * Read the network file. This is the file that contains
	 * all the nodes and the links between them.
	 * 
	 * @param networkFileName the String containing the network file name
	 */
	public void readNetworkFile(String networkFileName)
	{
		
		nodes = new HashMap<Integer, DataPoint>();
		links = new HashMap<Integer, int[]>();

		try {

			File file = new File(networkFileName);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			doc.getDocumentElement().normalize();
			
			//System.out.println("Root element " + doc.getDocumentElement().getNodeName());
			NodeList nodeList = doc.getElementsByTagName("node");
			//System.out.println("Information of all events");

			nodes = new HashMap<Integer,DataPoint>();

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

				//create a new dataPoint containing the coordinates
				DataPoint nodeDataPoint = new DataPoint(posX, posY);
				
				//add node ID and coordinates to node ArrayList
				nodes.put(nodeID, nodeDataPoint);
			
				System.out.println("node (" + nodeID + ") : x:" + posX + " | y: " + posY );

			}
			

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
				links.put(linkID, fromTo);
			
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
		return links;
	}

	public HashMap<Integer, DataPoint> getNodes()
	{
		return nodes;
	}



}
