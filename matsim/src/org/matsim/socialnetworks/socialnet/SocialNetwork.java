/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetwork.java
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

package org.matsim.socialnetworks.socialnet;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.config.groups.SocNetConfigGroup;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.socialnetworks.io.MakeSocialNetworkFromFile;

public class SocialNetwork {

	public boolean UNDIRECTED;
	private SocNetConfigGroup socnetConfig = Gbl.getConfig().socnetmodule();
	String linkRemovalCondition= socnetConfig.getSocNetLinkRemovalAlgo();
	String edge_type= socnetConfig.getEdgeType();
	double remove_p= Double.parseDouble(socnetConfig.getSocNetLinkRemovalP());
	double remove_age= Double.parseDouble(socnetConfig.getSocNetLinkRemovalAge());
	public double degree_saturation_rate= Double.parseDouble(socnetConfig.getDegSat());

	// a Collection of all the Links in the network
	public ArrayList<SocialNetEdge> linksList = new ArrayList<SocialNetEdge>();

//	public int setupIter=0;
	// public ArrayList<SocialNetNode> nodes;

	private final static Logger log = Logger.getLogger(SocialNetwork.class);

	public SocialNetwork(Plans plans) {

		if(edge_type.equals("UNDIRECTED")){
			UNDIRECTED=true;
		}else if(edge_type.equals("DIRECTED")){
			UNDIRECTED = false;
		}else Gbl.errorMsg("  config file. Social net edge_type can only be UNDIRECTED or DIRECTED");

		// Check parameters and call the right algorithm (which implements
		// SocialNetwork)
		String sNAlgorithmName_ = socnetConfig.getSocNetAlgo();

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

		} else if (sNAlgorithmName_.equals("read")){
			log.info("Preparing to read in the social network");
			initReadInNetwork(plans);
			
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

		int kbar = Integer.parseInt(socnetConfig.getSocNetKbar());
		log.info("Links the Persons together in UNDIRECTED Erdos/Renyi random graph. Dorogovtsev and Mendes 2003.");
		Person[] personList = new Person[1];
		personList = plans.getPersons().values().toArray( personList );

		int numLinks = (int) ((kbar * personList.length) / 2.);
		for (int i = 0; i < numLinks; i++) {
			Person person1 = personList[Gbl.random.nextInt(personList.length)];
			Person person2 = personList[Gbl.random.nextInt(personList.length)];

//			makeSocialContact( person1, person2, -1);
			makeSocialContact(person1, person2, 0, "random");
		}
	}

	void initWattsSocialNetwork( Plans plans ){

		log.info(socnetConfig.getSocNetAlgo()+" Unsupported.");
		throw new UnsupportedOperationException();
	}

	void initJGNSocialNetwork(Plans plans) {

		log.info(socnetConfig.getSocNetAlgo()+" Unsupported. To generate a similar network, initialize a random network and let a small number of agents introduce friends to each other.");
		throw new UnsupportedOperationException();
	}

	void initEmptySocialNetwork( Plans plans) {

		log.info(socnetConfig.getSocNetAlgo()+" Unsupported. Use a \"random\" social network instead, with kbar=0");
		throw new UnsupportedOperationException();
	}

	void initReadInNetwork(Plans plans){

		String filename = socnetConfig.getInDirName()+ "/edge.txt";
		new MakeSocialNetworkFromFile(this, plans).read(filename, Integer.valueOf(socnetConfig.getInitIter()).intValue());

	}

	/**
	 * Adds a single directed link from person1 to person2. If network is
	 * UNDIRECTED, it adds two links, one link in each direction,
	 * and records only one link in the links list,
	 * thus "undirected" rather than "bidirectional" 
	 * (symmetric network). Prevents
	 * multiple links and self-linking. Does not impose any other conditions
	 * on adding the link (do this upon calling the method).
	 * If a link is altered in
	 * one direction asymmetrically in another method, this method could cause
	 * undetected bugs if called again.
	 * 
	 * @param person1
	 *                The Person from which the link originates
	 * @param person2
	 *                The Person at which the link terminates
	 * @author J.Hackney
	 */

