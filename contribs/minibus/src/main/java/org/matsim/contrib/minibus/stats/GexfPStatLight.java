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

package org.matsim.contrib.minibus.stats;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.genericUtils.gexf.ObjectFactory;
import org.matsim.contrib.minibus.genericUtils.gexf.XMLAttributeContent;
import org.matsim.contrib.minibus.genericUtils.gexf.XMLAttributesContent;
import org.matsim.contrib.minibus.genericUtils.gexf.XMLAttrtypeType;
import org.matsim.contrib.minibus.genericUtils.gexf.XMLAttvalue;
import org.matsim.contrib.minibus.genericUtils.gexf.XMLAttvaluesContent;
import org.matsim.contrib.minibus.genericUtils.gexf.XMLClassType;
import org.matsim.contrib.minibus.genericUtils.gexf.XMLDefaultedgetypeType;
import org.matsim.contrib.minibus.genericUtils.gexf.XMLEdgeContent;
import org.matsim.contrib.minibus.genericUtils.gexf.XMLEdgesContent;
import org.matsim.contrib.minibus.genericUtils.gexf.XMLGexfContent;
import org.matsim.contrib.minibus.genericUtils.gexf.XMLGraphContent;
import org.matsim.contrib.minibus.genericUtils.gexf.XMLIdtypeType;
import org.matsim.contrib.minibus.genericUtils.gexf.XMLModeType;
import org.matsim.contrib.minibus.genericUtils.gexf.XMLNodeContent;
import org.matsim.contrib.minibus.genericUtils.gexf.XMLNodesContent;
import org.matsim.contrib.minibus.genericUtils.gexf.XMLTimeformatType;
import org.matsim.contrib.minibus.genericUtils.gexf.viz.PositionContent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlWriter;

/**
 * Uses a {@link CountPPaxHandler} to count passengers per paratransit vehicle and link, and writes them to a gexf network as dynamic link attributes.
 * 
 * @author aneumann
 *
 */
final class GexfPStatLight extends MatsimJaxbXmlWriter implements StartupListener, IterationEndsListener, ShutdownListener{
	
	private static final Logger log = Logger.getLogger(GexfPStatLight.class);
	
	private final static String XSD_PATH = "http://www.gexf.net/1.2draft/gexf.xsd";
	private final static String FILENAME = "pStat_light.gexf.gz";

	private ObjectFactory gexfFactory;
	private XMLGexfContent gexfContainer;

	private CountPPaxHandler globalPaxHandler;
	private CountPVehHandler vehHandler;
	private final String pIdentifier;
	private final int getWriteGexfStatsInterval;

	private HashMap<Id<Link>, XMLEdgeContent> edgeMap;
	private HashMap<Id<Link>, XMLAttvaluesContent> linkAttributeValueContentMap;

	private HashMap<Id<Link>, Integer> linkId2TotalCountsFromLastIteration;
	private HashMap<Id<Link>, Integer> linkId2VehCountsFromLastIteration;


	@Inject GexfPStatLight(PConfigGroup pConfig){
		this.getWriteGexfStatsInterval = pConfig.getGexfInterval();
		this.pIdentifier = pConfig.getPIdentifier();
		
		if (this.getWriteGexfStatsInterval > 0) {
			log.info("enabled");

			this.gexfFactory = new ObjectFactory();
			this.gexfContainer = this.gexfFactory.createXMLGexfContent();
			this.gexfContainer.setVersion("1.2");
		
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
            this.addNetworkAsLayer(event.getServices().getScenario().getNetwork(), 0);
			this.createAttValues();
			this.globalPaxHandler = new CountPPaxHandler(this.pIdentifier);
			event.getServices().getEvents().addHandler(this.globalPaxHandler);
			this.linkId2TotalCountsFromLastIteration = new HashMap<>();			
			this.vehHandler = new CountPVehHandler(this.pIdentifier);
			event.getServices().getEvents().addHandler(this.vehHandler);
			this.linkId2VehCountsFromLastIteration = new HashMap<>();
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (this.getWriteGexfStatsInterval > 0) {
			this.addValuesToGexf(event.getIteration());
			if ((event.getIteration() % this.getWriteGexfStatsInterval == 0) ) {
				this.write(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), GexfPStatLight.FILENAME));
			}			
		}		
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		if (this.getWriteGexfStatsInterval > 0) {
			this.write(event.getServices().getControlerIO().getOutputFilename(GexfPStatLight.FILENAME));
		}		
	}

	private void createAttValues() {
		this.linkAttributeValueContentMap = new HashMap<>();
		
		for (Entry<Id<Link>, XMLEdgeContent> entry : this.edgeMap.entrySet()) {
			XMLAttvaluesContent attValueContent = new XMLAttvaluesContent();
			entry.getValue().getAttvaluesOrSpellsOrColor().add(attValueContent);
			this.linkAttributeValueContentMap.put(entry.getKey(), attValueContent);
		}		
	}

	private void addValuesToGexf(int iteration) {
		for (Entry<Id<Link>, XMLAttvaluesContent> linkEntry : this.linkAttributeValueContentMap.entrySet()) {
			
			int countForLink = this.globalPaxHandler.getPaxCountForLinkId(linkEntry.getKey());
			
			if (this.linkId2TotalCountsFromLastIteration.get(linkEntry.getKey()) != null){
				// There is already an entry
				if (this.linkId2TotalCountsFromLastIteration.get(linkEntry.getKey()) == countForLink) {
					// same as last iteration - ignore
					continue;
				}
			}
			
			XMLAttvalue attValue = new XMLAttvalue();
			attValue.setFor("weight");
			attValue.setValue(Integer.toString(countForLink));
			attValue.setStart(Double.toString(iteration));
			attValue.setEnd(Double.toString(iteration));

			linkEntry.getValue().getAttvalue().add(attValue);
			this.linkId2TotalCountsFromLastIteration.put(linkEntry.getKey(), countForLink);
		}		
		
		for (Entry<Id<Link>, XMLAttvaluesContent> linkEntry : this.linkAttributeValueContentMap.entrySet()) {
			
			int countForLink = this.vehHandler.getVehCountForLinkId(linkEntry.getKey());
			
			if (this.linkId2VehCountsFromLastIteration.get(linkEntry.getKey()) != null){
				// There is already an entry
				if (this.linkId2VehCountsFromLastIteration.get(linkEntry.getKey()) == countForLink) {
					// same as last iteration - ignore
					continue;
				}
			}
			
			XMLAttvalue attValue = new XMLAttvalue();
			attValue.setFor("nVeh");
			attValue.setValue(Integer.toString(countForLink));
			attValue.setStart(Double.toString(iteration));
			attValue.setEnd(Double.toString(iteration));

			linkEntry.getValue().getAttvalue().add(attValue);
			this.linkId2VehCountsFromLastIteration.put(linkEntry.getKey(), countForLink);
		}
	}

	public void write(String filename) {
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(org.matsim.contrib.minibus.genericUtils.gexf.ObjectFactory.class);
			Marshaller m = jc.createMarshaller();
			super.setMarshallerProperties(GexfPStatLight.XSD_PATH, m);
			BufferedWriter bufout = IOUtils.getBufferedWriter(filename);
			m.marshal(this.gexfContainer, bufout);
			bufout.close();
//			log.info("Output written to " + filename);
		} catch (JAXBException e) {
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
			
			org.matsim.contrib.minibus.genericUtils.gexf.viz.ObjectFactory vizFac = new org.matsim.contrib.minibus.genericUtils.gexf.viz.ObjectFactory();
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
		
		this.edgeMap = new HashMap<>();
		
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