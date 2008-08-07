/* *********************************************************************** *
 * project: org.matsim.*
 * PersonExchangeKnowledge.java
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

package playground.jhackney.deprecated;

import java.util.ArrayList;

import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.population.Knowledge;
import org.matsim.population.Person;

import playground.jhackney.socialnet.SocialNetEdge;
import playground.jhackney.socialnet.SocialNetwork;

/**
 * This class contains methods to let a Person learn about a new Facility or person
 * @author J.Hackney
 */
public class PersonExchangeKnowledge {
    PersonFindKnownFacilities pff = new PersonFindKnownFacilities();
    SocialNetwork net;
    public PersonExchangeKnowledge(SocialNetwork snet){
	this.net=snet;
    }


    /**
     * Method to add a facility of given type to an agent's list of knowledge
     * @author jhackney
     * @param p
     * @param f
     * @param type
     */
    public void acquireNewKnowledge(Person p, Facility f, String type) {

	Knowledge k = p.getKnowledge();


	ActivityFacilities af = k.createActivityFacility(type);
//	If some condition is to be fulfilled, such as a score comparator,
//	you will need to either get a list of known facilities of this type,
//	or have a facility score stored in knowledge
	af.addFacility(f);

    }
    /**
     * This method takes a link and exchanges information along it about facilities
     * of a certain type that the agents at each end of the link know about. No
     * scoring, it's all random.
     *
     * @author jhackney
     * @param net
     * @param curLink
     * @param facType
     */
    public void exchangeRandomFacilityKnowledge(SocialNetEdge curLink, String facType){

	Person p2 = curLink.person2;
	Person p1 = curLink.person1;
	// get the facType facilities that person2 knows about
	ArrayList<Facility> allf2 = pff.personGetKnownFacilities(p2, facType);

	// get a random facility from Person 2's list
	// and add it to Person 1's knowledge
	if (allf2.size() > 0) {
	    Facility f21 = allf2.get(Gbl.random.nextInt(allf2.size()));
	    this.acquireNewKnowledge(p1, f21, facType);
	}

	// Vice versa (checks if graph is directed)

//	if(!p2.getKnowledge().egoNet.getEgoLinks().contains(p1)){
	    if(p2.getKnowledge().egoNet.knows(p1)){
	    ArrayList<Facility> allf1 = pff.personGetKnownFacilities(p1, facType);
	    // get a random facility from the list
	    if (allf1.size() > 0) {
		Facility f12 = allf1.get((Gbl.random.nextInt(Integer.MAX_VALUE) % allf1.size()));
		this.acquireNewKnowledge(p2, f12, facType);
	    }
	}
    }
    /**
     * This method receives a link and picks its origin person A and from A'
     * egonet, a random person B at the link's end.
     * The process is repeated for person B its egonet to find a third person, C:
     * The first, A, and and the third, B, are introduced.
     * They will make friends, provided that the first does not already
     * have enough friends (saturation).
     * NOTE: a slow-running algorithm simulating friends in a chain becoming a cluster.
     * NOTE: even if A has only 1 link, it can make a new friend this turn
     *
     * @author jhackney
     * @param net
     * @param myLink
     * @param iteration
     */
    public void randomlyIntroduceAtoCviaB(SocialNetEdge myLink, int iteration) {
//	TODO Auto-generated method stub
	Person myEgo = myLink.getPersonFrom();
	double prob_befriend = Math.exp(net.degree_saturation_rate*myEgo.getKnowledge().egoNet.getOutDegree());

//	pick an alter of this person and exchange a random alter with that person
	Person myAlter = myEgo.getKnowledge().egoNet.getRandomPerson(myEgo);

	// This network has at least Ego as an entry (if UNDIRECTED) and so will not be null
	// unless there is a mistake in adding or removing the links
	Person myAlterAlter = myAlter.getKnowledge().egoNet.getRandomPerson(myAlter);
	double rndInt4 = Gbl.random.nextDouble();
	if(rndInt4<prob_befriend){
	    if (net.UNDIRECTED) {
		net.generateUndirectedLink(myEgo, myAlterAlter, iteration);
	    } else
		net.generateDirectedLink(myEgo, myAlterAlter, iteration);
	}
    }
    /**
     * This method receives a link and picks its origin person A and two
     * random friends from A's egonet, B anc C.
     * B and C are introduced.
     * They will make friends, provided that B does not already
     * have enough friends (saturation).
     * NOTE: it does not take into account whether C is saturated with friends.
     * NOTE: nothing happens if A only has one friend
     *
     * @author jhackney
     * @param net
     * @param myLink
     * @param iteration
     */
    public void randomlyIntroduceBtoCviaA(SocialNetEdge myLink, int iteration){
	Person myEgo = myLink.getPersonFrom();
	Knowledge k0 = myEgo.getKnowledge();

	double prob_befriend = Math.exp(net.degree_saturation_rate*myEgo.getKnowledge().egoNet.getOutDegree());

	Person friend1 = k0.egoNet.getRandomPerson( myEgo );
	Person friend2 = k0.egoNet.getRandomPerson( myEgo );
	if( (friend1!=null) && (friend2!=null) ){

	    if(Math.random() < prob_befriend ){
		if (net.UNDIRECTED) {
		    net.generateUndirectedLink(friend1, friend2, iteration);
		} else{
		    net.generateDirectedLink(friend1, friend2, iteration);
		}
	    }
	}
    }
}
