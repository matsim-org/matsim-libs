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

package playground.andreas.P2.stats;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

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
 * Uses a {@link CountPPaxHandler} to count passengers per paratransit vehicle and link, {@link CountPCoopHandler} to count cooperatives and their ids and writes them to a gexf network as dynamic link attributes.
 * In addition, writes one column per link and cooperative with its number of passengers served.
 * 
 * @author aneumann
 *
 */
public class GexfPStat extends MatsimJaxbXmlWriter implements StartupListener, IterationEndsListener, ShutdownListener{
	
	private static final Logger log = Logger.getLogger(GexfPStat.class);
	
	private final static String XSD_PATH = "http://www.gexf.net/1.2draft/gexf.xsd";
	private final static String FILENAME = "pStat.gexf.gz";

	private ObjectFactory gexfFactory;
	private XMLGexfContent gexfContainer;

	private CountPPaxHandler globalPaxHandler;
	private CountPCoopHandler coopHandler;
	private String pIdentifier;
	private int getWriteGexfStatsInterval;
	private boolean writeCoopInDetail;

	private HashMap<Id,XMLEdgeContent> edgeMap;
	private HashMap<Id,XMLAttvaluesContent> linkAttributeValueContentMap;

	private HashMap<Id, Integer> linkId2TotalCountsFromLastIteration;
	private HashMap<Id, Set<Id>> linkId2CoopIdsFromLastIteration;
	private HashMap<Id, HashMap<String, Integer>> linkId2LineId2CountsFromLastIteration;

	private Set<String> lastLineIds;

	private XMLAttributesContent edgeAttributeContentsContainer;


