package playground.dziemke.analysis.general.srv;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.households.Household;

/**
 * @author gthunig on 30.03.2017.
 */
class Srv2MatsimPopulationUtils {

    private final static Logger log = Logger.getLogger(Srv2MatsimPopulationUtils.class);

    static Id<Person> createUniquePersonId(Id<Household> householdId, Id<Person> personId) {
        return Id.create(householdId + "_" + personId, Person.class);
    }

    // see p.45 Aufbereitungsbericht
    static String transformActType(int actTypeCode) {
        switch (actTypeCode) {
            case -9: return "other";
            case 1: return "work";
            case 2: return "work";
            case 3: return "other";
            case 4: return "other";
            //case 4: return "educ";
            case 5: return "other";
            //case 5: return "educ";
            case 6: return "other";
            //case 6: return "educ";
            case 7: return "shop";
            case 8: return "shop";
            case 9: return "other";
            case 10: return "leis";
            case 11: return "leis";
            case 12: return "leis";
            case 13: return "leis";
            case 14: return "leis";
            case 15: return "leis";
            case 16: return "leis";
            case 17: return "home";
            case 18: return "other";
            default:
                log.error(new IllegalArgumentException("actTypeNo="+actTypeCode+" not allowed."));
                //Gbl.errorMsg(new IllegalArgumentException("actTypeNo="+actTypeCode+" not allowed."));
                return null;
        }
    }
}
