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

package playground.andreas.P2.stats.gexfPStats.deprecated;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

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
import playground.andreas.P2.stats.gexfPStats.CountPCoopHandler;
import playground.andreas.P2.stats.gexfPStats.CountPPaxHandler;
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
 * Uses a {@link CountPPaxHandler} to count passengers per paratransit vehicle and link and writes them to a gexf network as dynamic link attributes.
 * 
 * @author aneumann
 *
 */
@Deprecated
public class GexfPCoopCount extends MatsimJaxbXmlWriter implements StartupListener, IterationEndsListener, ShutdownListener{
	
	private static final Logger log = Logger.getLogger(GexfPCoopCount.class);
	
	private final static String XSD_PATH = "http://www.gexf.net/1.2draft/gexf.xsd";
	private final static String FILENAME = "pCoopCount.gexf.gz";

	private ObjectFactory gexfFactory;
	private XMLGexfContent gexfContainer;

	private CountPCoopHandler eventsHandler;
	private String pIdentifier;
	private int getWriteGexfStatsInterval;

	private HashMap<Id,XMLEdgeContent> edgeMap;
	private HashMap<Id,XMLAttvaluesContent> attValueContentMap;

	private HashMap<Id, Set<Id>> linkId2CoopIdsFromLastIteration;

	public GexfPCoopCount(PConfigGroup pConfig){
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
			
			XMLAttributesContent attsContent = new XMLAttributesContent();
			attsContent.setClazz(XMLClassType.EDGE);
			attsContent.setMode(XMLModeType.DYNAMIC);
			
			XMLAttributeContent attContent = new XMLAttributeContent();
			attContent.setId("weight");
			attContent.setTitle("Number of cooperatives per iteration");
			attContent.setType(XMLAttrtypeType.FLOAT);
			
			attsContent.getAttribute().add(attContent);		
			this.gexfContainer.getGraph().getAttributesOrNodesOrEdges().add(attsContent);
			
			attContent = new XMLAttributeContent();
			attContent.setId("coopIds");
			attContent.setTitle("Ids of the cooperatives iteration");
			attContent.setType(XMLAttrtypeType.STRING);
			
			attsContent.getAttribute().add(attContent);		
			this.gexfContainer.getGraph().getAttributesOrNodesOrEdges().add(attsContent);
		}
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		if (this.getWriteGexfStatsInterval > 0) {
			this.addNetworkAsLayer(event.getControler().getNetwork(), 0);
			this.createAttValues();
			this.eventsHandler = new CountPCoopHandler(this.pIdentifier);
			event.getControler().getEvents().addHandler(this.eventsHandler);
			this.linkId2CoopIdsFromLastIteration = new HashMap<Id, Set<Id>>();
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (this.getWriteGexfStatsInterval > 0) {
			this.addValuesToGexf(event.getIteration(), this.eventsHandler);
			if ((event.getIteration() % this.getWriteGexfStatsInterval == 0) ) {
				this.write(event.getControler().getControlerIO().getIterationFilename(event.getIteration(), GexfPCoopCount.FILENAME));
			}			
		}		
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		if (this.getWriteGexfStatsInterval > 0) {
			this.write(event.getControler().getControlerIO().getOutputFilename(GexfPCoopCount.FILENAME));
		}		
	}

	private void createAttValues() {
		this.attValueContentMap = new HashMap<Id, XMLAttvaluesContent>();
		
		for (Entry<Id, XMLEdgeContent> entry : this.edgeMap.entrySet()) {
			XMLAttvaluesContent attValueContent = new XMLAttvaluesContent();
			entry.getValue().getAttvaluesOrSpellsOrColor().add(attValueContent);
			this.attValueContentMap.put(entry.getKey(), attValueContent);
		}		
	}

	private void addValuesToGexf(int iteration, CountPCoopHandler handler) {
		for (Entry<Id, XMLAttvaluesContent> entry : this.attValueContentMap.entrySet()) {
			
			Set<Id> coopsForLink = handler.getCoopsForLinkId(entry.getKey());
			
			// Test, if something changed
			if (this.linkId2CoopIdsFromLastIteration.get(entry.getKey()) != null){
				// There is already an entry
				if (this.linkId2CoopIdsFromLastIteration.get(entry.getKey()).equals(coopsForLink)) {
					// same as last iteration - ignore
					continue;
				}
			}
			
			// completely new or not the same
			XMLAttvalue coopIdValue = new XMLAttvalue();
			XMLAttvalue coopCountValue = new XMLAttvalue();
			
			coopIdValue.setFor("coopIds");
			coopCountValue.setFor("weight");
			
			if (coopsForLink == null) {
				coopIdValue.setValue("");
				coopCountValue.setValue("0");
			} else {
				coopIdValue.setValue(coopsForLink.toString());
				coopCountValue.setValue(Integer.toString(coopsForLink.size()));
			}
			
			coopIdValue.setStart(Double.toString(iteration));
			coopCountValue.setStart(Double.toString(iteration));			

			entry.getValue().getAttvalue().add(coopIdValue);
			entry.getValue().getAttvalue().add(coopCountValue);
			this.linkId2CoopIdsFromLastIteration.put(entry.getKey(), coopsForLink);
		}
	}

	public void write(String filename) {
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(playground.vsp.gexf.ObjectFactory.class);
			Marshaller m = jc.createMarshaller();
			super.setMarshallerProperties(GexfPCoopCount.XSD_PATH, m);
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

	private void addNetworkAsLayer(Network network, int zCoord) {
		List<Object> attr = this.gexfContainer.getGraph().getAttributesOrNodesOrEdges();
		
		// nodes
		XMLNodesContent nodes = this.gexfFactory.createXMLNodesContent();
		attr.add(nodes);
		List<XMLNodeContent> nodeList = nodes.getNode();
		
		for (Node node : network.getNodes().values()) {
			XMLNodeContent n = this.gexfFactory.createXMLNodeContent();
			n.setId(node.getId().toString());
			
			playground.vsp.gexf.viz.ObjectFactory vizFac = new playground.vsp.gexf.viz.ObjectFactory();
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