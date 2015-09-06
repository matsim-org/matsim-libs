package playground.pieter.singapore.utils.plans;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PersonImpl;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;

/**
 * @author fouriep
 * 
 */
class PlanExtractorFix {

	/**
	 * @param assignedMode
	 * @param suggestedMode
	 * @return true if the plan needs to be removed or not
	 */
	private boolean checkForRemoval(String assignedMode, String suggestedMode) {
		if (suggestedMode.equals("notravel"))
			return true;
		// if not explicitly assigned, keep the plan
		if (suggestedMode.equals("not_assigned"))
			return false;
		// if the mode assignment suggests pt for a car ownerdriver, keep the
		// plan, cos it might be ransformed to pt
		if (suggestedMode.equals("car") && assignedMode.equals("pt"))
			return false;
		// keep 50% of plans that were assigned mix of pt and car as passenger
        return assignedMode.equals("ptmix") && Math.random() > 0.5;

    }

	public void run(Population plans) {
		HashMap<Integer, String> paxTravelLookup = new HashMap<>();
		try {
			DataBaseAdmin dba = new DataBaseAdmin(new File(
					"data/matsim2.properties"));
			int planCount = 0;
			int carCount = 0;
			int carRemovalCount = 0;
			System.out.println("    running " + this.getClass().getName()
					+ " algorithm...");

			TreeSet<Id<Person>> pid_set = new TreeSet<>(); // ids of persons to remove
			Iterator<Id<Person>> pid_it = plans.getPersons().keySet().iterator();
			boolean removePerson = false;
			while (pid_it.hasNext()) {
				
				Id<Person> personId = pid_it.next();
				Person person = plans.getPersons().get(personId);

				String assignedMode = PersonImpl.getCarAvail(person).equals("always")?"car":"pt";
				if(assignedMode.equals("car"))
					carCount++;
				ResultSet rs = dba
						.executeQuery(String
								.format("SELECT travel from synthpop_travel_assignment where full_pop_pid = %s",
										personId.toString()));
				rs.next();
				String suggestedMode = rs.getString("travel");
				removePerson = checkForRemoval(assignedMode, suggestedMode);
				if (removePerson) {
					// the person has no plans left. remove the person
					// afterwards
					// (so we do not disrupt the Iterator)
					if(assignedMode.equals("car"))
							carRemovalCount++;
					pid_set.add(personId);
				}
				planCount++;
				if(planCount % 10000 ==0){
					System.out.println("Number of persons marked for removal: " + pid_set.size() + " out of " + planCount);
					System.out.println("of which were car plans:              " + carRemovalCount + " out of " + carCount);
//					break;
				}

			}

			// okay, now remove in a 2nd step all persons we do no longer need
			pid_it = pid_set.iterator();
			while (pid_it.hasNext()) {
				Id pid = pid_it.next();
				plans.getPersons().remove(pid);
			}

			System.out.println("    done.");
			System.out.println("Number of persons removed: " + pid_set.size() + " out of " + planCount);
			System.out.println("of which were car plans:   " + carRemovalCount + " out of " + carCount);
		} catch (InstantiationException | NoConnectionException | SQLException | IOException | ClassNotFoundException | IllegalAccessException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    }
}
