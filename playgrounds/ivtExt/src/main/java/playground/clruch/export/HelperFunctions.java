package playground.clruch.export;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

class HelperFunctions {
    public static final String PREFIX = "av_";

    /**
     * checks if person with id is a real person or an "av-driver", i.e. a virtual agent
     * the virtual agents start with "av".
     *
     * @param id
     * @return
     */
    static boolean isHuman(Id<Person> id) {
        return !id.toString().startsWith(PREFIX);
    }

    static boolean isPersonAV(Id<Person> id) {
        return !isHuman(id);
    }

    static boolean isVehicleAV(Id<Vehicle> id) {
        return id.toString().startsWith(PREFIX);
    }
}
