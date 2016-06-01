package playground.dhosse.gap.scenario.population.personGroups;

import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.ActivityFacility;

import playground.dhosse.gap.Global;
import playground.dhosse.gap.scenario.GAPScenarioBuilder;
import playground.dhosse.gap.scenario.population.utils.EgapPopulationUtils;
import playground.dhosse.gap.scenario.population.utils.PlanCreationUtils;
import playground.dhosse.scenarios.generic.population.io.commuters.CommuterDataElement;

import com.vividsolutions.jts.geom.Geometry;

public class CreateCommutersFromElsewhere {
	
	static TreeMap<Id<ActivityFacility>, ActivityFacility> facilities;
	
	private static final Logger log = Logger.getLogger(CreateCommutersFromElsewhere.class);
	
	private static LeastCostPathCalculator dijkstra;
	private static Scenario scenario;
	
	public static void run(Scenario scenario, Collection<CommuterDataElement> relations){

		CreateCommutersFromElsewhere.scenario = scenario;
		
		LeastCostPathCalculatorFactory dijkstraFactory = TripRouterFactoryBuilderWithDefaults.createDefaultLeastCostPathCalculatorFactory(scenario);
		
		TravelDisutility tdis = new FreespeedTravelTimeAndDisutility(scenario.getConfig().planCalcScore());
		TravelTime ttime = new FreespeedTravelTimeAndDisutility(scenario.getConfig().planCalcScore());
		dijkstra = dijkstraFactory.createPathCalculator(scenario.getNetwork(), tdis, ttime);
		
		PopulationFactoryImpl factory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		 facilities = scenario.getActivityFacilities().getFacilitiesForActivityType(Global.ActType.work.name());
		
		//parse over commuter relations
		for(CommuterDataElement relation : relations){
			
			//this is just for the reason that the shape file does not contain any diphthongs
			//therefore, they are removed from the relation names as well
			String[] diphtong = {"ä", "ö", "ü", "ß"};
			
			String fromId = relation.getFromId();
			String fromName = relation.getFromName();
			String toId = relation.getToId();
			String toName = relation.getToName();
			
			if(fromName.contains(",")){
				String[] f = fromName.split(",");
				fromName = f[0];
			}
			
			if(toName.contains(",")){
				String[] f = toName.split(",");
				toName = f[0];
			}
			
			for(String s : diphtong){
				fromName = fromName.replace(s, "");
				toName = toName.replace(s, "");
			}
			
			//assert the transformation to be gauss-kruger (transformation of the municipal, county and geometries)
			String fromTransf = "GK4";
			String toTransf = "GK4";
			
			if(fromId.length() <= 4 && fromId.length() > 2){
				fromTransf = "";
			}
			if(toId.length() <= 4 && toId.length() > 2){
				toTransf = "";
			}
			
			//get the geometries mapped to the keys specified in the relation
			//this should be municipalities
			Geometry from = GAPScenarioBuilder.getMunId2Geometry().get(fromId);
			Geometry to = GAPScenarioBuilder.getMunId2Geometry().get(toId);
			
			//if still any geometry should be null, skip this entry
			if(from != null && to != null){
				
				log.info("Relation: " + fromId + " (" + fromName + "), " + toId + " (" + toName + ")");
				
				//create as many persons as are specified in the commuter relation
				for(int i = 0; i < relation.getCommuters(); i++){
					
					Person person = factory.createPerson(Id.createPersonId(fromId + "_" + toId + "_" + i));
					
					int age = EgapPopulationUtils.setAge(20, 65);
//					agentAttributes.putAttribute(person.getId().toString(), Global.AGE, age);
					int sex = EgapPopulationUtils.setSex(age);
//					agentAttributes.putAttribute(person.getId().toString(), Global.SEX, sex);
					
					boolean hasLicense = true;//setBooleanAttribute(person.getId().toString(), data.getpLicense(), Global.LICENSE);
					boolean carAvail = true;//setBooleanAttribute(person.getId().toString(), data.getpCarAvail(), Global.CAR_AVAIL);
					
					if(fromId.startsWith("09180") && toId.startsWith("09810")){
						
						if(hasLicense){
							
							GAPScenarioBuilder.getSubpopulationAttributes().putAttribute(person.getId().toString(), Global.USER_GROUP, Global.LICENSE_OWNER);
							
							if(carAvail){
								
								GAPScenarioBuilder.getSubpopulationAttributes().putAttribute(person.getId().toString(), Global.USER_GROUP, Global.GP_CAR);
								
							}
							
						}
						
					} else {
						
						GAPScenarioBuilder.getSubpopulationAttributes().putAttribute(person.getId().toString(), Global.USER_GROUP, Global.COMMUTER);
						
					}
					
					createOrdinaryODPlan(factory, person, fromId, toId, from, to, fromTransf, toTransf);
					
					scenario.getPopulation().addPerson(person);
					
				}
				
				log.info("Done.");
				
			} else{
				
				log.warn("Could not find geometries for:" + fromId + " (" + fromName + "), " + toId + " (" + toName + ").");
				log.warn("Continuing with next relation...");
				
			}
			
		}
		
	}
	
