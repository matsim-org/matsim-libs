/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetz.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.fabrice.secondloc.socialnet;

import java.util.Collection;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.matsim.config.groups.SocNetConfigGroup;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;

public class SocialNetz {
	
	LinkedList<SocialNetEdge> links = new LinkedList<SocialNetEdge>();
	boolean isDirected;

	private final static Logger log = Logger.getLogger(SocialNetz.class);
	
	public SocialNetz( Plans plans ) {

		// Determine first if we have a directed or bi-directional graph
    	String edge_type = Gbl.getConfig().socnetmodule().getEdgeType();
    	if(edge_type.equals("UNDIRECTED"))
    		isDirected = false;
    	else if(edge_type.equals("DIRECTED"))
    		isDirected =  true ;
    	else Gbl.errorMsg("  config file. Social net edge_type can only be UNDIRECTED or DIRECTED");
    	
		// Check parameters and call the right algorithm (which implements
		// SocialNetwork)
		String sNAlgorithmName_ = Gbl.getConfig().socnetmodule().getSocNetAlgo();

		if (sNAlgorithmName_.equals("random")) {
			log.info("Setting up the " + sNAlgorithmName_ + " algorithm.");
			initRandomSocialNetwork(plans);
			
		} else if (sNAlgorithmName_.equals("wattssmallworld")) {
			log.info("Setting up the " + sNAlgorithmName_ + " algorithm.");
			initWattsSocialNetwork(plans);
			
		} else if (sNAlgorithmName_.equals("jingirnew")) {
			log.info("Setting up the " + sNAlgorithmName_ + " algorithm.");
			initJGNSocialNetwork(plans);
			
		} else if (sNAlgorithmName_.equals("empty")) {
			log.info("Setting up the " + sNAlgorithmName_ + " algorithm.");
			initEmptySocialNetwork(plans);
			
		} else {
			Gbl.errorMsg(" "+ getClass()
							+ ".run(). Social Network Algorithm > "
							+ sNAlgorithmName_
							+ " < is not known. Poor choice of input parameter in module "
							+ SocNetConfigGroup.GROUP_NAME
							+ ". Check spelling or choose from: random, wattssmallworld, jingirnew, empty");
		}
	}
	
	void initRandomSocialNetwork( Plans plans ){
		int kbar = Integer.parseInt(Gbl.getConfig().socnetmodule().getSocNetKbar());
		log.info("Links the Persons together in UNDIRECTED Erdos/Renyi random graph. Dorogovtsev and Mendes 2003.");
		Person[] personList = new Person[1];
		personList = plans.getPersons().values().toArray( personList );
		
		int numLinks = (int) ((kbar * personList.length) / 2.);
    	for (int i = 0; i < numLinks; i++) {
    		Person person1 = personList[Gbl.random.nextInt(personList.length)];
    		Person person2 = personList[Gbl.random.nextInt(personList.length)];

    		createSocialConnection( person1, person2);	
    	}
	}
	
	void initWattsSocialNetwork( Plans plans ){
		log.warn("Unsupported");
		// may be better:
//		throw new UnsupportedOperationException();
	}

	void initJGNSocialNetwork(Plans plans) {
		log.warn("Unsupported");
		// may be better:
//		throw new UnsupportedOperationException();
	}
	
	void initEmptySocialNetwork( Plans plans) {
	}
	
    public void createSocialConnection( Person p1, Person p2){
    	if( p1.equals(p2) )
    		return;
		
    	// Check if the social connection already exists
		SocialContext context1 = p1.getKnowledge().context;
		if( context1.knows(p2))
			return;
		
		SocialNetEdge edge = new SocialNetEdge( p1, p2);
		links.add( edge );
		context1.addConnection(edge);
		
    	if( isBidirectional() )
    		p2.getKnowledge().context.addConnection(edge);
    	
    }

	public boolean isDirected() {
		return isDirected;
	}
	
	public boolean isBidirectional(){
		return !isDirected;
	}
	
	public Collection<SocialNetEdge> getLinks(){
		return links;
	}
}
