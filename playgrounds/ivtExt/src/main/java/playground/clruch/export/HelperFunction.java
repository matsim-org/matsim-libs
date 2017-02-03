package playground.clruch.export;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.data.Vehicle;

class HelperFunction {

    /**
     * checks if person with id is a person or an "av-driver", i.e. a virtual agent
     * the virtual agents start with "av".
     *
     * @param id
     * @return
     */
    static boolean isPerson(Id<Person> id) {
        return !id.toString().startsWith("av_");
    }

    static boolean isAV(Id<Person> id) {
        return !isPerson(id);
    }

    static boolean vehicleisAV(Id<org.matsim.vehicles.Vehicle> id) {
        return id.toString().startsWith("av_");
    }
}
