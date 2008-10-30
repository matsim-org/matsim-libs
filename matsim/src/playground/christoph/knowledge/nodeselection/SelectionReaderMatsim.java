/* *********************************************************************** *
 * project: org.matsim.*
 * SelectionReaderMatsim.java
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
package playground.christoph.knowledge.nodeselection;

import java.io.IOException;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Knowledge;
import org.matsim.population.Person;
import org.matsim.population.Population;
import org.matsim.utils.io.MatsimXmlParser;
import org.matsim.utils.misc.Time;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;


public class SelectionReaderMatsim extends MatsimXmlParser implements SelectionReader{

	private static final Logger log = Logger.getLogger(SelectionReaderMatsim.class);

	public static String SELECTION = "selection";
	public static String PERSON = "person";
	public static String KNOWLEDGE = "knowledge";
	public static String ACTIVITYSPACE = "activityspace";
	public static String NODES = "nodes";
	public static String NODE = "node";
	public static String LINKS = "links";
	public static String LINK = "link";
	public static String ID = "id";

	public static boolean overwriteExistingSelection = true;
	
	protected Population population;
	protected NetworkLayer network;
	protected Person currentPerson;
	protected Knowledge currentKnowledge;
	protected Map<Id, Node> currentNodes;
	protected Map<Id, Link> currentLinks;
	
	protected Id personId;
	protected Id nodeId;
	protected Id linkId;
	
			
	public SelectionReaderMatsim(NetworkLayer network, Population population)
	{
		this.network = network;
		this.population = population;
	}
  
	public void setOverwriteExistingSelection(boolean value)
	{
		overwriteExistingSelection = value;
	}
  
	public Boolean getOverwriteExistingSelection()
	{
		return overwriteExistingSelection;
	}
	
	@Override
	public void endTag(String name, String content, Stack<String> context)
	{
		if (SELECTION.equalsIgnoreCase(name))
		{
		}
		else if (PERSON.equalsIgnoreCase(name))
		{
			currentPerson = null;
		}
		else if (KNOWLEDGE.equalsIgnoreCase(name))
		{
			currentKnowledge = null;
		}
		else if (ACTIVITYSPACE.equalsIgnoreCase(name))
		{
		}
		else if (NODES.equalsIgnoreCase(name))
		{
			currentNodes = null;
		}
		else if (LINKS.equalsIgnoreCase(name))
		{
			currentLinks = null;
		}
		else if (NODE.equalsIgnoreCase(name))
		{
		}
		else if (LINK.equalsIgnoreCase(name))
		{
		}
		else
		{
			log.warn("Ignoring endTag (beta implementation!): " + name);
		}
	} //end of endTag

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) 
	{
		if (SELECTION.equalsIgnoreCase(name))
		{
			
		}
		else if (PERSON.equalsIgnoreCase(name)) 
		{
			personId = getId(atts);
			
			if (personId != null)
			{
				currentPerson = population.getPerson(personId);	
			}
			else
			{
				currentPerson = null;
			}		
		} 
		
		else if (KNOWLEDGE.equalsIgnoreCase(name))
		{
			if (currentPerson != null)
			{
				currentKnowledge = currentPerson.getKnowledge();
	 
				if (currentKnowledge == null)
				{
					currentPerson.createKnowledge("activityroom");
					currentKnowledge = currentPerson.getKnowledge();
				}
			}
			else
			{
				currentKnowledge = null;
			}
		}
		
		else if (ACTIVITYSPACE.equalsIgnoreCase(name))
		{
			
		}
		
		else if (NODES.equalsIgnoreCase(name))
		{
			if (currentKnowledge != null)
			{
				Map<String,Object> customAttributes = currentKnowledge.getCustomAttributes();
	
				if (customAttributes != null)
				{
					if (customAttributes.containsKey("Nodes"))
					{
						currentNodes = (Map<Id, Node>)customAttributes.get("Nodes");
						
						if(overwriteExistingSelection) currentNodes.clear();
					}
					else
					{
						currentNodes = new TreeMap<Id, Node>();
						customAttributes.put("Nodes", currentNodes);
					}
				}
				else
				{
					currentNodes = null;
				}
			}
			else
			{
				currentNodes = null;
			}
			
		}
		
		else if (NODE.equalsIgnoreCase(name))
		{
			nodeId = getId(atts);
			
			if (nodeId != null)
			{
				Node node = network.getNode(nodeId);
				
				if (node != null)
				{
					if (currentNodes != null)
					{
						if(!currentNodes.containsKey(nodeId))
						{
							currentNodes.put(nodeId, node);
						}
					}
				}
			}
			
		}
		
		else if (LINKS.equalsIgnoreCase(name))
		{
			
		}
		
		else if (LINK.equalsIgnoreCase(name))
		{
			linkId = getId(atts);
		}
		
		else if (ID.equalsIgnoreCase(name))
		{
			
		}
	} //end of startTag
		
	private double parseTime(String time) 
	{
		return Time.parseTime(time);
	}
	
	private Id getId(Attributes atts) 
	{
		Id id = new IdImpl(atts.getValue(ID));
		return id;
	}
	
	public void readFile(String filename)
	{
		try 
		{
			super.parse(filename);
		} 
		catch (SAXException e)
		{
			e.printStackTrace();
		} 
		catch (ParserConfigurationException e) 
		{
			e.printStackTrace();
		} catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
}
