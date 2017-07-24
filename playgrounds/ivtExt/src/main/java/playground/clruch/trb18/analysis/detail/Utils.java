package playground.clruch.trb18.analysis.detail;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class Utils {
    static public boolean isValidAgent(Id<Person> personId) {
        String rawId = personId.toString();
        return !rawId.contains("av") && !rawId.contains("pt") && !rawId.contains("bus") && !rawId.contains("freight");
    }
}
