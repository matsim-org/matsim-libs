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

package playground.vsp.andreas.utils.ana.nce2gexf;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.io.MatsimNetworkReader;
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
public class Nce2Gexf extends MatsimJaxbXmlWriter{
	
	private static final Logger log = Logger.getLogger(Nce2Gexf.class);
	
	private final static String XSD_PATH = "http://www.gexf.net/1.2draft/gexf.xsd";
	
	private ObjectFactory gexfFactory;
	private XMLGexfContent gexfContainer;
	
	private XMLAttributesContent edgeContentContainer;
	private XMLAttributesContent nodeContentContainer;

	private LinkedList<NceContainer> nceEdges;

	private HashMap<String, Node> stringId2nodeMap;

	public static void main(String[] args) {
		
		Gbl.startMeasurement();
	
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	
		String networkFile = "f:/stab/network.xml";
		String nceDiffFile = "f:/stab/diff.txt";
		String outFilename = "f:/stab/diff.gexf.gz";
		
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFile);
		
		Nce2Gexf p2g = new Nce2Gexf();
		p2g.init(nceDiffFile, sc.getNetwork().getNodes());
		p2g.createNodes();
		p2g.createEdges();
		p2g.write(outFilename);
		
		Gbl.printElapsedTime();
	}

	public Nce2Gexf(){
		this.gexfFactory = new ObjectFactory();
		this.gexfContainer = this.gexfFactory.createXMLGexfContent();
	}
	
	private void init(String nceDiffFile, Map<Id<Node>, ? extends Node> nodes){
		
		ReadNce readNce = new ReadNce(nceDiffFile);
		readNce.parse();
		
		this.stringId2nodeMap = new HashMap<String, Node>();
		for (Node node : nodes.values()) {
			this.stringId2nodeMap.put(node.getId().toString(), node);
		}
		
		this.nceEdges = readNce.getNceContainterList();
		
		// init graph
		XMLGraphContent graph = this.gexfFactory.createXMLGraphContent();
		graph.setDefaultedgetype(XMLDefaultedgetypeType.DIRECTED);
		graph.setIdtype(XMLIdtypeType.STRING);
		graph.setMode(XMLModeType.DYNAMIC);
		graph.setTimeformat(XMLTimeformatType.INTEGER);
		this.gexfContainer.setGraph(graph);

		// init node attributes
		this.nodeContentContainer = new XMLAttributesContent();
		nodeContentContainer.setClazz(XMLClassType.NODE);
		nodeContentContainer.setMode(XMLModeType.DYNAMIC);
		
//		XMLAttributeContent nodeContent = new XMLAttributeContent();
//		nodeContent.setId("weight");
//		nodeContent.setTitle("Number of acts");
//		nodeContent.setType(XMLAttrtypeType.FLOAT);
//		nodeContentContainer.getAttribute().add(nodeContent);
		
		this.gexfContainer.getGraph().getAttributesOrNodesOrEdges().add(nodeContentContainer);
		
		// init edge attributes
		this.edgeContentContainer = new XMLAttributesContent();
		edgeContentContainer.setClazz(XMLClassType.EDGE);
		edgeContentContainer.setMode(XMLModeType.DYNAMIC);
		
		XMLAttributeContent edgeContent = new XMLAttributeContent();
		edgeContent.setId("weight");
		edgeContent.setTitle("diffPerLink");
		edgeContent.setType(XMLAttrtypeType.FLOAT);
		edgeContentContainer.getAttribute().add(edgeContent);
		
		this.gexfContainer.getGraph().getAttributesOrNodesOrEdges().add(edgeContentContainer);
	}
	
	private void createNodes() {
		List<Object> attr = this.gexfContainer.getGraph().getAttributesOrNodesOrEdges();
		XMLNodesContent nodes = this.gexfFactory.createXMLNodesContent();
		attr.add(nodes);
		List<XMLNodeContent> nodeList = nodes.getNode();
		
		for (Entry<String, Node> nodeEntry : this.stringId2nodeMap.entrySet()) {
			XMLNodeContent n = this.gexfFactory.createXMLNodeContent();
			n.setId(nodeEntry.getKey());
			n.setLabel(nodeEntry.getKey());
			
			playground.vsp.gexf.viz.ObjectFactory vizFac = new playground.vsp.gexf.viz.ObjectFactory();
			PositionContent pos = vizFac.createPositionContent();
			pos.setX((float) nodeEntry.getValue().getCoord().getX());
			pos.setY((float) nodeEntry.getValue().getCoord().getY());
			pos.setZ((float) 0.0);
	
			n.getAttvaluesOrSpellsOrNodes().add(pos);
			
			// add values to attributes
//			XMLAttvaluesContent attContent = new XMLAttvaluesContent();
//			n.getAttvaluesOrSpellsOrNodes().add(attContent);
			
//			int nActs = 0;
//			for (String actType : this.actTypes) {
//				XMLAttvalue attValue = new XMLAttvalue();
//				attValue.setFor(actType);
//				attValue.setValue(String.valueOf(node.getCountForAct(actType)));
//				attValue.setStart(Double.toString(0));
//	
//				attContent.getAttvalue().add(attValue);
//				
//				nActs += node.getCountForAct(actType);
//			}			
			
//			XMLAttvalue attValue = new XMLAttvalue();
//			attValue.setFor("weight");
//			attValue.setValue(String.valueOf(nActs));
//			attValue.setStart(Double.toString(0));
//	
//			attContent.getAttvalue().add(attValue);
	
			nodeList.add(n);
		}
	}

	private void createEdges() {
		List<Object> attr = this.gexfContainer.getGraph().getAttributesOrNodesOrEdges();
		XMLEdgesContent edges = this.gexfFactory.createXMLEdgesContent();
		attr.add(edges);
		List<XMLEdgeContent> edgeList = edges.getEdge();
		
		for (NceContainer nceC : this.nceEdges) {
			if(nceC.getFromNodeId().toString().equalsIgnoreCase(nceC.getToNodeId().toString())){
				log.debug("Omitting entry " + nceC);
			} else {
				XMLEdgeContent e = this.gexfFactory.createXMLEdgeContent();
				e.setId(nceC.getFromNodeId() + "-" + nceC.getToNodeId());
				e.setLabel(nceC.getFromNodeId() + "-" + nceC.getToNodeId());
				e.setSource(this.stringId2nodeMap.get(nceC.getFromNodeId().toString()).getId().toString());
				e.setTarget(this.stringId2nodeMap.get(nceC.getToNodeId().toString()).getId().toString());
				e.setWeight((float) nceC.getDiffPerLink());
				
				XMLAttvaluesContent attContent = new XMLAttvaluesContent();
				e.getAttvaluesOrSpellsOrColor().add(attContent);
				
//				int nLegs = 0;
//				for (String mode : this.transportModes) {
//					XMLAttvalue attValue = new XMLAttvalue();
//					attValue.setFor(mode);
//					attValue.setValue(String.valueOf(edge.getCountForMode(mode)));
//					attValue.setStart(Double.toString(0));
//
//					attContent.getAttvalue().add(attValue);
//					
//					nLegs += edge.getCountForMode(mode);
//				}
				
				XMLAttvalue attValue = new XMLAttvalue();
				attValue.setFor("weight");
				attValue.setValue(String.valueOf(nceC.getDiffPerLink()));
				attValue.setStart(Double.toString(0));

				attContent.getAttvalue().add(attValue);
				
//				if (nLegs > 0) {
					edgeList.add(e);
//				}
			}
		}
	}

	@Override
	public void write(String filename) {
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(playground.vsp.gexf.ObjectFactory.class);
			Marshaller m = jc.createMarshaller();
			super.setMarshallerProperties(Nce2Gexf.XSD_PATH, m);
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
}