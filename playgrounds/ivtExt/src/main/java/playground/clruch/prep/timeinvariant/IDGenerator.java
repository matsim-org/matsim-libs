/**
 * 
 */
package playground.clruch.prep.timeinvariant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/** @author Claudio Ruch This IDGenerator takes the set of usedIDs in the constructor
 *         and then finds the largest integer value in these IDs, every call of generateUnusedID
 *         then creates an id idName + largestinteger +i where i is initialized with the largest
 *         found integer */
public class IDGenerator {

    private HashSet<Id<Person>> usedIDs;
    private Integer smallestUnusedInt;
    private final String idName = "IDGenerator";
    private final String format = "%012d";

    /* package */ public IDGenerator(HashSet<Id<Person>> usedIDs) {
        this.usedIDs = usedIDs;

        // find largest integer used in IDs
        List<Integer> foundInts = new ArrayList<>();
        for (Id<Person> id : usedIDs) {
            foundInts.add(extractLargestInt(id.toString()));
        }
        Collections.sort(foundInts);
        if (foundInts.get(foundInts.size() - 1) != null) {
            smallestUnusedInt = foundInts.get(foundInts.size() - 1);
        } else {
            smallestUnusedInt = 1;
        }
    }

    /** @param usedIDs
     * @return new ID which is not yet in set usedIDs */
    public Id<Person> generateUnusedID() {
        smallestUnusedInt = smallestUnusedInt + 1;
        String newIDs = idName + String.format(format, smallestUnusedInt);
        Id<Person> newId = Id.create(newIDs, Person.class);
        usedIDs.add(newId);
        return newId;

    }

    /** @param str
     * @return List<Integer> with all unique Numbers found in str, list is sorted smallest to highest number */
    private Integer extractLargestInt(final String str) {

        // collect unique strings
        HashSet<String> uniqueStrs = new HashSet<>();
        String number = "";
        char[] chars = str.toCharArray();
        boolean isDigit = false;
        boolean wasDigit = false;

        for (int i = 0; i < chars.length; ++i) {
            char c = chars[i];
            if (Character.isDigit(c))
                isDigit = true;
            else
                isDigit = false;

            if (isDigit) {
                number = number + c;
            }

            if ((!isDigit && wasDigit) || (i == chars.length - 1)) {
                uniqueStrs.add(number);
                number = "";
            }
            wasDigit = isDigit;
        }

        // convert, sort and return
        List<Integer> integers = new ArrayList<>();
        uniqueStrs.stream().forEach(s -> integers.add(Integer.parseInt(s)));
        Collections.sort(integers);

        if (integers.size() > 0) {
            return integers.get(integers.size() - 1);
        } else {
            return null;
        }
    }

}
