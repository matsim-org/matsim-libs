/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.andreas.P2.stats.gexfPStats;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlWriter;

import playground.andreas.P2.helper.PConfigGroup;
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

/**
 * Uses a {@link CountPPaxHandler} to count passengers per paratransit vehicle and link, and writes them to a gexf network as dynamic link attributes.
 * 
 * @author aneumann
 *
 */
public class GexfPStatLight extends MatsimJaxbXmlWriter implements StartupListener, IterationEndsListener, ShutdownListener{
	
	private static final Logger log = Logger.getLogger(GexfPStatLight.class);
	
	private final static String XSD_PATH = "http://www.gexf.net/1.2draft/gexf.xsd";
	private final static String FILENAME = "pStat_light.gexf.gz";

	private ObjectFactory gexfFactory;
	private XMLGexfContent gexfContainer;

	private CountPPaxHandler globalPaxHandler;
	private CountPVehHandler vehHandler;
	private String pIdentifier;
	private int getWriteGexfStatsInterval;

	private HashMap<Id,XMLEdgeContent> edgeMap;
	private HashMap<Id,XMLAttvaluesContent> linkAttributeValueContentMap;

	private HashMap<Id, Integer> linkId2TotalCountsFromLastIteration;
	private HashMap<Id, Integer> linkId2VehCountsFromLastIteration;


