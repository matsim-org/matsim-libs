package playground.wrashid.PDES;

import org.matsim.plans.Person;

public class Vehicle extends SimUnit {

	private Person ownerPerson = null;

	public Vehicle(Person ownerPerson) {
		super();
		this.ownerPerson = ownerPerson;
	}

	@Override
	public void finalize() {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleMessage(Message m) {
		// TODO Auto-generated method stub

	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub

	}

}
