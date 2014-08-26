/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.andreas.P2.ana.log2something;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlWriter;
import org.matsim.core.utils.misc.Time;

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
 * Transforms {@link PlanElement} into a Gexf file.
 * 
 * @author aneumann
 *
 */
public class PlanElement2Gexf extends MatsimJaxbXmlWriter{
	
	private static final Logger log = Logger.getLogger(PlanElement2Gexf.class);
	
	private final static String XSD_PATH = "http://www.gexf.net/1.2draft/gexf.xsd";

	private ObjectFactory gexfFactory;
	private XMLGexfContent gexfContainer;

	public static void planElement2Gexf(ArrayList<PlanElement> planElements, String outputFile){
		PlanElement2Gexf planElement2Gexf = new PlanElement2Gexf();
		planElement2Gexf.writeAllNodes(planElements);
		planElement2Gexf.write(outputFile);
		
	}
	
	public PlanElement2Gexf(){
		log.info("enabled");
		
		this.gexfFactory = new ObjectFactory();
		this.gexfContainer = this.gexfFactory.createXMLGexfContent();
		
		XMLGraphContent graph = this.gexfFactory.createXMLGraphContent();
		graph.setDefaultedgetype(XMLDefaultedgetypeType.DIRECTED);
		graph.setIdtype(XMLIdtypeType.STRING);
		graph.setMode(XMLModeType.DYNAMIC);
		graph.setTimeformat(XMLTimeformatType.DOUBLE);
		this.gexfContainer.setGraph(graph);
		
		XMLAttributesContent nodeAttributeContentsContainer = new XMLAttributesContent();
		nodeAttributeContentsContainer.setClazz(XMLClassType.NODE);
		nodeAttributeContentsContainer.setMode(XMLModeType.DYNAMIC);
		
		XMLAttributeContent attributeContent = new XMLAttributeContent();
		attributeContent.setId("weight");
		attributeContent.setTitle("Weight");
		attributeContent.setType(XMLAttrtypeType.DOUBLE);
		nodeAttributeContentsContainer.getAttribute().add(attributeContent);		
		
		attributeContent = new XMLAttributeContent();
		attributeContent.setId("planId");
		attributeContent.setTitle("Unique Identifier based on coop and plan id");
		attributeContent.setType(XMLAttrtypeType.STRING);
		nodeAttributeContentsContainer.getAttribute().add(attributeContent);
		
		attributeContent = new XMLAttributeContent();
		attributeContent.setId("creator");
		attributeContent.setTitle("The plan's creator");
		attributeContent.setType(XMLAttrtypeType.STRING);
		nodeAttributeContentsContainer.getAttribute().add(attributeContent);
		
		attributeContent = new XMLAttributeContent();
		attributeContent.setId("status");
		attributeContent.setTitle("Current status of the plan");
		attributeContent.setType(XMLAttrtypeType.STRING);
		nodeAttributeContentsContainer.getAttribute().add(attributeContent);
		
		attributeContent = new XMLAttributeContent();
		attributeContent.setId("found");
		attributeContent.setTitle("Iteration the plan is found");
		attributeContent.setType(XMLAttrtypeType.INTEGER);
		nodeAttributeContentsContainer.getAttribute().add(attributeContent);
		
		attributeContent = new XMLAttributeContent();
		attributeContent.setId("ceased");
		attributeContent.setTitle("Iteration the plan ceased to exist");
		attributeContent.setType(XMLAttrtypeType.INTEGER);
		nodeAttributeContentsContainer.getAttribute().add(attributeContent);
		
		attributeContent = new XMLAttributeContent();
		attributeContent.setId("startTime");
		attributeContent.setTitle("Start time");
		attributeContent.setType(XMLAttrtypeType.STRING);
		nodeAttributeContentsContainer.getAttribute().add(attributeContent);
		
		attributeContent = new XMLAttributeContent();
		attributeContent.setId("endTime");
		attributeContent.setTitle("End time");
		attributeContent.setType(XMLAttrtypeType.STRING);
		nodeAttributeContentsContainer.getAttribute().add(attributeContent);
		
		attributeContent = new XMLAttributeContent();
		attributeContent.setId("stops");
		attributeContent.setTitle("Stops to be served");
		attributeContent.setType(XMLAttrtypeType.STRING);
		nodeAttributeContentsContainer.getAttribute().add(attributeContent);
		
		attributeContent = new XMLAttributeContent();
		attributeContent.setId("nVeh");
		attributeContent.setTitle("Number of vehicles");
		attributeContent.setType(XMLAttrtypeType.INTEGER);
		nodeAttributeContentsContainer.getAttribute().add(attributeContent);
		
		attributeContent = new XMLAttributeContent();
		attributeContent.setId("nPax");
		attributeContent.setTitle("Number of passengers");
		attributeContent.setType(XMLAttrtypeType.INTEGER);
		nodeAttributeContentsContainer.getAttribute().add(attributeContent);
		
		attributeContent = new XMLAttributeContent();
		attributeContent.setId("score");
		attributeContent.setTitle("Score of the plan");
		attributeContent.setType(XMLAttrtypeType.DOUBLE);
		nodeAttributeContentsContainer.getAttribute().add(attributeContent);
		
		attributeContent = new XMLAttributeContent();
		attributeContent.setId("budget");
		attributeContent.setTitle("Budget of the plan's operator");
		attributeContent.setType(XMLAttrtypeType.DOUBLE);
		nodeAttributeContentsContainer.getAttribute().add(attributeContent);
		
		this.gexfContainer.getGraph().getAttributesOrNodesOrEdges().add(nodeAttributeContentsContainer);
	}

