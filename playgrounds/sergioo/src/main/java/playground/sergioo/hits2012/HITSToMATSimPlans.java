package playground.sergioo.hits2012;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.MatsimFacilitiesReader;

import playground.sergioo.hits2012.stages.CycleStage;
import playground.sergioo.hits2012.stages.MotorDriverStage;
import playground.sergioo.hits2012.stages.MotorStage;
import playground.sergioo.hits2012.stages.OtherBusStage;
import playground.sergioo.hits2012.stages.PublicBusStage;
import playground.sergioo.hits2012.stages.StationStage;
import playground.sergioo.hits2012.stages.TaxiStage;
import playground.sergioo.hits2012.stages.WaitStage;

public class HITSToMATSimPlans {

	private static final double MAX_DISTANCE = 1000;
	private static Collection<TripMode> tripModes = new ArrayList<>();
	
	private static class WorkActivities {
	
		private Map<String, double[]> MAP;	
		{
			MAP = new HashMap<>();
			MAP.put("w_0700_0845", new double[]{7*3600,8.75*3600});
			MAP.put("w_0730_1230", new double[]{7.5*3600,12.5*3600});
			MAP.put("w_0800_1000", new double[]{8*3600,10*3600});
			MAP.put("w_0830_0400", new double[]{8.5*3600,4*3600});
			MAP.put("w_0900_0845", new double[]{9*3600,8.75*3600});
			MAP.put("w_1000_1100", new double[]{10*3600,11*3600});
			MAP.put("w_1030_0630", new double[]{10.5*3600,6.5*3600});
			MAP.put("w_1345_0800", new double[]{13.75*3600,8*3600});
			MAP.put("w_1530_0300", new double[]{15.5*3600,3*3600});
			MAP.put("w_2000_1115", new double[]{20*3600,11.25*3600});
		}
	
	}
	private static class TripMode {
		private double distance;
		private Map<String, Integer> modes = new HashMap<>();
		private Map<String, Double> tTimes = new HashMap<>();
		private String getMainMode() {
			String longerM = "";
			double longer = 0;
			for(Entry<String, Double> tMode:tTimes.entrySet())
				if(tMode.getValue()>longer) {
					longerM = tMode.getKey();
					longer = tMode.getValue();
				}
			return longerM;
		}
	}
	
