/* *********************************************************************** *
 * project: org.matsim.*
 * GetAllNodes.java
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

/**
 * @author Christoph Dobler
 * 
 * Liefert eine ArrayList von Nodes.
 * Übergabeparameter muss ein Netzwerk, eine Person oder ein Plan sein.
 * Wird eine Person übergeben, so wird der jeweils aktuelle Plan verwendet.
 * Wird zusätzlich noch eine ArrayList Nodes mit übergeben, so wird diese
 * mit den neu gefundenen Nodes erweitert. Andernfalls wird eine neue erstellt.
 *
 */


package playground.christoph.knowledge.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Route;

public class GetAllNodes {
	
	public ArrayList<Node> getAllNodes(NetworkLayer n)
	{
		return getNodes(n);
	}
	
	public void getAllNodes(NetworkLayer n, ArrayList<Node> nodes)
	{	
		getNodes(n, nodes);
	}
	
	public ArrayList<Node> getAllNodes(Person p)
	{
		//return getNodes(getLinks(p));
		return getNodes(new GetAllLinks().getAllLinks(p));
	}
	
	public void getAllNodes(Person p, ArrayList<Node> nodes)
	{
		//getNodes(getLinks(p), nodes);
		getNodes(new GetAllLinks().getAllLinks(p), nodes);
	}
	
	public ArrayList<Node> getAllNodes(Plan p)
	{
		//return getNodes(getLinks(p));
		return getNodes(new GetAllLinks().getAllLinks(p));
	}
	
	public void getAllNodes(Plan p, ArrayList<Node> nodes)
	{
		//getNodes(getLinks(p), nodes);
		getNodes(new GetAllLinks().getAllLinks(p), nodes);
	}
	

/*
	// Liefert eine ArryList aller Links, die Teil des selektierten Plans der übergebenen Person sind.
	protected ArrayList<Link> getLinks(Person person)
	{
		Plan plan = person.getSelectedPlan();

		return getLinks(plan);
	} //getLinks(Person)
*/
	
/*
	// Liefert eine ArryList aller Links, die Teil übergebenen Plans sind.
	protected ArrayList<Link> getLinks(Plan plan)
	{	
		ArrayList<Link> links = new ArrayList<Link>();
	
		// Links holen, an denen Acts stattfinden
		ActIterator actIterator = plan.getIteratorAct();
		while (actIterator.hasNext())
		{
			Act act = (Act)actIterator.next();
			
			// Hinzufügen, falls neues Element
			if(!links.contains(act.getLink())) links.add(act.getLink());
			
		}	// while actIterator.hasNext()

		// Routen holen, die die Acts verbinden
		LegIterator legIterator = plan.getIteratorLeg();
		while (legIterator.hasNext())
		{
			Leg leg = (Leg)legIterator.next();

			Route route = leg.getRoute();
				
			Link[] linkArray = route.getLinkRoute(); 
			
			for(int i = 0; i < linkArray.length; i++)
			{
				Link link = linkArray[i];
			
				// Hinzufügen, falls neues Element
				if(!links.contains(link)) links.add(link);
				
			}
		}	// while legIterator.hasNext()

		return links;
	} // getLinks(Plan)
*/
	
	// Liefert eine ArrayList aller Nodes, welche Teil der übergebenen Links sind.
	// Da keine ArrayList mit bereits selektieren Nodes übergeben wurde, wird diese neu erstellt. 
	protected ArrayList<Node> getNodes(ArrayList<Link> links)
	{
		ArrayList<Node> nodes = new ArrayList<Node>();
		
		getNodes(links, nodes);
		
		return nodes; 
	} // getNodes(ArrayList<Link>)
	
	
	// Liefert eine ArrayList aller Nodes, welche Teil der übergebenen Links sind.
	protected void getNodes(ArrayList<Link> links, ArrayList<Node> nodes)
	{
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

	}
	
	
	protected ArrayList<Node> getNodes(NetworkLayer n)
	{
		ArrayList<Node> nodes = new ArrayList<Node>();

		getNodes(n, nodes);
		
		return nodes;		
	} //getNodes(NetworkLayer n)
	
	
	protected void getNodes(NetworkLayer n, ArrayList<Node> nodes)
	{
		// Alle Nodes des Netzwerks holen
		TreeMap<Id, Node> nodeMap = (TreeMap<Id, Node>)n.getNodes();
		
		Iterator nodeIterator = nodeMap.entrySet().iterator();
		
		while(nodeIterator.hasNext())
		{
			// Wir wissen ja, was für Elemente zurückgegeben werden :)
			Map.Entry<Id, Node> nextNode = (Map.Entry<Id, Node>)nodeIterator.next();

			Node node = nextNode.getValue();
						
			// Prüfen, ob der Node bereits in der Liste enthalten ist
			if(!nodes.contains(node)) nodes.add(node);	
		}	// while nodeIterator.hasNext()
		
	} //getNodes(NetworkLayer n, ArrayList<Node) nodes)
	
}