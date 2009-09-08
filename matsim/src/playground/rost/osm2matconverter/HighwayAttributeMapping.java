/******************************************************************************
 *project: org.matsim.*
 * HighwayAttributeMapping.java
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


package playground.rost.osm2matconverter;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import playground.rost.osm2matconverter.xmlhighwaymapping.AtomicHighwayMapping;
import playground.rost.osm2matconverter.xmlhighwaymapping.HighwayMapping;

public class HighwayAttributeMapping {
	public Map<String, AtomicHighwayMapping> highwayMapping = new HashMap<String, AtomicHighwayMapping>();
	
	public HighwayAttributeMapping()
	{
		
	}
	
	public HighwayAttributeMapping(Map<String, AtomicHighwayMapping> highwayMapping)
	{
		this.highwayMapping = highwayMapping;
	}
	
	public double getWidth(String key)
	{
		if(highwayMapping.containsKey(key))
		{
			return highwayMapping.get(key).getWidth().doubleValue();
		}
		else 
			return highwayMapping.get("road").getWidth().doubleValue();
	}
	
	public static HighwayAttributeMapping readXMLFile(String filename) throws JAXBException, IOException
	{	
		HighwayAttributeMapping result = new HighwayAttributeMapping();	
		
		HighwayMapping highwayMapping;
		
		JAXBContext context = JAXBContext.newInstance("playground.rost.osm2matconverter.xmlhighwaymapping");

		Unmarshaller unmarshaller = context.createUnmarshaller();
		
		highwayMapping = (HighwayMapping)unmarshaller.unmarshal(new FileReader(filename));
		
		for(AtomicHighwayMapping aMappingElement : highwayMapping.getMappingElement())
		{
			result.highwayMapping.put(aMappingElement.getValue(), aMappingElement);
		}
		
		return result;
	}
	
	public void writeXMLFile(String filename) throws JAXBException, IOException
	{
		HighwayMapping highwayMapping = new HighwayMapping();
		for(AtomicHighwayMapping aHM : this.highwayMapping.values())
		{
			highwayMapping.getMappingElement().add(aHM);
		}
		JAXBContext jaxbContext=JAXBContext.newInstance("playground.rost.osm2matconverter.xmlhighwaymapping");
		Marshaller marshall = jaxbContext.createMarshaller();
		marshall.marshal(highwayMapping, new FileWriter(filename));
	}
	
	public static void writeDefaultValuesXMLFile(String filename)
	{
		HighwayAttributeMapping haMap = getDefaultValues();
		try {
			haMap.writeXMLFile(filename);
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static HighwayAttributeMapping getDefaultValues()
	{
		String[] defValues = { 
			"motorway,15",
			"motorway_link,15", 
			"trunk,10", 
			"trunk_link,10", 
			"primary,8", 
			"primary_link,8", 
			"secondary,5", 
			"secondary_link,5", 
			"tertiary,4", 
			"unclassified,3", 
			"road,3", 
			"residential,3", 
			"living_street,2.5", 
			"service,2.5", 
			"track,2.5", 
			"pedestrian,5", 
			"path,1.5", 
			"cycleway,1", 
			"footway,2", 
			"bridleway,2.5", 
			"byway,2.5",
			"steps,2"}; 
		
		HighwayAttributeMapping haMap = new HighwayAttributeMapping();
		for(String s : defValues)
		{
			String[] elem = s.split(",");
			String name = elem[0];
			double width =  Double.valueOf(elem[1]);
			AtomicHighwayMapping ahElement= new AtomicHighwayMapping();
			ahElement.setValue(name);
			ahElement.setWidth(new BigDecimal(width));
			haMap.highwayMapping.put(name, ahElement);	
		}
		return haMap;
	}
}



