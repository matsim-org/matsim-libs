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
		m.printMessageLogString();
		if (m instanceof StartingLegMessage){
			StartingLegMessage endActionMessage=(StartingLegMessage) m;
			Plan plan = ownerPerson.getSelectedPlan(); // that's the plan the person will execute
			ArrayList<Object> actsLegs = plan.getActsLegs();
			if (actsLegs.size()>endActionMessage.getLegIndex()+1){
				// it is clear that after an action comes a leg
				Leg leg = (Leg)actsLegs.get(endActionMessage.getLegIndex()+1);
				if ("car".equals(leg.getMode())) { // we only simulate car traffic
					Link[] route = leg.getRoute().getLinkRoute(); // these are the links the agent will drive along one after the other.
				}
				// when nicht car, was dann?????????????????????????????
			}
		}
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
		sendMessage(new StartingLegMessage(leg,ownerPerson.getId().toString(),1,0), this.unitNo, departureTime);
	}

	public Person getOwnerPerson() {
		return ownerPerson;
	}

}