	public GexfPStat(PConfigGroup pConfig, boolean writeCoopInDetail){
		this.getWriteGexfStatsInterval = pConfig.getGexfInterval();
		this.writeCoopInDetail = writeCoopInDetail;
		this.pIdentifier = pConfig.getPIdentifier();
		this.lastLineIds = new TreeSet<String>();
		
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
			this.edgeAttributeContentsContainer = edgeAttributeContentsContainer;
			edgeAttributeContentsContainer.setClazz(XMLClassType.EDGE);
			edgeAttributeContentsContainer.setMode(XMLModeType.DYNAMIC);
			
			XMLAttributeContent attributeContent = new XMLAttributeContent();
			attributeContent.setId("weight");
			attributeContent.setTitle("Number of paratransit passengers per iteration");
			attributeContent.setType(XMLAttrtypeType.FLOAT);
			edgeAttributeContentsContainer.getAttribute().add(attributeContent);		
			
			attributeContent = new XMLAttributeContent();
			attributeContent.setId("nCoops");
			attributeContent.setTitle("Number of cooperatives per iteration");
			attributeContent.setType(XMLAttrtypeType.FLOAT);
			edgeAttributeContentsContainer.getAttribute().add(attributeContent);		
			
			attributeContent = new XMLAttributeContent();
			attributeContent.setId("coopIds");
			attributeContent.setTitle("Ids of the cooperatives iteration");
			attributeContent.setType(XMLAttrtypeType.STRING);
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
			this.coopHandler = new CountPCoopHandler(this.pIdentifier);
			event.getControler().getEvents().addHandler(this.coopHandler);
			this.linkId2CoopIdsFromLastIteration = new HashMap<Id, Set<Id>>();
			this.linkId2LineId2CountsFromLastIteration = new HashMap<Id, HashMap<String,Integer>>();
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (this.getWriteGexfStatsInterval > 0) {
			this.addValuesToGexf(event.getIteration(), this.globalPaxHandler, this.coopHandler);
			if ((event.getIteration() % this.getWriteGexfStatsInterval == 0) ) {
				if (writeCoopInDetail) {
					this.write(event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "pStat_detail.gexf.gz"));
				} else {
					this.write(event.getControler().getControlerIO().getIterationFilename(event.getIteration(), GexfPStat.FILENAME));
				}
			}			
		}		
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		if (this.getWriteGexfStatsInterval > 0) {
			if (writeCoopInDetail) {
				this.write(event.getControler().getControlerIO().getOutputFilename("pStat_detail.gexf.gz"));
			} else {
				this.write(event.getControler().getControlerIO().getOutputFilename(GexfPStat.FILENAME));
			}
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

	private void addValuesToGexf(int iteration, CountPPaxHandler paxHandler, CountPCoopHandler coopHandler) {
		for (Entry<Id, XMLAttvaluesContent> linkEntry : this.linkAttributeValueContentMap.entrySet()) {
			
			int countForLink = paxHandler.getPaxCountForLinkId(linkEntry.getKey());
			
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

			Set<Id> coopsForLink = coopHandler.getCoopsForLinkId(linkEntry.getKey());
			
			// Test, if something changed
			if (this.linkId2CoopIdsFromLastIteration.get(linkEntry.getKey()) != null){
				// There is already an entry
				if (this.linkId2CoopIdsFromLastIteration.get(linkEntry.getKey()).equals(coopsForLink)) {
					// same as last iteration - ignore
					continue;
				}
			}
			
			// completely new or not the same
			XMLAttvalue coopIdValue = new XMLAttvalue();
			XMLAttvalue coopCountValue = new XMLAttvalue();
			
			coopIdValue.setFor("coopIds");
			coopCountValue.setFor("nCoops");
			
			if (coopsForLink == null) {
				coopIdValue.setValue("");
				coopCountValue.setValue("0");
			} else {
				
				// Do not use toString
				StringBuffer strB = new StringBuffer();
				for (Id id : coopsForLink) {
					strB.append(id.toString());strB.append(",");
				}
				coopIdValue.setValue(strB.toString());
//				coopIdValue.setValue(coopsForLink.toString());
				coopCountValue.setValue(Integer.toString(coopsForLink.size()));
			}
			
			coopIdValue.setStart(Double.toString(iteration));
			coopCountValue.setStart(Double.toString(iteration));			

			linkEntry.getValue().getAttvalue().add(coopIdValue);
			linkEntry.getValue().getAttvalue().add(coopCountValue);
			this.linkId2CoopIdsFromLastIteration.put(linkEntry.getKey(), coopsForLink);
		}
		
		if (writeCoopInDetail) {
			Set<String> currentLineIds = this.globalPaxHandler.getLineIds();
			// finish all cooperatives who do not exist anymore
			for (String lineId : this.lastLineIds) {
				if (!currentLineIds.contains(lineId)) {
					// does not exist anymore - terminate

					for (Entry<Id, XMLAttvaluesContent> linkEntry : this.linkAttributeValueContentMap.entrySet()) {
						if (this.linkId2LineId2CountsFromLastIteration.get(linkEntry.getKey()) != null){
							if (this.linkId2LineId2CountsFromLastIteration.get(linkEntry.getKey()).get(lineId) != null){
								// There is already an entry
								if (this.linkId2LineId2CountsFromLastIteration.get(linkEntry.getKey()).get(lineId).intValue() == 0) {
									// was already zero - ignore
									continue;
								}
							}
						}

						XMLAttvalue attValue = new XMLAttvalue();
						attValue.setFor(lineId);
						attValue.setValue(Integer.toString(0));
						attValue.setStart(Double.toString(iteration));
						linkEntry.getValue().getAttvalue().add(attValue);
					}
				}
			}

			// add new attribute for all new cooperatives
			for (String lineId : currentLineIds) {
				if (!this.lastLineIds.contains(lineId)) {
					// new cooperative - create new attribute
					XMLAttributeContent attributeContent = new XMLAttributeContent();
					attributeContent.setId(lineId);
					attributeContent.setTitle(lineId);
					attributeContent.setType(XMLAttrtypeType.FLOAT);
					this.edgeAttributeContentsContainer.getAttribute().add(attributeContent);
				}
			}

			// add count values for current line ids
			for (Entry<Id, XMLAttvaluesContent> linkEntry : this.linkAttributeValueContentMap.entrySet()) {

				for (String lineId : currentLineIds) {
					int countForLinkAndLineId = paxHandler.getPaxCountForLinkId(linkEntry.getKey(), lineId);

					if (this.linkId2LineId2CountsFromLastIteration.get(linkEntry.getKey()) != null){
						if (this.linkId2LineId2CountsFromLastIteration.get(linkEntry.getKey()).get(lineId) != null){
							// There is already an entry
							if (this.linkId2LineId2CountsFromLastIteration.get(linkEntry.getKey()).get(lineId).intValue() == countForLinkAndLineId) {
								// same as last iteration - ignore
								continue;
							}
						}
					}

					XMLAttvalue attributeContent = new XMLAttvalue();
					attributeContent.setFor(lineId);
					attributeContent.setValue(Integer.toString(countForLinkAndLineId));
					attributeContent.setStart(Double.toString(iteration));

					linkEntry.getValue().getAttvalue().add(attributeContent);

					if (this.linkId2LineId2CountsFromLastIteration.get(linkEntry.getKey()) == null) {
						this.linkId2LineId2CountsFromLastIteration.put(linkEntry.getKey(), new HashMap<String, Integer>());
					}
					this.linkId2LineId2CountsFromLastIteration.get(linkEntry.getKey()).put(lineId, countForLinkAndLineId);				
				}
			}
			this.lastLineIds = currentLineIds;
		}
	}

	public void write(String filename) {
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(playground.andreas.gexf.ObjectFactory.class);
			Marshaller m = jc.createMarshaller();
			super.setMarshallerProperties(GexfPStat.XSD_PATH, m);
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