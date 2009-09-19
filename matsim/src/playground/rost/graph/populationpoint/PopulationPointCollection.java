/******************************************************************************
 *project: org.matsim.*
 * PopulationPointCollection.java
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


package playground.rost.graph.populationpoint;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import playground.rost.graph.populationpoint.xmlpopulationpointlist.PopulationPointList;
import playground.rost.graph.populationpoint.xmlpopulationpointlist.SinglePopulationPoint;

public class PopulationPointCollection {
	Collection<PopulationPoint> popPoints = new HashSet<PopulationPoint>();
	
	public Collection<PopulationPoint> get()
	{
		return popPoints;
	}
	
	public void add(SinglePopulationPoint p)
	{
		PopulationPoint popPoint = new PopulationPoint(p.getX(), p.getY(), p.getPopulation());
		popPoints.add(popPoint);
	}
	
	public void add(PopulationPoint p)
	{
		popPoints.add(p);
	}
	
	public static PopulationPointCollection readXMLFile(String filename) throws JAXBException, IOException
	{
		PopulationPointCollection popPointCollection = new PopulationPointCollection();
		
		JAXBContext context = JAXBContext.newInstance("playground.rost.graph.populationpoint.xmlpopulationpointlist");

		Unmarshaller unmarshaller = context.createUnmarshaller();
		
		PopulationPointList popPointList = (PopulationPointList)unmarshaller.unmarshal(new FileReader(filename));
		
		for(SinglePopulationPoint sPP : popPointList.getPopPoint())
		{
			popPointCollection.add(sPP);
		}
		
		return popPointCollection;
	}
	
	public void writeXMLFile(String filename) throws JAXBException, IOException
	{
		PopulationPointList result = new PopulationPointList();
		for(PopulationPoint p : this.popPoints)
		{
			SinglePopulationPoint sPP = createSinglePopulationPoint(p);
			result.getPopPoint().add(sPP);
		}
		
		JAXBContext context = JAXBContext.newInstance("playground.rost.graph.populationpoint.xmlpopulationpointlist");
		Marshaller marshall = context.createMarshaller();
		marshall.marshal(result, new FileWriter(filename));
	}

	protected SinglePopulationPoint createSinglePopulationPoint(PopulationPoint p)
	{
		SinglePopulationPoint sPP = new SinglePopulationPoint();
		sPP.setX(p.point.x);
		sPP.setY(p.point.y);
		sPP.setPopulation(p.population);
		return sPP;
	}
		
}
