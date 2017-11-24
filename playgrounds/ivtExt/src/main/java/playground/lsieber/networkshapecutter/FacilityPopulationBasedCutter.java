package playground.lsieber.networkshapecutter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

public class FacilityPopulationBasedCutter implements FacilitiesCutter {

    Population population;

    public FacilityPopulationBasedCutter(Population population) {
        this.population = population;
    }

    @Override
    public ActivityFacilities filter(ActivityFacilities facilities) {
        System.out.println("    running " + this.getClass().getName() + " module...");

        HashSet<Id<ActivityFacility>> population_fid = new HashSet<Id<ActivityFacility>>();

        // create Hash Set of Activity IDs in Population
        Iterator<? extends Person> itPerson = population.getPersons().values().iterator();
        while (itPerson.hasNext()) {
            Person person = itPerson.next();
            for (Plan plan : person.getPlans()) {
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Activity) {
                        Activity act = (Activity) planElement;
                        population_fid.add(act.getFacilityId());
                    }
                }
            }
        }

        // Create TreeSet of ids to delete from original facilities
        TreeSet<Id<ActivityFacility>> fid_set = new TreeSet<Id<ActivityFacility>>();
        Iterator<Id<ActivityFacility>> fid_it = facilities.getFacilities().keySet().iterator();
        while (fid_it.hasNext()) {
            Id<ActivityFacility> fid = fid_it.next();
            if (!population_fid.contains(fid)) {
                fid_set.add(fid);
            }
        }

        // delete the facilities in Tree Set from the original Facilities
        System.out.println("      Number of facilities to be cut = " + fid_set.size() + "...");
        fid_it = fid_set.iterator();
        while (fid_it.hasNext()) {
            Id<ActivityFacility> fid = fid_it.next();
            facilities.getFacilities().remove(fid);
        }
        System.out.println(" ------>>> NUmber of Facilities in new output: " + facilities.getFacilities().size());
        System.out.println("    done.");
        return facilities;
    }

}
