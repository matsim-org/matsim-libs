/******************************************************************************
 *project: org.matsim.*
 * EvacArea.java
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 * copyright       : (C) 2009 by the members listed in the COPYING,           *
 *                   LICENSE and WARRANTY file.                               *
 * email           : info at matsim dot org                                   *
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 *   This program is free software; you can redistribute it and/or modify     *
 *   it under the terms of the GNU General Public License as published by     *
 *   the Free Software Foundation; either version 2 of the License, or        *
 *   (at your option) any later version.                                      *
 *   See also COPYING, LICENSE and WARRANTY file                              *
 *                                                                            *
 ******************************************************************************/


package playground.rost.graph.evacarea;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.matsim.api.core.v01.network.Node;

import playground.rost.graph.evacarea.xmlevacarea.AreaMapping;
import playground.rost.graph.evacarea.xmlevacarea.AtomicNodeInArea;

public class EvacArea {	
	public Set<String> evacAreaNodeIds = new HashSet<String>();
	public Set<String> evacBorderNodeIds = new HashSet<String>();
	
	public List<String> evacBorderOrderIds = new LinkedList<String>(); 
	
	public EvacArea()
	{
		
	}
	
	public EvacArea(Set<String> evacAreaNodeIds, Set<String> evacBorderNodeIds, List<Node> border)
	{
		if(evacAreaNodeIds == null || evacBorderNodeIds == null ||  border == null)
			throw new RuntimeException("Area shall not be null!");
		this.evacAreaNodeIds = evacAreaNodeIds;
		this.evacBorderNodeIds = evacBorderNodeIds;
		this.evacBorderOrderIds.clear();
		for(Node n : border)
		{
			this.evacBorderOrderIds.add(n.getId().toString());
		}
		
	}
	
	public static EvacArea readXMLFile(String filename) throws JAXBException, IOException
	{
		HashMap<Integer, String> borderOrderMap = new HashMap<Integer, String>();
		
		EvacArea eArea = new EvacArea();	
		
		AreaMapping areaMapping;
		
		JAXBContext context = JAXBContext.newInstance("playground.rost.graph.evacarea.xmlevacarea");

		Unmarshaller unmarshaller = context.createUnmarshaller();
		
		areaMapping = (AreaMapping)unmarshaller.unmarshal(new FileReader(filename));
		
		for(AtomicNodeInArea aNode : areaMapping.getAtomicNodeEntry())
		{
			if(aNode.isInArea())
			{
				eArea.evacAreaNodeIds.add(aNode.getNodeId());
			}
			else		
			{
				eArea.evacBorderNodeIds.add(aNode.getNodeId());
			}
			if(aNode.getBorderOrder() != -1)
			{
				borderOrderMap.put(aNode.getBorderOrder(), aNode.getNodeId());
			}
		}
		if(borderOrderMap.size() == 0)
			throw new RuntimeException("no border information!");
		for(int i = 0; i < borderOrderMap.size(); ++i)
		{
			String current = borderOrderMap.get(i);
			if(current != null)
			{
				eArea.evacBorderOrderIds.add(current);
			}
			else
				throw new RuntimeException("wrong border information in xml file..");
		}
		
		return eArea;
	}
	
	public void writeXMLFile(String filename) throws JAXBException, IOException
	{
		AreaMapping areaMap = new AreaMapping();
		for(String s : evacAreaNodeIds)
		{
			AtomicNodeInArea aNode = new AtomicNodeInArea();
			aNode.setNodeId(s);
			aNode.setInArea(true);
			areaMap.getAtomicNodeEntry().add(aNode);
		}
		for(String s : evacBorderNodeIds)
		{
			AtomicNodeInArea aNode = new AtomicNodeInArea();
			aNode.setNodeId(s);
			aNode.setInArea(false);
			areaMap.getAtomicNodeEntry().add(aNode);
		}
		int counter = 0;
		for(AtomicNodeInArea aNode : areaMap.getAtomicNodeEntry())
		{
			if(evacBorderOrderIds.contains(aNode.getNodeId()))
			{
				int index = evacBorderOrderIds.indexOf(aNode.getNodeId());
				aNode.setBorderOrder(index);
			}
			else
				aNode.setBorderOrder(-1);
		}
		JAXBContext jaxbContext=JAXBContext.newInstance("playground.rost.graph.evacarea.xmlevacarea");
		Marshaller marshall = jaxbContext.createMarshaller();
		marshall.marshal(areaMap, new FileWriter(filename));
	}
}
