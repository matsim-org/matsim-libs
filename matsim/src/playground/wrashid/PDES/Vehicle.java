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
		m.printMessageLogString();
		if (m instanceof StartingLegMessage){
			StartingLegMessage startingLegMessage=(StartingLegMessage) m;
			Plan plan = ownerPerson.getSelectedPlan(); // that's the plan the person will execute
			ArrayList<Object> actsLegs = plan.getActsLegs();
			Leg leg = (Leg)actsLegs.get(startingLegMessage.getLegIndex());
			// if current lag has more links
			if (leg.getRoute().getLinkRoute().length>startingLegMessage.getLinkIndex()+1){
				// get the next leg
				if ("car".equals(leg.getMode())) { // we only simulate car traffic
					Link nextLink = leg.getRoute().getLinkRoute()[startingLegMessage.getLinkIndex()+1]; // these are the links the agent will drive along one after the other.
					
				}
				// when nicht car, was dann?????????????????????????????
			}
		}
		*/
	}

	@Override
	// put the first event of each person is action completed. 
	// this is put into the MessageQueue 
	public void initialize() {
		Plan plan = ownerPerson.getSelectedPlan(); // that's the plan the person will execute
		ArrayList<Object> actsLegs = plan.getActsLegs();
		// the assumption here 
		Leg leg = (Leg)actsLegs.get(1);
		// the leg the agent performs
		double departureTime = leg.getDepTime(); // the time the agent departs at this activity
		sendMessage(new StartingLegMessage(leg,this,1,0,((Act)actsLegs.get(0)).getLink()), this.unitNo, departureTime);
	}

	public Person getOwnerPerson() {
		return ownerPerson;
	}

}
