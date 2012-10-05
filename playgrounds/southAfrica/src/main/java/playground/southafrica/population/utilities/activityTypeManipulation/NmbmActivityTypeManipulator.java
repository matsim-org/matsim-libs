package playground.southafrica.population.utilities.activityTypeManipulation;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.misc.Time;

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
	protected void adaptActivityTypes(Plan plan) {
		for(int i = 0; i < plan.getPlanElements().size(); i++){
			PlanElement pe = plan.getPlanElements().get(i);
			if(pe instanceof ActivityImpl){
				ActivityImpl act = (ActivityImpl) pe;
				if(act.getType().equalsIgnoreCase("h")){
					if(i == 0){
						act.setType("h1"); /* First activity of the chain. */
					} else if(i == plan.getPlanElements().size() - 1){
						act.setType("h2"); /* Final activity of the chain. */
					} else{
						act.setType("h3"); /* Intermediate activity. */
					}
				}
					
				double estimatedDuration = 0.0;
				if(i == 0){
					/* It is the first activity, and it is assumed it 
					 * started at 00:00:00. */
					estimatedDuration = act.getEndTime();
				} else{
					/* Since the method getStartTime is deprecated,
					 * estimate the start time as the end time of the
					 * previous activity plus the duration of the trip. */
					double previousEndTime = ((ActivityImpl)plan.getPlanElements().get(i-2)).getEndTime();
					double tripDuration = ((Leg)plan.getPlanElements().get(i-1)).getTravelTime();
					estimatedDuration = act.getEndTime() - (previousEndTime + tripDuration);
				}
				
				act.setType( getNewActivityType(act.getType(), estimatedDuration) );
			}
		}
	}
	
	
	
	
	private String getNewActivityType(String activityType, double activityDuration){
		String s = activityType;

		/* Final home activity */
		if(activityType.equalsIgnoreCase("h2")){				
			if(activityDuration <= Time.parseTime("05:30:00")){
				s = "h21";
			} else if(activityDuration <= Time.parseTime("07:00:00")){
				s = "h22";
			} else if(activityDuration <= Time.parseTime("08:40:00")){
				s = "h23";
			} else if(activityDuration <= Time.parseTime("09:30:00")){
				s = "h24";
			} else{
				s = "h25";
			}			

		/* Work */
		} else if(activityType.equalsIgnoreCase("w")){				
			if(activityDuration <= Time.parseTime("02:38:00")){
				s = "w1";
			} else if(activityDuration <= Time.parseTime("07:55:00")){
				s = "w2";
			} else if(activityDuration <= Time.parseTime("08:35:00")){
				s = "w3";
			} else if(activityDuration <= Time.parseTime("09:15:00")){
				s = "w4";
			} else{
				s = "w5";
			}			

		/* Tertiary education */
		} else if(activityType.equalsIgnoreCase("e2")){    			
			if(activityDuration <= Time.parseTime("01:45:00")){
				s = "e21";
			} else if(activityDuration <= Time.parseTime("03:55:00")){
				s = "e22";
			} else if(activityDuration <= Time.parseTime("05:50:00")){
				s = "e23";
			} else if(activityDuration <= Time.parseTime("06:40:00")){
				s = "e24";
			} else{
				s = "e25";
			}			
			
		/* Shopping */
		} else if(activityType.equalsIgnoreCase("s")){  	
			if(activityDuration <= Time.parseTime("00:05:00")){
				s = "s1";
			} else if(activityDuration <= Time.parseTime("00:25:00")){
				s = "s2";
			} else if(activityDuration <= Time.parseTime("00:50:00")){
				s = "s3";
			} else if(activityDuration <= Time.parseTime("01:35:00")){
				s = "s4";
			} else{
				s = "s5";
			}			
		} else if(activityType.equalsIgnoreCase("l")){		/* Leisure */
			if(activityDuration <= Time.parseTime("00:15:00")){
				s = "l1";
			} else if(activityDuration <= Time.parseTime("01:00:00")){
				s = "l2";
			} else if(activityDuration <= Time.parseTime("01:50:00")){
				s = "l3";
			} else if(activityDuration <= Time.parseTime("02:45:00")){
				s = "l4";
			} else{
				s = "l5";
			}			
		}
		
		return s;
	}

}
