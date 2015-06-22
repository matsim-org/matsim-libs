package playground.dhosse.cl.population;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;

public class Create24HourPlans {
	
	static String svnWorkingDir = "C:/Users/dhosse/workspace/shared-svn/studies/countries/cl/";
	static String workingDirInputFiles = svnWorkingDir + "Kai_und_Daniel/";
	static String boundariesInputDir = workingDirInputFiles + "exported_boundaries/";
	static String databaseFilesDir = workingDirInputFiles + "exportedFilesFromDatabase/";
	static String visualizationsDir = workingDirInputFiles + "Visualisierungen/";
	static String matsimInputDir = workingDirInputFiles + "inputFiles/";
	
	static String transitFilesDir = svnWorkingDir + "/santiago_pt_demand_matrix/pt_stops_schedule_2013/";
	static String gtfsFilesDir = svnWorkingDir + "/santiago_pt_demand_matrix/gtfs_201306/";

	public static void main(String args[]){
		
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, matsimInputDir + "config.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Map<String,Tuple<Double,Double>> actType2TypMinDuration = new HashMap<>();
		
		PopulationFactoryImpl popFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		
		for(Person person : scenario.getPopulation().getPersons().values()){
			
			Plan tempPlan = popFactory.createPlan();
			
			for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
				if(pe instanceof Activity){
					if(((Activity)pe).getStartTime() < Time.MIDNIGHT){
						tempPlan.addActivity((Activity)pe);
					}
				}  else{
					tempPlan.addLeg((Leg)pe);
				}
			}
			
			Activity firstActivity = null;
			Activity lastActivity = null;
			List<PlanElement> planElements = new ArrayList<>();
			
			int cnt = 0;
			
			for(PlanElement pe : tempPlan.getPlanElements()){
				
				if(pe instanceof Activity){
					
					double typDur = 0.;
					double minDur = 0.;
					
					Activity act = (Activity)pe;
					
					if(cnt == 0){
					
						firstActivity = act;
						
					} else if(cnt == person.getSelectedPlan().getPlanElements().size() - 1){
						
						lastActivity = act;
						
						if(lastActivity.getType().equals(firstActivity.getType())){
							
							double duration = Time.MIDNIGHT - lastActivity.getStartTime() + firstActivity.getEndTime();
							
							typDur = Math.floor(duration/3600)*3600;
							minDur = typDur / 2;
							
							String actType = act.getType().substring(0, 4).concat(typDur/ 3600 + "H");
							actType2TypMinDuration.put(actType, new Tuple<Double, Double>(typDur, minDur));
							
							Activity firstOut = popFactory.createActivityFromCoord(actType, firstActivity.getCoord());
							firstOut.setEndTime(firstActivity.getEndTime());
							Activity lastOut = popFactory.createActivityFromCoord(actType, lastActivity.getCoord());
							
							planElements.add(0, firstOut);
							planElements.add(lastOut);
							
						} else {
							
							double duration = firstActivity.getEndTime();
							typDur = Math.floor(duration/3600)*3600;
							minDur = typDur / 2;
							String actType = act.getType().substring(0, 4).concat(typDur/ 3600 + "H");
							
							Activity firstOut = popFactory.createActivityFromCoord(actType, firstActivity.getCoord());
							firstOut.setStartTime(0.);
							firstOut.setEndTime(firstActivity.getEndTime());
							
							duration = Time.MIDNIGHT - lastActivity.getStartTime();
							typDur = Math.floor(duration/3600)*3600;
							minDur = typDur / 2;
							actType = act.getType().substring(0, 4).concat(typDur/ 3600 + "H");
							
							Activity lastOut = popFactory.createActivityFromCoord(actType, lastActivity.getCoord());
							lastOut.setStartTime(lastActivity.getEndTime());
							lastOut.setEndTime(Time.MIDNIGHT);
							
							planElements.add(0, firstOut);
							planElements.add(lastOut);
							
						}
						
					} else{
					
						if(!act.getType().equals("pt interaction")){
							
							double duration = act.getEndTime() - act.getStartTime();
							
							typDur = Math.floor(duration/3600);
							if(typDur < 1){
								typDur = 1800 / 2;
							} else{
								typDur *= 3600;
							}
							minDur = typDur / 2;
							
							String actType = act.getType().substring(0, 4).concat(typDur/ 3600 + "H");
							Activity actOut = popFactory.createActivityFromCoord(actType, act.getCoord());
							actOut.setStartTime(act.getStartTime());
							actOut.setEndTime(act.getEndTime());
							planElements.add(actOut);
							actType2TypMinDuration.put(actType, new Tuple<Double, Double>(typDur, minDur));
							
						}
						
					}
					
				} else{
					
					planElements.add(pe);
					
				}
				
				cnt++;
				
			}
			
			for(Entry<String, Tuple<Double,Double>> entry : actType2TypMinDuration.entrySet()){
				System.out.println(entry.getKey() + "\t" + entry.getValue().getFirst() + "\t" + entry.getValue().getSecond());
			}
			
		}
		
	}
	
}
