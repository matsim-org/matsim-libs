package playground.dziemke.analysis.general.srv;

import org.matsim.api.core.v01.population.Person;

/**
 * @author gthunig on 30.03.2017.
 */
public class SrvPersonUtils {

    //custom attributes
    private static final String CA_WEIGHT = "weight";
    private static final String CA_STUDENT = "student";

    static final String HOUSEHOLD_ID = "HHNR";
    static final String PERSON_ID = "PNR";

    static final String WEIGHT = "GEWICHT_P";
    static final String AGE = "V_ALTER";
    static final String SEX = "V_GESCHLECHT";
    static final String EMPLOYED = "V_ERW";

    static final String DRIVERS_LICENCE = "V_FUEHR_PKW";
    // other possible variables: locationOfWork, locationOfSchool, parent

    static double getWeight(Person person) {
        return (double) person.getCustomAttributes().get(CA_WEIGHT);
    }

    static void setWeight(Person person, double weight) {
        //person.getCustomAttributes().put(CA_WEIGHT, weight);
        person.getAttributes().putAttribute(CA_WEIGHT, weight);
    }

    static Boolean isStudent(Person person) {
        return (Boolean) person.getCustomAttributes().get(CA_STUDENT);	}

    static void setStudent(Person person, Boolean student) {
        if (student!=null){
            //person.getCustomAttributes().put(CA_STUDENT, student);
            person.getAttributes().putAttribute(CA_STUDENT, student);
        }
    }
}