	public void makeSocialContact(Person person1, Person person2, int iteration) {

		SocialNetEdge newLink;
		SocialNetEdge newOpposingLink;

		if (!person1.equals(person2)) {

//			NOTE this could be made more efficient by directly accessing
//			the link and testing for null
			if(person1.getKnowledge().egoNet.knows(person2)){

				newLink = person1.getKnowledge().egoNet.getEgoLink(person2);
				newOpposingLink = person2.getKnowledge().egoNet.getEgoLink(person1);

				newLink.setTimeLastUsed(iteration);
				newLink.incrementNumberOfTimesMet();
				newOpposingLink.setTimeLastUsed(iteration);
				newOpposingLink.incrementNumberOfTimesMet();
			} else 
//				They do not know each other, make new link subject to saturation effects	
				if(Gbl.random.nextDouble()<Math.exp(this.degree_saturation_rate * person1.getKnowledge().egoNet.getOutDegree())){
					newLink = new SocialNetEdge(person1, person2);
					addLink(newLink,iteration);
					linksList.add(newLink);
//					New symmetric link if undirected network
					if(UNDIRECTED){
						newLink = new SocialNetEdge(person2, person1);
						addLink(newLink,iteration);
					}
				}
		}
	}

	public void makeSocialContact(Person person1, Person person2, int iteration, String linkType) {

		SocialNetEdge newLink;
		SocialNetEdge newOpposingLink;

		if (!person1.equals(person2)) {

//			NOTE this could be made more efficient by directly accessing
//			the link and testing for null
			if(person1.getKnowledge().egoNet.knows(person2)){

				newLink = person1.getKnowledge().egoNet.getEgoLink(person2);
				newOpposingLink = person2.getKnowledge().egoNet.getEgoLink(person1);

				newLink.setTimeLastUsed(iteration);
				newLink.setType(linkType);
				newLink.incrementNumberOfTimesMet();
				newOpposingLink.setTimeLastUsed(iteration);
				newOpposingLink.setType(linkType);
				newOpposingLink.incrementNumberOfTimesMet();
			} else 
//				They do not know each other, make new link subject to saturation effects	
				if(Gbl.random.nextDouble()<Math.exp(this.degree_saturation_rate * person1.getKnowledge().egoNet.getOutDegree())){
					newLink = new SocialNetEdge(person1, person2);
					addLink(newLink,iteration, linkType);
					linksList.add(newLink);
//					New symmetric link if undirected network
					if(UNDIRECTED){
						newLink = new SocialNetEdge(person2, person1);
						addLink(newLink,iteration, linkType);
					}
				}
		}
	}

	public void addLink(SocialNetEdge myLink, int iteration){
		
		myLink.getPersonFrom().getKnowledge().egoNet.addEgoLink(myLink);
		myLink.setTimeMade(iteration);
		myLink.setTimeLastUsed(iteration);
	}

	public void addLink(SocialNetEdge myLink, int iteration, String linkType){
		myLink.getPersonFrom().getKnowledge().egoNet.addEgoLink(myLink);
		myLink.setTimeMade(iteration);
		myLink.setTimeLastUsed(iteration);
		myLink.setType(linkType);
	}    