	public static void main(String[] args) throws NumberFormatException, IOException, ParseException {
		Map<String, Household> households = HitsReader.readHits(args[0]);
		CoordinateTransformation cT = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N);
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimFacilitiesReader(scenario).readFile(args[1]);
		Set<String> acts = new HashSet<>();
		for(ActivityFacility facility:scenario.getActivityFacilities().getFacilities().values())
			for(ActivityOption option:facility.getActivityOptions().values())
				acts.add(option.getType());
		for(String act:acts)
			System.out.println(act);
		PopulationFactory factory = scenario.getPopulation().getFactory();
		for(Household household:households.values()) {
			boolean allPersonGood = true;
			for(Person person:household.getPersons().values()) {
				Trip previousTrip = person.getTrips().get(person.getTrips().lastKey());
				boolean consistentLocation = true;
				if(previousTrip.getPurpose().equals(Trip.Purpose.HOME.text) != person.isStartHome())
					consistentLocation = false;
				Plan plan = factory.createPlan();
				for(Trip trip:person.getTrips().values()) {
					if(consistentLocation && !previousTrip.getEndPostalCode().equals(trip.getStartPostalCode()))
						if(CoordUtils.calcEuclideanDistance(cT.transform(Household.LOCATIONS.get(previousTrip.getEndPostalCode()).getCoord()), cT.transform(Household.LOCATIONS.get(trip.getStartPostalCode()).getCoord()))>MAX_DISTANCE)
							consistentLocation = false;
					if(consistentLocation) {
						ActivityFacility facility = scenario.getActivityFacilities().getFacilities().get(Id.create(Integer.parseInt(previousTrip.getEndPostalCode()), ActivityFacility.class));
						if(facility!=null) {
							TripMode tripMode = new TripMode();
							tripMode.distance = CoordUtils.calcEuclideanDistance(cT.transform(Household.LOCATIONS.get(trip.getStartPostalCode()).getCoord()), cT.transform(Household.LOCATIONS.get(trip.getEndPostalCode()).getCoord()));
							String activityT = getActivity(previousTrip.getPurpose(), getSeconds(previousTrip.getEndTime()), getSeconds(trip.getStartTime()), person.getEducation()); 
							Activity activity = factory.createActivityFromLinkId(activityT, facility.getLinkId());
							((ActivityImpl)activity).setFacilityId(facility.getId());
							activity.setStartTime(getSeconds(previousTrip.getEndTime()));
							activity.setEndTime(getSeconds(trip.getStartTime()));
							plan.addActivity(activity);
							if(trip.getMode().equals("Walk Only")){
								Integer num = tripMode.modes.get("walk");
								if(num==null)
									num=0;
								tripMode.modes.put("walk", num+1);
								Double time = tripMode.tTimes.get("walk");
								if(time==null)
									time=0.0;
								tripMode.tTimes.put("walk", time+getSeconds(trip.getEndTime())-getSeconds(trip.getStartTime()));
							}
							else
								for(Stage stage: trip.getStages().values())
									if(stage instanceof StationStage || stage instanceof PublicBusStage) {
										Integer num = tripMode.modes.get("pt");
										if(num==null)
											num=0;
										tripMode.modes.put("pt", num+1);
										Double time = tripMode.tTimes.get("pt");
										if(time==null)
											time=0.0;
										tripMode.tTimes.put("pt", time+60*(((Stage)stage).getWalkTime()+((Stage)stage).getInVehicleTime()+((Stage)stage).getLastWalkTime()+((WaitStage)stage).getWaitTime()));
									}
									else if(stage instanceof OtherBusStage) {
										Integer num = tripMode.modes.get("other");
										if(num==null)
											num=0;
										tripMode.modes.put("other", num+1);
										Double time = tripMode.tTimes.get("other");
										if(time==null)
											time=0.0;
										tripMode.tTimes.put("other", time+60*(((Stage)stage).getWalkTime()+((Stage)stage).getInVehicleTime()+((Stage)stage).getLastWalkTime()+((WaitStage)stage).getWaitTime()));
									}
									else if(stage instanceof CycleStage) {
										allPersonGood = false;
										Integer num = tripMode.modes.get("bike");
										if(num==null)
											num=0;
										tripMode.modes.put("bike", num+1);
										Double time = tripMode.tTimes.get("bike");
										if(time==null)
											time=0.0;
										tripMode.tTimes.put("bike", time+60*(((Stage)stage).getWalkTime()+((Stage)stage).getInVehicleTime()+((Stage)stage).getLastWalkTime()));
									}
									else if(stage instanceof TaxiStage) {
										Integer num = tripMode.modes.get("taxi");
										if(num==null)
											num=0;
										tripMode.modes.put("taxi", num+1);
										Double time = tripMode.tTimes.get("taxi");
										if(time==null)
											time=0.0;
										tripMode.tTimes.put("taxi", time+60*(((Stage)stage).getWalkTime()+((Stage)stage).getInVehicleTime()+((Stage)stage).getLastWalkTime()+((WaitStage)stage).getWaitTime()));
									}
									else if(stage instanceof MotorDriverStage) {
										Integer num = tripMode.modes.get("car");
										if(num==null)
											num=0;
										tripMode.modes.put("car", num+1);
										Double time = tripMode.tTimes.get("car");
										if(time==null)
											time=0.0;
										tripMode.tTimes.put("car", time+60*(((Stage)stage).getWalkTime()+((Stage)stage).getInVehicleTime()+((Stage)stage).getLastWalkTime()));
									}
									else if(stage instanceof MotorStage) {
										Integer num = tripMode.modes.get("pass");
										if(num==null)
											num=0;
										tripMode.modes.put("pass", num+1);
										Double time = tripMode.tTimes.get("pass");
										if(time==null)
											time=0.0;
										tripMode.tTimes.put("pass", time+60*(((Stage)stage).getWalkTime()+((Stage)stage).getInVehicleTime()+((Stage)stage).getLastWalkTime()));
									}
							String mode = tripMode.getMainMode();
							if(mode==null || mode.equals(""))
								allPersonGood = false;
							else
								plan.addLeg(factory.createLeg(mode));
							tripModes.add(tripMode);
						}
						else
							allPersonGood = false;
					}
					else
						allPersonGood = false;
					consistentLocation = true;
					previousTrip = trip;
				}
				Trip trip = person.getTrips().values().iterator().next();
				if(consistentLocation && !previousTrip.getEndPostalCode().equals(trip.getStartPostalCode()))
					if(CoordUtils.calcEuclideanDistance(cT.transform(Household.LOCATIONS.get(previousTrip.getEndPostalCode()).getCoord()), cT.transform(Household.LOCATIONS.get(trip.getStartPostalCode()).getCoord()))>MAX_DISTANCE)
						consistentLocation = false;
				if(consistentLocation) {
					ActivityFacility facility = scenario.getActivityFacilities().getFacilities().get(Id.create(Integer.parseInt(previousTrip.getEndPostalCode()), ActivityFacility.class));
					if(facility!=null) {
						Activity activity = factory.createActivityFromCoord(previousTrip.getPurpose(), facility.getCoord());
						activity.setStartTime(getSeconds(previousTrip.getEndTime()));
						activity.setEndTime(getSeconds(trip.getStartTime()));
						plan.addActivity(activity);
					}
					else
						allPersonGood = false;
				}
				else
					allPersonGood = false;
				if(allPersonGood) {
					org.matsim.api.core.v01.population.Person personM = factory.createPerson(Id.createPersonId(person.getId()));
					((ActivityImpl)plan.getPlanElements().get(0)).setStartTime(((Activity)plan.getPlanElements().get(0)).getStartTime()-24*3600);
					((ActivityImpl)plan.getPlanElements().get(plan.getPlanElements().size()-1)).setEndTime(((Activity)plan.getPlanElements().get(plan.getPlanElements().size()-1)).getEndTime()+24*3600);
					personM.addPlan(plan);
					scenario.getPopulation().addPerson(personM);
				}
			}
		}
		String[] modes = new String[]{"walk","pt","other","bike","taxi","car","pass"}; 
		Map<String, double[]> stats = new HashMap<>();
		for(String mode:modes)
			stats.put(mode, new double[4]);
		for(TripMode tripMode:tripModes) {
			int tot = 0;
			for(Integer modeT:tripMode.modes.values())
				tot+=modeT;
			for(Entry<String, Integer> modeT:tripMode.modes.entrySet()) {
				stats.get(modeT.getKey())[0] += tripMode.distance;
				stats.get(modeT.getKey())[1] += tripMode.distance*modeT.getValue()/tot;
				stats.get(modeT.getKey())[2] += modeT.getValue();
			}
			for(Entry<String, Double> modeT:tripMode.tTimes.entrySet()) {
				stats.get(modeT.getKey())[3] += modeT.getValue();
			}
		}
		for(String mode:modes) {
			for(Double num:stats.get(mode))
				System.out.print(num+"\t");
			System.out.println();
		}
		new PopulationWriter(scenario.getPopulation()).write(args[2]);
	}
	
	private static WorkActivities workActivities = new WorkActivities();
	
	private static String getActivity(String purpose, int begin, int end, String edu) {
		if(purpose.equals(Trip.Purpose.WORK.text)) {
			double min = Double.MAX_VALUE; 
			String minW = null;
			for(Entry<String, double[]> schedule:workActivities.MAP.entrySet()) {
				double dis = Math.hypot(begin-schedule.getValue()[0], end-begin-schedule.getValue()[1]);
				if(dis<min) {
					min = dis;
					minW = schedule.getKey();
				}
			}
			return minW;
		}
		else if(purpose.equals(Trip.Purpose.EDU.text)) {
			if(edu.equals("Primary") || edu.equals("Preschool"))
				return "primaryschool";
			else if(edu.equals("Private school") || edu.equals("International school") || edu.equals("Special education school"))
				return "foreignschool";
			else if(edu.equals("Secondary"))
				return "secondaryschool";
			else if(edu.equals("Post Secondary (JC/CI/ITE)") || edu.equals("Polytechnic") || edu.equals("University"))
				return "tertiaryschool";
		}
		else if(purpose.equals(Trip.Purpose.ERRANDS.text)||purpose.equals(Trip.Purpose.MEDICAL.text))
			return "personal";
		else if(purpose.equals(Trip.Purpose.WORK_FLEX.text)||purpose.equals(Trip.Purpose.DRIVE.text))
			return "biz";
		else if(purpose.equals(Trip.Purpose.EAT.text)||purpose.equals(Trip.Purpose.REC.text)||purpose.equals(Trip.Purpose.SOCIAL.text)||purpose.equals(Trip.Purpose.RELIGION.text))
			return "leisure";
		else if(purpose.equals(Trip.Purpose.SHOP.text))
			return "shop";
		else if(purpose.equals(Trip.Purpose.P_U_D_O.text))
			return "pudo";
		else if(purpose.equals(Trip.Purpose.HOME.text))
			return "home";
		return "";
	}
	private static int getSeconds(Date time) {
		Calendar cal = new GregorianCalendar();
		cal.setTime(time);
		return cal.get(Calendar.HOUR_OF_DAY)*3600+cal.get(Calendar.MINUTE)*60+cal.get(Calendar.SECOND);
	}
		

}
