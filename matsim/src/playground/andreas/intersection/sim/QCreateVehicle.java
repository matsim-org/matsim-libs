package playground.andreas.intersection.sim;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;

public class QCreateVehicle extends PersonAlgorithm {

	// ////////////////////////////////////////////////////////////////////
	// run Method, creates a new Vehicle for every person
	// ////////////////////////////////////////////////////////////////////

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.plans.algorithms.PersonAlgorithm#run(org.matsim.plans.Person)
	 */
	@Override
	public void run(Person person) {
		Plan plan = person.getSelectedPlan();
		if (plan != null) {
			QVehicle veh = new QVehicle();
			veh.setActLegs(plan.getActsLegs());
			veh.setDriverID(person.getId().toString());
			veh.setDriver(person);
			veh.initVeh();
		}
	}

	// this function is just for testing the serialize code, not of any other
	// use!
	// TODO [DS] well, if it's for testing only, can't we move this somewhere
	// else? [MR, jan07]
	public static Object deepCopy(Object o) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		new ObjectOutputStream(baos).writeObject(o);

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

		return new ObjectInputStream(bais).readObject();
	}

}
