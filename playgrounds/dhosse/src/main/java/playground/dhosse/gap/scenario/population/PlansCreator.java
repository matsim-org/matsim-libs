package playground.dhosse.gap.scenario.population;

import java.util.ArrayList;
import java.util.Collection;
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
import playground.dhosse.gap.scenario.population.utils.EgapPopulationUtils;
import playground.dhosse.utils.EgapHashGenerator;

public class PlansCreator {
	
	private static int inhabitantsCounter = 0;

	private static final Logger log = Logger.getLogger(PlansCreator.class);
	
	/**
	 * Creates initial plans with home-work-home journeys at the moment.
	 * dhosse, Aug 10
	 * 
	 * @param scenario The scenario containing the population to be created.
	 * @param commuterFilename The text (csv) file containing information about the commuters.
	 * @param reverseCommuterFilename
	 * @param equallyDistributedEndTimes Defines whether the activity end times should be distributed equally within a given interval or normally.
	 * @return The population
	 */
	public static Population createPlans(Scenario scenario, String commuterFilename, String reverseCommuterFilename){
		
		Population population = scenario.getPopulation();

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
		
		return population;
		
	}
	
	private static void createPersonsWithDemographicData(Scenario scenario, Map<String, CommuterDataElement> relations){
		
		MiDCSVReader reader = new MiDCSVReader();
//		reader.read(Global.matsimInputDir + "MID_Daten_mit_Wegeketten/travelsurvey_m.csv");
		Map<String, MiDSurveyPerson> persons = reader.getPersons();
		
		MiDPersonGroupTemplates templates = new MiDPersonGroupTemplates();
		
		int WA = 0;
		int WB = 0;
		int WE = 0;
		int WF = 0;
		int WS = 0;
		int AW = 0;
		int BW = 0;
		int EW = 0;
		int FW = 0;
		int SW = 0;
		int SA = 0;
		int AS = 0;
		int SS = 0;
		
		for(MiDSurveyPerson person : persons.values()){
			
			String lastActType = ((Activity)person.getPlan().getPlanElements().get(1)).getType().equals("home") ? "other" : "home";
			
			for(PlanElement pe : person.getPlan().getPlanElements()){
				if(pe instanceof Activity){
					String actType = ((Activity)pe).getType();
					
					if(lastActType.equals("home")){
						
						if(actType.equals("work")){
							WA++;
						} else if(actType.equals("education")){
							WB++;
						} else if(actType.equals("shop")){
							WE++;
						} else if(actType.equals("leisure")){
							WF++;
						}else{
							WS++;
						}
						
					} else if(lastActType.equals("work")){
						if(actType.equals("home")){
							AW++;
						} else{
							AS++;
						}
					} else if(lastActType.equals("education")){
						if(actType.equals("home")){
							BW++;
						} else {
							SS++;
						}
					} else if(lastActType.equals("shop")){
						if(actType.equals("home")){
							EW++;
						} else if(actType.equals("work")){
							SA++;
						}
						else{
							SS++;
						}
					} else if(lastActType.equals("leisure")){
						if(actType.equals("home")){
							FW++;
						} else if(actType.equals("work")){
							SA++;
						}
						else{
							SS++;
						}
					} else {
						if(actType.equals("home")){
							SW++;
						} else if(actType.equals("work")){
							SA++;
						}
						else{
							SS++;
						}
					}
					
					lastActType = actType; 
				}
			}
			
			templates.handlePerson(person);
			
		}
		
		System.out.println("WA:" + WA + "\tWB:" + WB + "\tWE:" + WE + "\tWF:" + WF + "\tWS:" + WS + "\tAW:" + AW + "\tBW:" + BW + "\tEW:" + EW + "\tFW:" + FW + "\tSW:" + SW + "\tAS:" + AS + "\tSA:" + SA + "\tSS:" + SS);
		
		Map<String, MiDPersonGroupData> personGroupData = EgapPopulationUtils.createMiDPersonGroups();
		
		for(Entry<String, Municipality> entry : Municipalities.getMunicipalities().entrySet()){
			
			createPersonsFromPersonGroup(entry.getKey(), 10, 18, 19, entry.getValue().getnStudents(), scenario, personGroupData, templates);
			
			int nCommuters = 0;
			List<String> keysToRemove = new ArrayList<>();
			
			for(String relation : relations.keySet()){
				
				String[] relationParts = relation.split("_");
				
				if(relation.startsWith(entry.getKey())){

					nCommuters += relations.get(relation).getCommuters();
					
					if(relationParts[1].startsWith("09180")){
						
						createCommutersFromKey(scenario, relations.get(relation), personGroupData, templates);
						keysToRemove.add(relation);
						
					}
					
				}
				
			}
			
			for(String s : keysToRemove){
				
				relations.remove(s);
				
			}
			
			createPersonsFromPersonGroup(entry.getKey(), 20, 65, 69, entry.getValue().getnAdults() - nCommuters, scenario, personGroupData, templates);
			createPersonsFromPersonGroup(entry.getKey(), 65, 89, 89, entry.getValue().getnPensioners(), scenario, personGroupData, templates);
			
		}
		
		createCommuters(scenario.getPopulation(), relations.values(), personGroupData);
		
	}
	
