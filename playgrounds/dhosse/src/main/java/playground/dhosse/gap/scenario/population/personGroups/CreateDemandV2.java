package playground.dhosse.gap.scenario.population.personGroups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

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
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.facilities.ActivityFacility;
import org.matsim.matrices.Matrix;

import com.vividsolutions.jts.geom.Geometry;

import playground.dhosse.gap.GAPMatrices;
import playground.dhosse.gap.Global;
import playground.dhosse.gap.scenario.GAPScenarioBuilder;
import playground.dhosse.gap.scenario.mid.MiDPersonGroupTemplates;
import playground.dhosse.gap.scenario.mid.MiDTravelChain;
import playground.dhosse.gap.scenario.mid.MiDTravelChain.MiDTravelStage;
import playground.dhosse.gap.scenario.population.PlansCreatorV2;
import playground.dhosse.gap.scenario.population.io.CommuterDataElement;
import playground.dhosse.gap.scenario.population.utils.EgapPopulationUtils;
import playground.dhosse.gap.scenario.population.utils.LegModeCreator;
import playground.dhosse.gap.scenario.population.utils.PlanCreationUtils;
import playground.dhosse.utils.EgapHashGenerator;

public class CreateDemandV2 {
	
	private static String lastMunId = "";
	
	private static Map<String,Double> NinetyPctDistances = new HashMap<>();
	
