package playground.acmarmol.matsim2030.forecasts;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.controler.Controler;

import playground.acmarmol.matsim2030.microcensus2010.MZConstants;

public class TXTInputCreator {

	private TreeMap<Id,Double> scenario_tt_total;
	private TreeMap<Id,Double> reference_tt_total;
	private TreeMap<Id,Double> reference_time_activities;
	private TreeMap<Id, Integer> reference_nact;
	
	
	
	public static void main(String[] args){
		
		String inputBase = "C:/local/marmolea/input/Activity Chains Forecast/";
		String outputBase = "C:/local/marmolea/output/Activity Chains Forecast/";
		
		Config config = ConfigUtils.loadConfig(inputBase + "config.xml");
		config.setParam("plans", "inputPlansFile", inputBase + "plans.xml");//"population.12.MZ2010.xml");
		config.setParam("network", "inputNetworkFile", inputBase + "01-MIV_2030+_DWV_Ref_Mit_Iteration_MasterGerman.xml.gz");
		//config.setParam(moduleName, paramName, value):
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network net = scenario.getNetwork();
		
 		TXTInputCreator creator = new TXTInputCreator();
		creator.extractDataReferenceScenario(scenario);
		//this.extractDataForecastScenario(inputBase + "population2030.xml.gz");
		
	}
	
	
	public TXTInputCreator(){
		
		this.scenario_tt_total = new TreeMap<Id,Double>();
		this.reference_tt_total = new TreeMap<Id,Double>();
		this.reference_time_activities = new TreeMap<Id,Double>();
		this.reference_nact = new TreeMap<Id,Integer>(); 
		
		
	}
	
	
	public void routePlans(Scenario scenario){
		
		scenario.getConfig().getModule("controler").addParam("lastIteration", "1");
		Controler controler = new Controler(scenario) ;
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		TreeMap<String, ConfigGroup> a = controler.getConfig().getModules();
		PlansCalcRouteConfigGroup pcrcg = (PlansCalcRouteConfigGroup) controler.getConfig().getModules().get("planscalcroute");
		//pcrcg.getTeleportedModeSpeeds().put(MZConstants.MOFA, 1.0);
		//pcrcg.getTeleportedModeSpeeds().put(MZConstants.TRAIN, 1.0);
		
		controler.run() ;
		
		
		
	}
	
	
	public  void extractDataReferenceScenario(Scenario scenario){
		
		
		
		this.routePlans(scenario);
			
		
		
		
		for(Person person: scenario.getPopulation().getPersons().values()){
			
			double tt_total = 0;
			double time_activities = 0;
			int nact = 0;
		
			if(person.getSelectedPlan()!=null){
			Plan plan = person.getSelectedPlan();
			
			for(PlanElement pe: plan.getPlanElements()){
				
				if(pe instanceof Activity){
					
					Activity act = (Activity) pe;
					
					if(!act.getType().equals(MZConstants.HOME)){
						nact++;
					}
						
					if(act.getStartTime()== Double.NEGATIVE_INFINITY){
						time_activities += act.getEndTime();
					}else if(act.getEndTime()== Double.NEGATIVE_INFINITY) {
						time_activities += 86400-act.getStartTime();
					}else{time_activities += act.getEndTime()-act.getStartTime(); }
				
				
				}else if(pe instanceof Leg){
					Leg leg = (Leg) pe;
					tt_total += leg.getTravelTime();
					
				}
				
			}
			
				this.reference_time_activities.put(person.getId(), time_activities);
				this.reference_tt_total.put(person.getId(), tt_total);
				this.reference_nact.put(person.getId(), nact);
				
			
			}else{
				this.reference_time_activities.put(person.getId(), 0.0);
				this.reference_tt_total.put(person.getId(), 0.0);
				this.reference_nact.put(person.getId(), 1);
			}
			
			
		}
		
		
		System.out.println("...done");		
		
	}
	
	
	public void extractDataForecastScenario(String forecastScenarioPopulationFile){
		
	}
	
	
}
