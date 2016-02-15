package playground.southafrica.population.utilities.activityTypeManipulation;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.TreeMap;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import playground.southafrica.utilities.Header;

/**
 * Class to adapt the activity types based on the R-analysis of the activity 
 * durations.
 * 
 * @author jwjoubert
 */
public class CapeTownActivityTypeManipulator extends ActivityTypeManipulator {
	private Config config;

	public CapeTownActivityTypeManipulator() {
		this.config = ConfigUtils.createConfig();

		/* Add the activities with only a single typical duration. */ 
		/* Home full day. */
		ActivityParams h = new ActivityParams("h");
		h.setTypicalDuration(Time.parseTime("24:00:00"));
		h.setScoringThisActivityAtAll(false);
		this.config.planCalcScore().addActivityParams(h);
		/* School-going kids. */
		ActivityParams e1 = new ActivityParams("e1");
		e1.setTypicalDuration(Time.parseTime("07:00:00"));
		this.config.planCalcScore().addActivityParams(e1);
		/* Dropping/collecting kids from school. */
		ActivityParams e3 = new ActivityParams("e3");
		e3.setTypicalDuration(Time.parseTime("00:05:00"));
		this.config.planCalcScore().addActivityParams(e3);

		/* Chopped chain starts. */
		ActivityParams cs = new ActivityParams("chopStart");
		cs.setTypicalDuration(Time.parseTime("00:00:01"));
		this.config.planCalcScore().addActivityParams(cs);
		/* Chopped chain ends. */
		ActivityParams ce = new ActivityParams("chopEnd");
		ce.setTypicalDuration(Time.parseTime("00:00:01"));
		this.config.planCalcScore().addActivityParams(ce);
	}

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(CapeTownActivityTypeManipulator.class.toString(), args);

		/* ===================  For persons. ===================== */
		String population = args[0];
		String decileFile = args[1];
		String outputPopulation = args[2];
		CapeTownActivityTypeManipulator atm = new CapeTownActivityTypeManipulator();
		atm.parseDecileFile(decileFile);
		atm.parsePopulation(population);
		atm.run();
		/* Write the population to file. */
		new PopulationWriter(atm.sc.getPopulation()).write(outputPopulation);

		/* ===================  For freight. ===================== */
		population = args[3];
		decileFile = args[4];
		outputPopulation = args[5];
		atm = new CapeTownActivityTypeManipulator();
		atm.parseDecileFile(decileFile);
		atm.parsePopulation(population);
		atm.run();
		/* Write the population to file. */
		new PopulationWriter(atm.sc.getPopulation()).write(outputPopulation);
		/* ======================================================= */

		/* Write the config to file. */
		String outputConfig = args[6];
		new ConfigWriter(atm.config).write(outputConfig);

		Header.printFooter();
	}

	@Override
	protected void adaptActivityTypes(Plan plan) {
		if(plan.getPlanElements().size() == 1){
			/* Ignore those persons who only have one stay-at-home activity. */
		} else{
			for(int i = 0; i < plan.getPlanElements().size(); i+= 2){
				ActivityImpl act = (ActivityImpl)plan.getPlanElements().get(i);
				double estimatedDuration = 0.0;

				if(i == 0){
					/* It is the first activity, 
					 * and it is assumed it started at 00:00:00. */
					estimatedDuration = act.getEndTime();

					if(act.getType().equalsIgnoreCase("h")){
						act.setType("h1"); /* First activity of the chain. */
					}
				} else if(i == plan.getPlanElements().size() - 1){
					/* The last activity. */
					
					/* We need to distinguish between persons and freight, since
					 * freight does not have a start time, duration, or end
					 * time set for the last 'major' activity - June 2014.
					 * 
					 * If the chain ends with a chop, then the duration is 
					 * 00:00:01 else we use the median time as calculated in 
					 * the activity duration analyses (Feb 2016). */ 
					if(plan.getPerson().getId().toString().startsWith("coct")){
						/* Using the median of activity durations analysed. */
						String type = act.getType();
						if(type.equalsIgnoreCase("minor")){
							estimatedDuration = Time.parseTime("00:14:45");
						} else if(type.equalsIgnoreCase("major")){
							estimatedDuration = Time.parseTime("07:31:17");
						} else if(type.equalsIgnoreCase("chopEnd")){
							estimatedDuration = Time.parseTime("00:00:01");
						} else{
							LOG.error("Don't know what to do with last activity of type " + act.getType());
							estimatedDuration = Time.UNDEFINED_TIME;
						}
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
					if(plan.getPerson().getId().toString().startsWith("coct")){
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
				} else if(estimatedDuration == Time.UNDEFINED_TIME){
					LOG.error("Found undefined duration.");
				}

				int daysAdded = 0;
				while(estimatedDuration < 0 && daysAdded++ <= 10){
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
