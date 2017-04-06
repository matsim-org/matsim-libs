package playground.johannes.synpop.util;

import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;

import java.util.Set;

/**
 * Created by johannesillenberger on 06.04.17.
 */
public class PopulationStats {

    private int numPersons;
    private int numEpisodes;
    private int numActivities;
    private int numLegs;

    public int getNumPersons() {
        return numPersons;
    }

    public int getNumEpisodes() {
        return numEpisodes;
    }

    public int getNumActivities() {
        return numActivities;
    }

    public int getNumLegs() {
        return numLegs;
    }

    public void run(Set<? extends Person> persons) {
        numPersons = persons.size();
        for(Person person : persons) {
            for(Episode episode : person.getEpisodes()) {
                numActivities += episode.getActivities().size();
                numLegs += episode.getLegs().size();
            }
            numEpisodes += person.getEpisodes().size();
        }
    }
}
