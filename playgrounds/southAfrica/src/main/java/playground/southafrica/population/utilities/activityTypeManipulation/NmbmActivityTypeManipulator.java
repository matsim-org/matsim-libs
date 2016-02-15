package playground.southafrica.population.utilities.activityTypeManipulation;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.TreeMap;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import playground.southafrica.utilities.Header;

/**
 *  
 *
 * @author jwjoubert
 */
public class NmbmActivityTypeManipulator extends ActivityTypeManipulator {
	private Config config;

	public NmbmActivityTypeManipulator() {
		this.config = ConfigUtils.createConfig();
		/*TODO Add the activities with only a single typical duration. */ 

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(NmbmActivityTypeManipulator.class.toString(), args);
		String population = args[0];
		String decileFile = args[1];
		String outputPopulation = args[2];
		String outputConfig = args[3];
		
		NmbmActivityTypeManipulator atm = new NmbmActivityTypeManipulator();
		atm.parseDecileFile(decileFile);
		atm.parsePopulation(population);
		atm.run();
		
		/* Write the population to file. */
		PopulationWriter pw = new PopulationWriter(atm.getScenario().getPopulation(), atm.getScenario().getNetwork());
		pw.write(outputPopulation);
		
		/* Write the config to file. */
		ConfigWriter cw = new ConfigWriter(atm.config);
		cw.write(outputConfig);
		
		Header.printFooter();
	}

	@Override
	protected void adaptActivityTypes(Plan plan) {
		for(int i = 0; i < plan.getPlanElements().size(); i++){
			PlanElement pe = plan.getPlanElements().get(i);
			if(pe instanceof ActivityImpl){
				ActivityImpl act = (ActivityImpl) pe;
				double estimatedDuration = 0.0;

				if(i == 0){
					/* It is the first activity, 
					 * and it is assumed it started at 00:00:00. */
					estimatedDuration = act.getEndTime();

					if(act.getType().equalsIgnoreCase("h")){
						act.setType("h1"); /* First activity of the chain. */
					}
				} else if(i == plan.getPlanElements().size() - 1){
					/* We need to distinguish between persons and freight, since
					 * freight does not have a start time, duration, or end
					 * time set for the last 'major' activity - June 2014 
					 * 
					 * FIXME Question is, so what do we change the activity type 
					 * TO for the last activity?!?!*/
					
					if(plan.getPerson().getId().toString().startsWith("com")){
						estimatedDuration = 4*60*60; /* Thumb suck!! */
					} else{
						/* Since the method getStartTime is deprecated,
						 * estimate the start time as the end time of the
						 * previous activity plus the duration of the trip. */
						double previousEndTime = ((ActivityImpl)plan.getPlanElements().get(i-2)).getEndTime();
						double tripDuration = ((Leg)plan.getPlanElements().get(i-1)).getTravelTime();
						estimatedDuration = Math.max(24*60*60, (previousEndTime + tripDuration) ) - (previousEndTime + tripDuration);
						
						if(act.getType().equalsIgnoreCase("h")){
							act.setType("h2"); /* Final activity of the chain. */
						}
					}
				} else{
					/* We need to distinguish between persons and freight, since
					 * freight works with maximum duration, and not start- and
					 * end times. JWJ, June 2014*/
					if(plan.getPerson().getId().toString().startsWith("com")){
						estimatedDuration = act.getMaximumDuration();
					} else{
						/* We assume it is a person... */
						
						/* Since the method getStartTime is deprecated,
						 * estimate the start time as the end time of the
						 * previous activity plus the duration of the trip. 
						 * 
						 * This will only work with Persons (JWJ, June 2014)*/
						double previousEndTime = ((ActivityImpl)plan.getPlanElements().get(i-2)).getEndTime();
						double tripDuration = ((Leg)plan.getPlanElements().get(i-1)).getTravelTime();
						estimatedDuration = act.getEndTime() - (previousEndTime + tripDuration);
						
						if(act.getType().equalsIgnoreCase("h")){
							act.setType("h3"); /* Intermediate activity. */
						}
					}
				}
				
				if(estimatedDuration == Double.NEGATIVE_INFINITY){
					LOG.error("Found -Infinity");
				}

				while(estimatedDuration < 0){
					LOG.warn("Updated duration: " + act.getType() + " (" + estimatedDuration + ")");
					estimatedDuration += (24*60*60);
				}
				
				act.setType( getNewActivityType(act.getType(), estimatedDuration) );
			}
		}
	}
	
	
	private String getNewActivityType(String activityType, double activityDuration){
		String s = activityType;
		if(deciles.containsKey(s)){
			/* It is one of the activity types that should be changed. */
			for(String ss : deciles.get(s).keySet()){
				if(activityDuration <= Time.parseTime(ss)){
					s = deciles.get(s).get(ss).getFirst();
					break;
				}
			}
			
			/* It is possible that the actual activity's duration is still 
			 * longer than the upper limit of the longest decile... is this
			 * indeed possible?! Maybe due to rounding errors?! How do we handle
			 * those? Because unless they're handled, they'll remain, for 
			 * example, `s' in the case of shopping facilities.  
			 */
			if(s.equalsIgnoreCase(activityType)){
				LOG.warn("Could not change activity type for `" + s + "' - duration: " + activityDuration);
			}
		}
		return s;
	}

	
	@Override
	protected void parseDecileFile(String filename) {
		LOG.info("Parsing deciles from R output...");
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try {
			String s = br.readLine(); /* Header */
			while((s = br.readLine()) != null){
				String[] sa = s.split(",");
				String type = sa[0];
				TreeMap<String, Tuple<String,String>> map = new TreeMap<String, Tuple<String,String>>();
				int sub = 1;
				for(int i = 3; i < sa.length; i+=2){
					String quantileUpperLimit = sa[i];
					String quantileTypicalValue = sa[i-1];
					String quantileName = type + sub++;
					Tuple<String, String> tuple = new Tuple<String, String>(quantileName, quantileTypicalValue);
					map.put(quantileUpperLimit, tuple);
				}
				this.deciles.put(type, map);
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not read from BufferedReader "
					+ filename);
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedReader "
						+ filename);
			}
		}
		
		LOG.info("Done parsing deciles:");
		for(String s : this.deciles.keySet()){
			String output = "  " + s + ": ";
			for(Tuple<String, String> t : this.deciles.get(s).values()){
				output += t.getFirst() + " (" + t.getSecond() + "); ";
			}
			LOG.info(output);
		}
		
		LOG.info("Creating config parameters from parsed decile file...");
		for(String s : deciles.keySet()){
			for(String ss : deciles.get(s).keySet()){
				ActivityParams ap = new ActivityParams(deciles.get(s).get(ss).getFirst());
				ap.setTypicalDuration(Time.parseTime(deciles.get(s).get(ss).getSecond()));
				config.planCalcScore().addActivityParams(ap);
			}
		}
		LOG.info("Done.");
	}

}
