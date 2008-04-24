/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialInteractor.java
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
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;

import playground.jhackney.socialnet.SocialNetwork;

/**
 * Persons meet in space and interact. The idea is that face-to-face communication
 * is the means by which social links are rejuvenated and networks are reinforced.
 * Note this is becoming fundamentally different from the non-spatial exchange of information that
 * is also currently called an "interactor", such that it may make sense to introduce two
 * separate types of interactor interfaces: spatial and non-spatial.
 * Put in the desired algorithm to modify social links (here, a method called "generateLinks1"):
 * Simplest: Persons visiting same facility are linked with probabilityOfMakingNewLink.
 * Smarter: Persons visiting same facility in time window with probabilityOfMakingNewLink.
 * Better: Persons visiting same facility in time window with minimum duration.
 * Better: Persons visiting same facility in time window with min duration and min frequency
 * (this is a history we don't have)
 *
 * @author J. Hackney
 * @deprecated
 */
@Deprecated
public class SpatialInteractor {

    SocialNetwork net;
    Object persons[];

    double probabilityOfMakingNewLink = Double.parseDouble(Gbl.getConfig().socnetmodule().getPBefriend());// [0.0,1.0]
    double pct_interacting = Double.parseDouble(Gbl.getConfig().socnetmodule().getFractSInteract());// [0.0,1.0]
    String interaction_type = Gbl.getConfig().socnetmodule().getSocNetInteractor2();

    PersonFindVisitedLocations pff = new PersonFindVisitedLocations();

    public SpatialInteractor(SocialNetwork snet) {
	this.net=snet;
    }

    /**
     * Implement spatial interactions. Note that the fraction of meetings is for
     * the average population (e.g. 40% of relationships begin at work) but might not
     * be correct for an individual (e.g. if a person has no workplace, but only education,
     * it could be that 100% of relationships are formed in school, yet for the average
     * over the population, this might be some other number, like 10%. Thus this person with
     * no job might be given far too few opportunities to make friends). How to handle this?
     * Evidently one has to re-scale the weights to 1.0 AFTER knowing WHICH facilities a
     * particular person has visited ... ?
     * @author jhackney
     *
     */
    public void interact(Plans plans, String[] facTypes, double[] facFractions, int iteration) {
	// TODO make this faster by pre-picking which agents meet in which facilities,
	// instead of doing the whole loop num-facilities x num-agents times

	String facType;

	System.out.println(" Making the TreeMap of all persons who visited facilities for iteration " + iteration + "...");
	for (int j = 0; j < facTypes.length; j++) {

//	    Only track the visits to Facility types we are interested in
	    if(facFractions[j]>0){
		facType = facTypes[j];

//		Define the table of who was where
		TreeMap<Integer, ArrayList<Person>> locationVisitTable =whoVisitedWhere(facType, plans);
		System.out.println("...finished.");

		System.out.println(" Iterating through facility ID's and linking people with each other");
		personsMeetFaceToFace(locationVisitTable, iteration, facFractions[j], facType);
		System.out.println("...finished");
	    }
	}
    }

    private void personsMeetFaceToFace(TreeMap<Integer, ArrayList<Person>> locationVisitTable, int iteration, double d, String facType) {
	// TODO Auto-generated method stub
	System.out.println("  "+pct_interacting+" fraction of the following people per facility will have a chance to initiate an interaction.");
	Set visitedLocations = locationVisitTable.keySet();

	Iterator vFIter = visitedLocations.iterator();

	// At this point decide whether the facFractions, which is valid only for the population average, should be modified for a particular person or plan
	while (vFIter.hasNext()) {
	    int newLocationId = (Integer) vFIter.next();
	    ArrayList<Person> personsAtLocation = locationVisitTable.get(newLocationId);
	    System.out.println("   Location (link) " + newLocationId + " with facility of activity type \"" + facType
		    + "\" has " + personsAtLocation.size() + " interacting people");

	    // Now call the appropriate link generation algorithm, depending
	    // on what rules you want to use for making friends face-to-face
	    // Simple one: all agents visiting same facility are befriended
	    // with p<=1. Note that this would not be an interaction but an
	    // associative algorithm that belongs in the family of network
	    // generators.
	    // Really this Interactor shouldn't make network links (or, at the least,
	    // an Interactor should be a kind of network object). It
	    // should call the network generator to modify them.
	    generateLinks1(personsAtLocation, iteration, d);
	}
	System.out.println(" Clearing \"Visited\" table of facility type " + facType);
    }

    private TreeMap<Integer, ArrayList<Person>> whoVisitedWhere(String facType, Plans plans) {
	// TODO Make this faster or put it in a different structure...see fabrice' CoolPlaces
	TreeMap<Integer, ArrayList<Person>> locationVisitTable=new TreeMap<Integer, ArrayList<Person>>();
	ArrayList<Person> peeple= new ArrayList<Person>(plans.getPersons().values());

	persons = peeple.toArray();

	int numIter = persons.length;
	for (int i = 0; i < numIter; i++) {
	    Person p1 = (Person) persons[i];
	    ArrayList<LinkImpl> allf1 = pff.personGetVisitedLocations(p1, facType);
	    Iterator f1Iter = allf1.iterator();
	    while (f1Iter.hasNext()) {
		LinkImpl p1VisitedLocation = (LinkImpl) f1Iter.next();
		int myLocationId = Integer.parseInt(p1VisitedLocation.getId().toString());
		if (locationVisitTable.get(myLocationId) != null) {

		    // then a visit to this location has been recorded
		    // if p1 has not been there, add p1 to the list
		    if (!locationVisitTable.get(myLocationId).contains(p1)) {
			locationVisitTable.get(myLocationId).add(p1);
		    }
		} else { // the facility has not been visited
		    ArrayList<Person> newList = new ArrayList<Person>();
		    System.out.println(" |adding "+facType+" facility " + p1VisitedLocation.getId() + " to \"Visited\" table");
		    newList.add(p1);
		    locationVisitTable.put(myLocationId, newList);
		}
	    }
	}
	return locationVisitTable;
    }

    /**
     * This method randomly associates people in the spatial places they visited,
     * without regard to the time of day they were there. First, it picks a subset
     * of the people to treat. Then, for each of them, it picks a random individual
     * who was also at the location that day. If they already know each other, their
     * friendship is immediately renewed. Else, if a random criterion is fulfilled,
     * they will make friends, as long as the first person's saturation allows.
     * @author jhackney
     * @param people
     * @param iteration
     * @param pFacility
     */
    public void generateLinks1(ArrayList<Person> people, int iteration, double pFacility) {

	java.util.Collections.shuffle(people);
	Object personList[] = people.toArray();
	int numPersons = personList.length;
	for (int i = 0; i < (int) (numPersons * pct_interacting); i++) {
	    Person person1 = (Person) personList[i];
	    for (int j = 0; j < numPersons; j++) {
		Person person2 = (Person) personList[j];

//		If the two are friends, renew the friendship
		double rand = Gbl.random.nextDouble();
		if(person1.getKnowledge().egoNet.knows(person2)){
		    if(rand < pFacility){
			net.makeSocialContact(person1, person2, iteration);
		    }
		}else{

//		    If the two do not already know each other
		    double total_probability = probabilityOfMakingNewLink*pFacility*Math.exp(net.degree_saturation_rate*person1.getKnowledge().egoNet.getOutDegree());
		    if (rand < total_probability) {
			net.makeSocialContact(person1, person2, iteration);
		    }
		}
	    }
	}
    }
}
