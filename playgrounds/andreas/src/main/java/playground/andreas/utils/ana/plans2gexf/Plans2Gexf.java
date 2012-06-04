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

package playground.andreas.utils.ana.plans2gexf;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlWriter;

import playground.andreas.P2.stats.GexfPStat;
import playground.andreas.gexf.ObjectFactory;
import playground.andreas.gexf.XMLAttributeContent;
import playground.andreas.gexf.XMLAttributesContent;
import playground.andreas.gexf.XMLAttrtypeType;
import playground.andreas.gexf.XMLAttvalue;
import playground.andreas.gexf.XMLAttvaluesContent;
import playground.andreas.gexf.XMLClassType;
import playground.andreas.gexf.XMLDefaultedgetypeType;
import playground.andreas.gexf.XMLEdgeContent;
import playground.andreas.gexf.XMLEdgesContent;
import playground.andreas.gexf.XMLGexfContent;
import playground.andreas.gexf.XMLGraphContent;
import playground.andreas.gexf.XMLIdtypeType;
import playground.andreas.gexf.XMLModeType;
import playground.andreas.gexf.XMLNodeContent;
import playground.andreas.gexf.XMLNodesContent;
import playground.andreas.gexf.XMLTimeformatType;
import playground.andreas.gexf.viz.PositionContent;
import playground.andreas.utils.pop.SetPersonCoordsToBoundingBox;

/**
 * 
 * @author aneumann
 *
 */
public class Plans2Gexf extends MatsimJaxbXmlWriter{
	
	private static final Logger log = Logger.getLogger(Plans2Gexf.class);
	
	private final static String XSD_PATH = "http://www.gexf.net/1.2draft/gexf.xsd";
	private final double gridSize;
	
	private ObjectFactory gexfFactory;
	private XMLGexfContent gexfContainer;
	
	private HashMap<String, GridNode> gridNodeId2GridNode = new HashMap<String, GridNode>();
	private HashMap<GridNode, HashMap<GridNode, GridEdge>> fromNode2toNode2EdgeMap = new HashMap<GridNode, HashMap<GridNode,GridEdge>>();
	private Set<String> transportModes = new TreeSet<String>();
	private Set<String> actTypes = new TreeSet<String>();
	private XMLAttributesContent edgeContentContainer;
	private XMLAttributesContent nodeContentContainer;

	public Plans2Gexf(double gridSize){
		this.gridSize = gridSize;
		this.gexfFactory = new ObjectFactory();
		this.gexfContainer = this.gexfFactory.createXMLGexfContent();
		
		
	}
	
	private void init(){
		// init graph
		XMLGraphContent graph = this.gexfFactory.createXMLGraphContent();
		graph.setDefaultedgetype(XMLDefaultedgetypeType.DIRECTED);
		graph.setIdtype(XMLIdtypeType.STRING);
		graph.setMode(XMLModeType.DYNAMIC);
		graph.setTimeformat(XMLTimeformatType.DATE_TIME);
		this.gexfContainer.setGraph(graph);

		// init node attributes
		this.nodeContentContainer = new XMLAttributesContent();
		nodeContentContainer.setClazz(XMLClassType.NODE);
		nodeContentContainer.setMode(XMLModeType.DYNAMIC);
		
		XMLAttributeContent nodeContent = new XMLAttributeContent();
		nodeContent.setId("weight");
		nodeContent.setTitle("Number of acts");
		nodeContent.setType(XMLAttrtypeType.FLOAT);
		nodeContentContainer.getAttribute().add(nodeContent);
		
		this.gexfContainer.getGraph().getAttributesOrNodesOrEdges().add(nodeContentContainer);
		
		// init edge attributes
		this.edgeContentContainer = new XMLAttributesContent();
		edgeContentContainer.setClazz(XMLClassType.EDGE);
		edgeContentContainer.setMode(XMLModeType.DYNAMIC);
		
		XMLAttributeContent edgeContent = new XMLAttributeContent();
		edgeContent.setId("weight");
		edgeContent.setTitle("Number of trips");
		edgeContent.setType(XMLAttrtypeType.FLOAT);
		edgeContentContainer.getAttribute().add(edgeContent);
		
		this.gexfContainer.getGraph().getAttributesOrNodesOrEdges().add(edgeContentContainer);
		

		
	}
	
	
	public static void main(String[] args) {
		
		Gbl.startMeasurement();

		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		String networkFile = "f:/p/network_real.xml";
		String inPlansFile = "f:/p/all2all_25min/ITERS/it.30/all2all_25min.30.plans.xml.gz";
		String outFilename = "f:/p/popTest.gexf.gz";

//		Network net = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(networkFile);

		Population inPop = sc.getPopulation();
		PopulationReader popReader = new MatsimPopulationReader(sc);
		popReader.readFile(inPlansFile);
		
		Plans2Gexf p2g = new Plans2Gexf(3000.0);
		p2g.init();
		p2g.parsePopulation(inPop);
		p2g.createNodes();
		p2g.createEdges();
		p2g.write(outFilename);
		
		Gbl.printElapsedTime();
	}