	public GexfPStatLight(PConfigGroup pConfig){
		this.getWriteGexfStatsInterval = pConfig.getGexfInterval();
		this.pIdentifier = pConfig.getPIdentifier();
		
		if (this.getWriteGexfStatsInterval > 0) {
			log.info("enabled");

			this.gexfFactory = new ObjectFactory();
			this.gexfContainer = this.gexfFactory.createXMLGexfContent();
		
			XMLGraphContent graph = this.gexfFactory.createXMLGraphContent();
			graph.setDefaultedgetype(XMLDefaultedgetypeType.DIRECTED);
			graph.setIdtype(XMLIdtypeType.STRING);
			graph.setMode(XMLModeType.DYNAMIC);
			graph.setTimeformat(XMLTimeformatType.DOUBLE);
			this.gexfContainer.setGraph(graph);
			
			XMLAttributesContent edgeAttributeContentsContainer = new XMLAttributesContent();
			edgeAttributeContentsContainer.setClazz(XMLClassType.EDGE);
			edgeAttributeContentsContainer.setMode(XMLModeType.DYNAMIC);
			
			XMLAttributeContent attributeContent = new XMLAttributeContent();
			attributeContent.setId("weight");
			attributeContent.setTitle("Number of paratransit passengers per iteration");
			attributeContent.setType(XMLAttrtypeType.FLOAT);
			edgeAttributeContentsContainer.getAttribute().add(attributeContent);		
			
			attributeContent = new XMLAttributeContent();
			attributeContent.setId("nVeh");
			attributeContent.setTitle("Number of paratransit vehicles per iteration");
			attributeContent.setType(XMLAttrtypeType.FLOAT);
			edgeAttributeContentsContainer.getAttribute().add(attributeContent);
			
			this.gexfContainer.getGraph().getAttributesOrNodesOrEdges().add(edgeAttributeContentsContainer);
		}
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		if (this.getWriteGexfStatsInterval > 0) {
			this.addNetworkAsLayer(event.getControler().getNetwork(), 0);
			this.createAttValues();
			this.globalPaxHandler = new CountPPaxHandler(this.pIdentifier);
			event.getControler().getEvents().addHandler(this.globalPaxHandler);
			this.linkId2TotalCountsFromLastIteration = new HashMap<Id, Integer>();			
			this.vehHandler = new CountPVehHandler(this.pIdentifier);
			event.getControler().getEvents().addHandler(this.vehHandler);
			this.linkId2VehCountsFromLastIteration = new HashMap<Id, Integer>();
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (this.getWriteGexfStatsInterval > 0) {
			this.addValuesToGexf(event.getIteration());
			if ((event.getIteration() % this.getWriteGexfStatsInterval == 0) ) {
				this.write(event.getControler().getControlerIO().getIterationFilename(event.getIteration(), GexfPStatLight.FILENAME));
			}			
		}		
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		if (this.getWriteGexfStatsInterval > 0) {
			this.write(event.getControler().getControlerIO().getOutputFilename(GexfPStatLight.FILENAME));
		}		
	}

	private void createAttValues() {
		this.linkAttributeValueContentMap = new HashMap<Id, XMLAttvaluesContent>();
		
		for (Entry<Id, XMLEdgeContent> entry : this.edgeMap.entrySet()) {
			XMLAttvaluesContent attValueContent = new XMLAttvaluesContent();
			entry.getValue().getAttvaluesOrSpellsOrColor().add(attValueContent);
			this.linkAttributeValueContentMap.put(entry.getKey(), attValueContent);
		}		
	}

	private void addValuesToGexf(int iteration) {
		for (Entry<Id, XMLAttvaluesContent> linkEntry : this.linkAttributeValueContentMap.entrySet()) {
			
			int countForLink = this.globalPaxHandler.getPaxCountForLinkId(linkEntry.getKey());
			
			if (this.linkId2TotalCountsFromLastIteration.get(linkEntry.getKey()) != null){
				// There is already an entry
				if (this.linkId2TotalCountsFromLastIteration.get(linkEntry.getKey()).intValue() == countForLink) {
					// same as last iteration - ignore
					continue;
				}
			}
			
			XMLAttvalue attValue = new XMLAttvalue();
			attValue.setFor("weight");
			attValue.setValue(Integer.toString(countForLink));
			attValue.setStart(Double.toString(iteration));

			linkEntry.getValue().getAttvalue().add(attValue);
			this.linkId2TotalCountsFromLastIteration.put(linkEntry.getKey(), countForLink);
		}		
		
		for (Entry<Id, XMLAttvaluesContent> linkEntry : this.linkAttributeValueContentMap.entrySet()) {
			
			int countForLink = this.vehHandler.getVehCountForLinkId(linkEntry.getKey());
			
			if (this.linkId2VehCountsFromLastIteration.get(linkEntry.getKey()) != null){
				// There is already an entry
				if (this.linkId2VehCountsFromLastIteration.get(linkEntry.getKey()).intValue() == countForLink) {
					// same as last iteration - ignore
					continue;
				}
			}
			
			XMLAttvalue attValue = new XMLAttvalue();
			attValue.setFor("nVeh");
			attValue.setValue(Integer.toString(countForLink));
			attValue.setStart(Double.toString(iteration));

			linkEntry.getValue().getAttvalue().add(attValue);
			this.linkId2VehCountsFromLastIteration.put(linkEntry.getKey(), countForLink);
		}
	}

	public void write(String filename) {
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(playground.andreas.gexf.ObjectFactory.class);
			Marshaller m = jc.createMarshaller();
			super.setMarshallerProperties(GexfPStatLight.XSD_PATH, m);
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

	private void addNetworkAsLayer(Network network, int zCoord) {
		List<Object> attr = this.gexfContainer.getGraph().getAttributesOrNodesOrEdges();
		
		// nodes
		XMLNodesContent nodes = this.gexfFactory.createXMLNodesContent();
		attr.add(nodes);
		List<XMLNodeContent> nodeList = nodes.getNode();
		
		for (Node node : network.getNodes().values()) {
			XMLNodeContent n = this.gexfFactory.createXMLNodeContent();
			n.setId(node.getId().toString());
			
			playground.andreas.gexf.viz.ObjectFactory vizFac = new playground.andreas.gexf.viz.ObjectFactory();
			PositionContent pos = vizFac.createPositionContent();
			pos.setX((float) node.getCoord().getX());
			pos.setY((float) node.getCoord().getY());
			pos.setZ((float) zCoord);

			n.getAttvaluesOrSpellsOrNodes().add(pos);

			nodeList.add(n);
		}
		
		// edges
		XMLEdgesContent edges = this.gexfFactory.createXMLEdgesContent();
		attr.add(edges);
		List<XMLEdgeContent> edgeList = edges.getEdge();
		
		this.edgeMap = new HashMap<Id, XMLEdgeContent>();
		
		for (Link link : network.getLinks().values()) {
			
			if(link.getFromNode().getId().toString().equalsIgnoreCase(link.getToNode().getId().toString())){
				log.debug("Omitting link " + link.getId().toString() + " Gephi cannot display edges with the same to and fromNode, yet, Sep'11");
			} else {
				XMLEdgeContent e = this.gexfFactory.createXMLEdgeContent();
				e.setId(link.getId().toString());
				e.setLabel("network link");
				e.setSource(link.getFromNode().getId().toString());
				e.setTarget(link.getToNode().getId().toString());
				e.setWeight(new Float(1.0));

				edgeList.add(e);

				edgeMap.put(link.getId(), e);
			}
		}		
	}
}