	public void removeLinks(int iteration) {
		// Establish the link removal policy from config parameters and call
		// method
		ArrayList<SocialNetEdge> linksToRemove = new ArrayList<SocialNetEdge>();
		log.info("  removeLinks() algorithm \"" + linkRemovalCondition + "\"");

		if(linkRemovalCondition.equals("none")) return;
		if (linkRemovalCondition.equals("random")) {
			log.info("  Removing links older than "+remove_age+" with probability "+remove_p);
			log.info("  Number of links before removal: "+this.getLinks().size());
			Iterator<SocialNetEdge> it_link = this.getLinks().iterator();
			while (it_link.hasNext()) {
				SocialNetEdge myLink = it_link.next();
				double randremove=Gbl.random.nextDouble();
				if ((iteration - myLink.getTimeLastUsed()) > remove_age && randremove<remove_p ) {
					linksToRemove.add(myLink);
				}
			}
			log.info("  Number of links after removal: "+(this.getLinks().size()-linksToRemove.size()));
		}else if(linkRemovalCondition.equals("random_node_degree")){
			// Removal probability proportional to node degree
			// Implemented in Jin, Girvan, Newman 2001
			log.info("  Removing links older than "+remove_age+" proportional to degree times probability "+remove_p);
			log.info("  Number of links before removal: "+this.getLinks().size());
			Iterator<SocialNetEdge> it_link = this.getLinks().iterator();
			while (it_link.hasNext()) {
				SocialNetEdge myLink = it_link.next();
				double randremove=Gbl.random.nextDouble();
				int degree =myLink.getPersonFrom().getKnowledge().egoNet.getOutDegree();
				if ((iteration - myLink.getTimeLastUsed()) > remove_age && randremove<remove_p*degree ) {
					linksToRemove.add(myLink);
				}
			}
			log.info("  Number of links after removal: "+(this.getLinks().size()-linksToRemove.size()));
		}else if(linkRemovalCondition.equals("random_link_age")){
			// Removal probability proportional to node degree
			// Implemented in Jin, Girvan, Newman 2001
			log.info("  Removing links proportional to age times probability "+remove_p);
			log.info("  Number of links before removal: "+this.getLinks().size());
			Iterator<SocialNetEdge> it_link = this.getLinks().iterator();
			while (it_link.hasNext()) {
				SocialNetEdge myLink = it_link.next();
				double randremove=Gbl.random.nextDouble();
				int age =iteration - myLink.getTimeLastUsed();
				if ((iteration - myLink.getTimeLastUsed()) > remove_age && randremove<remove_p*age ) {
					linksToRemove.add(myLink);
				}
			}
			log.info("  Number of links after removal: "+(this.getLinks().size()-linksToRemove.size()));
		}else{
			Gbl.errorMsg("Supported removal algorithms: \"random_link_age\""+", \"random_node_degree\""+", \"random\"");
		}

		// This runs for all removal algorithms
		log.info("Removing Flagged Links Now");
		removeFlaggedLinks(linksToRemove);

	}
	public void removeFlaggedLinks(ArrayList<SocialNetEdge> links){
		Iterator<SocialNetEdge> itltr = links.iterator();
		while (itltr.hasNext()) {
			SocialNetEdge myLink = itltr.next();
			this.removeLink(myLink);

			if (UNDIRECTED) {
				SocialNetEdge myOpposingLink=myLink.getPersonTo().getKnowledge().egoNet.getEgoLink(myLink.getPersonFrom());
				this.removeLink(myOpposingLink);
			}
		}
	}
	public void removeLink(SocialNetEdge myLink) {
		// Directed links are counted once per link and are not added
		// symmetrically. "linksList" is the list of directed links.
//		log.info("Removing link "+ myLink.person1.getId()+myLink.person2.getId());
		linksList.remove(myLink);
		myLink.getPersonFrom().getKnowledge().egoNet.removeEgoLink(myLink);

	}

	public ArrayList<SocialNetEdge> getLinks() {
		return linksList;
	}

	public void printLinks() {
		// Call writer instance (use a config parameter). Options
		// are XML (pick a DTD and see other XML writers),
		// Pajek (consider also time-dependent Pajek format),
		// Something readable in R (flat file easiest), i.e. same as SNModel
		// Just a test
		int ii = 0;
		for (SocialNetEdge link : linksList) {
			log.info(ii + " " + link.person1.getId() + " " + link.person2.getId());
			ii++;
		}
	}

}
