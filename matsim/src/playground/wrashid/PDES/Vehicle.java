package playground.wrashid.PDES;

import java.util.ArrayList;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

import org.matsim.network.Link;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;

import sun.security.jca.GetInstance.Instance;

public class Vehicle extends SimUnit {

	private Person ownerPerson = null;
	private Leg currentLeg = null;
	private int legIndex;
	private Link currentLink = null;
	private int linkIndex;

	public Vehicle(Scheduler scheduler, Person ownerPerson) {
		super(scheduler);
		this.ownerPerson = ownerPerson;
	}

	@Override
	public void finalize() {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleMessage(Message m) {
		/*
		 * m.printMessageLogString(); if (m instanceof StartingLegMessage){
		 * StartingLegMessage startingLegMessage=(StartingLegMessage) m; Plan
		 * plan = ownerPerson.getSelectedPlan(); // that's the plan the person
		 * will execute ArrayList<Object> actsLegs = plan.getActsLegs(); Leg
		 * leg = (Leg)actsLegs.get(startingLegMessage.getLegIndex()); // if
		 * current lag has more links if
		 * (leg.getRoute().getLinkRoute().length>startingLegMessage.getLinkIndex()+1){ //
		 * get the next leg if ("car".equals(leg.getMode())) { // we only
		 * simulate car traffic Link nextLink =
		 * leg.getRoute().getLinkRoute()[startingLegMessage.getLinkIndex()+1]; //
		 * these are the links the agent will drive along one after the other.
		 *  } // when nicht car, was dann????????????????????????????? } }
		 */
	}

	@Override
	// put the first event of each person is action completed.
	// this is put into the MessageQueue
	public void initialize() {
		legIndex = 1;
		// we must start with linkIndex -1, because the first link on which the
		// start activity resides is not
		// in the Leg. So, for being consistent with the rest of the simulation,
		// we start with linkIndex -1
		linkIndex = -1;

		Plan plan = ownerPerson.getSelectedPlan(); // that's the plan the
													// person will execute
		ArrayList<Object> actsLegs = plan.getActsLegs();
		// the assumption here
		currentLeg = (Leg) actsLegs.get(legIndex);
		// the leg the agent performs
		double departureTime = currentLeg.getDepTime(); // the time the agent
														// departs at this
														// activity

		
		// this is the link, where the first activity took place
		currentLink = ((Act) actsLegs.get(0)).getLink();

		sendMessage(new StartingLegMessage(scheduler, this), this.unitNo, departureTime);
	}

	public Person getOwnerPerson() {
		return ownerPerson;
	}

	public Leg getCurrentLeg() {
		return currentLeg;
	}

	public int getLegIndex() {
		return legIndex;
	}

	public Link getCurrentLink() {
		return currentLink;
	}

	public int getLinkIndex() {
		return linkIndex;
	}

	public void setCurrentLink(Link currentLink) {
		this.currentLink = currentLink;
	}

	public void setLinkIndex(int linkIndex) {
		this.linkIndex = linkIndex;
	}

}