	private void createEdges() {
		
		// add additional attributes
		for (String mode : this.transportModes) {
			XMLAttributeContent edgeContent = new XMLAttributeContent();
			edgeContent.setId(mode);
			edgeContent.setTitle(mode);
			edgeContent.setType(XMLAttrtypeType.FLOAT);
			this.edgeContentContainer.getAttribute().add(edgeContent);
		}
		
		List<Object> attr = this.gexfContainer.getGraph().getAttributesOrNodesOrEdges();
		XMLEdgesContent edges = this.gexfFactory.createXMLEdgesContent();
		attr.add(edges);
		List<XMLEdgeContent> edgeList = edges.getEdge();
		
		for (HashMap<GridNode, GridEdge> toNode2EdgeMap : this.fromNode2toNode2EdgeMap.values()) {
			for (GridEdge edge : toNode2EdgeMap.values()) {
				

				if(edge.getFromNode().getId().toString().equalsIgnoreCase(edge.getToNode().getId().toString())){
					log.debug("Omitting link " + edge.getId().toString() + " Gephi cannot display edges with the same to and fromNode, yet, Sep'11");
				} else {
					XMLEdgeContent e = this.gexfFactory.createXMLEdgeContent();
					e.setId(edge.getId().toString());
					e.setLabel(edge.toString());
					e.setSource(edge.getFromNode().getId().toString());
					e.setTarget(edge.getToNode().getId().toString());
					e.setWeight((float) edge.getnEntries());

					edgeList.add(e);
					
					XMLAttvaluesContent attContent = new XMLAttvaluesContent();
					e.getAttvaluesOrSpellsOrColor().add(attContent);
					
					int nLegs = 0;
					for (String mode : this.transportModes) {
						XMLAttvalue attValue = new XMLAttvalue();
						attValue.setFor(mode);
						attValue.setValue(String.valueOf(edge.getCountForMode(mode)));
						attValue.setStart(Double.toString(0));

						attContent.getAttvalue().add(attValue);
						
						nLegs += edge.getCountForMode(mode);
					}
					
					XMLAttvalue attValue = new XMLAttvalue();
					attValue.setFor("weight");
					attValue.setValue(String.valueOf(nLegs));
					attValue.setStart(Double.toString(0));

					attContent.getAttvalue().add(attValue);
				}
				
			}
		}
	}

