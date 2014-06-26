package playground.southafrica.population.demographics;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;

/**
 * Class to convert a numeric number of household members into a predefined 
 * class.
 * 
 * @author jwjoubert
 */
public enum SaDemographicsHouseholdSize {
	Small, Medium, Large;
	
	final private static Logger LOG = Logger.getLogger(SaDemographicsHouseholdSize.class);
	
	/**
	 * Converts a given number of household members into one of the following 
	 * classes:
	 * <ul>
	 * 		<li><b>Small</b>: 0, 1 or 2 members;
	 * 		<li><b>Medium</b>: 3 - 10 members; and
	 * 		<li><b>Large</b>: greater than 10 members.
	 * </ul>
	 * @param numberOfhouseholdMembers
	 * @return
	 */
	public static SaDemographicsHouseholdSize getHouseholdSizeClass(int numberOfHouseholdMembers){
		if(numberOfHouseholdMembers <= 2){
			return Small;
		} else if(numberOfHouseholdMembers <= 10){
			return Medium;
		} else{
			return Large;
		}
	}
	
	
	/**
	 * Make single step-change to household size class. If 'Small' or 'Large', 
	 * the change will be to 'Medium'. If 'Medium', the change will be equally 
	 * likely to either 'Small' or 'Large'.
	 *
	 * @param age
	 * @return a neighbouring household size class, or 'Medium' be default.
	 */
	public static SaDemographicsHouseholdSize getHouseholdSizePerturbation(SaDemographicsHouseholdSize householdSize){
		switch (householdSize) {
		case Small:
			return Medium;
		case Medium:
			return MatsimRandom.getRandom().nextDouble() < 0.5 ? Small : Large;
		case Large:
			return Medium;
		}
		LOG.warn("Unknown household size class: " + householdSize.toString() + "; returning 'Medium'");
		return Medium;
	}

	

}
