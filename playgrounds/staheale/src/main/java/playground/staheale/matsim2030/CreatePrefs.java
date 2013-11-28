package playground.staheale.matsim2030;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class CreatePrefs {
	
	private final static Logger log = Logger.getLogger(CreatePrefs.class);
		
	public static void main(String[] args) {
		
		Random random = new Random(37835409);
		
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population population = sc.getPopulation();
		
		//////////////////////////////////////////////////////////////////////
		// read in population

		log.info("Reading plans...");	
		MatsimPopulationReader PlansReader = new MatsimPopulationReader(sc); 
		PlansReader.readFile("./input/population2030combined.xml.gz");
		log.info("Reading plans...done.");
		log.info("Population size is " +population.getPersons().size());

		//////////////////////////////////////////////////////////////////////
		// write prefs
		
		ObjectAttributes prefs = new ObjectAttributes();
		int counter = 0;
		int nextMsg = 1;
		int nrOfActs = 0;
		int nrWorkActs = 0;
		double timeBudget = 24*3600.0;
		String actChain = "_";
		boolean education = false;
		boolean shopping = false;
		boolean leisure = false;
		
		for (Person p : population.getPersons().values()) {
			
			if (p.getSelectedPlan() != null) {
				
				// get number of activities and actChain
				for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
					if (pe instanceof ActivityImpl) {
						nrOfActs += 1;
						ActivityImpl act = (ActivityImpl) pe;
						actChain = actChain.concat(act.getType().substring(0, 1));
						if (act.getType().equals("work")){
							nrWorkActs += 1;
						}
						else if (act.getType().equals("education")) {
							education = true;
						}
						else if (act.getType().equals("shopping")) {
							shopping = true;
						}
						else if (act.getType().equals("leisure")) {
							leisure = true;
						}
					}
				}
				//log.info("person p " +p.getId().toString()+ " has " +nrOfActs+ " activities defined and the following actChain: " +actChain);

				// draw a duration for work activities
				if (nrWorkActs > 0) {
					double typicalWorkDuration = 0;
					// if number of nonWorkActs < 4:
					// 4-7h with 10% prob, 7-9h with 80% prob and 9-12 hours with 10% prob
					if (nrOfActs-nrWorkActs < 4) {
						double prob = random.nextDouble();
						if (prob <= 0.1) {
							typicalWorkDuration = 14400 + random.nextInt(2)*3600.0 + random.nextDouble()*3600.0;
						}
						else if (prob > 0.1 && prob < 0.9) {
							typicalWorkDuration = 25200 + random.nextInt(1)*3600.0 + random.nextDouble()*3600.0;
						}
						else if (prob >= 0.9) {
							typicalWorkDuration = 32400 + random.nextInt(2)*3600.0 + random.nextDouble()*3600.0;
						}
					}
					// if number of nonWorkActs >= 4:
					// 3-8h
					else {
						typicalWorkDuration = 10800 + random.nextInt(4) + random.nextDouble()*3600.0;
					}
					
					prefs.putAttribute(p.getId().toString(), "typicalDuration_work", typicalWorkDuration);
					prefs.putAttribute(p.getId().toString(), "minimalDuration_work", 0.5 * 3600.0);
					prefs.putAttribute(p.getId().toString(), "earliestEndTime_work", 0.0 * 3600.0);
					prefs.putAttribute(p.getId().toString(), "latestStartTime_work", 24.0 * 3600.0);
					
					timeBudget -= typicalWorkDuration;
					//log.info("person p " +p.getId().toString()+ " has working duration: " +typicalWorkDuration/3600);
				}
				
				// draw a duration for education activities
				if (education) {
					double typicalEducationDuration;
					// less than 4 acts or only one other secondary activity type:
					// 7-9h
					if (nrOfActs < 4 || (leisure && shopping == false) || (leisure == false && shopping)) {
						typicalEducationDuration = 25200 + random.nextInt(1)*3600.0 + random.nextDouble()*3600.0;
					}
					// else:
					// 4-6
					else {
						typicalEducationDuration = 14400 + random.nextInt(1)*3600.0 + random.nextDouble()*3600.0;
					}
					
					prefs.putAttribute(p.getId().toString(), "typicalDuration_education", typicalEducationDuration);
					prefs.putAttribute(p.getId().toString(), "minimalDuration_education", 0.5 * 3600.0);
					prefs.putAttribute(p.getId().toString(), "earliestEndTime_education", 0.0 * 3600.0);
					prefs.putAttribute(p.getId().toString(), "latestStartTime_education", 24.0 * 3600.0);
					
					timeBudget -= typicalEducationDuration;
					//log.info("person p " +p.getId().toString()+ " has education duration: " +typicalEducationDuration/3600);
					
					education = false;
				}
				
				// draw a duration for secondary activities
				if (shopping || leisure) {
					// agent should be home at least for 5-7 hours
					double maxSecActDur = timeBudget-(5*3600.0+random.nextInt(2));
					double typicalShoppingDuration;
					double typicalLeisureDuration;
					
					// if both act types are reported:
					// both between 0.5 and available time budget
					if (shopping && leisure) {
						// agent should be home at least for 5-7 hours
						typicalShoppingDuration = 1800.0 + random.nextInt((int) (maxSecActDur-3600.0));
						typicalLeisureDuration = maxSecActDur - typicalShoppingDuration;
						
						prefs.putAttribute(p.getId().toString(), "typicalDuration_shopping", typicalShoppingDuration);
						prefs.putAttribute(p.getId().toString(), "minimalDuration_shopping", 0.5 * 3600.0);
						prefs.putAttribute(p.getId().toString(), "earliestEndTime_shopping", 0.0 * 3600.0);
						prefs.putAttribute(p.getId().toString(), "latestStartTime_shopping", 24.0 * 3600.0);
						
						prefs.putAttribute(p.getId().toString(), "typicalDuration_leisure", typicalLeisureDuration);
						prefs.putAttribute(p.getId().toString(), "minimalDuration_leisure", 0.5 * 3600.0);
						prefs.putAttribute(p.getId().toString(), "earliestEndTime_leisure", 0.0 * 3600.0);
						prefs.putAttribute(p.getId().toString(), "latestStartTime_leisure", 24.0 * 3600.0);
						
						timeBudget -= typicalLeisureDuration;
						timeBudget -= typicalShoppingDuration;
						//log.info("person p " +p.getId().toString()+ " has leisure duration: " +typicalLeisureDuration/3600);
						//log.info("person p " +p.getId().toString()+ " has shopping duration: " +typicalShoppingDuration/3600);

					}
					else if (shopping && leisure == false) {
						typicalShoppingDuration = 1800 + random.nextInt((int) ((maxSecActDur-1800.0)));
						
						prefs.putAttribute(p.getId().toString(), "typicalDuration_shopping", typicalShoppingDuration);
						prefs.putAttribute(p.getId().toString(), "minimalDuration_shopping", 0.5 * 3600.0);
						prefs.putAttribute(p.getId().toString(), "earliestEndTime_shopping", 0.0 * 3600.0);
						prefs.putAttribute(p.getId().toString(), "latestStartTime_shopping", 24.0 * 3600.0);
						
						timeBudget -= typicalShoppingDuration;
						//log.info("person p " +p.getId().toString()+ " has shopping duration: " +typicalShoppingDuration/3600);

					}
					else if (shopping == false && leisure) {
						typicalLeisureDuration = 1800 + random.nextInt((int) ((maxSecActDur-1800.0)));
						
						prefs.putAttribute(p.getId().toString(), "typicalDuration_leisure", typicalLeisureDuration);
						prefs.putAttribute(p.getId().toString(), "minimalDuration_leisure", 0.5 * 3600.0);
						prefs.putAttribute(p.getId().toString(), "earliestEndTime_leisure", 0.0 * 3600.0);
						prefs.putAttribute(p.getId().toString(), "latestStartTime_leisure", 24.0 * 3600.0);
						
						timeBudget -= typicalLeisureDuration;
						//log.info("person p " +p.getId().toString()+ " has leisure duration: " +typicalLeisureDuration/3600);

					}
					shopping = false;
					leisure = false;
				}
				
				// assign remaining timeBudget to home activities
				double typicalHomeDuration = timeBudget;
				
				prefs.putAttribute(p.getId().toString(), "typicalDuration_home", typicalHomeDuration);
				prefs.putAttribute(p.getId().toString(), "minimalDuration_home", 0.5 * 3600.0);
				prefs.putAttribute(p.getId().toString(), "earliestEndTime_home", 0.0 * 3600.0);
				prefs.putAttribute(p.getId().toString(), "latestStartTime_home", 24.0 * 3600.0);
				
				//log.info("person p " +p.getId().toString()+ " has home duration: " +typicalHomeDuration/3600);

			}
			else {
				log.warn("person " +p.getId().toString()+ " has no plan defined");
			}
			
			counter++;
			if (counter % nextMsg == 0) {
				nextMsg *= 2;
				log.info(" person # " + counter);
			}
			nrOfActs = 0;
			nrWorkActs = 0;
			timeBudget = 24*3600.0;
			actChain = "_";
		}
		ObjectAttributesXmlWriter betaWriter = new ObjectAttributesXmlWriter(prefs);
		betaWriter.writeFile("./output/prefs2030combined.xml.gz");		
	}
}
