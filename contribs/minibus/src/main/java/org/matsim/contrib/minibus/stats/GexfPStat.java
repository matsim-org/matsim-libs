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
import org.matsim.contrib.minibus.operator.Operator;
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
import java.util.Set;
import java.util.TreeSet;

/**
 * Uses a {@link CountPPaxHandler} to count passengers per paratransit vehicle and link, {@link CountPOperatorHandler} to count operators and their ids and writes them to a gexf network as dynamic link attributes.
 * In addition, writes one column per link and operator with its number of passengers served.
 * 
 * @author aneumann
 *
 */
final class GexfPStat extends MatsimJaxbXmlWriter implements StartupListener, IterationEndsListener, ShutdownListener{
	
	private static final Logger log = Logger.getLogger(GexfPStat.class);
	
	private final static String XSD_PATH = "http://www.gexf.net/1.2draft/gexf.xsd";
	private final static String FILENAME = "pStat.gexf.gz";

	private ObjectFactory gexfFactory;
	private XMLGexfContent gexfContainer;

	private CountPPaxHandler globalPaxHandler;
	private CountPOperatorHandler operatorHandler;
	private CountPVehHandler vehHandler;
	private final String pIdentifier;
	private final int getWriteGexfStatsInterval;
	private final boolean writeOperatorInDetail;

	private HashMap<Id<Link>, XMLEdgeContent> edgeMap;
	private HashMap<Id<Link>, XMLAttvaluesContent> linkAttributeValueContentMap;

	private HashMap<Id<Link>, Integer> linkId2TotalCountsFromLastIteration;
	private HashMap<Id<Link>, Set<Id<Operator>>> linkId2OperatorIdsFromLastIteration;
	private HashMap<Id<Link>, HashMap<String, Integer>> linkId2LineId2CountsFromLastIteration;
	private HashMap<Id<Link>, Integer> linkId2VehCountsFromLastIteration;

	private Set<String> lastLineIds;

	private XMLAttributesContent edgeAttributeContentsContainer;


