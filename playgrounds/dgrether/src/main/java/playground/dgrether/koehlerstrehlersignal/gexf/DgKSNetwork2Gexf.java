/* *********************************************************************** *
 * project: org.matsim.*
 * DgKSNetwork2Gexf
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
package playground.dgrether.koehlerstrehlersignal.gexf;

import java.util.List;

import playground.dgrether.koehlerstrehlersignal.data.DgCrossing;
import playground.dgrether.koehlerstrehlersignal.data.DgCrossingNode;
import playground.dgrether.koehlerstrehlersignal.data.DgKSNetwork;
import playground.dgrether.koehlerstrehlersignal.data.DgStreet;
import playground.vsp.gexf.GexfWriter;
import playground.vsp.gexf.ObjectFactory;
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
 * @author dgrether
 */
public class DgKSNetwork2Gexf {

	private ObjectFactory gexfFactory;
	private XMLGexfContent gexfContent;
	private float zCoord = 1.0f;
	
	
	public DgKSNetwork2Gexf(){
		this.gexfFactory = new ObjectFactory();
		this.gexfContent = this.gexfFactory.createXMLGexfContent();
		XMLGraphContent graph = this.gexfFactory.createXMLGraphContent();
		graph.setDefaultedgetype(XMLDefaultedgetypeType.DIRECTED);
		graph.setIdtype(XMLIdtypeType.STRING);
		graph.setMode(XMLModeType.DYNAMIC);
		graph.setTimeformat(XMLTimeformatType.INTEGER);
		this.gexfContent.setGraph(graph);	
	}
	
	public void convertAndWrite(DgKSNetwork ksNet, String filename) {
		List<Object> attr = this.gexfContent.getGraph().getAttributesOrNodesOrEdges();
		XMLNodesContent nodes = this.gexfFactory.createXMLNodesContent();
		attr.add(nodes);

		XMLEdgesContent edges = this.gexfFactory.createXMLEdgesContent();
		attr.add(edges);

		List<XMLEdgeContent> edgeList = edges.getEdge();
		List<XMLNodeContent> nodeList = nodes.getNode();
		
		for (DgStreet street : ksNet.getStreets().values()){
			DgCrossingNode fromNode = street.getFromNode();
			XMLNodeContent n = this.createNode(fromNode);
			nodeList.add(n);
			
			DgCrossingNode toNode = street.getToNode();
			n = this.createNode(toNode);
			nodeList.add(n);
			
			XMLEdgeContent e = this.createEdge(street);
			edgeList.add(e);
		}
		
		for (DgCrossing crossing : ksNet.getCrossings().values()){
			for (DgStreet street : crossing.getLights().values()){
					XMLEdgeContent e = this.createEdge(street);
					edgeList.add(e);
			}
		}
		
		new GexfWriter(this.gexfContent).write(filename);
	}
	
	private XMLEdgeContent createEdge(DgStreet street){
		XMLEdgeContent e = this.gexfFactory.createXMLEdgeContent();
		e.setId(street.getId().toString());
		e.setLabel("network link");
		e.setSource(street.getFromNode().getId().toString());
		e.setTarget(street.getToNode().getId().toString());
		return e;
	}

	private XMLNodeContent createNode(DgCrossingNode nn){
		XMLNodeContent n = this.gexfFactory.createXMLNodeContent();
		n.setId(nn.getId().toString());
		n.setLabel("node");
		playground.vsp.gexf.viz.ObjectFactory vizFac = new playground.vsp.gexf.viz.ObjectFactory();
		PositionContent pos = vizFac.createPositionContent();
		pos.setX((float) nn.getCoordinate().getX());
		pos.setY((float) nn.getCoordinate().getY());
		pos.setZ(zCoord);
		n.getAttvaluesOrSpellsOrNodes().add(pos);
		return n;
	}
	
}


