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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.config.groups.SocNetConfigGroup;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.socialnetworks.io.MakeSocialNetworkFromFile;
import org.matsim.utils.geometry.CoordI;

public class SocialNetwork {

	private boolean UNDIRECTED;
	private SocNetConfigGroup socnetConfig = Gbl.getConfig().socnetmodule();
	String linkRemovalCondition= socnetConfig.getSocNetLinkRemovalAlgo();
	String edge_type= socnetConfig.getEdgeType();
	double remove_p= Double.parseDouble(socnetConfig.getSocNetLinkRemovalP());
	double remove_age= Double.parseDouble(socnetConfig.getSocNetLinkRemovalAge());
	private double degree_saturation_rate= Double.parseDouble(socnetConfig.getDegSat());
	private Collection<Person> persons;

	// a Collection of all the Links in the network
	public ArrayList<SocialNetEdge> linksList = new ArrayList<SocialNetEdge>();

//	public int setupIter=0;
	// public ArrayList<SocialNetNode> nodes;

	private final static Logger log = Logger.getLogger(SocialNetwork.class);

	public SocialNetwork(Plans plans) {

		this.persons=plans.getPersons().values();

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

		} else if (sNAlgorithmName_.equals("barabasialbert")) {
			log.info("Setting up the " + sNAlgorithmName_ + " algorithm.");
			initBASocialNetwork(plans);
		}else if (sNAlgorithmName_.equals("euclidrandom")){
			log.info("Setting up the " + sNAlgorithmName_ +" algorithm.");
			initEuclidRandomNetwork(plans);

		} else if (sNAlgorithmName_.equals("read")){
			log.info("Preparing to read in the social network");
			initReadInNetwork(plans);

		} else {
			Gbl.errorMsg(" "+ getClass()
					+ ".run(). Social Network Algorithm > "
					+ sNAlgorithmName_
					+ " < is not known. Poor choice of input parameter in module "
					+ SocNetConfigGroup.GROUP_NAME
					+ ". Check spelling or choose from: random, wattssmallworld, jingirnew, barabasialbert, empty, read");
		}
	}

	/**
	 * Generates a Bernoulli (Erdös/Renyi) network with link
	 * probability modified by the distance between alters
	 *
	 * 
	 * Need to change this algorithm so that it doesn't choose to link to random people
	 *  if the qualifications are met, but to choose two people given qualifications
	 *  and to link them; stratified sampling.
	 * @param plans
	 */
	private void initEuclidRandomNetwork(Plans plans) {

		int kbar = Integer.parseInt(socnetConfig.getSocNetKbar());
		log.info("Links the Persons together in UNDIRECTED distance-dependent Erdos/Renyi random graph.");
		Person[] personList = new Person[1];
		personList = plans.getPersons().values().toArray( personList );

		int numLinks = (int) ((kbar * personList.length) / 2.);
		
		double p0=1.;
		double rmin=1000.;
		double rmax=100000.;
		double alpha=1.5;
		double c=p0/(Math.pow(rmin,-alpha));
		System.out.println(alpha+" "+c+" "+p0+" "+rmin);
		int i=0;
		while(i<numLinks){
//			for (int i = 0; i < numLinks; i++) {
			Person person1 = personList[Gbl.random.nextInt(personList.length)];
			Person person2 = personList[Gbl.random.nextInt(personList.length)];
			CoordI home1=((Act)person1.getSelectedPlan().getActsLegs().get(0)).getFacility().getCenter();
			CoordI home2=((Act)person2.getSelectedPlan().getActsLegs().get(0)).getFacility().getCenter();
			double distance = home1.calcDistance(home2);
			double pdist=c*Math.pow((distance+rmin),-alpha);

			if(Gbl.random.nextDouble()<pdist){
				if(makeSocialContactNotify(person1, person2, 0, "random")==2){//new link made
					System.out.println("new link made dist "+distance+" "+pdist);
					i++;
				}
			}
		}
	}

	/**
	 * Generates a Barabasi-Albert scale-free network with random spatial
	 * distribution and fixed parameters gamma and initial core size, m0.
	 * 
	 * Each agent enters the network and constructs on average kbar/2 new edges.
	 * (average degree is kbar).
	 * 
	 * 2-3 is typically observed in large growing social networks
	 * where adding an additional link brings more benefit/cost ratio than adding the first link to
	 * a node (citation of papers, airline routes, or internet routers, for example).
	 * The established activities at the node, or flows through the node, or some other
	 * dynamic in the "market" (say for example, the ease of citing a well-known author who
	 * was already cited in a paper you've just read, versus searching all available literature for
	 * the most relevant paper) make this economy of preferential attachment emerge.
	 * 
	 * Gamma=2 is the minimum for scale-free percolation. Scale-free means that the minimum spanning
	 * tree for the phase change to a discontinuous non-percolating network is null (zero nodes).
	 * 
	 * M0 is a parameter for the initial number of nodes in the network, here set to 1.
	 * M is a parameter for the number of links to add each step and must be < m0.
	 * 
	 * @author jhackney
	 */
	private void initBASocialNetwork(Plans plans) {

		double gamma = 2.5; //
		int kbar=Integer.parseInt(socnetConfig.getSocNetKbar()); 
		int m0= (int) (((double)kbar)/2.); // initial core size
		int m=m0;// number of links to add each step, m< m0
//		if(m>m0){
//		Gbl.errorMsg(this.getClass()+" m must be >= m0");
//		}
		double A = 2.*(gamma-2);
		int E; // number of edges in network each time m links are added

		Person[] personList = new Person[1];
		personList = plans.getPersons().values().toArray( personList );

		int maxE = (int) ((double)kbar/2.)*personList.length;

		log.info("Links the Persons together in UNDIRECTED Barabasi/Albert random graph of Gamma="+gamma+", core="+m0+", Emax="+maxE);

		ArrayList<Person> population = new ArrayList<Person>();
		for (int i=0;i<personList.length;i++){
			population.add(personList[i]);
		}

		//Define the core network of m0 nodes, chosen from personList
		ArrayList<Person> core = new ArrayList<Person>();
		for (int ii=0;ii<m0;ii++){
			int pick=Gbl.random.nextInt(population.size());
			core.add(population.get(pick));
			population.remove(pick);
		}

		//Loop through the remainder of the population and let each one add m new links to the core
		E=this.getLinks().size();
		int coreSize = core.size();
		while(population.size()>0 && E< maxE){
			Person pI=population.get(Gbl.random.nextInt(population.size()));
			// Attachment probabilities are based on last iteration
			boolean met=false;
			int j=0;
			while (j<Math.min(m,coreSize)){ // the new member links to this many old members. m < m0
				int pick=Gbl.random.nextInt(coreSize);//start at a random core member
				Person pJ = core.get(pick);
				double pIJ= (pJ.getKnowledge().getEgoNet().getOutDegree()+A)/(2*E + coreSize*A);
				if(coreSize==0){
					pIJ = 1.;
				}


				if (Gbl.random.nextInt()<pIJ){
					if(makeSocialContactNotify(pI, pJ, 0, "Barabasi-Albert")==2){
						met=true;
						j++;
//						log.info(" "+pJ.getId()+" "+pJ.getKnowledge().egoNet.getOutDegree());
					}
				}
				if(met==false){
					pick=(pick+1)%coreSize;
					pJ=core.get(pick);
					pIJ=(pJ.getKnowledge().getEgoNet().getOutDegree()+A)/(2*E + coreSize*A);
				}
			}
			core.add(pI);// add pI to core
			coreSize = core.size();
			population.remove(pI); // remove pI from un-linked population
			E=this.getLinks().size(); // increase number of edges for pIJ calculation
		}
	}

	/**
	 * Generates a classical (Erdös/Renyi) undirected random graph of degree kbar
	 * @author jhackney
	 * 
	 */
	void initRandomSocialNetwork( Plans plans ){

		int kbar = Integer.parseInt(socnetConfig.getSocNetKbar());
		log.info("Links the Persons together in UNDIRECTED Erdos/Renyi random graph. Dorogovtsev and Mendes 2003.");
		Person[] personList = new Person[1];
		personList = plans.getPersons().values().toArray( personList );

		int numLinks = (int) ((kbar * personList.length) / 2.);
		double pdist=2.*(double)numLinks/((personList.length*(personList.length-1)));
		int i=0;
		while(i<numLinks){
//		for (int i = 0; i < numLinks; i++) {
			Person person1 = personList[Gbl.random.nextInt(personList.length)];
			Person person2 = personList[Gbl.random.nextInt(personList.length)];
			CoordI home1=((Act)person1.getSelectedPlan().getActsLegs().get(0)).getFacility().getCenter();
			CoordI home2=((Act)person2.getSelectedPlan().getActsLegs().get(0)).getFacility().getCenter();
			double distance = home1.calcDistance(home2);
			
//			makeSocialContact( person1, person2, -1);
			if(makeSocialContactNotify(person1, person2, 0, "random")==2){
			System.out.println("new link made dist "+distance+" "+pdist);
			i++;
			}
		}
	}

	/**
	 * Not implemented
	 */
	void initWattsSocialNetwork( Plans plans ){

		log.info(socnetConfig.getSocNetAlgo()+" Unsupported.");
		throw new UnsupportedOperationException();
	}

	/**
	 * Not implemented
	 */
	void initJGNSocialNetwork(Plans plans) {

		log.info(socnetConfig.getSocNetAlgo()+" Unsupported. To generate a similar network, initialize a random network and let a small number of agents introduce friends to each other.");
		throw new UnsupportedOperationException();
	}

	/**
	 * Unsupported. Use "random" keyword and kbar = 0
	 */
	void initEmptySocialNetwork( Plans plans) {

		log.info(socnetConfig.getSocNetAlgo()+" Unsupported. Use a \"random\" social network instead, with kbar=0");
		throw new UnsupportedOperationException();
	}

	/**
	 * Reads in any social network "edge.txt" file in the format of {@link socialnetworks.statistics.SocialNetworkStatistics.java}.
	 * Requires a valid plans file (thus in the standard configuration where plans are written out every 10 time steps
	 * or else only at the end of a run, you are restricted to re-starting or loading a social network from these iterations).
	 * Text file containing a mapping of activities to acts must be present for the corresponding plans file, if
	 * indicated in the config.xml
	 * 
	 * @author jhackney
	 */
	void initReadInNetwork(Plans plans){

		String filename = socnetConfig.getInDirName()+ "edge.txt";
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
	 * @param iteration
	 *                The socializing iteration (iteration of the social network)
	 * @author jhackney
	 */

	public void makeSocialContact(Person person1, Person person2, int iteration) {

		SocialNetEdge newLink;
		SocialNetEdge newOpposingLink;

		if (!person1.equals(person2)) {

//			NOTE this could be made more efficient by directly accessing
//			the link and testing for null
			if(person1.getKnowledge().getEgoNet().knows(person2)){

				newLink = person1.getKnowledge().getEgoNet().getEgoLink(person2);
				newOpposingLink = person2.getKnowledge().getEgoNet().getEgoLink(person1);

				newLink.setTimeLastUsed(iteration);
				newLink.incrementNumberOfTimesMet();
				newOpposingLink.setTimeLastUsed(iteration);
				newOpposingLink.incrementNumberOfTimesMet();
			} else 
//				They do not know each other, make new link subject to saturation effects	
				if(Gbl.random.nextDouble()<Math.exp(this.degree_saturation_rate * person1.getKnowledge().getEgoNet().getOutDegree())){
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
	/**
	 * Adds a single directed link from person1 to person2 of type "type".
	 * If network is
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
	 *                
	 * @param iteration
	 *                The socializing iteration (iteration of the social network)
	 *                
	 * @param linkType
	 *                The type of social interaction
	 *                
	 * @author jhackney
	 */
	public void makeSocialContact(Person person1, Person person2, int iteration, String linkType) {

		SocialNetEdge newLink;
		SocialNetEdge newOpposingLink;

		if (!person1.equals(person2)) {

			if(person1.getKnowledge().getEgoNet().knows(person2)){

				newLink = person1.getKnowledge().getEgoNet().getEgoLink(person2);
				newOpposingLink = person2.getKnowledge().getEgoNet().getEgoLink(person1);

				newLink.setTimeLastUsed(iteration);
				newLink.setType(linkType);
				newLink.incrementNumberOfTimesMet();
				newOpposingLink.setTimeLastUsed(iteration);
				newOpposingLink.setType(linkType);
				newOpposingLink.incrementNumberOfTimesMet();
			} else 
//				They do not know each other, make new link subject to saturation effects	
				if(Gbl.random.nextDouble()<Math.exp(this.degree_saturation_rate * person1.getKnowledge().getEgoNet().getOutDegree())){
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
	/**
	 * Adds a single directed link from person1 to person2 of type "type".
	 * If network is
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
	 *                
	 * @param iteration
	 *                The socializing iteration (iteration of the social network)
	 *                
	 * @param linkType
	 *                The type of social interaction
	 *   
	 *                
	 * @return int
	 *              <li>status=0: means that person1=person2. No self-links are made</li>
	 *              <li>status=1: means existing link was renewed</li>
	 *              <li>status=2: means the new link was made</li>
	 *              <li>status=3: means that the link was not made because the ego is saturated with links</li>
	 *              
	 * @author jhackney
	 */
	public int makeSocialContactNotify(Person person1, Person person2, int iteration, String linkType) {

		int status=0;
		SocialNetEdge newLink;
		SocialNetEdge newOpposingLink;

		if (!person1.equals(person2)) {

//			NOTE this could be made more efficient by directly accessing
//			the link and testing for null
			if(person1.getKnowledge().getEgoNet().knows(person2)){

				newLink = person1.getKnowledge().getEgoNet().getEgoLink(person2);
				newOpposingLink = person2.getKnowledge().getEgoNet().getEgoLink(person1);

				newLink.setTimeLastUsed(iteration);
				newLink.setType(linkType);
				newLink.incrementNumberOfTimesMet();
				newOpposingLink.setTimeLastUsed(iteration);
				newOpposingLink.setType(linkType);
				newOpposingLink.incrementNumberOfTimesMet();
				status=1; // status=1 means existing link was renewed
			} else 
//				They do not know each other, make new link subject to saturation effects	
				if(Gbl.random.nextDouble()<Math.exp(this.degree_saturation_rate * person1.getKnowledge().getEgoNet().getOutDegree())){
					newLink = new SocialNetEdge(person1, person2);
					addLink(newLink,iteration, linkType);
					linksList.add(newLink);
//					New symmetric link if undirected network
					if(UNDIRECTED){
						newLink = new SocialNetEdge(person2, person1);
						addLink(newLink,iteration, linkType);
					}
					status=2;// status=2 means the new link was made
				}else status=3; // status=3 means that the link was not made because the ego is saturated with links
		}else{
			status=0; // status=0 means that person1=person2 and no self-links are made
		}
		return status;
	}

	public void addLink(SocialNetEdge myLink, int iteration){

		myLink.getPersonFrom().getKnowledge().getEgoNet().addEgoLink(myLink);
		myLink.setTimeMade(iteration);
		myLink.setTimeLastUsed(iteration);
	}

	public void addLink(SocialNetEdge myLink, int iteration, String linkType){
		myLink.getPersonFrom().getKnowledge().getEgoNet().addEgoLink(myLink);
		myLink.setTimeMade(iteration);
		myLink.setTimeLastUsed(iteration);
		myLink.setType(linkType);
	}    

	/**
	 * Removes links each iteration of the socializing dynamic.
	 * Each algorithm flags edges for removal, which are removed in a call to {@link removeFlaggedLinks}. Removal automatically handles either DIRECTED or UNDIRECTED graphs.
	 * <br><br>
	 * Error message results for situations in which the flagging algorithm has not yet been tested (i.e. DIRECTED networks in some algorithms).
	 * <br><br>
	 * The linkRemovalCondition determines the algorithm for removing the links,
	 * using parameters set in the configuration file, see
	 * {@link org.matsim.config.groups.SocNetConfigGroup.java}.
	 * <br><br>
	 * Each algorithm uses the parameter remove_age (measured in iterations) as a threshold
	 * under which no link is flagged and above which the algorithm begins processing links.
	 * <br><br>
	 * 
	 *  <li>"none": no removal</li>
	 *  <li>"random": iterates through edges and removes each with a probability "remove_p"
	 *  <li>"random_node_degree" iterates through edges and removes each with probability proportional to the normalized degree of the "From" person times the probability "remove_p". Degree is normalized by dividing by the graph maximimum degree.</li>
	 *  <li>"random_link_age" iterates through edges and removes each with probability proportional to the normalized link age times probability remove_p. Link age is normalized by dividing by the iteration number.</li>
	 *  <li>"random_constant_kbar" keeps average degree constant. First calculates the number of edges to remove, then randomly picks this number of random edge indices and removes these edges.
	 *  </li> 
	 * <br><br>
	 * @param iteration
	 *                 The iteration of the socializing dynamic
	 * @author jhackney
	 */
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
			int maxDeg=this.getMaxDegree();
			Iterator<SocialNetEdge> it_link = this.getLinks().iterator();
			while (it_link.hasNext()) {
				SocialNetEdge myLink = it_link.next();
				double randremove=Gbl.random.nextDouble();
				int degree =myLink.getPersonFrom().getKnowledge().getEgoNet().getOutDegree();
				if(degree > maxDeg){
					Gbl.errorMsg(this.getClass()+" degree of person "+myLink.getPersonFrom().getId()+" = "+degree+" > maxDegree="+maxDeg);
				}
				if ((iteration - myLink.getTimeLastUsed()) > remove_age && randremove<remove_p*((double)degree/(double)maxDeg)) {
					linksToRemove.add(myLink);
				}
			}
			log.info("  Number of links after removal: "+(this.getLinks().size()-linksToRemove.size()));

		}else if(linkRemovalCondition.equals("random_link_age")){
			// Removal probability proportional to edge age
			log.info("  Removing links proportional to age times probability "+remove_p);
			log.info("  Number of links before removal: "+this.getLinks().size());
			Iterator<SocialNetEdge> it_link = this.getLinks().iterator();
			while (it_link.hasNext()) {
				SocialNetEdge myLink = it_link.next();
				double randremove=Gbl.random.nextDouble();
				int age =iteration - myLink.getTimeLastUsed();
				if(age > iteration){
					Gbl.errorMsg(this.getClass()+" age of edge from "+myLink.getPersonFrom().getId()+" to "+ myLink.getPersonTo().getId()+" = "+age+" > iteration ="+iteration);
				}
				if ((iteration - myLink.getTimeLastUsed()) > remove_age && randremove<remove_p*((double)age/(double)iteration) ) {
					linksToRemove.add(myLink);
				}
			}
			log.info("  Number of links after removal: "+(this.getLinks().size()-linksToRemove.size()));

		}else if(linkRemovalCondition.equals("random_constant_kbar")){
			// Removal probability proportional to edge age
			log.info("  Removing links keeping the average degree roughly constant");
			log.info("  Number of links before removal: "+this.getLinks().size());
			int kbar=Integer.parseInt(socnetConfig.getSocNetKbar());
//			int numRemoved=0;
			if (UNDIRECTED) {
				int nRemove=this.getLinks().size()-(int) ((double) kbar/2.*this.persons.size());
				log.info("  Number of links to remove: "+nRemove);
				int i=0;
				while(i<nRemove){
//					for(int i=0;i<nRemove;i++){
					int index = Gbl.random.nextInt(this.getLinks().size());
					SocialNetEdge edge = (SocialNetEdge) this.getLinks().get(index);
//					removeLink(edge);

//					SocialNetEdge myOpposingLink=edge.getPersonTo().getKnowledge().egoNet.getEgoLink(edge.getPersonFrom());
//					this.removeLink(myOpposingLink);
					if ((iteration - edge.getTimeLastUsed()) > remove_age ) {
						SocialNetEdge opposite_edge=edge.getPersonTo().getKnowledge().getEgoNet().getEgoLink(edge.getPersonFrom());
						if(!(linksToRemove.contains(opposite_edge) || linksToRemove.contains(edge))){
							linksToRemove.add(edge);
							i++;
						}
					}
//					numRemoved++;
				}
			}else if(!UNDIRECTED){
				Gbl.errorMsg(this.getClass()+" does not support DIRECTED networks yet.");
			}
			log.info("  Number of links after removal: "+(this.getLinks().size()-linksToRemove.size()));
		}else{
			Gbl.errorMsg("Supported removal algorithms: \"none\""+","+"\"random_link_age\""+", \"random_node_degree\""+", \"random\""+", \"random_constant_kbar\"");
		}
		removeFlaggedLinks(linksToRemove);
	}
	private int getMaxDegree() {
		// TODO Auto-generated method stub
		int maxDegree=0;
		Object myPersons[]= this.getNodes().toArray();

		for(int i=0;i<myPersons.length;i++){
			Person myPerson = (Person) myPersons[i];
			int deg= myPerson.getKnowledge().getEgoNet().getOutDegree();
			if(maxDegree<deg){
				maxDegree=deg;
			}
		}
		return maxDegree;
	}

	public void removeFlaggedLinks(ArrayList<SocialNetEdge> links){
		Iterator<SocialNetEdge> itltr = links.iterator();
		while (itltr.hasNext()) {
			SocialNetEdge myLink = itltr.next();
			this.removeLink(myLink);

			if (UNDIRECTED) {
				SocialNetEdge myOpposingLink=myLink.getPersonTo().getKnowledge().getEgoNet().getEgoLink(myLink.getPersonFrom());
				this.removeLink(myOpposingLink);
			}
		}
	}
	public void removeLink(SocialNetEdge myLink) {
		// Directed links are counted once per link and are not added
		// symmetrically. "linksList" is the list of directed links.
//		log.info("Removing link "+ myLink.person1.getId()+myLink.person2.getId());
		linksList.remove(myLink);
		if(myLink.equals(null)){
			System.out.println("DEBUG STOP");
		}
		myLink.getPersonFrom().getKnowledge().getEgoNet().removeEgoLink(myLink);

	}

	public ArrayList<SocialNetEdge> getLinks() {
		return linksList;
	}

	public Collection<Person> getNodes(){
		return this.persons;
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

	public boolean isUNDIRECTED(){
		return UNDIRECTED;
	}

}
