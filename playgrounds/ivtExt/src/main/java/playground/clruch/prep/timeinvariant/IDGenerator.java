/**
 * 
 */
package playground.clruch.prep.timeinvariant;

import java.util.HashSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/** @author Claudio Ruch */
public class IDGenerator {

    private HashSet<Id<Person>> usedIDs;
    private int lastNewID = -1;

    /* package */ IDGenerator(HashSet<Id<Person>> usedIDs) {
        this.usedIDs = usedIDs;

    }

    /** @param usedIDs
     * @return new ID which is not yet in set usedIDs */
    /* package */ Id<Person> generateUnusedID() {
        Integer i = lastNewID + 1;
        Id<Person> newId;
        do {
            ++i;
            String newIDs = Integer.toString(i);
            newId = Id.create(newIDs, Person.class);
        } while (usedIDs.contains(newId));

        lastNewID = i;
        return newId;
    }

}
