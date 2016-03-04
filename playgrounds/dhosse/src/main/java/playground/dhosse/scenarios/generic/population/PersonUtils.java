package playground.dhosse.scenarios.generic.population;

import org.matsim.api.core.v01.population.Person;

import playground.dhosse.scenarios.generic.utils.ActivityTypes;

public class PersonUtils {
	
	static final String ATT_AGE = "age";
	static final String ATT_SEX = "sex";

	public static String getEducationalActTypeForPerson(Person person){
		
		int age = (int) person.getCustomAttributes().get(ATT_AGE);
		
		if(age < 6){
			
			return ActivityTypes.KINDERGARTEN;
			
		} else if(age >= 6 && age < 13){
			
			return ActivityTypes.PRIMARY_SCHOOL;
			
		} else if(age >= 13 && age < 18){
			
			return ActivityTypes.SECONDARY_SCHOOL;
			
		} else{
			
			return ActivityTypes.UNIVERSITY;
			
		}
		
	}
	
}
