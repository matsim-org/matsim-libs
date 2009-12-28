/******************************************************************************
 *project: org.matsim.*
 * OSM2MATConverter.java
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
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.rost.graph.GraphAlgorithms;
import playground.rost.osm2matconverter.xmlosmbase.Nd;
import playground.rost.osm2matconverter.xmlosmbase.Node;
import playground.rost.osm2matconverter.xmlosmbase.Osm;
import playground.rost.osm2matconverter.xmlosmbase.Tag;
import playground.rost.osm2matconverter.xmlosmbase.Way;
import playground.rost.util.PathTracker;

/**
 * @author sub
 *
 */
public class OSM2MATConverter {
	
	private static final Logger log = Logger.getLogger(OSM2MATConverter.class);

	protected static Map<BigInteger, Node> mapNd2Node;
	
	public static NetworkLayer readOSM(String filename) throws JAXBException, IOException {
		
		long kreuzungId = 99999;
		Osm osmdata;
		NetworkLayer network = new NetworkLayer();
		JAXBContext context = JAXBContext.newInstance("playground.rost.osm2matconverter.xmlosmbase");
		Unmarshaller unmarshaller = context.createUnmarshaller();
		osmdata = (Osm)unmarshaller.unmarshal(new FileReader(filename));
		mapNd2Node = createNodeMap(osmdata);
		Map<BigInteger, Node> strassenKnoten = parseNodes(network, osmdata);
		parseStrassen(network, osmdata, strassenKnoten);
		return network;
	}
	
