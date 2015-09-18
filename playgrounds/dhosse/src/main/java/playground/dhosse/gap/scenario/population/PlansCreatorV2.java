package playground.dhosse.gap.scenario.population;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.ObjectAttributes;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import playground.dhosse.gap.Global;
import playground.dhosse.gap.scenario.GAPScenarioBuilder;
import playground.dhosse.gap.scenario.mid.MiDCSVReader;
import playground.dhosse.gap.scenario.mid.MiDPersonGroupData;
import playground.dhosse.gap.scenario.mid.MiDPersonGroupTemplates;
import playground.dhosse.gap.scenario.mid.MiDSurveyPerson;
import playground.dhosse.gap.scenario.population.io.CommuterDataElement;
import playground.dhosse.gap.scenario.population.io.CommuterFileReader;
import playground.dhosse.gap.scenario.population.personGroups.CreateCommutersFromElsewhere;
import playground.dhosse.gap.scenario.population.personGroups.CreateDemand;
import playground.dhosse.utils.EgapHashGenerator;

public class PlansCreatorV2 {
	
	private static final Logger log = Logger.getLogger(PlansCreatorV2.class);
	public static ObjectAttributes demographicAttributes = new ObjectAttributes();
	
	public static Population createPlans(Scenario scenario, String commuterFilename, String reverseCommuterFilename){
		
		//create a commuter reader and add municipalities to the filter in order to create commuter relations
		//that start or end in these municipalities
		CommuterFileReader cdr = new CommuterFileReader();
		
		cdr.addFilter("09180"); //GaPa (Kreis)
		cdr.addFilter("09180113"); //Bad Bayersoien
		cdr.addFilter("09180112"); //Bad Kohlgrub
		cdr.addFilter("09180114"); //Eschenlohe
		cdr.addFilter("09180115"); //Ettal
		cdr.addFilter("09180116"); //Farchant
		cdr.addFilter("09180117"); //Garmisch-Partenkirchen
		cdr.addFilter("09180118"); //Grainau
		cdr.addFilter("09180119"); //Großweil
		cdr.addFilter("09180122"); //Krün
		cdr.addFilter("09180123"); //Mittenwald
		cdr.addFilter("09180124"); //Murnau a Staffelsee
		cdr.addFilter("09180125"); //Oberammergau
		cdr.addFilter("09180126"); //Oberau
		cdr.addFilter("09180127"); //Ohlstadt
		cdr.addFilter("09180128"); //Riegsee
		cdr.addFilter("09180129"); //Saulgrub
		cdr.addFilter("09180131"); //Schwaigen
		cdr.addFilter("09180132"); //Seehausen a Staffelsee
		cdr.addFilter("09180134"); //Uffind a Staffelsee
		cdr.addFilter("09180135"); //Unterammergau
		cdr.addFilter("09180136"); //Wallgau
		
		cdr.read(reverseCommuterFilename, true);
		cdr.read(commuterFilename, false);
		
		//the actual demand generation
		createPersonsWithDemographicData(scenario, cdr.getCommuterRelations());
		
		return scenario.getPopulation();
		
	}
	
	private static void createPersonsWithDemographicData(Scenario scenario, Map<String, CommuterDataElement> relations){
		
		Map<String,MiDPersonGroupData> personGroupData = EgapPopulationUtilsV2.createMiDPersonGroups();
		
		MiDPersonGroupTemplates templates = new MiDPersonGroupTemplates();
		
		MiDCSVReader reader = new MiDCSVReader();
		reader.readV2(Global.matsimInputDir + "MID_Daten_mit_Wegeketten/travelsurvey_m.csv", templates);
		templates.setWeights();
		
		
//		for(MiDSurveyPerson p : persons.values()){
//			templates.handlePerson(p);
//		}
		
		for(Entry<String, Municipality> entry : Municipalities.getMunicipalities().entrySet()){
			
			CreateDemand.run(entry.getKey(), 6, 17, entry.getValue().getnStudents(), scenario, templates);
//			createPersonsFromPersonGroup(entry.getKey(), 6, 17, entry.getValue().getnStudents(), scenario, personGroupData.get("0_17"));
			
			int nCommuters = 0;
			List<String> keysToRemove = new ArrayList<>();
			
			for(String relation : relations.keySet()){
				
				String[] relationParts = relation.split("_");
				
				if(relationParts[0].startsWith(entry.getKey())){
	
					nCommuters += relations.get(relation).getCommuters();
					
					if(relationParts[1].startsWith("09180")){
						
//						createCommutersFromKey(scenario, relations.get(relation), personGroupData, templates);
						keysToRemove.add(relation);
						
					}
					
				}
				
			}
			
			for(String s : keysToRemove){
				
				relations.remove(s);
				
			}
			
			CreateDemand.run(entry.getKey(), 18, 65, entry.getValue().getnAdults() - nCommuters, scenario, templates);
			CreateDemand.run(entry.getKey(), 66, 100, entry.getValue().getnPensioners(), scenario, templates);
			
		}
		
		CreateCommutersFromElsewhere.run(scenario.getPopulation(), relations.values(), personGroupData);
		
	}
	
