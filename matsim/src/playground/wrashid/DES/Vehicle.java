package playground.wrashid.DES;

import java.util.ArrayList;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

import org.matsim.network.Link;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;

import sun.security.jca.GetInstance.Instance;

public class Vehicle extends SimUnit {

	private Person ownerPerson = null;
	private Leg currentLeg = null;
	private int legIndex;
	private Link currentLink = null;
	private int linkIndex;
	private Link[] currentLinkRoute=null;

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
		 * these are the links the agent will drive along one after the other. } //
		 * when nicht car, was dann????????????????????????????? } } => delete
		 * this 'comment', as soon tests are written and run
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
		setCurrentLeg((Leg) actsLegs.get(legIndex));
		// the leg the agent performs
		double departureTime = getCurrentLeg().getDepTime(); // the time the agent
		// departs at this
		// activity

		// this is the link, where the first activity took place
		currentLink = ((Act) actsLegs.get(legIndex - 1)).getLink();

		Road road=Road.allRoads.get(getCurrentLink().getId().toString());
		scheduleStartingLegMessage(departureTime, road);
		
		//TODO: Das hier noch einbauchen
		//if ("car".equals(leg.getMode())) { // we only simulate car traffic
		//	Link[] route = leg.getRoute().getLinkRoute(); // these are the links the agent will drive along one after the other.
		//}
		
		
		
		//System.out.println("departureTime:"+departureTime+";simTime:"+scheduler.simTime);
	}

	public void setCurrentLeg(Leg currentLeg) {
		this.currentLeg = currentLeg;
		currentLinkRoute=currentLeg.getRoute().getLinkRoute();
	}
	
	public Link[] getCurrentLinkRoute(){
		return currentLinkRoute;
	}
	

	public void setLegIndex(int legIndex) {
		this.legIndex = legIndex;
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

	// findes out, if the vehical is in endingLegMode
	// this means, that the vehical is just waiting until it can enter the
	// last link (without entering it) and then ends the leg
	public boolean isEndingLegMode() {
		if (getCurrentLinkRoute().length == getLinkIndex()) {
			return true;
		} else {
			return false;
		}
	}

	public void initiateEndingLegMode() {
		linkIndex = getCurrentLinkRoute().length;
	}

	// public void leavePreviousRoad(){
	// leave previous road (if there is a previous road)
	// if (this.getLinkIndex()>=1){
	// Link
	// previousLink=this.getCurrentLeg().getRoute().getLinkRoute()[this.getLinkIndex()-1];
	// Road previousRoad=Road.allRoads.get(previousLink.getId().toString());
	// previousRoad.leaveRoad(this);
	// EventMessage.printLogMessage(Scheduler.simTime,
	// Integer.parseInt(getOwnerPerson().getId().toString()),getLegIndex()-1,Integer.parseInt(previousLink.getId().toString()),Integer.parseInt(previousLink.getFromNode().getId().toString()),Integer.parseInt(previousLink.getToNode().getId().toString()),SimulationParameters.LEAVE_LINK);
	// }
	// }

	public void scheduleEnterRoadMessage(double scheduleTime, Road road) {
		// before entering the new road, we musst leave the previous road (if there is a previous road)
		if (this.getLinkIndex()>=0){
			// the first link does not need to be left (which has index -1)
			scheduleLeavePreviousRoadMessage(scheduleTime);
		}
		
		if (isEndingLegMode()) {
			// attention: as we are not actually entering the road, we need to
			// give back the promised space to the road
			// else a precondition of the enterRequest would not be correct any
			// more (which involves the noOfCarsPromisedToEnterRoad variable)
			road.giveBackPromisedSpaceToRoad(); // next road
			scheduleEndLegMessage(scheduleTime, road);
		} else {
			sendMessage(MessageFactory.getEnterRoadMessage(road.scheduler, this), road
					.getUnitNo(), scheduleTime);
		}
	}

	public void scheduleLeavePreviousRoadMessage(double scheduleTime) {
		
		Road previousRoad=null;
		Link previousLink=null;
		if (this.getLinkIndex()==0){
			Plan plan = ownerPerson.getSelectedPlan(); 
			ArrayList<Object> actsLegs = plan.getActsLegs();
			previousLink = ((Act) actsLegs.get(legIndex - 1)).getLink();
			//System.out.println("AscheduleLeavePreviousRoadMessage:"+previousLink.getId().toString());
			previousRoad=Road.allRoads.get(previousLink.getId().toString());
		} else if (this.getLinkIndex()>=1){
			previousLink=this.getCurrentLinkRoute()[this.getLinkIndex()-1];
			//System.out.println("BscheduleLeavePreviousRoadMessage:"+previousLink.getId().toString());
			previousRoad=Road.allRoads.get(previousLink.getId().toString());
		}
		
		scheduleLeaveRoadMessage(scheduleTime, previousRoad);
		
		//
		/*
		if (this.getLinkIndex()>=1){
			// the first link does not need to be left (which has index -1)
			Road previousRoad=Road.allRoads.get(this.getCurrentLink().getId().toString());
			scheduleLeaveRoadMessage(scheduleTime, previousRoad);
		}	
		*/
	}

	public void scheduleEndRoadMessage(double scheduleTime, Road road) {
		sendMessage(MessageFactory.getEndRoadMessage(road.scheduler, this), road.getUnitNo(),
				scheduleTime);
	}

	public void scheduleLeaveRoadMessage(double scheduleTime, Road road) {
		sendMessage(MessageFactory.getLeaveRoadMessage(road.scheduler, this), road
				.getUnitNo(), scheduleTime);
	}

	public void scheduleEndLegMessage(double scheduleTime, Road road) {
		sendMessage(MessageFactory.getEndLegMessage(road.scheduler, this), road.getUnitNo(),
				scheduleTime);
	}
	
	public void scheduleStartingLegMessage(double scheduleTime, Road road) {
		sendMessage(MessageFactory.getStartingLegMessage(road.scheduler, this), road.getUnitNo(),
				scheduleTime);
	}

	public DeadlockPreventionMessage scheduleDeadlockPreventionMessage(double scheduleTime, Road road) {
		DeadlockPreventionMessage dpMessage= MessageFactory.getDeadlockPreventionMessage(road.scheduler, this);
		sendMessage(dpMessage, road.getUnitNo(),scheduleTime);
		return dpMessage;
	}
	
}
