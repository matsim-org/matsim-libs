package playground.dhosse.gap.scenario.population.personGroups;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.utils.geometry.CoordUtils;

import com.vividsolutions.jts.geom.Geometry;

import playground.dhosse.gap.Global;
import playground.dhosse.gap.scenario.GAPScenarioBuilder;
import playground.dhosse.gap.scenario.mid.MiDPersonGroupTemplates;
import playground.dhosse.gap.scenario.mid.MiDTravelChain;
import playground.dhosse.gap.scenario.mid.MiDTravelChain.MiDTravelStage;
import playground.dhosse.gap.scenario.population.EgapPopulationUtils;
import playground.dhosse.gap.scenario.population.PlanCreationUtils;
import playground.dhosse.gap.scenario.population.PlansCreatorV2;
import playground.dhosse.gap.scenario.population.io.CommuterDataElement;
import playground.dhosse.utils.EgapHashGenerator;

public class CreateDemand {
	
	public static void run(String munId, int a0, int aX, int amount, Scenario scenario, MiDPersonGroupTemplates templates){
		
		Population population = scenario.getPopulation();
		PopulationFactory factory = population.getFactory();
		
		for(int i = 0; i < amount; i++){
			
			int age = EgapPopulationUtils.setAge(a0, aX);
			int sex = EgapPopulationUtils.setSex(age);
			
			boolean isEmployed = false;
			boolean hasLicense = false;
			boolean carAvail = false;
			
			double rnd = Global.random.nextDouble();
			
			if(sex == 0){
			
				if(age < 10){
					
					if(rnd <= 0.4388){
						
						isEmployed = true;
						hasLicense = false;
						carAvail = false;
						
					} else{
						
						isEmployed = false;
						hasLicense = false;
						carAvail = false;
						
					}
					
				} else if(age >= 10 && age < 20){
					
					if(rnd <= 0.1801){
						
						isEmployed = true;
						hasLicense = true;
						carAvail = true;
						
					} else{
						
						isEmployed = true;
						hasLicense = false;
						carAvail = false;
						
					}
					
				} else if(age >= 20 && age < 30){
					
						
					isEmployed = false;
					hasLicense = true;
					carAvail = true;
					
				} else if(age >= 30 && age < 40){
					
					i--;
					continue;
					
				} else if(age >= 40 && age < 50){
					
					if(rnd <= 0.8333){
						
						isEmployed = false;
						hasLicense = true;
						carAvail = true;
						
					} else if(rnd <= 0.9815){
						
						isEmployed = false;
						hasLicense = true;
						carAvail = true;
						
					} else{
						
						isEmployed = false;
						hasLicense = true;
						carAvail = false;
						
					}
					
				} else if(age >= 50 && age < 60){
					
					if(rnd <= 0.9474){
						
						isEmployed = false;
						hasLicense = true;
						carAvail = true;
						
					} else{
						
						isEmployed = false;
						hasLicense = false;
						carAvail = false;
						
					}
					
				} else if(age >= 60 && age < 70){
					
					if(rnd <= 0.9669){
						
						isEmployed = false;
						hasLicense = true;
						carAvail = true;
						
					} else if(rnd <= 0.9752){
						
						isEmployed = false;
						hasLicense = true;
						carAvail = false;
						
					} else{
						
						isEmployed = false;
						hasLicense = false;
						carAvail = false;
						
					}
					
				} else if(age >= 70 && age < 80){
					
					if(rnd <= 0.0357){
						
						isEmployed = true;
						hasLicense = true;
						carAvail = true;
						
					} else if(rnd <= 0.9762){
						
						isEmployed = false;
						hasLicense = true;
						carAvail = true;
						
					} else{
						
						isEmployed = false;
						hasLicense = false;
						carAvail = false;
						
					}
					
				} else{
					
					if(rnd <= 0.7778){
						
						isEmployed = false;
						hasLicense = true;
						carAvail = true;
						
					} else if(rnd <= 0.8889){
						
						isEmployed = false;
						hasLicense = true;
						carAvail = false;
						
					} else{
						
						isEmployed = false;
						hasLicense = false;
						carAvail = false;
						
					}
					
				}
				
			} else {
				
				if(age < 10){
					
					if(rnd <= 0.5139){
						
						isEmployed = true;
						hasLicense = false;
						carAvail = false;
						
					} else{
						
						isEmployed = false;
						hasLicense = false;
						carAvail = false;
						
					}
					
				} else if(age >= 10 && age < 20){
					
					if(rnd <= 0.2176){
						
						isEmployed = true;
						hasLicense = true;
						carAvail = true;
						
					} else if(rnd <= 0.2315){
						
						isEmployed = false;
						hasLicense = true;
						carAvail = true;
						
					} else if(rnd <= 0.2407){
						
						isEmployed = true;
						hasLicense = true;
						carAvail = false;
						
					} else{
						
						isEmployed = true;
						hasLicense = false;
						carAvail = false;
						
					}
					
				} else if(age >= 20 && age < 30){
					
					if(rnd <= 0.8125){

						isEmployed = false;
						hasLicense = true;
						carAvail = true;
						
					} else{
						
						isEmployed = false;
						hasLicense = false;
						carAvail = false;
						
					}
					
				} else if(age >= 30 && age < 40){
					
					isEmployed = false;
					hasLicense = true;
					carAvail = true;
					
				} else if(age >= 40 && age < 50){
					
					if(rnd <= 0.9275){
						
						isEmployed = false;
						hasLicense = true;
						carAvail = true;
						
					} else{
						
						isEmployed = false;
						hasLicense = false;
						carAvail = false;
						
					}
					
				} else if(age >= 50 && age < 60){
					
					if(rnd <= 0.9351){
						
						isEmployed = false;
						hasLicense = true;
						carAvail = true;
						
					} else if(rnd <= 0.9481){
						
						isEmployed = false;
						hasLicense = true;
						carAvail = false;
						
					} else{
						
						isEmployed = false;
						hasLicense = false;
						carAvail = false;
						
					}
					
				} else if(age >= 60 && age < 70){
					
					if(rnd <= 0.7642){
						
						isEmployed = false;
						hasLicense = true;
						carAvail = true;
						
					} else if(rnd <= 0.783){
						
						isEmployed = false;
						hasLicense = true;
						carAvail = false;
						
					} else{
						
						isEmployed = false;
						hasLicense = false;
						carAvail = false;
						
					}
					
				} else if(age >= 70 && age < 80){
					
					if(rnd <= 0.0164){
						
						isEmployed = true;
						hasLicense = true;
						carAvail = true;
						
					} else if(rnd <= 0.5902){
						
						isEmployed = false;
						hasLicense = true;
						carAvail = true;
						
					} else if(rnd <= 0.6066){
						
						isEmployed = false;
						hasLicense = true;
						carAvail = false;
						
					} else{
						
						isEmployed = false;
						hasLicense = false;
						carAvail = false;
						
					}
					
				} else{
					
					if(rnd <= 0.3846){
						
						isEmployed = false;
						hasLicense = true;
						carAvail = true;
						
					} else{
						
						isEmployed = false;
						hasLicense = false;
						carAvail = false;
						
					}
					
				}
				
			}
			
			double timeShift = PlansCreatorV2.createRandomTimeShift(1);
			
			Person person = factory.createPerson(Id.createPersonId(munId + "_" + EgapHashGenerator.generateAgeGroupHash(a0, aX) + "_" + i));
			Plan plan = factory.createPlan();
			
			GAPScenarioBuilder.getDemographicAttributes().putAttribute(person.getId().toString(), Global.SEX, Integer.toString(sex));
			GAPScenarioBuilder.getDemographicAttributes().putAttribute(person.getId().toString(), Global.AGE, Integer.toString(age));
			GAPScenarioBuilder.getDemographicAttributes().putAttribute(person.getId().toString(), Global.LICENSE, Boolean.toString(hasLicense));
			GAPScenarioBuilder.getDemographicAttributes().putAttribute(person.getId().toString(), Global.CAR_AVAIL, Boolean.toString(carAvail));
			
			if(hasLicense){
				
				GAPScenarioBuilder.getSubpopulationAttributes().putAttribute(person.getId().toString(), Global.CARSHARING, Global.CAR_OPTION);
				
				if(carAvail){
					
					GAPScenarioBuilder.getSubpopulationAttributes().putAttribute(person.getId().toString(), Global.USER_GROUP, Global.GP_CAR);
					
				}
				
			}
			
			String pHash = EgapHashGenerator.generatePersonHash(age, sex, carAvail, hasLicense, isEmployed);
			
			Map<String, List<MiDTravelChain>> patterns = templates.getTravelPatterns(pHash);
			
			if(patterns == null){
				i--;
				continue;
			}
			
			MiDTravelChain patternTemplate = null;
			double patternRandom = Global.random.nextDouble(); 
			double accumulatedWeight = 0.;
			
			for(Entry<String, List<MiDTravelChain>> entry : patterns.entrySet()){
				
				accumulatedWeight += entry.getValue().size() / templates.getWeightForPersonGroupHash(pHash);
				
				if(accumulatedWeight >= patternRandom){
					
					int rndIndex = Global.random.nextInt(entry.getValue().size());
					patternTemplate = entry.getValue().get(rndIndex);
					
				}
				
			}
			
			Coord homeCoord = Global.gk4ToUTM32N.transform(PlanCreationUtils.shoot(GAPScenarioBuilder.getMunId2Geometry().get(munId)));
			
			Activity homeAct = factory.createActivityFromCoord(Global.ActType.home.name(), homeCoord);
			
			Activity currentAct = null;
			
			for(MiDTravelStage stage : patternTemplate.getStages()){
				
				String prevActType = stage.getPreviousActType();
				String nextActType = stage.getNextActType();
				String legMode = stage.getLegMode().equals("car (passenger)") ? TransportMode.ride : stage.getLegMode();
				double departure = stage.getDepartureTime();
				double arrival = stage.getArrivalTime();

				if(plan.getPlanElements().size() == 0){
					
					if(prevActType.equals(Global.ActType.home.name())){

						currentAct = factory.createActivityFromCoord(homeAct.getType(), homeCoord);
//						((ActivityImpl)currentAct).setLinkId(NetworkUtils.getNearestLink(scenario.getNetwork(), homeCoord).getId());
						
					} else{
						
						Coord coord = Global.gk4ToUTM32N.transform(PlanCreationUtils.shoot(GAPScenarioBuilder.getMunId2Geometry().get(munId)));
						currentAct = factory.createActivityFromCoord(Global.ActType.other.name(), coord);
//						((ActivityImpl)currentAct).setLinkId(NetworkUtils.getNearestLink(scenario.getNetwork(), coord).getId());
						
					}
					
					currentAct.setStartTime(0.);
					
				}
				
				currentAct.setEndTime(departure + timeShift);
				
				if(currentAct.getEndTime() < currentAct.getStartTime()){
					currentAct.setEndTime(24 * 3600);
					plan.addActivity(currentAct);
					break;
				}
				
				Leg leg = factory.createLeg(legMode);
				leg.setDepartureTime(departure);
				
				double d = PlanCreationUtils.getTravelDistanceForMode(legMode);
				if(d > 50000/1.3){
					d = 50000/1.3;
				}
				
				Coord c = null;
				
				if(nextActType.equals(Global.ActType.home.name())){
					
					c = homeCoord;
					
				} else{
					
					do{
						
						c = PlanCreationUtils.createNewRandomCoord(currentAct.getCoord(), d);
						c = Global.UTM32NtoGK4.transform(PlanCreationUtils.createNewRandomCoord(currentAct.getCoord(), d));
						Geometry g = GAPScenarioBuilder.getBuiltAreaQT().get(c.getX(), c.getY());
						c = Global.gk4ToUTM32N.transform(PlanCreationUtils.shoot(g));
						
					} while(CoordUtils.calcDistance(currentAct.getCoord(), c) > 50000/1.3);
					
					Coord coord = checkQuadTreesForFacilityCoords(nextActType, c);
					if(coord != null){
						c = coord;
					}
					
				}
				
				Activity nextAct = factory.createActivityFromCoord(nextActType, c);
//				((ActivityImpl)nextAct).setLinkId(NetworkUtils.getNearestLink(scenario.getNetwork(), c).getId());
				nextAct.setStartTime(arrival + timeShift);

				plan.addActivity(currentAct);
				plan.addLeg(leg);
				
				if(patternTemplate.getStages().indexOf(stage) >= patternTemplate.getStages().size() - 1){
					
					nextAct.setEndTime(24 * 3600);
					plan.addActivity(nextAct);
					
				}
				
				currentAct = nextAct;
				
			}
			
			person.addPlan(plan);
			population.addPerson(person);
			
		}
		
	}
	
