package playground.dhosse.gap.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.dhosse.gap.Global;
import playground.vsp.analysis.modules.legModeDistanceDistribution.LegModeDistanceDistribution;

public class PersonAnalysis {

	public static void main(String args[]){
		
		restrictPlansTo24Hours(Global.matsimInputDir + "Pläne/plansV3.xml.gz", "/home/dhosse/plansV3_cleaned.xml.gz");
		getPersonsWithActivitiesAfterMidnight("/home/dhosse/plansV3_cleaned.xml.gz", "/home/dhosse/plansOut2.xml");
//		getPersonsWithNegativeScores("/home/dhosse/run9/output/ITERS/it.10/10.plans.xml.gz");
//		analyzeModeChoice("/home/danielhosse/run8b/output/ITERS/it.10/10.plans.xml.gz", Global.matsimDir + "OUTPUT/" + Global.runID +"/input/subpopulationAtts.xml");
		
	}
	
	public static void restrictPlansTo24Hours(String plansFile, String outputPlans){
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		new MatsimPopulationReader(scenario).readFile(plansFile);
		
		Scenario scOut = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		PopulationFactory fac = scOut.getPopulation().getFactory();
			
		for(Person person : scenario.getPopulation().getPersons().values()){
			
			Person currentPerson = fac.createPerson(person.getId());
			Plan plan = fac.createPlan();
			
			for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
				
				boolean valid = true;
				
				if(pe instanceof Activity){
					
					Activity act = (Activity)pe;
					
					double startTime = act.getStartTime();
					double endTime = act.getEndTime();
					
					if(startTime != Time.UNDEFINED_TIME){
						
						if(startTime > 24 * 3600){
							
							valid = false;
							
						}
						
					}
					
					if(endTime != Time.UNDEFINED_TIME){
						
						if(endTime > 24 * 3600){
							
							valid = false;
							
						}
						
					}
					
					if(startTime != Time.UNDEFINED_TIME && endTime != Time.UNDEFINED_TIME){
						
						if(!valid){
							
							if(24 * 3600 - startTime >= 1800){
								
								act.setEndTime(24 * 3600);
								valid = true;
								
							}
							
						}
						
					}
					
					if(valid){
						plan.addActivity(act);
					} else{
						break;
					}
					
				} else{
					
					plan.addLeg((Leg)pe);
					
				}
				
			}
			
			if(plan.getPlanElements().get(plan.getPlanElements().size()-1) instanceof Leg){
				plan.getPlanElements().remove(plan.getPlanElements().size() - 1);
			}
			
			currentPerson.addPlan(plan);
			currentPerson.setSelectedPlan(plan);
			scOut.getPopulation().addPerson(currentPerson);
			
		}
		
		new PopulationWriter(scOut.getPopulation()).write(outputPlans);
		
	}
	
	public static void getPersonsWithActivitiesAfterMidnight(String plansFile, String outputPlans){
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		new MatsimPopulationReader(scenario).readFile(plansFile);
		
		Scenario scOut = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		for(Person person : scenario.getPopulation().getPersons().values()){
			
			if(person.getSelectedPlan().getPlanElements().size() < 3){
				scOut.getPopulation().addPerson(person);
				continue;
			}
			
			for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
				
				if(pe instanceof Activity){
					Activity act = (Activity)pe;
					if(act.getStartTime() > 24*3600 || act.getEndTime() > 24*3600){
						scOut.getPopulation().addPerson(person);
						break;
					}
				}
				
			}
			
		}
		
		new PopulationWriter(scOut.getPopulation()).write(outputPlans);
		
	}
	
	public static void createLegModeDistanceDistribution(String plansFile, String outputFolder){
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		new MatsimPopulationReader(scenario).readFile(plansFile);
		
		LegModeDistanceDistribution lmdd = new LegModeDistanceDistribution();
		lmdd.init(scenario);
		lmdd.preProcessData();
		lmdd.postProcessData();
		lmdd.writeResults(outputFolder);
		
	}
	
	private static void getPersonsWithNegativeScores(String plansFile){
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		new MatsimPopulationReader(scenario).readFile(plansFile);
		int cnt = 0;
		
		Scenario sc2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		for(Person person : scenario.getPopulation().getPersons().values()){
			
			double score = person.getSelectedPlan().getScore();
			
			if(score < 0){
				
				cnt++;
				
				if(score < -500){
					
					System.out.println(person.getId() + "\t" + score);
					sc2.getPopulation().addPerson(person);
					
				}
				
			}
			
		}
		
		new PopulationWriter(sc2.getPopulation()).write("/home/dhosse/Dokumente/personsWithNegativeScores.xml");
		
		System.out.println(cnt + " persons with negative scores...");
		
	}
	
	private static void analyzeModeChoice(String plansFile, String personAttributesFile){
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		new MatsimPopulationReader(scenario).readFile(plansFile);
		
		ObjectAttributes subpopAtts = new ObjectAttributes();
		new ObjectAttributesXmlReader(subpopAtts).parse(personAttributesFile);
		
		int nNonCarUsersUsingCar = 0;
		
		for(Person person : scenario.getPopulation().getPersons().values()){
			
			String userGroup = (String)subpopAtts.getAttribute(person.getId().toString(), "usrGroup");
			
			for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
				
				if(pe instanceof Leg){
					
					Leg leg = (Leg)pe;
					
					if(userGroup == null){
						
						if(leg.getMode().equals(TransportMode.car)){
							nNonCarUsersUsingCar++;
							break;
						}
						
					}
					
				}
				
			}
			
		}
		
		System.out.println(nNonCarUsersUsingCar + " persons should not have access to car but are using it!");
		
	}
	
}
