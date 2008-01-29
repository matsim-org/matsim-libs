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

package playground.jhackney.module.socialnet;

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.config.groups.SocNetConfigGroup;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;

public class SocialNetwork {

    public boolean UNDIRECTED;
    String linkRemovalCondition= Gbl.getConfig().socnetmodule().getSocNetLinkRemovalAlgo();
    String edge_type= Gbl.getConfig().socnetmodule().getEdgeType();
    double remove_p= Double.parseDouble(Gbl.getConfig().socnetmodule().getSocNetLinkRemovalP());
    double remove_age= Double.parseDouble(Gbl.getConfig().socnetmodule().getSocNetLinkRemovalAge());
    public double degree_saturation_rate= Double.parseDouble(Gbl.getConfig().socnetmodule().getDegSat());

    // a Collection of all the Links in the network
    public ArrayList<SocialNetEdge> linksList = new ArrayList<SocialNetEdge>();

    public int setupIter=0;
    // public ArrayList<SocialNetNode> nodes;

    public SocialNetwork(Plans plans) {

	if(edge_type.equals("UNDIRECTED")){
	    UNDIRECTED=true;
	}else if(edge_type.equals("DIRECTED")){
	    UNDIRECTED = false;
	}else Gbl.errorMsg("  config file. Social net edge_type can only be UNDIRECTED or DIRECTED");

	// Check parameters and call the right algorithm (which implements
	// SocialNetwork)
	String sNAlgorithmName_ = Gbl.getConfig().socnetmodule().getSocNetAlgo();

	if (sNAlgorithmName_.equals("random")) {
	    System.out.println("Setting up the " + sNAlgorithmName_
		    + " algorithm.");
	    initRandomSocialNetwork(plans);

	} else if (sNAlgorithmName_.equals("wattssmallworld")) {
	    System.out.println("Setting up the " + sNAlgorithmName_
		    + " algorithm.");
	    initWattsSocialNetwork(plans);

	} else if (sNAlgorithmName_.equals("jingirnew")) {
	    System.out.println("Setting up the " + sNAlgorithmName_
		    + " algorithm.");
	    initJGNSocialNetwork(plans);

	} else if (sNAlgorithmName_.equals("empty")) {
	    System.out.println("Setting up the " + sNAlgorithmName_
		    + " algorithm.");
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
	System.out.println("Links the Persons together in UNDIRECTED Erdos/Renyi random graph. Dorogovtsev and Mendes 2003.");
	Person[] personList = new Person[1];
	personList = plans.getPersons().values().toArray( personList );

	int numLinks = (int) ((kbar * personList.length) / 2.);
	for (int i = 0; i < numLinks; i++) {
	    Person person1 = personList[Gbl.random.nextInt(personList.length)];
	    Person person2 = personList[Gbl.random.nextInt(personList.length)];

//	    makeSocialContact( person1, person2, -1);
	    makeSocialContact(person1, person2, 0, "initialized");
	}
    }

    void initWattsSocialNetwork( Plans plans ){
	Gbl.noteMsg(getClass(), "initWattsSocialNetwork", "Unsupported");
    }

    void initJGNSocialNetwork(Plans plans) {
	Gbl.noteMsg(getClass(), "initJGNSocialNetwork", "Unsupported");
    }

    void initEmptySocialNetwork( Plans plans) {
	setupIter=1;

	Person [] personList = new Person[1];
	personList = plans.getPersons().values().toArray(personList);

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

//	    NOTE this could be made more efficient by directly accessing
//	    the link and testing for null
	    if(person1.getKnowledge().egoNet.knows(person2)){

		newLink = person1.getKnowledge().egoNet.getEgoLink(person2);
		newOpposingLink = person2.getKnowledge().egoNet.getEgoLink(person1);

		newLink.setTimeLastUsed(iteration);
		newLink.incrementNumberOfTimesMet();
		newOpposingLink.setTimeLastUsed(iteration);
		newOpposingLink.incrementNumberOfTimesMet();
	    } else 
//		They do not know each other, make new link subject to saturation effects	
		if(Gbl.random.nextDouble()<Math.exp(this.degree_saturation_rate * person1.getKnowledge().egoNet.getOutDegree())){
		    newLink = new SocialNetEdge(person1, person2);
		    addLink(newLink,iteration);
		    linksList.add(newLink);
//		    New symmetric link if undirected network
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

//	    NOTE this could be made more efficient by directly accessing
//	    the link and testing for null
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
//		They do not know each other, make new link subject to saturation effects	
		if(Gbl.random.nextDouble()<Math.exp(this.degree_saturation_rate * person1.getKnowledge().egoNet.getOutDegree())){
		    newLink = new SocialNetEdge(person1, person2);
		    addLink(newLink,iteration, linkType);
		    linksList.add(newLink);
//		    New symmetric link if undirected network
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
	System.out.println("  removeLinks() algorithm \"" + linkRemovalCondition + "\"");

	if(linkRemovalCondition.equals("none")) return;
	if (linkRemovalCondition.equals("random")) {
	    System.out.println("  Removing links older than "+remove_age+" with probability "+remove_p);
	    System.out.println("  Number of links before removal: "+this.getLinks().size());
	    Iterator it_link = this.getLinks().iterator();
	    while (it_link.hasNext()) {
		SocialNetEdge myLink = (SocialNetEdge) it_link.next();
		double randremove=Gbl.random.nextDouble();
		if ((iteration - myLink.getTimeLastUsed()) > remove_age && randremove<remove_p ) {
		    linksToRemove.add(myLink);
		}
	    }
	    System.out.println("  Number of links after removal: "+(this.getLinks().size()-linksToRemove.size()));
	}else if(linkRemovalCondition.equals("random_node_degree")){
	    // Removal probability proportional to node degree
	    // Implemented in Jin, Girvan, Newman 2001
	    System.out.println("  Removing links older than "+remove_age+" proportional to degree times probability "+remove_p);
	    System.out.println("  Number of links before removal: "+this.getLinks().size());
	    Iterator it_link = this.getLinks().iterator();
	    while (it_link.hasNext()) {
		SocialNetEdge myLink = (SocialNetEdge) it_link.next();
		double randremove=Gbl.random.nextDouble();
		int degree =myLink.getPersonFrom().getKnowledge().egoNet.getOutDegree();
		if ((iteration - myLink.getTimeLastUsed()) > remove_age && randremove<remove_p*degree ) {
		    linksToRemove.add(myLink);
		}
	    }
	    System.out.println("  Number of links after removal: "+(this.getLinks().size()-linksToRemove.size()));
	}else if(linkRemovalCondition.equals("random_link_age")){
	    // Removal probability proportional to node degree
	    // Implemented in Jin, Girvan, Newman 2001
	    System.out.println("  Removing links proportional to age times probability "+remove_p);
	    System.out.println("  Number of links before removal: "+this.getLinks().size());
	    Iterator it_link = this.getLinks().iterator();
	    while (it_link.hasNext()) {
		SocialNetEdge myLink = (SocialNetEdge) it_link.next();
		double randremove=Gbl.random.nextDouble();
		int age =iteration - myLink.getTimeLastUsed();
		if ((iteration - myLink.getTimeLastUsed()) > remove_age && randremove<remove_p*age ) {
		    linksToRemove.add(myLink);
		}
	    }
	    System.out.println("  Number of links after removal: "+(this.getLinks().size()-linksToRemove.size()));
	}else{
	    Gbl.errorMsg("Supported removal algorithms: \"random_link_age\""+", \"random_node_degree\""+", \"random\"");
	}

	// This runs for all removal algorithms
	System.out.println("Removing Flagged Links Now");
	removeFlaggedLinks(linksToRemove);

    }
    public void removeFlaggedLinks(ArrayList<SocialNetEdge> links){
	Iterator itltr = links.iterator();
	while (itltr.hasNext()) {
	    SocialNetEdge myLink = (SocialNetEdge) itltr.next();
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
//	System.out.println("Removing link "+ myLink.person1.getId()+myLink.person2.getId());
	linksList.remove(myLink);
	myLink.getPersonFrom().getKnowledge().egoNet.removeEgoLink(myLink);

    }

    public ArrayList<SocialNetEdge> getLinks() {
	// TODO Auto-generated method stub
	return linksList;
    }

    public void printLinks() {
	// Call writer instance (use a config parameter). Options
	// are XML (pick a DTD and see other XML writers),
	// Pajek (consider also time-dependent Pajek format),
	// Something readable in R (flat file easiest), i.e. same as SNModel
	// Just a test
	Iterator itLink = linksList.iterator();
	int ii = 0;
	while (itLink.hasNext()) {
	    SocialNetEdge printLink = (SocialNetEdge) itLink.next();
	    Person printPerson1 = printLink.person1;
	    Person printPerson2 = printLink.person2;
	    System.out.println(ii + " " + printPerson1.getId() + " " + printPerson2.getId());
	    ii++;
	}
    }

}
