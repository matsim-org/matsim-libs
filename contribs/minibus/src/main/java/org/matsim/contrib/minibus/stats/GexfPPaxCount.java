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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.genericUtils.gexf.*;
import org.matsim.contrib.minibus.genericUtils.gexf.viz.PositionContent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

/**
 * Uses a {@link CountPPaxHandler} to count passengers per paratransit vehicle and link and writes them to a gexf network as dynamic link attributes.
 * 
 * @author aneumann
 *
 */
@Deprecated
final class GexfPPaxCount extends MatsimJaxbXmlWriter implements StartupListener, IterationEndsListener, ShutdownListener{
	
	private static final Logger log = Logger.getLogger(GexfPPaxCount.class);
	
	private final static String XSD_PATH = "http://www.gexf.net/1.2draft/gexf.xsd";
	private final static String FILENAME = "pPaxCount.gexf.gz";

	private ObjectFactory gexfFactory;
	private XMLGexfContent gexfContainer;

	private CountPPaxHandler eventsHandler;
	private final String pIdentifier;
	private final int getWriteGexfStatsInterval;

	private HashMap<Id<Link>, XMLEdgeContent> edgeMap;
	private HashMap<Id<Link>, XMLAttvaluesContent> attValueContentMap;

	private HashMap<Id<Link>, Integer> linkId2CountsFromLastIteration;

	private GexfPPaxCount(PConfigGroup pConfig){
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
			
			XMLAttributesContent attsContent = new XMLAttributesContent();
			attsContent.setClazz(XMLClassType.EDGE);
			attsContent.setMode(XMLModeType.DYNAMIC);
			
			XMLAttributeContent attContent = new XMLAttributeContent();
			attContent.setId("weight");
			attContent.setTitle("Number of paratransit passengers per iteration");
			attContent.setType(XMLAttrtypeType.FLOAT);
			
			attsContent.getAttribute().add(attContent);		
			this.gexfContainer.getGraph().getAttributesOrNodesOrEdges().add(attsContent);
		}
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		if (this.getWriteGexfStatsInterval > 0) {
            this.addNetworkAsLayer(event.getServices().getScenario().getNetwork(), 0);
			this.createAttValues();
			this.eventsHandler = new CountPPaxHandler(this.pIdentifier);
			event.getServices().getEvents().addHandler(this.eventsHandler);
			this.linkId2CountsFromLastIteration = new HashMap<>();
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (this.getWriteGexfStatsInterval > 0) {
			this.addValuesToGexf(event.getIteration(), this.eventsHandler);
			if ((event.getIteration() % this.getWriteGexfStatsInterval == 0) ) {
				this.write(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), GexfPPaxCount.FILENAME));
			}			
		}		
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		if (this.getWriteGexfStatsInterval > 0) {
			this.write(event.getServices().getControlerIO().getOutputFilename(GexfPPaxCount.FILENAME));
		}		
	}

	private void createAttValues() {
		this.attValueContentMap = new HashMap<>();
		
		for (Entry<Id<Link>, XMLEdgeContent> entry : this.edgeMap.entrySet()) {
			XMLAttvaluesContent attValueContent = new XMLAttvaluesContent();
			entry.getValue().getAttvaluesOrSpellsOrColor().add(attValueContent);
			this.attValueContentMap.put(entry.getKey(), attValueContent);
		}		
	}

	private void addValuesToGexf(int iteration, CountPPaxHandler handler) {
		for (Entry<Id<Link>, XMLAttvaluesContent> entry : this.attValueContentMap.entrySet()) {
			
			int countForLink = handler.getPaxCountForLinkId(entry.getKey());
			
			if (this.linkId2CountsFromLastIteration.get(entry.getKey()) != null){
				// There is already an entry
				if (this.linkId2CountsFromLastIteration.get(entry.getKey()) == countForLink) {
					// same as last iteration - ignore
					continue;
				}
			}
			
			XMLAttvalue attValue = new XMLAttvalue();
			attValue.setFor("weight");
//			attValue.setValue(Integer.toString(Math.max(1, countForLink)));
			attValue.setValue(Integer.toString(countForLink));
			attValue.setStart(Double.toString(iteration));
			attValue.setEnd(Double.toString(iteration));

			entry.getValue().getAttvalue().add(attValue);
			this.linkId2CountsFromLastIteration.put(entry.getKey(), countForLink);
		}
	}

	public void write(String filename) {
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(org.matsim.contrib.minibus.genericUtils.gexf.ObjectFactory.class);
			Marshaller m = jc.createMarshaller();
			super.setMarshallerProperties(GexfPPaxCount.XSD_PATH, m);
			BufferedWriter bufout = IOUtils.getBufferedWriter(filename);
			m.marshal(this.gexfContainer, bufout);
			bufout.close();
			log.info("Output written to " + filename);
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