	public static void runTryout(String munId, int a0, int aX, int amount, Scenario scenario, MiDPersonGroupTemplates templates, Map<String, Matrix> odMatrices){
		
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
				
				GAPScenarioBuilder.getSubpopulationAttributes().putAttribute(person.getId().toString(), Global.USER_GROUP, Global.LICENSE_OWNER);
				
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
					break;
					
				}
				
			}
			
			Coord homeCoord = null;
			
			if(munId.length() <= 4 || munId.contains("AT")){
				
				homeCoord = Global.ct.transform(PlanCreationUtils.shoot(GAPScenarioBuilder.getMunId2Geometry().get(munId)));

			} else {

					homeCoord = Global.gk4ToUTM32N.transform(PlanCreationUtils.shoot(GAPScenarioBuilder.getMunId2Geometry().get(munId)));
				
			}
			
			Activity homeAct = factory.createActivityFromCoord(Global.ActType.home.name(), homeCoord);
			
			Activity currentAct = null;
			
			lastMunId = munId;
			
			for(MiDTravelStage stage : patternTemplate.getStages()){
				
				String prevActType = stage.getPreviousActType();
				String nextActType = stage.getNextActType();
//				String legMode = stage.getLegMode().equals("car (passenger)") ? TransportMode.ride : stage.getLegMode();
				String legMode = LegModeCreator.getLegModeForDistance(1000*stage.getDistance()*1.5, carAvail, hasLicense, age, sex);
				double departure = stage.getDepartureTime();
				double currentTime = departure;

				if(plan.getPlanElements().size() == 0){
					
					if(prevActType.equals(Global.ActType.home.name())){

						currentAct = factory.createActivityFromCoord(homeAct.getType(), homeCoord);
						((ActivityImpl)currentAct).setLinkId(NetworkUtils.getNearestLink(scenario.getNetwork(), homeCoord).getId());
						
					} else{
						
						Coord coord = null;
						if(munId.length() <= 4 || munId.contains("AT")){
							
							coord = Global.ct.transform(PlanCreationUtils.shoot(GAPScenarioBuilder.getMunId2Geometry().get(munId)));
							
						} else{
							
							coord = Global.gk4ToUTM32N.transform(PlanCreationUtils.shoot(GAPScenarioBuilder.getMunId2Geometry().get(munId)));
							
						}
						currentAct = factory.createActivityFromCoord(Global.ActType.other.name(), coord);
						((ActivityImpl)currentAct).setLinkId(NetworkUtils.getNearestLink(scenario.getNetwork(), coord).getId());
						
					}
					
					currentAct.setStartTime(0.);
					
				}
				
				currentAct.setEndTime(currentTime + timeShift);
				if(currentAct.getEndTime() - currentAct.getStartTime() < 1800){
					if(currentAct.getStartTime() == 0){
						currentAct.setEndTime(currentAct.getStartTime() + Global.random.nextInt(3601));
					} else{
						currentAct.setEndTime(currentAct.getStartTime() + 1800);
					}
				}
				currentTime = currentAct.getEndTime();
				
				if(currentAct.getEndTime() > 24*3600 || currentAct.getEndTime() < currentAct.getStartTime()){
					currentAct.setEndTime(24 * 3600);
					if(currentAct.getEndTime() - currentAct.getStartTime() >= 1800){
						plan.addActivity(currentAct);
					} else{
						plan.getPlanElements().remove(plan.getPlanElements().size()-1);
					}
					break;
				}
				
				Leg leg = factory.createLeg(legMode);
				leg.setDepartureTime(currentTime);
				
				double d = 1000*stage.getDistance() * 1.5;
				
				Coord c = null;
				
				String toId = null;
				
				String pattern = getActPattern(prevActType, nextActType);
				if(legMode.equals(TransportMode.walk)){
					toId = lastMunId;
				} else{
					toId = distributeTrip(nextActType, odMatrices.get(pattern), d, legMode);
				}
				
				if(nextActType.equals(Global.ActType.home.name())){
					
					c = homeCoord;
					toId = munId;
					
				} else{
					
					c = getActivityLocation(toId, nextActType, currentAct.getCoord(), d);
					
					if(c == null){
						
						if(nextActType.equals(Global.ActType.education.name())){
							
							c = GAPScenarioBuilder.getEducationQT().getClosest(currentAct.getCoord().getX(), currentAct.getCoord().getY()).getCoord();
							
						} else if(nextActType.equals(Global.ActType.leisure.name())){
							
							c = GAPScenarioBuilder.getLeisureQT().getClosest(currentAct.getCoord().getX(), currentAct.getCoord().getY()).getCoord();
							
						} else if(nextActType.equals(Global.ActType.shop.name())){
							
							c = GAPScenarioBuilder.getShopQT().getClosest(currentAct.getCoord().getX(), currentAct.getCoord().getY()).getCoord();
							
						} else if(nextActType.equals(Global.ActType.work.name())){
							
							c = GAPScenarioBuilder.getWorkLocations().getClosest(currentAct.getCoord().getX(), currentAct.getCoord().getY()).getCoord();
							
						} else{
							
							c = GAPScenarioBuilder.getOtherQT().getClosest(currentAct.getCoord().getX(), currentAct.getCoord().getY()).getCoord();
							
						}
						
					}
					
				}
				
				lastMunId = toId;
				
				((ActivityImpl)currentAct).setLinkId(NetworkUtils.getNearestLink(scenario.getNetwork(), currentAct.getCoord()).getId());
				
				double ttime = CoordUtils.calcEuclideanDistance(currentAct.getCoord(), c) / getSpeedForMode(legMode);
				
				Activity nextAct = factory.createActivityFromCoord(nextActType, c);
//				((ActivityImpl)nextAct).setLinkId(NetworkUtils.getNearestLink(scenario.getNetwork(), c).getId());
				nextAct.setStartTime(currentAct.getEndTime() + ttime);
				
				if(nextAct.getStartTime() > 24 * 3600){
					currentAct.setEndTime(24 * 3600);
					if(currentAct.getEndTime() - currentAct.getStartTime() >= 1800){
						plan.addActivity(currentAct);
					} else{
						plan.getPlanElements().remove(plan.getPlanElements().size()-1);
					}
					break;
				}
				
				plan.addActivity(currentAct);
				plan.addLeg(leg);

				if(nextAct.getEndTime() > 24 * 3600){
					nextAct.setEndTime(24*3600);
					if(nextAct.getEndTime()- nextAct.getStartTime() >= 1800){
						plan.addActivity(nextAct);
					} else{
						plan.getPlanElements().remove(plan.getPlanElements().size()-1);
					}
					break;
				}
				
				if(patternTemplate.getStages().indexOf(stage) >= patternTemplate.getStages().size() - 1){
					
					nextAct.setEndTime(24 * 3600);
					if(nextAct.getEndTime()- nextAct.getStartTime() >= 1800){
						plan.addActivity(nextAct);
					} else{
						plan.getPlanElements().remove(plan.getPlanElements().size()-1);
					}
					
				}
				
				currentAct = nextAct;
				
			}
			
			person.addPlan(plan);
			person.setSelectedPlan(plan);
			population.addPerson(person);
			
		}
		
	}
	
	private static List<ActivityFacility> getFacilitiesInRange(Coord lastCoord, List<ActivityFacility> facilities, double distance){
		
		List<ActivityFacility> result = new ArrayList<>();
		
		for(ActivityFacility facility : facilities){
			if(CoordUtils.calcEuclideanDistance(lastCoord, facility.getCoord()) <= distance){
				result.add(facility);
			}
		}
		
		return result;
		
	}
	
	private static boolean isPrimaryActType(String actType){
		
		return(actType.equals(Global.ActType.work.name()) || actType.equals(Global.ActType.work.name()) || actType.equals(Global.ActType.education.name()));
		
	}
	
	private static String getActPattern(String prevActType, String nextActType){
		
		if(prevActType.equals(Global.ActType.home.name())){
			
			if(nextActType.equals(Global.ActType.work.name())){
				
				return "WA";
				
			} else if(nextActType.equals(Global.ActType.education.name())){
				
				return "WB";
				
			} else if(nextActType.equals(Global.ActType.shop.name())){
				
				return "WE";
				
			} else if(nextActType.equals(Global.ActType.leisure.name())){
				
				return "WF";
				
			} else{
				
				return "WS";
				
			}
			
		} else if(prevActType.equals(Global.ActType.work.name())){
			
			if(nextActType.equals(Global.ActType.home.name())){
				
				return "AW";
				
			} else{
				
				return "WS";
				
			}
			
		} else if(prevActType.equals(Global.ActType.education.name())){
			
			if(nextActType.equals(Global.ActType.home.name())){
				
				return "BW";
				
			} else{
				
				return "SS";
				
			}
			
		} else if(prevActType.equals(Global.ActType.shop.name())){
			
			if(nextActType.equals(Global.ActType.home.name())){
				
				return "EW";
				
			} else if(nextActType.equals(Global.ActType.work.name())){
				
				return "SA";
				
			} else {
				
				return "SS";
				
			}
			
		} else if(prevActType.equals(Global.ActType.leisure.name())){
			
			if(nextActType.equals(Global.ActType.home.name())){
				
				return "FW";
				
			} else{
				
				return "SS";
				
			}
			
		} else{
			
			if(nextActType.equals(Global.ActType.home.name())){
				
				return "SW";
				
			} else if(nextActType.equals(Global.ActType.work.name())){
				
				return "SA";
				
			} else{
				
				return "SS";
				
			}
			
		}
		
	}

	static Comparator<org.matsim.matrices.Entry> matrixEntryComparator = new Comparator<org.matsim.matrices.Entry>() {

		@Override
		public int compare(org.matsim.matrices.Entry o1, org.matsim.matrices.Entry o2) {
			if(o1.getValue() > o2.getValue()){
				return -1;
			} else if(o1.getValue() < o2.getValue()){
				return 1;
			}
			return 0;
		}
	};
	
	private static String distributeTrip(String actType, Matrix od, double distance, String mode){
		
		if(od == null){
			return null;
		}
		
		double proba = LegModeCreator.getProbaForDistance(mode, distance);
		
		ArrayList<org.matsim.matrices.Entry> entries = od.getFromLocations().get(lastMunId);
		
		ArrayList<org.matsim.matrices.Entry> entriesInRange = new ArrayList<>();
		int i = 0;
		if(entries == null){
			return null;
		}
		
		double accumulatedWeights = 0.;
		
		for(org.matsim.matrices.Entry entry : entries){
			double d = GAPMatrices.getDistances().getFromLocations().get(lastMunId).get(i).getValue();
			if(d <= distance || entry.getToLocation().equals(lastMunId)){
				entriesInRange.add(entry);
				accumulatedWeights += entry.getValue() * proba;
			}
			i++;
		}
		
		double random = Global.random.nextDouble() * accumulatedWeights;
		
		if(entriesInRange.size() < 1) return lastMunId;
		
		Collections.sort(entriesInRange, matrixEntryComparator);
		
		double weight = 0.;
		
		for(org.matsim.matrices.Entry entry : entriesInRange){
			
			weight += entry.getValue();
			if(weight >= random){
				return entry.getToLocation();
			}
			
		}
		
		return null;
		
	}
	
	public static void createCommuters(String munId, String workId, int a0, int aX, CommuterDataElement relation,
			Scenario scenario, MiDPersonGroupTemplates templates, Map<String,Matrix> odMatrices){
		
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
				
				GAPScenarioBuilder.getSubpopulationAttributes().putAttribute(person.getId().toString(), Global.USER_GROUP, Global.LICENSE_OWNER);
				
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
					break;
					
				}
				
			}
			
			Coord homeCoord = null;
			Coord workCoord = null;
			
			lastMunId = munId;
			
			if(munId.length() <= 4 || munId.contains("AT")){
				
				homeCoord = Global.ct.transform(PlanCreationUtils.shoot(GAPScenarioBuilder.getMunId2Geometry().get(munId)));

			} else {

				homeCoord = Global.gk4ToUTM32N.transform(PlanCreationUtils.shoot(GAPScenarioBuilder.getMunId2Geometry().get(munId)));
					
			}
			
			
			if(workCoord == null){
				if(workId.length() <= 4 || workId.contains("AT")){
					workCoord = Global.ct.transform(PlanCreationUtils.shoot(GAPScenarioBuilder.getMunId2Geometry().get(workId)));
				} else{
					workCoord = chooseWorkLocation(workId);
				}
			}
			
			Activity homeAct = factory.createActivityFromCoord(Global.ActType.home.name(), homeCoord);
			
			Activity currentAct = null;
			
			for(MiDTravelStage stage : patternTemplate.getStages()){
				
				String prevActType = stage.getPreviousActType();
				String nextActType = stage.getNextActType();
//				String legMode = stage.getLegMode().equals("car (passenger)") ? TransportMode.ride : stage.getLegMode();
				String legMode = LegModeCreator.getLegModeForDistance(1000*stage.getDistance()*1.5, carAvail, hasLicense, age, sex);
				double departure = stage.getDepartureTime();
				double currentTime = departure;

				if(plan.getPlanElements().size() == 0){
					
					if(prevActType.equals(Global.ActType.home.name())){

						currentAct = factory.createActivityFromCoord(homeAct.getType(), homeCoord);
//						((ActivityImpl)currentAct).setLinkId(NetworkUtils.getNearestLink(scenario.getNetwork(), homeCoord).getId());
						
					} else if(prevActType.equals(Global.ActType.work.name())){
						
						currentAct = factory.createActivityFromCoord(Global.ActType.work.name(), homeCoord);
						
					} else{
						
						Coord coord = null;
						if(munId.length() <= 4 || munId.contains("AT")){
							coord = Global.ct.transform(PlanCreationUtils.shoot(GAPScenarioBuilder.getMunId2Geometry().get(munId)));
						} else{
							coord = Global.gk4ToUTM32N.transform(PlanCreationUtils.shoot(GAPScenarioBuilder.getMunId2Geometry().get(munId)));
						}
						currentAct = factory.createActivityFromCoord(Global.ActType.other.name(), coord);
						
					}
					
					currentAct.setStartTime(0.);
					
				}
				
				currentAct.setEndTime(departure + timeShift);
				if(currentAct.getEndTime() - currentAct.getStartTime() < 1800){
					if(currentAct.getStartTime() == 0){
						currentAct.setEndTime(currentAct.getStartTime() + Global.random.nextInt(3601));
					} else{
						currentAct.setEndTime(currentAct.getStartTime() + 1800);
					}
				}	
				currentTime = currentAct.getEndTime();
				
				if(currentAct.getEndTime() > 24*3600 || currentAct.getEndTime() < currentAct.getStartTime()){
					currentAct.setEndTime(24 * 3600);
					if(currentAct.getEndTime() - currentAct.getStartTime() >= 1800){
						plan.addActivity(currentAct);
					} else{
						plan.getPlanElements().remove(plan.getPlanElements().size()-1);
					}
					break;
				}
				
				Leg leg = factory.createLeg(legMode);
				leg.setDepartureTime(currentTime);
				
				double d = 1000 * stage.getDistance() * 1.5;
				
				Coord c = null;
				
				String toId = null;
				
				String pattern = getActPattern(prevActType, nextActType);
				if(legMode.equals(TransportMode.walk)){
					toId = lastMunId;
				} else{
					toId = distributeTrip(nextActType, odMatrices.get(pattern), d, legMode);
					if(toId == null){
						toId = lastMunId;
					}
				}
				
				if(nextActType.equals(Global.ActType.home.name())){
					
					c = homeCoord;
					toId = munId;
					
				} else if(nextActType.equals(Global.ActType.work.name()) && !prevActType.equals(Global.ActType.work.name())){
					
					c = workCoord;
					toId = workId;
					
				}
				else{
					
					c = getActivityLocation(toId, nextActType, currentAct.getCoord(), d);
					
					if(c == null){
						
						if(nextActType.equals(Global.ActType.education.name())){
							
							c = GAPScenarioBuilder.getEducationQT().getClosest(currentAct.getCoord().getX(), currentAct.getCoord().getY()).getCoord();
							
						} else if(nextActType.equals(Global.ActType.leisure.name())){
							
							c = GAPScenarioBuilder.getLeisureQT().getClosest(currentAct.getCoord().getX(), currentAct.getCoord().getY()).getCoord();
							
						} else if(nextActType.equals(Global.ActType.shop.name())){
							
							c = GAPScenarioBuilder.getShopQT().getClosest(currentAct.getCoord().getX(), currentAct.getCoord().getY()).getCoord();
							
						} else if(nextActType.equals(Global.ActType.work.name())){
							
							c = GAPScenarioBuilder.getWorkLocations().getClosest(currentAct.getCoord().getX(), currentAct.getCoord().getY()).getCoord();
							
						} else{
							
							c = GAPScenarioBuilder.getOtherQT().getClosest(currentAct.getCoord().getX(), currentAct.getCoord().getY()).getCoord();
							
						}
						
					}
					
				}
				
				lastMunId = toId;
				
				((ActivityImpl)currentAct).setLinkId(NetworkUtils.getNearestLink(scenario.getNetwork(), currentAct.getCoord()).getId());
				
				double ttime = CoordUtils.calcEuclideanDistance(currentAct.getCoord(), c) / getSpeedForMode(legMode);
				
				Activity nextAct = factory.createActivityFromCoord(nextActType, c);
				nextAct.setStartTime(currentAct.getEndTime() + ttime);
				
				if(nextAct.getStartTime() > 24 * 3600){
					currentAct.setEndTime(24 * 3600);
					if(currentAct.getEndTime() - currentAct.getStartTime() >= 1800){
						plan.addActivity(currentAct);
					} else{
						plan.getPlanElements().remove(plan.getPlanElements().size()-1);
					}
					break;
				}

				plan.addActivity(currentAct);
				plan.addLeg(leg);
				
				if(nextAct.getEndTime() > 24 * 3600){
					nextAct.setEndTime(24*3600);
					if(nextAct.getEndTime()- nextAct.getStartTime() >= 1800){
						plan.addActivity(nextAct);
					} else{
						plan.getPlanElements().remove(plan.getPlanElements().size()-1);
					}
					break;
				}
				
				if(patternTemplate.getStages().indexOf(stage) >= patternTemplate.getStages().size() - 1){
					
					nextAct.setEndTime(24 * 3600);
					if(nextAct.getEndTime()- nextAct.getStartTime() >= 1800){
						plan.addActivity(nextAct);
					} else{
						plan.getPlanElements().remove(plan.getPlanElements().size()-1);
					}
					
				}
				
				currentAct = nextAct;
				
			}
			
			person.addPlan(plan);
			person.setSelectedPlan(plan);
			population.addPerson(person);
			
		}
		
	}
	
	private static double getSpeedForMode(String legMode){
		
		if(legMode.equals(TransportMode.bike)){
			
			return 15 / 3.6;
			
		} else if(legMode.equals(TransportMode.car) || legMode.equals(TransportMode.ride)){
			
			return 40/3.6;
			
		} else if(legMode.equals(TransportMode.pt)){
			
			return 35 / 3.6;
			
		} else if(legMode.equals(TransportMode.walk)){
			
			return 4 / 3.6;
			
		} else{
			
			return 0.;
			
		}
		
	}
	
	public static Map<String,Double> getNinetyPctDistances() {
		return NinetyPctDistances;
	}

	public static void setNinetyPctDistances(Map<String,Double> ninetyPctDistances) {
		NinetyPctDistances = ninetyPctDistances;
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
	
	private static Coord getActivityLocation(String munId, String actType, Coord current, double distance){
		
		List<ActivityFacility> facilities = null;
		
		if(actType.equals(Global.ActType.education.name())){
			
			facilities = GAPScenarioBuilder.getMunId2EducationFacilities().get(munId);
			
		} else if(actType.equals(Global.ActType.leisure.name())){
			
			facilities = GAPScenarioBuilder.getMunId2LeisureFacilities().get(munId);
			
		} else if(actType.equals(Global.ActType.other.name())){
			
			facilities = GAPScenarioBuilder.getMunId2OtherFacilities().get(munId);
			
		} else if(actType.equals(Global.ActType.shop.name())){
			
			facilities = GAPScenarioBuilder.getMunId2ShopFacilities().get(munId);
			
		} else if(actType.equals(Global.ActType.work.name())){
			
			facilities = GAPScenarioBuilder.getMunId2WorkLocation().get(munId);
			
		}
		
		if(facilities == null){
			
			return null;
			
		}
		
		if( facilities.size() < 1){
			
			return null;
			
		}
		
		List<ActivityFacility> inRange = new ArrayList<>();

		for(ActivityFacility facility : facilities){
			
			double d = CoordUtils.calcEuclideanDistance(current, facility.getCoord());
			if(d <= distance) inRange.add(facility);
			
		}
		
		if(inRange.size() < 1) return null;
		
		return inRange.get(Global.random.nextInt(inRange.size())).getCoord();
		
//		double accumulatedWeight = 0.;
//		double random = Global.random.nextDouble();
//		double weight = 0.;
//		
//		for(ActivityFacility facility : facilities){
//			
//			accumulatedWeight += facility.getActivityOptions().get(actType).getCapacity();
//			
//		}
//		
//		random *= accumulatedWeight;
//		
//		for(ActivityFacility facility : facilities){
//			
//			weight += facility.getActivityOptions().get(actType).getCapacity();
//			
//			if(weight <= random){
//				
//				return facility.getCoord();
//				
//			}
//			
//		}
		
	}
	
}
