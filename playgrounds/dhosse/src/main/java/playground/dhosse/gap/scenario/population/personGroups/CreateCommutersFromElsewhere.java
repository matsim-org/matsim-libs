package playground.dhosse.gap.scenario.population.personGroups;

import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.PopulationFactoryImpl;

import com.vividsolutions.jts.geom.Geometry;

import playground.dhosse.gap.Global;
import playground.dhosse.gap.scenario.GAPScenarioBuilder;
import playground.dhosse.gap.scenario.mid.MiDPersonGroupData;
import playground.dhosse.gap.scenario.population.EgapPopulationUtils;
import playground.dhosse.gap.scenario.population.PlanCreationUtils;
import playground.dhosse.gap.scenario.population.io.CommuterDataElement;
import playground.dhosse.utils.EgapHashGenerator;

public class CreateCommutersFromElsewhere {
	
	private static final Logger log = Logger.getLogger(CreateCommutersFromElsewhere.class);
	
	public static void run(Population population, Collection<CommuterDataElement> relations, Map<String, MiDPersonGroupData> groupData){

		PopulationFactoryImpl factory = (PopulationFactoryImpl) population.getFactory();
		
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
			
			if(fromId.length() == 3){
				fromTransf = "";
			}
			if(toId.length() == 3){
				toTransf = "";
			}
			
			//get the geometries mapped to the keys specified in the relation
			//this should be municipalities
			Geometry from = GAPScenarioBuilder.getMunId2Geometry().get(fromId);
			Geometry to = GAPScenarioBuilder.getMunId2Geometry().get(toId);
			
			//if still any geometry should be null, skip this entry
			if(from != null && to != null){
				
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
							
							GAPScenarioBuilder.getSubpopulationAttributes().putAttribute(person.getId().toString(), Global.CARSHARING, Global.CAR_OPTION);
							
							if(carAvail){
								
								GAPScenarioBuilder.getSubpopulationAttributes().putAttribute(person.getId().toString(), Global.USER_GROUP, Global.GP_CAR);
								
							}
							
						}
						
					} else {
						
						GAPScenarioBuilder.getSubpopulationAttributes().putAttribute(person.getId().toString(), Global.USER_GROUP, Global.COMMUTER);
						
					}
					
					createOrdinaryODPlan(factory, person, fromId, toId, from, to, fromTransf, toTransf);
					
					population.addPerson(person);
					
				}
				
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
			workCoord = Global.gk4ToUTM32N.transform(PlanCreationUtils.shoot(to));
		} else{
			workCoord = Global.ct.transform(PlanCreationUtils.shoot(to));
		}
		
		if(fromId.length() < 8 && !fromId.contains("A")){
			
			Coord c = Global.UTM32NtoGK4.transform(homeCoord);
			Geometry nearestToHome = GAPScenarioBuilder.getBuiltAreaQT().get(c.getX(), c.getY());
			homeCoord = Global.gk4ToUTM32N.transform(PlanCreationUtils.shoot(nearestToHome));
			
		}
		
		if(toId.length() < 8 && !toId.contains("A")){
			
			Coord c = Global.UTM32NtoGK4.transform(workCoord);
			Geometry nearestToWork = GAPScenarioBuilder.getBuiltAreaQT().get(c.getX(), c.getY());
			workCoord = Global.gk4ToUTM32N.transform(PlanCreationUtils.shoot(nearestToWork));
			if(toId.startsWith("09180")){
				workCoord = GAPScenarioBuilder.getWorkLocations().get(workCoord.getX(), workCoord.getY()).getCoord();
			}
			
		}
		
		Activity actHome = factory.createActivityFromCoord("home", homeCoord);
		actHome.setStartTime(0.);
		
		//create an activity end time (they can either be equally or normally distributed, depending on the boolean that
		//has been passed to the method
		double endTime = 0;
		
		do{

			endTime = 7*3600 + PlanCreationUtils.createRandomTimeShift(2);
			
		}while(endTime <= 0 || (endTime + 10 * 3600) > 24*3600);
		
		actHome.setEndTime(endTime);
		plan.addActivity(actHome);
		
		plan.addLeg(factory.createLeg(TransportMode.car));
		
		//create other activity and set the end time nine hours after the first activity's end time
		Activity actWork = factory.createActivityFromCoord("work", workCoord);
		actWork.setStartTime(actHome.getEndTime());
		endTime = endTime + 10 * 3600;
		actWork.setEndTime(endTime);
		plan.addActivity(actWork);
		
		plan.addLeg(factory.createLeg(TransportMode.car));
		
		actHome = factory.createActivityFromCoord("home", homeCoord);
		actHome.setStartTime(endTime);
		plan.addActivity(actHome);
		
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		
	}

}
