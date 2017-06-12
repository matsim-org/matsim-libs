package playground.dziemke.actitopp;

import java.util.List;

import edu.kit.ifv.mobitopp.actitopp.ActitoppPerson;
import edu.kit.ifv.mobitopp.actitopp.HActivity;
import edu.kit.ifv.mobitopp.actitopp.HDay;
import edu.kit.ifv.mobitopp.actitopp.HWeekPattern;
import edu.kit.ifv.mobitopp.actitopp.InvalidPatternException;
import edu.kit.ifv.mobitopp.actitopp.ModelFileBase;
import edu.kit.ifv.mobitopp.actitopp.RNGHelper;

public class ActiToppExample {

	private static ModelFileBase fileBase = new ModelFileBase();
	private static RNGHelper randomgenerator = new RNGHelper(1234);
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		ActitoppPerson testperson = new ActitoppPerson(
				99, 	// PersIndex
				0, 		// Kinder 0-10
				1, 		// Kinder unter 18
				55, 	// Alter
				1, 		// Beruf
				1, 		// Geschlecht
				2, 		// Raumtyp
				2			// Pkw im HH
				);		
		System.out.println(testperson);
			
		try 
		{
			testperson.generateSchedule(fileBase, randomgenerator);
		} 
		catch (InvalidPatternException e) 
		{
			e.printStackTrace();
		}
		
//		testperson.getWeekPattern().printOutofHomeActivitiesList();
		testperson.getWeekPattern().printAllActivitiesList();
		
		HWeekPattern pattern = testperson.getWeekPattern();
		
		List<HActivity> activities = pattern.getAllActivities();
		for (HActivity activity : activities) {
			if (activity.getDay().getWeekday() == 1) {
				System.out.println("Start time = " + activity.getStartTime());
				System.out.println("End time = " + activity.getEndTime());
				System.out.println("Type = " + activity.getType());
			}
		}


	}

}