	private void createNodes() {
		// add additional attributes
		for (String actType : this.actTypes) {
			XMLAttributeContent nodeContent = new XMLAttributeContent();
			nodeContent.setId(actType);
			nodeContent.setTitle(actType);
			nodeContent.setType(XMLAttrtypeType.FLOAT);
			this.nodeContentContainer.getAttribute().add(nodeContent);
		}
		
		List<Object> attr = this.gexfContainer.getGraph().getAttributesOrNodesOrEdges();
		XMLNodesContent nodes = this.gexfFactory.createXMLNodesContent();
		attr.add(nodes);
		List<XMLNodeContent> nodeList = nodes.getNode();
		
		for (GridNode node : this.gridNodeId2GridNode.values()) {
			XMLNodeContent n = this.gexfFactory.createXMLNodeContent();
			n.setId(node.getId().toString());
			n.setLabel(node.toString());
			
			playground.andreas.gexf.viz.ObjectFactory vizFac = new playground.andreas.gexf.viz.ObjectFactory();
			PositionContent pos = vizFac.createPositionContent();
			pos.setX((float) node.getX());
			pos.setY((float) node.getY());
			pos.setZ((float) 0.0);

			n.getAttvaluesOrSpellsOrNodes().add(pos);
			
			// add values to attributes
			XMLAttvaluesContent attContent = new XMLAttvaluesContent();
			n.getAttvaluesOrSpellsOrNodes().add(attContent);
			
			int nActs = 0;
			for (String actType : this.actTypes) {
				XMLAttvalue attValue = new XMLAttvalue();
				attValue.setFor(actType);
				attValue.setValue(String.valueOf(node.getCountForAct(actType)));
				attValue.setStart(Double.toString(0));

				attContent.getAttvalue().add(attValue);
				
				nActs += node.getCountForAct(actType);
			}			
			
			XMLAttvalue attValue = new XMLAttvalue();
			attValue.setFor("weight");
			attValue.setValue(String.valueOf(nActs));
			attValue.setStart(Double.toString(0));

			attContent.getAttvalue().add(attValue);

			nodeList.add(n);
		}
	}

	private void parsePopulation(Population pop) {
		for (Person person : pop.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			
			GridNode lastNode = null;
			Leg lastLeg = null;
			
			for (PlanElement pE : plan.getPlanElements()) {
				
				if (pE instanceof Activity) {
					Activity act = (Activity) pE;
					this.actTypes.add(act.getType());
					
					if (lastNode == null) {
						lastNode = getNodeFromAct(act);
						lastNode.addActivity(act);
					} else{
						GridNode currentNode = getNodeFromAct(act);
						currentNode.addActivity(act);
						
						if (this.fromNode2toNode2EdgeMap.get(lastNode) == null) {
							this.fromNode2toNode2EdgeMap.put(lastNode, new HashMap<GridNode, GridEdge>());
						}
						
						if (this.fromNode2toNode2EdgeMap.get(lastNode).get(currentNode) == null) {
							this.fromNode2toNode2EdgeMap.get(lastNode).put(currentNode, new GridEdge(lastNode, currentNode));
						}
						
						this.fromNode2toNode2EdgeMap.get(lastNode).get(currentNode).addLeg(lastLeg);						
					}
				}
				
				if (pE instanceof Leg) {
					lastLeg = (Leg) pE;
					this.transportModes.add(lastLeg.getMode());
				}
			}
		}
	}
	
	public void write(String filename) {
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(playground.andreas.gexf.ObjectFactory.class);
			Marshaller m = jc.createMarshaller();
			super.setMarshallerProperties(Plans2Gexf.XSD_PATH, m);
			BufferedWriter bufout = IOUtils.getBufferedWriter(filename);
			m.marshal(this.gexfContainer, bufout);
			bufout.close();
			log.info("Output written to " + filename);
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	private GridNode getNodeFromAct(Activity act) {
		int xSlot = getSpaceSlotForCoord(act.getCoord().getX());
		int ySlot = getSpaceSlotForCoord(act.getCoord().getY());
		
		Id gridNodeId = GridNode.createGridNodeId(xSlot, ySlot);
		
		if (this.gridNodeId2GridNode.get(gridNodeId.toString()) == null) {
			this.gridNodeId2GridNode.put(gridNodeId.toString(), new GridNode(gridNodeId));
		}
		
		return this.gridNodeId2GridNode.get(gridNodeId.toString());
	}

	private int getSpaceSlotForCoord(double coord){
		return (int) (coord / this.gridSize);
	}

}
