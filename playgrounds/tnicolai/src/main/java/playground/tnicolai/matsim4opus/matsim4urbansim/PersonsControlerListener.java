package playground.tnicolai.matsim4opus.matsim4urbansim;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioUtils;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.utils.helperObjects.Benchmark;

public class PersonsControlerListener implements ShutdownListener{

	private static final Logger log = Logger.getLogger(PersonsControlerListener.class);
	
	private Benchmark benchmark;
	
	public PersonsControlerListener(Benchmark benchmark){
		this.benchmark = benchmark;
	}
	
	/**
	 * agent-based performance feedback (time spent travelling, money spent travelling, etc.)
	 */
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		
		int benchmarkID = this.benchmark.addMeasure("Person Contoler");
		
		// get the controller and scenario
		Controler controler = event.getControler();
		// get persons
		Population population = controler.getPopulation();
		Iterator<? extends Person> persons = population.getPersons().values().iterator();
		
//		while(persons.hasNext()){
//			
//			Person p = persons.next();
//			
//			Plan plan = p.getSelectedPlan();
//			
//			boolean isFirstPlanActivity = true;
//			String lastZoneId = null;
//			
//			if(plan.getPlanElements().size() <= 1) // check if activities available (then size of plan elements > 1)
//				continue;
//			
//			for ( PlanElement pe : plan.getPlanElements() ) {
//				if ( pe instanceof Activity ) {
//					Activity act = (Activity) pe;
//					Id id = act.getFacilityId();
//					if( id == null) // that person plan doesn't contain any activity, continue with next person
//						continue;
//					
//					ActivityFacility fac = allFacilities.get(id);
//					if(fac == null)
//						continue;
//					String zone_ID = ((Id) fac.getCustomAttributes().get(Constants.ZONE_ID)).toString() ;
//
//					if (isFirstPlanActivity)
//						isFirstPlanActivity = false; 
//					else {
//						matrixEntry = originDestinationMatrix.getEntry(new IdImpl(lastZoneId), new IdImpl(zone_ID));
//						if(matrixEntry != null){
//							double value = matrixEntry.getValue();
//							originDestinationMatrix.setEntry(new IdImpl(lastZoneId), new IdImpl(zone_ID), value+1.);
//						}
//						else	
//							originDestinationMatrix.createEntry(new IdImpl(lastZoneId), new IdImpl(zone_ID), 1.);
//							// see PtPlanToPlanStepBasedOnEvents.addPersonToVehicleContainer (or zone coordinate addition)
//					}
//					lastZoneId = zone_ID; // stores the first activity (e. g. "home")
//				}
//			}
//		}
	}

	/**
	 * for testing only
	 * @param args
	 */
	public static void main(String[] args){
		
		String plansFileName = "/Users/thomas/Development/opus_home/data/zurich_parcel/data/data/plans/zurich2000_10pct_100it_plans_withstoragecap_merged_zurich_bigRoads_network.xml.gz";
		String networkFileName = "/Users/thomas/Development/opus_home/data/zurich_parcel/data/data/network/merged_zurich_network_bigRoads.xml";
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		new MatsimPopulationReader(scenario).readFile(networkFileName);
		new MatsimPopulationReader(scenario).readFile(plansFileName);
		
		Population population = scenario.getPopulation();
		
		log.info("... done!");
		
	}
}