	private static void createCommutersFromKey(Scenario scenario, CommuterDataElement relation, Map<String, MiDPersonGroupData> personGroupData, MiDPersonGroupTemplates templates){
		
		PopulationFactoryImpl factory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		
			//parse over commuter relations
			//this is just for the reason that the shape file does not contain any diphtongs
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
			
			if(fromId.length() == 3 && !fromId.contains("AT")){
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
					
					int age = EgapPopulationUtils.setAge(20, 79);
//					agentAttributes.putAttribute(person.getId().toString(), Global.AGE, Integer.toString(age));
					int sex = EgapPopulationUtils.setSex(age);
//					agentAttributes.putAttribute(person.getId().toString(), Global.SEX, Integer.toString(sex));
					
					MiDPersonGroupData data = personGroupData.get(EgapHashGenerator.generateAgeGroupHash(18,65));

					if(data == null){
						
						i--;
						continue;
						
					}
					
					boolean hasLicense = PlanCreationUtils.setBooleanAttribute(person.getId().toString(), data.getpLicense(), Global.LICENSE);
					boolean carAvail = PlanCreationUtils.setBooleanAttribute(person.getId().toString(), data.getpCarAvail(), Global.CAR_AVAIL);
					
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
					
					List<MiDSurveyPerson> templatePersons = templates.getPersonGroups().get(EgapHashGenerator.generatePersonHash(age, sex, carAvail, hasLicense, true));
					
					if(templatePersons == null){
						
						i--;
						continue;
						
					}
					
					if(templatePersons.size() < 1){
						
						i--;
						continue;
						
					}
					
					int randomIndex = (int)(Global.random.nextDouble() * templatePersons.size());
					
					MiDSurveyPerson templatePerson = templatePersons.get(randomIndex);
					
					if(templatePerson == null){
						
						i--;
						continue;
						
					}
					
					Plan plan = factory.createPlan();
					
					Coord homeCoord = null;
					Coord workCoord = null;
					
					//shoot the activity coords inside the given geometries
					if(fromTransf.equals("GK4")){
						homeCoord = Global.gk4ToUTM32N.transform(shoot(from));
					} else{
						homeCoord = Global.ct.transform(shoot(from));
					}
					if(toTransf.equals("GK4")){
						workCoord = Global.gk4ToUTM32N.transform(shoot(to));
					} else{
						workCoord = Global.ct.transform(shoot(to));
					}
					
					if(fromId.length() < 8 && !fromId.contains("A")){
						
						Coord c = Global.UTM32NtoGK4.transform(homeCoord);
						Geometry nearestToHome = GAPScenarioBuilder.getBuiltAreaQT().get(c.getX(), c.getY());
						homeCoord = Global.gk4ToUTM32N.transform(shoot(nearestToHome));
						
					}
					
					if(toId.length() < 8 && !toId.contains("A")){
						
						Coord c = Global.UTM32NtoGK4.transform(workCoord);
						Geometry nearestToWork = GAPScenarioBuilder.getBuiltAreaQT().get(c.getX(), c.getY());
						workCoord = Global.gk4ToUTM32N.transform(shoot(nearestToWork));
						if(toId.startsWith("09180")){
							workCoord = GAPScenarioBuilder.getWorkLocations().get(workCoord.getX(), workCoord.getY()).getCoord();
						}
						
					}
					
					Activity actHome = factory.createActivityFromCoord("home", homeCoord);
					actHome.setStartTime(0.);
					
					Leg firstLeg = (Leg) templatePerson.getPlan().getPlanElements().get(0);
					
					double timeShift = 0.;
					
					if(firstLeg.getDepartureTime() != Time.UNDEFINED_TIME){

						do{

							timeShift = createRandomTimeShift(20);

							if(firstLeg.getDepartureTime() + timeShift > 0){
								
								actHome.setEndTime(firstLeg.getDepartureTime() + timeShift);
								
							} else{
								
								timeShift = 0;
								
							}
							
						} while(timeShift == 0);
						
					}
					
					plan.addActivity(actHome);
					
					int index = 1;
					
					for(PlanElement pe : templatePerson.getPlan().getPlanElements()){
						
						if(pe instanceof Activity){
							
							Activity act = (Activity)pe;
							
							String type = act.getType();
							double startTime = act.getStartTime();
							double endTime = act.getEndTime();
							
							Coord c = null;
							
							double distance = ((Leg)templatePerson.getPlan().getPlanElements().get(index - 2)).getRoute().getDistance();
							
							if(!carAvail || !hasLicense){
								
								distance = Math.min(distance, 6000);
								
							}
							
							Activity lastAct = (Activity)plan.getPlanElements().get(plan.getPlanElements().size() - 2);
							
							if(type.equals(Global.ActType.home.name())){
								
								c = homeCoord;
								
							} else if(type.equals(Global.ActType.other.name())){ //if the act type equals "other" or "work", shoot a random coordinate
								
								double rndX = Global.random.nextDouble();
								int signX = rndX >= 0.5 ? -1 : 1;
								double rndY = Global.random.nextDouble();
								int signY = rndY >= 0.5 ? -1: 1;
								
								double x = signX * Global.random.nextDouble() * distance;
								double y = signY * Math.sqrt(distance * distance - x * x);
								
								c = new Coord(lastAct.getCoord().getX() + x, lastAct.getCoord().getY() + y);
								
								c = Global.UTM32NtoGK4.transform(c);
								Geometry nearest = GAPScenarioBuilder.getBuiltAreaQT().get(c.getX(), c.getY());
								c = Global.gk4ToUTM32N.transform(shoot(nearest));
								
							} else if(type.equals(Global.ActType.work.name())){
								
								c = workCoord;
								
							}
							else{ //for all activities apart from "other" and "home", shoot a random coordinate and get the nearest activity facility
								
								ActivityFacility facility = null;
								
								do{
									
									double rndX = Global.random.nextDouble();
									int signX = rndX >= 0.5 ? -1 : 1;
									double rndY = Global.random.nextDouble();
									int signY = rndY >= 0.5 ? -1: 1;
									
									double x = signX * Global.random.nextDouble() * distance;
									double y = signY * Math.sqrt(distance * distance - x * x);
									
									if(type.equals(Global.ActType.education.name())){
										
										facility = GAPScenarioBuilder.getEducationQT().get(lastAct.getCoord().getX() + x, lastAct.getCoord().getY() + y);
										
									} else if(type.equals(Global.ActType.shop.name())){
										
										facility = GAPScenarioBuilder.getShopQT().get(lastAct.getCoord().getX() + x, lastAct.getCoord().getY() + y);
										
									} else if(type.equals(Global.ActType.leisure.name())){
										
										facility = GAPScenarioBuilder.getLeisureQT().get(lastAct.getCoord().getX() + x, lastAct.getCoord().getY() + y);
										
									}
									
								}while(facility == null);
								
								c = new Coord(facility.getCoord().getX(), facility.getCoord().getY());
								
							}
							
							//create a new activity at the position
							Activity newAct = factory.createActivityFromCoord(type, c);
							
							if(lastAct.getEndTime() > startTime + timeShift){
								
								plan.getPlanElements().remove(plan.getPlanElements().size() - 1);
								break;
								
							}
							
							newAct.setStartTime(startTime + timeShift);
							
							//the end time has either not been read correctly or there simply was none given in the mid survey file
							if(endTime <= 0. || endTime + timeShift <= 0){
								
								endTime = startTime + timeShift + 1800;
								
							}
							
							//acts must not have zero or negative duration, a minimum duration of 0.5 hours is assumed...
							if(endTime - startTime <= 0){
								
								timeShift += 1800;
								
								if(endTime + timeShift - newAct.getStartTime() <= 0){
									
									newAct.setEndTime(24 * 3600);
									plan.addActivity(newAct);
									break;
									
								}
								
							}
							
							newAct.setEndTime(endTime + timeShift);
							plan.addActivity(newAct);
							
						} else{
							
							Leg leg = (Leg)pe;
							
							
							plan.addLeg(factory.createLeg(leg.getMode()));
							
						}
						
						index++;
						
					}
					
					person.addPlan(plan);
					person.setSelectedPlan(plan);
					
					scenario.getPopulation().addPerson(person);
					
				}
				
			} else{
				
				log.warn("Could not find geometries for:" + fromId + " (" + fromName + "), " + toId + " (" + toName + ").");
				log.warn("Continuing with next relation...");
				
			}
			
	}
	
	private static void createPersonsFromPersonGroup(String munId, int a0, int aX, int amount, Scenario scenario, MiDPersonGroupData data){
		
		Population population = scenario.getPopulation();
		PopulationFactory factory = population.getFactory();
		
		for(int i = 0; i < amount; i++){
			
			double p1 = Global.random.nextDouble();
			double p2 = Global.random.nextDouble();
			double p3 = Global.random.nextDouble();
			
			boolean isEmployed = data.getpEmployment() <= p1 ? true : false;
			boolean hasLicense = data.getpLicense() <= p2 ? true : false;
			boolean carAvail = data.getpCarAvail() <= p3 ? true : false;
			
			if(aX < 18){
				isEmployed = false;
			}
			
			Person person = factory.createPerson(Id.createPersonId(munId + "_" + EgapHashGenerator.generateAgeGroupHash(a0, aX) + "_" + i));
			Plan plan = factory.createPlan();
			
			int age = a0 + Global.random.nextInt(aX - a0);
			demographicAttributes.putAttribute(person.getId().toString(), Global.AGE, age);
			
			if(munId.startsWith("09180")){
				
				if(hasLicense){
					
					if(carAvail){
						
						//car option attribute
						
					}
					
					//carsharing attribute
					
				}
				
			}
			
			double nAvgLegs = data.getLegsPerPersonAndDay();
			
			long nLegs = Math.round(Global.random.nextDouble() * nAvgLegs * 2);
			
			if(nLegs > 1){
				
				//home act
				Coord homeCoord = Global.gk4ToUTM32N.transform(shoot(GAPScenarioBuilder.getMunId2Geometry().get(munId)));
				Activity homeAct = factory.createActivityFromCoord("home", homeCoord);
				homeAct.setStartTime(0.);
				plan.addActivity(homeAct);
				
				Activity lastAct = homeAct;
				String legMode = null;
				
				StringBuffer sb = new StringBuffer();
				
				if(aX < 18){
					
					legMode = EgapPopulationUtilsV2.getLegModeForAgeGroupAndActType(age, hasLicense, carAvail, Global.ActType.education.name());
					double d = getTravelDistanceForMode(legMode);
					Coord c = createNewRandomCoord(lastAct.getCoord(), d);
					c = GAPScenarioBuilder.getEducationQT().get(c.getX(), c.getY()).getCoord();
					
					lastAct = factory.createActivityFromCoord(Global.ActType.education.name(), c);
					lastAct.setStartTime(9. * 3600);
					lastAct.setEndTime((14 + Global.random.nextInt(4))*3600);
					homeAct.setEndTime((5 + 3*Global.random.nextDouble())*3600);
					
					plan.addLeg(factory.createLeg(legMode));
					plan.addActivity(lastAct);
					
					nLegs--;
					
				}
				
				for(int k = 0; k < nLegs; k++){
					
					String purpose = data.getRandomPurpose(person, age, isEmployed, Global.random.nextDouble());
					legMode = EgapPopulationUtilsV2.getLegModeForAgeGroupAndActType(age, hasLicense, carAvail, purpose);
					double d = getTravelDistanceForMode(legMode);
					Coord c = createNewRandomCoord(lastAct.getCoord(), d);
					
					if(purpose.equals(Global.ActType.leisure.name())){
						
						c = GAPScenarioBuilder.getLeisureQT().get(c.getX(), c.getY()).getCoord();
						
					} else if(purpose.equals(Global.ActType.shop.name())){
						
						c = GAPScenarioBuilder.getShopQT().get(c.getX(), c.getY()).getCoord();
						
					} else if(purpose.equals(Global.ActType.education.name())){
						
						c = GAPScenarioBuilder.getEducationQT().get(c.getX(), c.getY()).getCoord();
						
					}
					
					sb.append("_" + legMode + "_" + purpose);
					
					plan.addLeg(factory.createLeg(legMode));
					
					if(lastAct.getEndTime() == Time.UNDEFINED_TIME){
						
						lastAct.setEndTime(lastAct.getStartTime() + 3600 * (1 + Global.random.nextDouble()));
						
					}
					
					double startTime = lastAct.getEndTime() + 3600;
					
					lastAct = factory.createActivityFromCoord(purpose, c);
					lastAct.setStartTime(startTime);
					
					plan.addActivity(lastAct);
					
				}
				
				plan.addLeg(factory.createLeg(legMode));
				
				//home act
				Activity homeAct2 = factory.createActivityFromCoord("home", homeCoord);
				homeAct2.setEndTime(24*3600);
				plan.addActivity(homeAct2);
				
				person.addPlan(plan);
				person.setSelectedPlan(plan);
				population.addPerson(person);
				
			}
			
		}
		
	}
	
	private static Coord shoot(Geometry geometry){
		
		Point point = null;
		double x, y;
		
		do{
			
			x = geometry.getEnvelopeInternal().getMinX() + Global.random.nextDouble() * (geometry.getEnvelopeInternal().getMaxX() - geometry.getEnvelopeInternal().getMinX());
	  	    y = geometry.getEnvelopeInternal().getMinY() + Global.random.nextDouble() * (geometry.getEnvelopeInternal().getMaxY() - geometry.getEnvelopeInternal().getMinY());
	  	    point = MGC.xy2Point(x, y);
			
		}while(!geometry.contains(point));
		
		return MGC.point2Coord(point);
		
	}
	
	private static double getTravelDistanceForMode(String mode){
		
		double mean = 0.;
		
		if(mode.equals(TransportMode.car)){
			
			mean = 14560;
			
		} else if(mode.equals(TransportMode.ride)){
			
			mean = 19091;
			
		} else if(mode.equals(TransportMode.walk)){
			
			mean = 1276;
			
		} else if(mode.equals(TransportMode.bike)){
			
			mean = 3786;
			
		} else if(mode.equals(TransportMode.pt)){
			
			mean = 21515;
			
		} else {
			
			log.error("Unknown mode!");
			
		}
		
		double rnd = Global.random.nextDouble();
		return (- mean * Math.log(rnd));
		
	}
	
	private static Coord createNewRandomCoord(Coord c, double d){
		
		double x = Global.random.nextDouble() * d;
		double y = Math.sqrt(d * d - x * x);
		
		int signX = Global.random.nextDouble() <= 0.5 ? 1 : -1;
		int signY = Global.random.nextDouble() <= 0.5 ? 1 : -1;
		
		return new Coord(c.getX() + signX * x, c.getY() + signY * y);
		
	}
	
	public static double createRandomTimeShift(double variance){
		
		//draw two random numbers [0;1] from uniform distribution
		double r1 = Global.random.nextDouble();
		double r2 = Global.random.nextDouble();
		
		//Box-Muller-Method in order to get a normally distributed variable
		double normal = Math.cos(2 * Math.PI * r1) * Math.sqrt(-2 * Math.log(r2));
		double endTime = variance * 3600 * normal;
		
		return endTime;
		
	}
	
}