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

package playground.vsp.andreas.utils.ana.plans2gexf;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlWriter;

import playground.vsp.gexf.ObjectFactory;
import playground.vsp.gexf.XMLAttributeContent;
import playground.vsp.gexf.XMLAttributesContent;
import playground.vsp.gexf.XMLAttrtypeType;
import playground.vsp.gexf.XMLAttvalue;
import playground.vsp.gexf.XMLAttvaluesContent;
import playground.vsp.gexf.XMLClassType;
import playground.vsp.gexf.XMLDefaultedgetypeType;
import playground.vsp.gexf.XMLEdgeContent;
import playground.vsp.gexf.XMLEdgesContent;
import playground.vsp.gexf.XMLGexfContent;
import playground.vsp.gexf.XMLGraphContent;
import playground.vsp.gexf.XMLIdtypeType;
import playground.vsp.gexf.XMLModeType;
import playground.vsp.gexf.XMLNodeContent;
import playground.vsp.gexf.XMLNodesContent;
import playground.vsp.gexf.XMLTimeformatType;
import playground.vsp.gexf.viz.PositionContent;

/**
 * 
 * @author aneumann
 *
 */
public class Plans2Gexf extends MatsimJaxbXmlWriter{
	
	private static final Logger log = Logger.getLogger(Plans2Gexf.class);
	
	private final static String XSD_PATH = "http://www.gexf.net/1.2draft/gexf.xsd";
	private final double gridSize;
	private final Set<String> restrictToTheseLegModes;
	
	private ObjectFactory gexfFactory;
	private XMLGexfContent gexfContainer;
	
	private HashMap<String, GridNode> gridNodeId2GridNode = new HashMap<String, GridNode>();
	private HashMap<GridNode, HashMap<GridNode, GridEdge>> fromNode2toNode2EdgeMap = new HashMap<GridNode, HashMap<GridNode,GridEdge>>();
	private Set<String> transportModes = new TreeSet<String>();
	private Set<String> actTypes = new TreeSet<String>();
	private XMLAttributesContent edgeContentContainer;
	private XMLAttributesContent nodeContentContainer;

	public static void main(String[] args) {
		
		Gbl.startMeasurement();
	
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	
		String networkFile = "f:/p_runs/txl/network.final.xml.gz";
		String inPlansFile = "f:/p_runs/txl/run25/it.380/run25.380.plans.xml.gz";
		String outFilename = "f:/p_runs/txl/run25/it.380/popAna.gexf.gz";
		Set<String> restrictToTheseLegModes = new TreeSet<String>();
		restrictToTheseLegModes.add(TransportMode.pt);
		
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFile);
	
		Population inPop = sc.getPopulation();
		MatsimReader popReader = new PopulationReader(sc);
		popReader.readFile(inPlansFile);
		
		Plans2Gexf p2g = new Plans2Gexf(restrictToTheseLegModes, 500.0);
		p2g.init();
		p2g.parsePopulation(inPop);
		p2g.createNodes();
		p2g.createEdges();
		p2g.write(outFilename);
		
		Gbl.printElapsedTime();
	}

	public Plans2Gexf(Set<String> restrictToTheseLegModes, double gridSize){
		this.restrictToTheseLegModes = restrictToTheseLegModes;
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
						lastNode.addPoint(act.getType(), act.getCoord());
					} else{
						GridNode currentNode = getNodeFromAct(act);
						currentNode.addPoint(act.getType(), act.getCoord());
						
						if (this.fromNode2toNode2EdgeMap.get(lastNode) == null) {
							this.fromNode2toNode2EdgeMap.put(lastNode, new HashMap<GridNode, GridEdge>());
						}
						
						if (this.fromNode2toNode2EdgeMap.get(lastNode).get(currentNode) == null) {
							this.fromNode2toNode2EdgeMap.get(lastNode).put(currentNode, new GridEdge(lastNode, currentNode));
						}
						
						if (restrictToTheseLegModes.contains(lastLeg.getMode())) {
							this.fromNode2toNode2EdgeMap.get(lastNode).get(currentNode).addLeg(lastLeg);
						}
					}
				}
				
				if (pE instanceof Leg) {
					lastLeg = (Leg) pE;
					if (restrictToTheseLegModes.contains(lastLeg.getMode())) {
						this.transportModes.add(lastLeg.getMode());
					}
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
			
			playground.vsp.gexf.viz.ObjectFactory vizFac = new playground.vsp.gexf.viz.ObjectFactory();
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
				attValue.setValue(String.valueOf(node.getCountForType(actType)));
				attValue.setStart(Double.toString(0));
	
				attContent.getAttvalue().add(attValue);
				
				nActs += node.getCountForType(actType);
			}			
			
			XMLAttvalue attValue = new XMLAttvalue();
			attValue.setFor("weight");
			attValue.setValue(String.valueOf(nActs));
			attValue.setStart(Double.toString(0));
	
			attContent.getAttvalue().add(attValue);
	
			nodeList.add(n);
		}
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
					
					if (nLegs > 0) {
						edgeList.add(e);
					}
				}
			}
		}
	}

	public void write(String filename) {
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(playground.vsp.gexf.ObjectFactory.class);
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
		String gridNodeId = GridNode.getGridNodeIdForCoord(act.getCoord(), this.gridSize);
		
		if (this.gridNodeId2GridNode.get(gridNodeId.toString()) == null) {
			this.gridNodeId2GridNode.put(gridNodeId.toString(), new GridNode(gridNodeId));
		}
		
		return this.gridNodeId2GridNode.get(gridNodeId.toString());
	}
}