/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateKnowledge.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.christoph.knowledge;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Knowledge;
import org.matsim.population.Population;

import playground.christoph.knowledge.utils.GetAllLinks;

public class GenerateKnowledge {

	Population population;
	NetworkLayer network;
	double distance;
	
	public GenerateKnowledge (Population pop, NetworkLayer net, double dist)
	{
		population = pop;
		network = net;
		distance = dist;
		
		getPersons();
	}
	
	
	// F�hrt alle Schritte durch, um das Wissen (in diesem Fall die Kenntnis der Umgegung)
	// der Personen des aktuellen Objekts zu beschr�nken.
	protected void getPersons()
	{
		Iterator<Person> PersonIterator = population.iterator();
		while (PersonIterator.hasNext())
		{
			Person person = PersonIterator.next();
			
			ArrayList<Link> links = new GetAllLinks().getAllLinks(person);
			
			setKnowledge(person, links);
			
			// alte Version - Berechnung �ber Abstand Node zu Node und nicht Node zu Link
			//ArrayList<Node> nodes = getNodes(links);
			//setKnowledge(person, nodes);
		}
	}
	
	
	// Liefert eine ArrayList aller Nodes, welche Teil der �bergebenen Links sind.
	protected ArrayList<Node> getNodes(ArrayList<Link> links)
	{
		ArrayList<Node> nodes = new ArrayList<Node>();
		
		Iterator<Link> linksIterator = links.iterator();
		
		while(linksIterator.hasNext())
		{
			Link link = linksIterator.next();

			//nodes.add(link.getFromNode());
			//nodes.add(link.getToNode());
			
			Node fromNode = link.getFromNode();
			Node toNode = link.getToNode();
			
			if (!nodes.contains(fromNode)) nodes.add(fromNode);
			if (!nodes.contains(toNode)) nodes.add(toNode);
		}
		
		return nodes;
	} //getNodes(ArrayList<Link>)
	
	
	// Beschr�nkt die Kenntnis der Umgebung der �bergebenen Person.
	protected void setKnowledge(Person person, ArrayList<Link> links)
	{		
		Knowledge knowledge = person.getKnowledge();
		
		ArrayList<Id> includedLinkIds = (ArrayList<Id>)knowledge.getCustomAttributes().get("IncludedLinkIDs");
		
		// Nodes innerhalb der vorgegebenen Distanz holen...
		ArrayList<Node> includedNodes = getIncludedNodes(links);
			
		// ... und daraus die Links innerhalb der vorgegebenen Distanz generieren.
		ArrayList<Link> includedLinks = getIncludedLinks(includedNodes);
		
		// Links zum Knowledge der Person hinzuf�gen
		for(int i = 0; i < includedLinks.size(); i++)
		{
			Id id = includedLinks.get(i).getId();
			if(!includedLinkIds.contains(id)) includedLinkIds.add(id);
		}
		System.out.println("PersonenID: " + person.getId().toString() + " gefundene Links: " + includedLinkIds.size());
	} //setKnowledge(ArrayList<Node>)

	
	// Gibt eine ArrayList mit Knoten zur�ck, die innerhalb eines vorgegebenen
	// Abstands zu einer vorgeggebenen Route liegen.
	protected ArrayList<Node> getIncludedNodes(ArrayList<Link> links)
	{
		ArrayList<Node> includedNodes = new ArrayList<Node>();
		
		// Alle Knoten des Netzwerks holen
		TreeMap<Id, Node> nodeMap = (TreeMap<Id, Node>)network.getNodes();
		
		Iterator nodeIterator = nodeMap.entrySet().iterator();
		
		while(nodeIterator.hasNext())
		{
			// Wir wissen ja, was f�r Elemente zur�ckgegeben werden :)
			Map.Entry<Id, Node> nextLink = (Map.Entry<Id, Node>)nodeIterator.next();
			//Id id = nextLink.getKey();
			Node node = nextLink.getValue();
			
			Coord coord = node.getCoord();
			
			// Abstand zu allen Links der �bergebenen Person untersuchen
			for (int i = 0; i < links.size(); i++)
			{
				double dist = links.get(i).calcDistance(coord);
				
				// Innerhalb des Bereichs?
				if (dist <= distance)
				{
					// Knoten in Liste speichern
					if (!includedNodes.contains(node)) includedNodes.add(node);
					
					// Treffer -> Schleife abbrechen
					i = links.size();
				}
			}			
		}	// while nodeIterator.hasNext()
		
		return includedNodes;
	}
	
	
	// Gibt eine ArrayList mit Links zur�ck, die innerhalb eines vorgegebenen
	// Abstands zu einer vorgeggebenen Route liegen.
	protected ArrayList<Link> getIncludedLinks(ArrayList<Node> includedNodes)
	{
		ArrayList<Link> includedLinks = new ArrayList<Link>();
		
		// Alle Links des Netzwerks holen
		TreeMap<Id, Link> linkMap = (TreeMap<Id, Link>)network.getLinks();
		
		Iterator linkIterator = linkMap.entrySet().iterator();
		
		while(linkIterator.hasNext())
		{
			// Wir wissen ja, was f�r Elemente zur�ckgegeben werden :)
			Map.Entry<Id, Link> nextLink = (Map.Entry<Id, Link>)linkIterator.next();
			//Id id = nextLink.getKey();
			Link link = nextLink.getValue();
			
			Node fromNode = link.getFromNode();
			Node toNode = link.getToNode();
			
			// Pr�fen, ob der Node in der �bergebenen Liste enthalten ist
			if(includedNodes.contains(fromNode) && includedNodes.contains(toNode))
			{
				//... also beide Nodes enthalten -> Link enthalten
				includedLinks.add(link);
			}		
			
		}	// while nodeIterator.hasNext()
		
		return includedLinks;
	}
	
}