	public static void createCommuters(String munId, String workId, int a0, int aX, CommuterDataElement relation,
			Scenario scenario, MiDPersonGroupTemplates templates){
		
		Population population = scenario.getPopulation();
		PopulationFactory factory = population.getFactory();
		
		for(int i = 0; i < relation.getCommuters(); i++){
			
			int age = EgapPopulationUtils.setAge(a0, aX);
			int sex = EgapPopulationUtils.setSex(age);
			
			boolean isEmployed = false;
			boolean hasLicense = false;
			boolean carAvail = false;
			
			double rnd = Global.random.nextDouble();
			
			if(age < 20 || age >= 70){
				
				i--;
				continue;
				
			} else if(age >= 10 && age < 20){
				
				i--;
				continue;
				
			}
			
			if(sex == 0){
			
				if(age >= 20 && age < 30){
					
					if(rnd <= 0.9194){

						isEmployed = true;
						hasLicense = true;
						carAvail = true;
						
					} else{
						
						isEmployed = true;
						hasLicense = false;
						carAvail = false;
						
					}
					
				} else if(age >= 30 && age < 40){
					
					isEmployed = true;
					hasLicense = true;
					carAvail = true;
					
				} else if(age >= 40 && age < 50){
					
					if(rnd <= 0.9886){
						
						isEmployed = true;
						hasLicense = true;
						carAvail = true;
						
					} else{
						
						isEmployed = true;
						hasLicense = false;
						carAvail = false;
						
					}
					
				} else if(age >= 50 && age < 60){
					
					if(rnd <= 0.9679){
						
						isEmployed = true;
						hasLicense = true;
						carAvail = true;
						
					} else if(rnd <= 0.9733){
						
						isEmployed = true;
						hasLicense = true;
						carAvail = false;
						
					} else{
						
						isEmployed = true;
						hasLicense = false;
						carAvail = false;
						
					}
					
				} else if(age >= 60 && age < 70){
					
					if(rnd <= 0.92){
						
						isEmployed = true;
						hasLicense = true;
						carAvail = true;
						
					} else{
						
						isEmployed = true;
						hasLicense = false;
						carAvail = false;
						
					}
					
				} else{
					
					i--;
					continue;
					
				}
				
			} else {
				
				if(age >= 20 && age < 30){
					
					if(rnd <= 0.956){

						isEmployed = true;
						hasLicense = true;
						carAvail = true;
						
					} else if(rnd <= 0.967){
						
						isEmployed = true;
						hasLicense = true;
						carAvail = false;
						
					} else{
						
						isEmployed = true;
						hasLicense = false;
						carAvail = false;
						
					}
					
				} else if(age >= 30 && age < 40){
					
					if(rnd <= 0.9792){
						
						isEmployed = true;
						hasLicense = true;
						carAvail = true;
						
					}else{
						
						isEmployed = true;
						hasLicense = false;
						carAvail = false;
						
					}
					
				} else if(age >= 40 && age < 50){
					
					if(rnd <= 0.986){
						
						isEmployed = true;
						hasLicense = true;
						carAvail = true;
						
					} else{
						
						isEmployed = true;
						hasLicense = false;
						carAvail = false;
						
					}
					
				} else if(age >= 50 && age < 60){
					
					if(rnd <= 0.9524){
						
						isEmployed = true;
						hasLicense = true;
						carAvail = true;
						
					} else if(rnd <= 0.9603){
						
						isEmployed = true;
						hasLicense = true;
						carAvail = false;
						
					} else{
						
						isEmployed = true;
						hasLicense = false;
						carAvail = false;
						
					}
					
				} else if(age >= 60 && age < 70){
					
					if(rnd <= 0.8696){
						
						isEmployed = true;
						hasLicense = true;
						carAvail = true;
						
					} else if(rnd <= 0.913){
						
						isEmployed = true;
						hasLicense = true;
						carAvail = false;
						
					} else{
						
						isEmployed = true;
						hasLicense = false;
						carAvail = false;
						
					}
					
				} else{
					
					i--;
					continue;
					
				}
				
			}
			
			double timeShift = PlansCreatorV2.createRandomTimeShift(1);
			
			Person person = factory.createPerson(Id.createPersonId(munId + "_" + workId + "_" + EgapHashGenerator.generateAgeGroupHash(a0, aX) + "_" + i));
			Plan plan = factory.createPlan();
			
			GAPScenarioBuilder.getDemographicAttributes().putAttribute(person.getId().toString(), Global.SEX, Integer.toString(sex));
			GAPScenarioBuilder.getDemographicAttributes().putAttribute(person.getId().toString(), Global.AGE, Integer.toString(age));
			GAPScenarioBuilder.getDemographicAttributes().putAttribute(person.getId().toString(), Global.LICENSE, Boolean.toString(hasLicense));
			GAPScenarioBuilder.getDemographicAttributes().putAttribute(person.getId().toString(), Global.CAR_AVAIL, Boolean.toString(carAvail));
			
			if(hasLicense){
				
				GAPScenarioBuilder.getSubpopulationAttributes().putAttribute(person.getId().toString(), Global.CARSHARING, Global.CAR_OPTION);
				
				if(carAvail){
					
					GAPScenarioBuilder.getSubpopulationAttributes().putAttribute(person.getId().toString(), Global.USER_GROUP, Global.GP_CAR);
					
				}
				
			}
			
			String pHash = EgapHashGenerator.generatePersonHash(age, sex, carAvail, hasLicense, isEmployed);
			
			Map<String, List<MiDTravelChain>> patterns = templates.getTravelPatterns(pHash);
			
			if(patterns == null){
				i--;
				continue;
			}
			
			MiDTravelChain patternTemplate = null;
			double patternRandom = Global.random.nextDouble(); 
			double accumulatedWeight = 0.;
			
			for(Entry<String, List<MiDTravelChain>> entry : patterns.entrySet()){
				
				accumulatedWeight += entry.getValue().size() / templates.getWeightForPersonGroupHash(pHash);
				
				if(accumulatedWeight >= patternRandom){
					
					int rndIndex = Global.random.nextInt(entry.getValue().size());
					patternTemplate = entry.getValue().get(rndIndex);
					
				}
				
			}
			
			Coord homeCoord = Global.gk4ToUTM32N.transform(PlanCreationUtils.shoot(GAPScenarioBuilder.getMunId2Geometry().get(munId)));
			Activity homeAct = factory.createActivityFromCoord(Global.ActType.home.name(), homeCoord);
			
			Coord workCoord = Global.gk4ToUTM32N.transform(PlanCreationUtils.shoot(GAPScenarioBuilder.getMunId2Geometry().get(workId)));
			
			Activity currentAct = null;
			
			for(MiDTravelStage stage : patternTemplate.getStages()){
				
				String prevActType = stage.getPreviousActType();
				String nextActType = stage.getNextActType();
				String legMode = stage.getLegMode().equals("car (passenger)") ? TransportMode.ride : stage.getLegMode();
				double departure = stage.getDepartureTime();
				double arrival = stage.getArrivalTime();

				if(plan.getPlanElements().size() == 0){
					
					if(prevActType.equals(Global.ActType.home.name())){

						currentAct = factory.createActivityFromCoord(homeAct.getType(), homeCoord);
//						((ActivityImpl)currentAct).setLinkId(NetworkUtils.getNearestLink(scenario.getNetwork(), homeCoord).getId());
						
					} else if(prevActType.equals(Global.ActType.work.name())){
						
						currentAct = factory.createActivityFromCoord(Global.ActType.work.name(), homeCoord);
						
					} else{
						
						Coord coord = Global.gk4ToUTM32N.transform(PlanCreationUtils.shoot(GAPScenarioBuilder.getMunId2Geometry().get(munId)));
						currentAct = factory.createActivityFromCoord(Global.ActType.other.name(), coord);
//						((ActivityImpl)currentAct).setLinkId(NetworkUtils.getNearestLink(scenario.getNetwork(), coord).getId());
						
					}
					
					currentAct.setStartTime(0.);
					
				}
				
				currentAct.setEndTime(departure + timeShift);
				
				if(currentAct.getEndTime() < currentAct.getStartTime()){
					currentAct.setEndTime(24 * 3600);
					plan.addActivity(currentAct);
					break;
				}
				
				Leg leg = factory.createLeg(legMode);
				leg.setDepartureTime(departure);
				
				double d = PlanCreationUtils.getTravelDistanceForMode(legMode);
				if(d > 50000/1.3){
					d = 50000/1.3;
				}
				
				Coord c = null;
				
				if(nextActType.equals(Global.ActType.home.name())){
					
					c = homeCoord;
					
				} else if(nextActType.equals(Global.ActType.work.name())){
					
					c = workCoord;
					
				} else{
					
					do{
						
						c = PlanCreationUtils.createNewRandomCoord(currentAct.getCoord(), d);
						c = Global.UTM32NtoGK4.transform(PlanCreationUtils.createNewRandomCoord(currentAct.getCoord(), d));
						Geometry g = GAPScenarioBuilder.getBuiltAreaQT().get(c.getX(), c.getY());
						c = Global.gk4ToUTM32N.transform(PlanCreationUtils.shoot(g));
						
					} while(CoordUtils.calcDistance(currentAct.getCoord(), c) > 50000/1.3);
					
					Coord coord = checkQuadTreesForFacilityCoords(nextActType, c);
					if(coord != null){
						c = coord;
					}
					
				}
				
				Activity nextAct = factory.createActivityFromCoord(nextActType, c);
//				((ActivityImpl)nextAct).setLinkId(NetworkUtils.getNearestLink(scenario.getNetwork(), c).getId());
				nextAct.setStartTime(arrival + timeShift);

				plan.addActivity(currentAct);
				plan.addLeg(leg);
				
				if(patternTemplate.getStages().indexOf(stage) >= patternTemplate.getStages().size() - 1){
					
					nextAct.setEndTime(24 * 3600);
					plan.addActivity(nextAct);
					
				}
				
				currentAct = nextAct;
				
			}
			
			person.addPlan(plan);
			population.addPerson(person);
			
		}
		
	}
	
	private static Coord checkQuadTreesForFacilityCoords(String nextActType, Coord coord){
		
		Coord c = null;
		
		if(nextActType.equals(Global.ActType.leisure.name())){
			
			c = GAPScenarioBuilder.getLeisureQT().get(coord.getX(), coord.getY()).getCoord();
			
		} else if(nextActType.equals(Global.ActType.shop.name())){
			
			c = GAPScenarioBuilder.getShopQT().get(coord.getX(), coord.getY()).getCoord();
			
		} else if(nextActType.equals(Global.ActType.education.name())){
			
			c = GAPScenarioBuilder.getEducationQT().get(coord.getX(), coord.getY()).getCoord();
			
		} else if(nextActType.equals(Global.ActType.work.name())){
			c = GAPScenarioBuilder.getWorkLocations().get(coord.getX(), coord.getY()).getCoord();
		}
		
		return c;
		
	}
	
}
