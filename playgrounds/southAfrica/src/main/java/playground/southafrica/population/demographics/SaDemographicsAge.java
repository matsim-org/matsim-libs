package playground.southafrica.population.demographics;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;

import playground.southafrica.population.capeTownTravelSurvey.PersonEnums;

/**
 * Class to convert a numeric age into a predefined class.
 * 
 * @author jwjoubert
 */
public enum SaDemographicsAge {
	Infant, Child, Young, EarlyCareer, LateCareer, Retired;
	
	private final static Logger LOG = Logger.getLogger(SaDemographicsAge.class);
	
	/**
	 * Converts a given age into one of the following classes:
	 * <ul>
	 * 		<li><b>Infant</b>: age <= 5;
	 * 		<li><b>Child</b>: 5 < age <= 12;
	 * 		<li><b>Young</b>: 12 < age <= 23;
	 * 		<li><b>EarlyCareer</b>: 23 < age <= 45;
	 * 		<li><b>LateCareer</b>: 45 < age <= 68;
	 * 		<li><b>Retired</b>: > 68;
	 * </ul>
	 * @param age
	 * @return
	 */
	public static SaDemographicsAge getAgeClass(double age){
		if(age <= 5){
			return Infant;
		} else if(age <= 12){
			return Child;
		} else if(age <= 23){
			return Young;
		} else if(age <= 45){
			return EarlyCareer;
		} else if(age <= 68){
			return LateCareer;
		} else{
			return Retired;
		}
	}
	
	/**
	 * To be compatible with this Census 2011, the Class {@link PersonEnums.AgeGroup}
	 * converts the year of birth to an age class, but retains the 'unknown' 
	 * option. Here we just report those as 'retired'. Otherwise, the age 
	 * classification is the same as the method {@link #getAgeClass(double)}.
	 * @param ageClass
	 * @return
	 */
	public static SaDemographicsAge getCapeTown2013AgeClass(String ageClass){
		PersonEnums.AgeGroup ageGroup = PersonEnums.AgeGroup.parseFromDescription(ageClass);
		switch (ageGroup) {
		case INFANT:
			return Infant;
		case YOUNG:
			return Young;
		case CHILD:
			return Child;
		case EARLYCAREER:
			return EarlyCareer;
		case LATECAREER:
			return LateCareer;
		case RETIRED:
		case UNKNOWN:
			return Retired;
		}
		return Retired;
	}
	
	/**
	 * Make single step-change to age class. If 'Infant' or 'Retired', then
	 * the change will be in the direction of the only neighbour. Otherwise,
	 * the step will be randomly to either neighbour in the class. 
	 * 
	 * @param age
	 * @return a neighbouring age class, or 'EarlyCareer' be default.
	 */
	public static SaDemographicsAge getAgePerturbation(SaDemographicsAge age){
		switch (age) {
		case Infant:
			return Child;
		case Child:
			return MatsimRandom.getRandom().nextDouble() < 0.5 ? Infant : Young;
		case Young:
			return MatsimRandom.getRandom().nextDouble() < 0.5 ? Child : EarlyCareer;
		case EarlyCareer:
			return MatsimRandom.getRandom().nextDouble() < 0.5 ? Young : LateCareer;
		case LateCareer:
			return MatsimRandom.getRandom().nextDouble() < 0.5 ? EarlyCareer : Retired;
		case Retired:
			return LateCareer;
		}
		LOG.warn("Unknown age class: " + age.toString() + "; returning 'EarlyCareer'");
		return EarlyCareer;
	}
	

}