	/**
	 * This method creates an initial population of car commuters based on the survey by the Arbeitsagentur.
	 * The commuter data elements are used as templates.
	 * 
	 * @param population
	 * @param relations
	 */
	private static void createCommuters(Population population, Collection<CommuterDataElement> relations, Map<String, MiDPersonGroupData> groupData){

		PopulationFactoryImpl factory = (PopulationFactoryImpl) population.getFactory();
		
		//parse over commuter relations
		for(CommuterDataElement relation : relations){
			
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
//					agentAttributes.putAttribute(person.getId().toString(), Global.AGE, age);
					int sex = EgapPopulationUtils.setSex(age);
//					agentAttributes.putAttribute(person.getId().toString(), Global.SEX, sex);
					
					MiDPersonGroupData data = groupData.get(EgapHashGenerator.generatePersonGroupHash(age, sex));

					boolean hasLicense = setBooleanAttribute(person.getId().toString(), data.getpLicense(), Global.LICENSE);
					boolean carAvail = setBooleanAttribute(person.getId().toString(), data.getpCarAvail(), Global.CAR_AVAIL);
					
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
			Geometry nearestToHome = GAPScenarioBuilder.getBuiltAreaQT().getClosest(c.getX(), c.getY());
			homeCoord = Global.gk4ToUTM32N.transform(shoot(nearestToHome));
			
		}
		
		if(toId.length() < 8 && !toId.contains("A")){
			
			Coord c = Global.UTM32NtoGK4.transform(workCoord);
			Geometry nearestToWork = GAPScenarioBuilder.getBuiltAreaQT().getClosest(c.getX(), c.getY());
			workCoord = Global.gk4ToUTM32N.transform(shoot(nearestToWork));
			if(toId.startsWith("09180")){
				workCoord = GAPScenarioBuilder.getWorkLocations().getClosest(workCoord.getX(), workCoord.getY()).getCoord();
			}
			
		}
		
		Activity actHome = factory.createActivityFromCoord("home", homeCoord);
		actHome.setStartTime(0.);
		
		//create an activity end time (they can either be equally or normally distributed, depending on the boolean that
		//has been passed to the method
		double endTime = 0;
		
		do{

			endTime = 8*3600 + createRandomTimeShift();
			
		}while(endTime <= 0 && (endTime + 11 * 3600) > 24*3600);
		
		actHome.setEndTime(endTime);
		plan.addActivity(actHome);
		
		plan.addLeg(factory.createLeg(TransportMode.car));
		
		//create other activity and set the end time nine hours after the first activity's end time
		Activity actWork = factory.createActivityFromCoord("work", workCoord);
		actWork.setStartTime(actHome.getEndTime() + 3600);
		endTime = endTime + 9 * 3600;
		actWork.setEndTime(endTime);
		plan.addActivity(actWork);
		
		plan.addLeg(factory.createLeg(TransportMode.car));
		
		actHome = factory.createActivityFromCoord("home", homeCoord);
		actHome.setStartTime(endTime + 3600);
		plan.addActivity(actHome);
		
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		
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
					
					MiDPersonGroupData data = personGroupData.get(EgapHashGenerator.generatePersonGroupHash(age, sex));

					if(data == null){
						
						i--;
						continue;
						
					}
					
					boolean hasLicense = setBooleanAttribute(person.getId().toString(), data.getpLicense(), Global.LICENSE);
					boolean carAvail = setBooleanAttribute(person.getId().toString(), data.getpCarAvail(), Global.CAR_AVAIL);
					
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
						Geometry nearestToHome = GAPScenarioBuilder.getBuiltAreaQT().getClosest(c.getX(), c.getY());
						homeCoord = Global.gk4ToUTM32N.transform(shoot(nearestToHome));
						
					}
					