	public void write(String filename) {
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(playground.vsp.gexf.ObjectFactory.class);
			Marshaller m = jc.createMarshaller();
			super.setMarshallerProperties(PlanElement2Gexf.XSD_PATH, m);
			BufferedWriter bufout = IOUtils.getBufferedWriter(filename);
			m.marshal(this.gexfContainer, bufout);
			bufout.close();
//			log.info("Output written to " + filename);
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	private void writeAllNodes(ArrayList<PlanElement> planElements) {
		List<Object> attr = this.gexfContainer.getGraph().getAttributesOrNodesOrEdges();
		
		// nodes
		XMLNodesContent nodes = this.gexfFactory.createXMLNodesContent();
		attr.add(nodes);
		List<XMLNodeContent> nodeList = nodes.getNode();
		
		for (PlanElement node : planElements) {
			XMLNodeContent n = this.gexfFactory.createXMLNodeContent();
			n.setId(node.getUniquePlanIdentifier());
			
			playground.vsp.gexf.viz.ObjectFactory vizFac = new playground.vsp.gexf.viz.ObjectFactory();
			PositionContent pos = vizFac.createPositionContent();
			pos.setX((float) node.getIterationFounded());
			pos.setY((float) Integer.parseInt(node.getCoopId().split("_")[1]));
			pos.setZ((float) Integer.parseInt(node.getPlanId().split("_")[1]));

			n.getAttvaluesOrSpellsOrNodes().add(pos);
			
			n.setLabel(node.getUniquePlanIdentifier());
			n.setStart(Integer.toString(node.getIterationFounded()));
			n.setEnd(Integer.toString(node.getIterationCeased()));

			nodeList.add(n);
			
			
			XMLAttvaluesContent attValueContent = new XMLAttvaluesContent();
			n.getAttvaluesOrSpellsOrNodes().add(attValueContent);
			XMLAttvalue attValue = new XMLAttvalue();
			attValue.setFor("weight");
			attValue.setValue(Integer.toString(0));
			attValue.setStart(Double.toString(node.getIterationFounded()));
			attValueContent.getAttvalue().add(attValue);
			
			attValue = new XMLAttvalue();
			attValue.setFor("planId");
			attValue.setValue(node.getPlanId());
			attValueContent.getAttvalue().add(attValue);
			
			attValue = new XMLAttvalue();
			attValue.setFor("creator");
			attValue.setValue(node.getCreatorId());
			attValueContent.getAttvalue().add(attValue);
			
			String lastStatus = "";
			for (Tuple<Integer, String> status : node.getStatus()) {
				if (!lastStatus.equalsIgnoreCase(status.getSecond())) {
					attValue = new XMLAttvalue();
					attValue.setFor("status");
					attValue.setValue(status.getSecond());
					attValue.setStart(Double.toString(status.getFirst()));
					attValueContent.getAttvalue().add(attValue);
					lastStatus = status.getSecond();
				}
			}
			
			attValue = new XMLAttvalue();
			attValue.setFor("found");
			attValue.setValue(Integer.toString(node.getIterationFounded()));
			attValueContent.getAttvalue().add(attValue);
			
			attValue = new XMLAttvalue();
			attValue.setFor("ceased");
			attValue.setValue(Integer.toString(node.getIterationCeased()));
			attValueContent.getAttvalue().add(attValue);
			
			attValue = new XMLAttvalue();
			attValue.setFor("startTime");
			attValue.setValue(Time.writeTime(node.getStartTime()));
			attValueContent.getAttvalue().add(attValue);
			
			attValue = new XMLAttvalue();
			attValue.setFor("endTime");
			attValue.setValue(Time.writeTime(node.getEndTime()));
			attValueContent.getAttvalue().add(attValue);
			
			StringBuffer strB = new StringBuffer();
			for (String stop : node.getStopsToBeServed()) {
				strB.append(stop + ", ");
			}
			
			attValue = new XMLAttvalue();
			attValue.setFor("stops");
			attValue.setValue(strB.toString());
			attValueContent.getAttvalue().add(attValue);
			
			int lastNVeh = Integer.MAX_VALUE;
			for (Tuple<Integer, Integer> nVeh : node.getnVeh()) {
				if (lastNVeh != nVeh.getSecond().intValue()) {
					attValue = new XMLAttvalue();
					attValue.setFor("nVeh");
					attValue.setValue(Integer.toString(nVeh.getSecond()));
					attValue.setStart(Double.toString(nVeh.getFirst()));
					attValueContent.getAttvalue().add(attValue);
					lastNVeh = nVeh.getSecond().intValue();
				}
			}
			
			int lastNPax = Integer.MAX_VALUE;
			for (Tuple<Integer, Integer> nPax : node.getnPax()) {
				if (lastNPax != nPax.getSecond().intValue()) {
					attValue = new XMLAttvalue();
					attValue.setFor("nPax");
					attValue.setValue(Integer.toString(nPax.getSecond()));
					attValue.setStart(Double.toString(nPax.getFirst()));
					attValueContent.getAttvalue().add(attValue);
					lastNPax = nPax.getSecond().intValue();
				}
			}
			
			for (Tuple<Integer, Double> score : node.getScore()) {
				attValue = new XMLAttvalue();
				attValue.setFor("score");
				attValue.setValue(Double.toString(score.getSecond()));
				attValue.setStart(Double.toString(score.getFirst()));
				attValueContent.getAttvalue().add(attValue);
			}
			
			for (Tuple<Integer, Double> budget : node.getBudget()) {
				attValue = new XMLAttvalue();
				attValue.setFor("budget");
				attValue.setValue(Double.toString(budget.getSecond()));
				attValue.setStart(Double.toString(budget.getFirst()));
				attValueContent.getAttvalue().add(attValue);
			}
			
		}
		
		// edges
		XMLEdgesContent edges = this.gexfFactory.createXMLEdgesContent();
		attr.add(edges);
		List<XMLEdgeContent> edgeList = edges.getEdge();
		
		for (PlanElement planElement : planElements) {
			if (planElement.getAncestor() != null) {
				// create one edge from ancestor to child
				XMLEdgeContent e = this.gexfFactory.createXMLEdgeContent();
				e.setId(planElement.getAncestor().getUniquePlanIdentifier() + "-" + planElement.getUniquePlanIdentifier());
				e.setLabel(planElement.getCreatorId());
				e.setSource(planElement.getAncestor().getUniquePlanIdentifier());
				e.setTarget(planElement.getUniquePlanIdentifier());
				e.setWeight(new Float(1.0));
				edgeList.add(e);
			}
		}
	}
}