	protected static void parseStrassen(NetworkLayer network, Osm osmdata, Map<BigInteger, Node> strassenKnoten)
	{
		Map<String, Integer> roadStats = new HashMap<String, Integer>();
		
		
		log.debug("Start parsing roads..");
		
		log.debug("Get HighwayAttributes..");
		HighwayAttributeMapping haMap;
		try {
			 haMap = HighwayAttributeMapping.readXMLFile(PathTracker.resolve("highwayMappingDefault"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.debug(e.getStackTrace());
			throw new RuntimeException("HighwayAttributes-File [" + PathTracker.resolve("highwayMappingDefault") + "] nicht gefunden");
		}
		long debug_count = 0;
		long debug_iter = 0;
		long streetId = 50000000;
		for(Way way : osmdata.getWay())
		{
			boolean isRoad = false;
			String highwayType = "";
			//�berpr�fe ob es sich wirklich um eine Stra�e handelt
			for(Tag tag : way.getTag())
			{
				if(tag.equals("highway"))
				{
					isRoad = true;
					highwayType = tag.getV();
					break;
				}
			}
			if(!isRoad)
				continue;
			Integer countOfType = roadStats.get(highwayType);
			if(countOfType == null)
			{
				roadStats.put(highwayType, 1);
			}
			else
			{
				roadStats.put(highwayType, ++countOfType);
			}
			//es handelt sich um eine Stra�e.
			//extrahiere alle Teilstra�en, das sind Stra�en, zwischen zwei stra�enKnoten
			Nd start, intermediate, end;
			Node startNode, intermediateNode, endNode;
			int laenge;
			for(int i = 0; i < way.getNd().size()-1; ++i)
			{
				laenge = 0;
				start = way.getNd().get(i);
				startNode = mapNd2Node.get(start.getRef());
				++i;
				//dummy initialization
				end = way.getNd().get(i);
				endNode = mapNd2Node.get(end.getRef());
				for(; i < way.getNd().size(); ++i)
				{
					//greife auf vorherigen Node zu
					intermediate = way.getNd().get(i-1);
					intermediateNode = mapNd2Node.get(intermediate.getRef());
					//und addiere die Teill�nge
					end = way.getNd().get(i);
					endNode = mapNd2Node.get(end.getRef());
					laenge += GraphAlgorithms.getDistanceMeter(intermediateNode.getLon(), intermediateNode.getLat(), endNode.getLon(), endNode.getLat());
					if(strassenKnoten.containsKey(end.getRef()))
						break;
				}
				++debug_count;
				//erzeuge Stra�e
				double capacity = Math.max(haMap.getWidth(highwayType) / 1.25, 1);
				network.createAndAddLink(new IdImpl(String.valueOf(++streetId)), 
									network.getNode(String.valueOf(startNode.getId())), 
									network.getNode(String.valueOf(endNode.getId())), 
									laenge, 
									1.3, //TODO 
									capacity,  //TODO
									1);  //TODO!
				

				network.createAndAddLink(new IdImpl(String.valueOf(++streetId)), 
									network.getNode(String.valueOf(endNode.getId())), 
									network.getNode(String.valueOf(startNode.getId())), 
									laenge, 
									1.3, //TODO 
									capacity,  //TODO
									1);  //TODO!
				//endknoten wird neuer startknoten:
				--i;
			}
			if(debug_count - debug_iter > 100)
			{
				log.debug("road count: " + debug_count);
				debug_iter = debug_count;
			}
		}
		log.debug("Statistik: Haeufigkeit von Stra�entypen");
		while(roadStats.size() > 0)
		{
			Integer maxCount = Integer.MIN_VALUE;
			String bestKey = "";
			for(String key : roadStats.keySet())
			{
				if(roadStats.get(key) > maxCount)
				{
					maxCount = roadStats.get(key);
					bestKey = key;
				}
			}
			String output = bestKey + ": " + maxCount;
			log.debug(output);
			roadStats.remove(bestKey);
		}
		
	}

	
	/**
	 * Aus den bestehenden Wegen des Osm-Materials werden diejenigen Knoten extrahiert,
	 * welche auch wirklich Stra�enkreuzungen darstellen und nicht etwa Geb�udebegrenzungen oder �hnliches sind.
	 * Zur�ckgegeben wird eine Map der Stra�enkreuzungen
	 * 
	 * @param network Das Netwerk, in dem die Knoten eingef�gt werden sollen
	 * @param osmdata Osm-Daten
	 * @return Map von Knoten Id auf Knoten
	 */
	protected static Map<BigInteger, Node> parseNodes(NetworkLayer network, Osm osmdata)
	{
		long debug_count = 0;
		long debug_iter = 0;
		log.debug("Start parsing nodes!");
		Map<BigInteger, Node> wegKnoten = new HashMap<BigInteger,Node>(4000);
		Map<BigInteger, Node> strassenKnoten = new HashMap<BigInteger, Node>(4000);
		//parse Kreuzungen
		for(Way way : osmdata.getWay())
		{
			Node node;
			boolean isWay = false;
			//wir interessieren uns nur f�r knoten die an Wegen h�ngen
			//nicht an Geb�uden oder so
			for(Tag tag : way.getTag())
			{
				if(tag.equals("highway"))
				{
					isWay = true;
					break;
				}
			}
			if(!isWay)
				continue;
			//wir haben einen richtigen Weg!
			Nd nd = way.getNd().get(0);
			node = mapNd2Node.get(nd.getRef());
			if(node == null)
			{
				debug_count--;
				
			}
			if(!strassenKnoten.containsKey(node.getId()))
			{
				++debug_count;
				strassenKnoten.put(way.getNd().get(0).getRef(), node);
			}
			int end = way.getNd().size()-1;
			node = mapNd2Node.get(way.getNd().get(end).getRef());
			if(!strassenKnoten.containsKey(node.getId()))
			{
				++debug_count;
				strassenKnoten.put(way.getNd().get(end).getRef(), node);
			}
			for(int i = 1; i < way.getNd().size()-1; ++i)
			{
				nd = way.getNd().get(i);
				if(strassenKnoten.containsKey(nd.getRef()))
					continue;
				node = mapNd2Node.get(nd.getRef());
				if(wegKnoten.containsKey(nd.getRef()))
				{
					
					//der Knoten liegt auch auf einem anderen Weg ist also eine Kreuzung
					++debug_count;
					strassenKnoten.put(nd.getRef(), node);
					wegKnoten.remove(nd.getRef());
				}
				else
				{
					wegKnoten.put(nd.getRef(), node);
				}
			}
			if(debug_count - debug_iter > 500)
			{
				log.debug("node count: " +debug_iter);
				debug_iter = debug_count;
			}
			
		}
		//Knoten im eigentlichen Netzwerk abspeichern
		for(Node node: strassenKnoten.values())
		{
			//lese Daten
			String id = node.getId().toString();
			String x  = String.valueOf(node.getLon());
			String y  = String.valueOf(node.getLat());
			//erstelle Daten f�r das Netzwerk
			Coord coord = new CoordImpl(x,y);
			Id matsimid  = new IdImpl(id);
			network.createAndAddNode(matsimid, coord);
		}
		return strassenKnoten;
	}
	
	protected static Map<BigInteger, Node> createNodeMap(Osm osmdata)
	{
		log.debug("Start building NodeMap..");
		Map<BigInteger, Node> result = new HashMap<BigInteger, Node>(10000);
		for(Node node : osmdata.getNode())
		{
			result.put(node.getId(), node);
		} 
		return result;
	}

	public static void parseAndWrite()
	{
		HighwayAttributeMapping.writeDefaultValuesXMLFile(PathTracker.resolve("highwayMappingDefault"));
		
		String inputfile = PathTracker.resolve("osmMap");
		String outfile = PathTracker.resolve("matMap");
		try {
			NetworkLayer network = readOSM(inputfile);
			network.setCapacityPeriod(1); //TODO changed
			new NetworkWriter(network).writeFile(outfile);
			System.out.println(inputfile + "  converted successfully \n"
					+ "output written in: " + outfile);
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		parseAndWrite();
	}
}