	public GexfPStat(boolean writeOperatorInDetail, PConfigGroup pConfig){
		this.getWriteGexfStatsInterval = pConfig.getGexfInterval();
		this.writeOperatorInDetail = writeOperatorInDetail;
		this.pIdentifier = pConfig.getPIdentifier();
		this.lastLineIds = new TreeSet<>();
		
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
			this.edgeAttributeContentsContainer = edgeAttributeContentsContainer;
			edgeAttributeContentsContainer.setClazz(XMLClassType.EDGE);
			edgeAttributeContentsContainer.setMode(XMLModeType.DYNAMIC);
			
			XMLAttributeContent attributeContent = new XMLAttributeContent();
			attributeContent.setId("weight");
			attributeContent.setTitle("Number of paratransit passengers per iteration");
			attributeContent.setType(XMLAttrtypeType.FLOAT);
			edgeAttributeContentsContainer.getAttribute().add(attributeContent);		
			
			attributeContent = new XMLAttributeContent();
			attributeContent.setId("nOperators");
			attributeContent.setTitle("Number of operators per iteration");
			attributeContent.setType(XMLAttrtypeType.FLOAT);
			edgeAttributeContentsContainer.getAttribute().add(attributeContent);		
			
			attributeContent = new XMLAttributeContent();
			attributeContent.setId("operatorIds");
			attributeContent.setTitle("Ids of the operators iteration");
			attributeContent.setType(XMLAttrtypeType.STRING);
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
			this.operatorHandler = new CountPOperatorHandler(this.pIdentifier);
			event.getServices().getEvents().addHandler(this.operatorHandler);
			this.vehHandler = new CountPVehHandler(this.pIdentifier);
			event.getServices().getEvents().addHandler(this.vehHandler);
			this.linkId2OperatorIdsFromLastIteration = new HashMap<>();
			this.linkId2LineId2CountsFromLastIteration = new HashMap<>();
			this.linkId2VehCountsFromLastIteration = new HashMap<>();
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (this.getWriteGexfStatsInterval > 0) {
			this.addValuesToGexf(event.getIteration(), this.globalPaxHandler, this.operatorHandler);
			if ((event.getIteration() % this.getWriteGexfStatsInterval == 0) ) {
				if (writeOperatorInDetail) {
					this.write(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "pStat_detail.gexf.gz"));
				} else {
					this.write(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), GexfPStat.FILENAME));
				}
			}			
		}		
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		if (this.getWriteGexfStatsInterval > 0) {
			if (writeOperatorInDetail) {
				this.write(event.getServices().getControlerIO().getOutputFilename("pStat_detail.gexf.gz"));
			} else {
				this.write(event.getServices().getControlerIO().getOutputFilename(GexfPStat.FILENAME));
			}
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

	private void addValuesToGexf(int iteration, CountPPaxHandler paxHandler, CountPOperatorHandler operatorHandler) {
		for (Entry<Id<Link>, XMLAttvaluesContent> linkEntry : this.linkAttributeValueContentMap.entrySet()) {
			
			int countForLink = paxHandler.getPaxCountForLinkId(linkEntry.getKey());
			
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

			Set<Id<Operator>> operatorsForLink = operatorHandler.getOperatorIdsForLinkId(linkEntry.getKey());
			
			// Test, if something changed
			if (this.linkId2OperatorIdsFromLastIteration.get(linkEntry.getKey()) != null){
				// There is already an entry
				if (this.linkId2OperatorIdsFromLastIteration.get(linkEntry.getKey()).equals(operatorsForLink)) {
					// same as last iteration - ignore
					continue;
				}
			}
			
			// completely new or not the same
			XMLAttvalue operatorIdValue = new XMLAttvalue();
			XMLAttvalue operatorCountValue = new XMLAttvalue();
			
			operatorIdValue.setFor("operatorIds");
			operatorCountValue.setFor("nOperators");
			
			if (operatorsForLink == null) {
				operatorIdValue.setValue("");
				operatorCountValue.setValue("0");
			} else {
				
				// Do not use toString
				StringBuffer strB = new StringBuffer();
				for (Id<Operator> id : operatorsForLink) {
					strB.append(id.toString());strB.append(",");
				}
				operatorIdValue.setValue(strB.toString());
//				operatorIdValue.setValue(operatorsForLink.toString());
				operatorCountValue.setValue(Integer.toString(operatorsForLink.size()));
			}
			
			operatorIdValue.setStart(Double.toString(iteration));
			operatorIdValue.setEnd(Double.toString(iteration));
			operatorCountValue.setStart(Double.toString(iteration));			
			operatorCountValue.setEnd(Double.toString(iteration));	

			linkEntry.getValue().getAttvalue().add(operatorIdValue);
			linkEntry.getValue().getAttvalue().add(operatorCountValue);
			this.linkId2OperatorIdsFromLastIteration.put(linkEntry.getKey(), operatorsForLink);
		}
		
		for (Entry<Id<Link>, XMLAttvaluesContent> linkEntry : this.linkAttributeValueContentMap.entrySet()) {
			
			int countForLink = vehHandler.getVehCountForLinkId(linkEntry.getKey());
			
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
		
		
		if (writeOperatorInDetail) {
			Set<String> currentLineIds = this.globalPaxHandler.getLineIds();
			// finish all operators who do not exist anymore
			for (String lineId : this.lastLineIds) {
				if (!currentLineIds.contains(lineId)) {
					// does not exist anymore - terminate

					for (Entry<Id<Link>, XMLAttvaluesContent> linkEntry : this.linkAttributeValueContentMap.entrySet()) {
						if (this.linkId2LineId2CountsFromLastIteration.get(linkEntry.getKey()) != null){
							if (this.linkId2LineId2CountsFromLastIteration.get(linkEntry.getKey()).get(lineId) != null){
								// There is already an entry
								if (this.linkId2LineId2CountsFromLastIteration.get(linkEntry.getKey()).get(lineId) == 0) {
									// was already zero - ignore
									continue;
								}
							}
						}

						XMLAttvalue attValue = new XMLAttvalue();
						attValue.setFor(lineId);
						attValue.setValue(Integer.toString(0));
						attValue.setStart(Double.toString(iteration));
						attValue.setEnd(Double.toString(iteration));
						linkEntry.getValue().getAttvalue().add(attValue);
					}
				}
			}

			// add new attribute for all new operators
			for (String lineId : currentLineIds) {
				if (!this.lastLineIds.contains(lineId)) {
					// new operator - create new attribute
					XMLAttributeContent attributeContent = new XMLAttributeContent();
					attributeContent.setId(lineId);
					attributeContent.setTitle(lineId);
					attributeContent.setType(XMLAttrtypeType.FLOAT);
					this.edgeAttributeContentsContainer.getAttribute().add(attributeContent);
				}
			}

			// add count values for current line ids
			for (Entry<Id<Link>, XMLAttvaluesContent> linkEntry : this.linkAttributeValueContentMap.entrySet()) {

				for (String lineId : currentLineIds) {
					int countForLinkAndLineId = paxHandler.getPaxCountForLinkId(linkEntry.getKey(), lineId);

					if (this.linkId2LineId2CountsFromLastIteration.get(linkEntry.getKey()) != null){
						if (this.linkId2LineId2CountsFromLastIteration.get(linkEntry.getKey()).get(lineId) != null){
							// There is already an entry
							if (this.linkId2LineId2CountsFromLastIteration.get(linkEntry.getKey()).get(lineId) == countForLinkAndLineId) {
								// same as last iteration - ignore
								continue;
							}
						}
					}

					XMLAttvalue attributeContent = new XMLAttvalue();
					attributeContent.setFor(lineId);
					attributeContent.setValue(Integer.toString(countForLinkAndLineId));
					attributeContent.setStart(Double.toString(iteration));
					attributeContent.setEnd(Double.toString(iteration));

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
			jc = JAXBContext.newInstance(org.matsim.contrib.minibus.genericUtils.gexf.ObjectFactory.class);
			Marshaller m = jc.createMarshaller();
			super.setMarshallerProperties(GexfPStat.XSD_PATH, m);
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