	/**
	 * This is the standard version of plan generation. A simple home-work-home journey is created.
	 *  
	 * @param factory
	 * @param person
	 * @param fromId
	 * @param toId
	 * @param from
	 * @param to
	 * @param fromTransf
	 * @param toTransf
	 */
	private static void createOrdinaryODPlan(PopulationFactory factory, Person person, String fromId, String toId, Geometry from, Geometry to, String fromTransf, String toTransf){
		
		Plan plan = factory.createPlan();
		
		Coord homeCoord = null;
		Coord workCoord = null;
		
		//shoot the activity coords inside the given geometries
		if(fromTransf.equals("GK4")){
			homeCoord = Global.gk4ToUTM32N.transform(PlanCreationUtils.shoot(from));
		} else{
			homeCoord = Global.ct.transform(PlanCreationUtils.shoot(from));
		}
		if(toTransf.equals("GK4")){
//			chooseWorkLocation(workMunId);
			workCoord = Global.gk4ToUTM32N.transform(PlanCreationUtils.shoot(to));
		} else{
			workCoord = Global.ct.transform(PlanCreationUtils.shoot(to));
		}
		
		if(!fromId.contains("A")){
			
			Coord c = Global.UTM32NtoGK4.transform(homeCoord);
			Geometry nearestToHome = GAPScenarioBuilder.getBuiltAreaQT().getClosest(c.getX(), c.getY());
			homeCoord = Global.gk4ToUTM32N.transform(PlanCreationUtils.shoot(nearestToHome));
			
		}
		
		if(!toId.contains("A")){
			
			if(toId.startsWith("09180")){
				workCoord = chooseWorkLocation(toId);
			} else {
				Coord c = Global.UTM32NtoGK4.transform(workCoord);
				Geometry nearest = GAPScenarioBuilder.getBuiltAreaQT().getClosest(c.getX(), c.getY());
				workCoord = Global.gk4ToUTM32N.transform(PlanCreationUtils.shoot(nearest));
			}
			
		}
		
		Activity actHome = factory.createActivityFromCoord(Global.ActType.home.name(), homeCoord);
		actHome.setStartTime(0.);
		Link fromLink = NetworkUtils.getNearestLink(scenario.getNetwork(), homeCoord);
		Link toLink = NetworkUtils.getNearestLink(scenario.getNetwork(), workCoord);
		
		//create an activity end time (they can either be equally or normally distributed, depending on the boolean that
		//has been passed to the method
		double homeEndTime = 0;
		
		double ttime = dijkstra.calcLeastCostPath(fromLink.getToNode(), toLink.getToNode(), 0., person, null).travelTime;
		
		double mean2 = 0;
		
		do{

			homeEndTime = 9 * 3600 + PlanCreationUtils.createRandomTimeShift(3);
			mean2 = 17.5 * 3600 + PlanCreationUtils.createRandomTimeShift(2);
			
		}while(homeEndTime <= 0 || (mean2 - homeEndTime - ttime) < 0 || (mean2 + ttime) > 24*3600);
		
		actHome.setEndTime(homeEndTime);
		((ActivityImpl)actHome).setLinkId(fromLink.getId());
		plan.addActivity(actHome);
		
		plan.addLeg(factory.createLeg(TransportMode.car));
		
		//create other activity and set the end time nine hours after the first activity's end time
		Activity actWork = factory.createActivityFromCoord(Global.ActType.work.name(), workCoord);
		actWork.setStartTime(actHome.getEndTime() + ttime);
		actWork.setEndTime(mean2);
		((ActivityImpl)actWork).setLinkId(toLink.getId());
		plan.addActivity(actWork);
		
		plan.addLeg(factory.createLeg(TransportMode.car));
		
		actHome = factory.createActivityFromCoord(Global.ActType.home.name(), homeCoord);
		actHome.setStartTime(actWork.getEndTime() + ttime);
		((ActivityImpl)actHome).setLinkId(fromLink.getId());
		plan.addActivity(actHome);
		
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		
	}
	
	private static Coord chooseWorkLocation(String workMunId){
		
		List<ActivityFacility> workFacilities = GAPScenarioBuilder.getMunId2WorkLocation().get(workMunId);
		
		//in case no activity facilities exist within the borders of the municipality
		if(workFacilities ==  null){
			
			Coord coord = PlanCreationUtils.shoot(GAPScenarioBuilder.getMunId2Geometry().get(workMunId));
			return Global.gk4ToUTM32N.transform(PlanCreationUtils.shoot(GAPScenarioBuilder.getBuiltAreaQT().getClosest(coord.getX(), coord.getY())));
			
		}
		
		double accumulatedWeight = 0.;
		double random = Global.random.nextDouble();
		double weight = 0.;
		
		for(ActivityFacility facility : workFacilities){
			
			accumulatedWeight += facility.getActivityOptions().get(Global.ActType.work.name()).getCapacity();
			
		}
		
		random *= accumulatedWeight;
		
		for(ActivityFacility facility : workFacilities){
			
			weight += facility.getActivityOptions().get(Global.ActType.work.name()).getCapacity();
			
			if(weight >= random){
				
				return facility.getCoord();
				
			}
			
		}
		
		//if the above shouldn't work, return a random facility coord
		return workFacilities.get(Global.random.nextInt(workFacilities.size())).getCoord();
		
	}

}
