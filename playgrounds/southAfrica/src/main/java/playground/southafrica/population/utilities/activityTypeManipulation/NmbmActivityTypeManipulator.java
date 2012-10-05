package playground.southafrica.population.utilities.activityTypeManipulation;

import org.matsim.api.core.v01.population.PopulationWriter;

import playground.southafrica.utilities.Header;


public class NmbmActivityTypeManipulator extends ActivityTypeManipulator {

	public NmbmActivityTypeManipulator() {

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(NmbmActivityTypeManipulator.class.toString(), args);
		String population = args[0];
		String network = args[1];
		String outputPopulation = args[2];
		
		NmbmActivityTypeManipulator atm = new NmbmActivityTypeManipulator();
		atm.parsePopulation(population, network);
		atm.run();
		
		/* Write the population to file. */
		PopulationWriter pw = new PopulationWriter(atm.getScenario().getPopulation(), atm.getScenario().getNetwork());
		pw.write(outputPopulation);
		
		Header.printFooter();
	}

	@Override
	protected String getAdaptedActivityType(String activityType, double activityDuration) {
		String s = activityType;
		if(activityType.equalsIgnoreCase("s")){    			/* Shopping */
			if(activityDuration <= 30*60){
				s = "s1";
			} else if(activityDuration <= 75*60){
				s = "s2";
			} else if(activityDuration <= 115*60){
				s = "s3";
			} else if(activityDuration <= 180*60){
				s = "s4";
			} else{
				s = "s5";
			}			
		} else if(activityType.equalsIgnoreCase("l")){		/* Leisure TODO update times */
			if(activityDuration <= 30*60){
				s = "l1";
			} else if(activityDuration <= 75*60){
				s = "l2";
			} else if(activityDuration <= 115*60){
				s = "l3";
			} else if(activityDuration <= 180*60){
				s = "l4";
			} else{
				s = "l5";
			}			
		}

		return s;
	}

}