					if(toId.length() < 8 && !toId.contains("A")){
						
						Coord c = Global.UTM32NtoGK4.transform(workCoord);
						Geometry nearestToWork = GAPScenarioBuilder.getBuiltAreaQT().getClosest(c.getX(), c.getY());
						workCoord = Global.gk4ToUTM32N.transform(shoot(nearestToWork));
						if(toId.startsWith("09180")){
							workCoord = GAPScenarioBuilder.getWorkLocations().getClosest(workCoord.getX(), workCoord.getY()).getCoord();
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
								
//								do{
//								
//								Coord otherCoord = shoot(munId2Geometry.get("09180"));
//								Geometry nearestToWork = builtAreaQT.get(otherCoord.getX(), otherCoord.getY());
//								c = GAPMain.gk4ToUTM32N.transform(shoot(nearestToWork));
//								
//								}while (CoordUtils.calcDistance(lastAct.getCoord(), c) > distance);
								
								double rndX = Global.random.nextDouble();
								int signX = rndX >= 0.5 ? -1 : 1;
								double rndY = Global.random.nextDouble();
								int signY = rndY >= 0.5 ? -1: 1;
								
								double x = signX * Global.random.nextDouble() * distance;
								double y = signY * Math.sqrt(distance * distance - x * x);
								
								c = new Coord(lastAct.getCoord().getX() + x, lastAct.getCoord().getY() + y);
								
								c = Global.UTM32NtoGK4.transform(c);
								Geometry nearest = GAPScenarioBuilder.getBuiltAreaQT().getClosest(c.getX(), c.getY());
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
										
										facility = GAPScenarioBuilder.getEducationQT().getClosest(lastAct.getCoord().getX() + x, lastAct.getCoord().getY() + y);
										
									} else if(type.equals(Global.ActType.shop.name())){
										
										facility = GAPScenarioBuilder.getShopQT().getClosest(lastAct.getCoord().getX() + x, lastAct.getCoord().getY() + y);
										
									} else if(type.equals(Global.ActType.leisure.name())){
										
										facility = GAPScenarioBuilder.getLeisureQT().getClosest(lastAct.getCoord().getX() + x, lastAct.getCoord().getY() + y);
										
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
	
	/**
	 * 
	 * @param mName name of the municipality
	 * @param a0 lower bound of agents' age
	 * @param aX upper bound of agents' age
	 * @param aR for computation reasons, this is needed to access the MiD survey persons, since their age categories do not fit those used in the census
	 * @param amount number of persons to be created
	 * @param scenario
	 * @param personGroupData
	 * @param templates
	 */
	private static void createPersonsFromPersonGroup(String mName, int a0, int aX, int aR, int amount, Scenario scenario, Map<String, MiDPersonGroupData> personGroupData, MiDPersonGroupTemplates templates){
		
		PopulationFactoryImpl factory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		
		for(int i = 0; i < amount; i++){
			
			Person person = factory.createPerson(Id.createPersonId(mName + "_" + a0 + "_" + (aX) + "_" + i));
			Plan plan = factory.createPlan();
			
			int age = EgapPopulationUtils.setAge(a0, aR);
//			agentAttributes.putAttribute(person.getId().toString(), Global.AGE, Integer.toString(age));
			int sex = EgapPopulationUtils.setSex(age);
//			agentAttributes.putAttribute(person.getId().toString(), Global.SEX, Integer.toString(sex));
			
			MiDPersonGroupData data = personGroupData.get(EgapHashGenerator.generatePersonGroupHash(age, sex));
			
			if(data == null){
				
				i--;
				continue;
				
			}
			
			boolean isEmployed = false;
			boolean carAvail = setBooleanAttribute(person.getId().toString(), data.getpCarAvail(), Global.CAR_AVAIL);
			boolean hasLicense = setBooleanAttribute(person.getId().toString(), data.getpLicense(), Global.LICENSE);
			
			if(mName.startsWith("09180")){
				
				if(hasLicense){
					
					GAPScenarioBuilder.getSubpopulationAttributes().putAttribute(person.getId().toString(), Global.USER_GROUP, Global.LICENSE_OWNER);
					
					if(carAvail){
						
						GAPScenarioBuilder.getSubpopulationAttributes().putAttribute(person.getId().toString(), Global.USER_GROUP, Global.GP_CAR);
						
					}
					
				}
				
			} else {
				
				GAPScenarioBuilder.getSubpopulationAttributes().putAttribute(person.getId().toString(), Global.USER_GROUP, Global.COMMUTER);
				
			}
			
			String personHash = EgapHashGenerator.generatePersonHash(age, sex, carAvail, hasLicense, isEmployed);
			
			List<MiDSurveyPerson> templatePersons = templates.getPersonGroups().get(personHash);
			
			if(templatePersons != null){
				
				if(templatePersons.size() > 0){
					
					MiDSurveyPerson templatePerson = null;
					
					do{
						
						int randomIndex = (int)(Global.random.nextDouble() * templatePersons.size());
						templatePerson = templatePersons.get(randomIndex);
						
					} while (templatePerson == null);
					
					String firstAct = null;
					if(((Activity)templatePerson.getPlan().getPlanElements().get(1)).getType().equals(Global.ActType.home.name())){
						firstAct = Global.ActType.other.name();
					} else{
						firstAct = Global.ActType.home.name();
					}
					
					//create a home activity as starting point
					Coord homeCoord = Global.gk4ToUTM32N.transform(shoot(GAPScenarioBuilder.getMunId2Geometry().get(mName)));
					Coord firstCoord = Global.gk4ToUTM32N.transform(shoot(GAPScenarioBuilder.getMunId2Geometry().get(mName)));
					Activity firstActivity = factory.createActivityFromCoord(firstAct, firstCoord);
					firstActivity.setStartTime(0.);

					Leg firstLeg = (Leg) templatePerson.getPlan().getPlanElements().get(0);
					
					double timeShift = 0.;
					
					if(firstLeg.getDepartureTime() != Time.UNDEFINED_TIME){

						do{

							timeShift = createRandomTimeShift(20);

							if(firstLeg.getDepartureTime() + timeShift > 0){
								
								firstActivity.setEndTime(firstLeg.getDepartureTime() + timeShift);
								
							} else{
								
								timeShift = 0;
								
							}
							
						} while(timeShift == 0);
						
					}
					
					plan.addActivity(firstActivity);
					
					int index = 1;
					
					for(PlanElement pe : templatePerson.getPlan().getPlanElements()){
						
						if(pe instanceof Activity){
							
							Activity act = (Activity)pe;
							
							String type = act.getType();
							double startTime = act.getStartTime();
							double endTime = act.getEndTime();
							
							double distance = ((Leg)templatePerson.getPlan().getPlanElements().get(index - 2)).getRoute().getDistance();
							
							if(!carAvail || !hasLicense){
								
								distance = Math.min(distance, 6000);
								
							}
							
							Coord c = null;
							
							Activity lastAct = (Activity)plan.getPlanElements().get(plan.getPlanElements().size() - 2);
							
							if(type.equals(Global.ActType.home.name())){
								
								c = homeCoord;
								
							} else if(type.equals(Global.ActType.other.name()) || type.equals(Global.ActType.work.name())){ //if the act type equals "other" or "work", shoot a random coordinate
								
								double rndX = Global.random.nextDouble();
								int signX = rndX >= 0.5 ? 1 : -1;
								double rndY = Global.random.nextDouble();
								int signY = rndY >= 0.5 ? 1: -1;
								
								double x = signX * Global.random.nextDouble() * distance;
								double y = signY * Math.sqrt(distance * distance - x * x);
								
								c = new Coord(lastAct.getCoord().getX() + x, lastAct.getCoord().getY() + y);
								
								
								if(type.equals(Global.ActType.work.name())){
									
									c = GAPScenarioBuilder.getWorkLocations().getClosest(c.getX(), c.getY()).getCoord();
									
								} else{
									
									c = Global.UTM32NtoGK4.transform(c);
									Geometry nearest = GAPScenarioBuilder.getBuiltAreaQT().getClosest(c.getX(), c.getY());
									c = Global.gk4ToUTM32N.transform(shoot(nearest));
									
								}
								
							} else{ //for all activities apart from "other" and "home", shoot a random coordinate and get the nearest activity facility
								
								ActivityFacility facility = null;
								
								do{
									
									double rndX = Global.random.nextDouble();
									int signX = rndX >= 0.5 ? 1 : -1;
									double rndY = Global.random.nextDouble();
									int signY = rndY >= 0.5 ? 1: -1;
									
									double x = signX * Global.random.nextDouble() * distance;
									double y = signY * Math.sqrt(distance * distance - x * x);
									
									if(type.equals(Global.ActType.education.name())){
										
										facility = GAPScenarioBuilder.getEducationQT().getClosest(lastAct.getCoord().getX() + x, lastAct.getCoord().getY() + y);
										
									} else if(type.equals(Global.ActType.shop.name())){
										
										facility = GAPScenarioBuilder.getShopQT().getClosest(lastAct.getCoord().getX() + x, lastAct.getCoord().getY() + y);
										
									} else if(type.equals(Global.ActType.leisure.name())){
										
										facility = GAPScenarioBuilder.getLeisureQT().getClosest(lastAct.getCoord().getX() + x, lastAct.getCoord().getY() + y);
										
									}
									
								} while(facility == null);
								
								c = facility.getCoord();
								
							}
							
							//create a new activity at the position
							Activity newAct = factory.createActivityFromCoord(type, c);
							
							if(startTime + timeShift > 24 * 3600 || endTime + timeShift > 24*3600){
								
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
					
					scenario.getPopulation().addPerson(person);
					
				} else{
					
					i--;
					
				}
				
			} else{
				
				i--;
				
			}
			
		}
		
	}
	
	private static boolean setBooleanAttribute(String personId, double proba, String attribute){
		
		double random = Global.random.nextDouble();
		boolean attr = random <= proba ? true : false;
//		agentAttributes.putAttribute(personId, attribute, attr);
		
		return attr;
		
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
	
	private static double createRandomTimeShift(){
		
		//draw two random numbers [0;1] from uniform distribution
		double r1 = Global.random.nextDouble();
		double r2 = Global.random.nextDouble();
		
		//Box-Muller-Method in order to get a normally distributed variable
		double normal = Math.cos(2 * Math.PI * r1) * Math.sqrt(-2 * Math.log(r2));
		double endTime = 60*60 * normal;
		
		return endTime;
		
	}
	
	private static double createRandomTimeShift(double variance){
		
		//draw two random numbers [0;1] from uniform distribution
		double r1 = Global.random.nextDouble();
		double r2 = Global.random.nextDouble();
		
		//Box-Muller-Method in order to get a normally distributed variable
		double normal = Math.cos(2 * Math.PI * r1) * Math.sqrt(-2 * Math.log(r2));
		double endTime = variance*60 * normal;
		
		return endTime;
		
	}

	public static int getInhabitantsCounter() {
		return inhabitantsCounter;
	}

	public static void setInhabitantsCounter(int inhabitantsCounter) {
		PlansCreator.inhabitantsCounter = inhabitantsCounter;
	}
	
}
