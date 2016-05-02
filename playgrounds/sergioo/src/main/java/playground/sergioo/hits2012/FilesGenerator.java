package playground.sergioo.hits2012;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.sergioo.hits2012.Person.Day;
import playground.sergioo.hits2012.stages.OtherBusStage;
import playground.sergioo.hits2012.stages.PublicBusStage;
import playground.sergioo.hits2012.stages.StationStage;
import playground.sergioo.hits2012.stages.WaitStage;

public class FilesGenerator {

	private enum TypeActivity implements Serializable {
		HOME,
		WORK,
		OTHER;
	}
	private static final String SEPARATOR = ",";
	
	private static class Activity implements Serializable {
		
		/**
		 * Serial version 
		 */
		private static final long serialVersionUID = 1L;
		
		private Day day;
		private double startTime;
		private double duration;
		/*private double[] location;*/
		private char[] stop;
		private TypeActivity type;
		private boolean isStudent;
		
		public Activity(Day day, double startTime, double duration, char[] stop/*double[] location*/, boolean isStudent) {
			super();
			this.day = day;
			this.startTime = startTime;
			this.duration = duration;
			this.stop = stop;
			this.isStudent = isStudent;
			//this.location = location;
		}
		public Activity(String activityLine) {
			String[] parts = activityLine.split(SEPARATOR);
			this.day = Day.values()[Integer.parseInt(parts[0])];
			this.startTime = Double.parseDouble(parts[1]);
			this.duration = Double.parseDouble(parts[2]);
			//this.location = new double[]{Double.parseDouble(parts[3]), Double.parseDouble(parts[4])};
			char[] stop = new char[parts[3].length()];
			for(int i=0; i<stop.length; i++)
				stop[i] = parts[3].charAt(i);
			this.stop = stop;
			this.isStudent = Boolean.parseBoolean(parts[4]);
			this.type = TypeActivity.values()[Integer.parseInt(parts[5])];
		}
		
		@Override
		public String toString() {
			String stopS = "";
			for(int i=0; i<stop.length; i++)
				stopS += stop[i];
			return day.ordinal()+SEPARATOR+startTime+SEPARATOR+duration+SEPARATOR+/*location[0]+SEPARATOR+location[1]*/stopS+SEPARATOR+isStudent+SEPARATOR+type.ordinal();
		}
		
	}
	
	private static final double MAX_DISTANCE = 1000;
	private static final double MIN_DURATION = 45*60;
	private static final double NEAR_DISTANCE = 200;
	private static final double VERY_NEAR_DISTANCE = 100;

	/**
	 * 
	 * @param 
	 * @throws NumberFormatException
	 * @throws IOException
	 * @throws ParseException
	 */
	public static void main(String[] args) throws NumberFormatException, IOException, ParseException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseTransit(true);
		new TransitScheduleReader(scenario).readFile("./data/cepas/transitScheduleT.xml");
		Map<String, Household> households = HitsReader.readHits(args[0]);
		Set<Coord> studyCoords = new HashSet<>();
		for(Household household:households.values())
			for(Person person:household.getPersons().values())
				for(Trip trip:person.getTrips().values())
					if(isStudentCard(person) && trip.getPurpose().equals(Trip.Purpose.EDU.text))
						studyCoords.add(Household.LOCATIONS.get(trip.getEndPostalCode()).getCoord());
		PrintWriter writerW = new PrintWriter(new File("./data/cepas/workPTActivitiesHits.txt"));
		PrintWriter writerS = new PrintWriter(new File("./data/cepas/studyPTActivitiesHits.txt"));
		writeHeaderW(writerW);
		writeHeaderS(writerS);
		int total = 0, nonconsDis = 0, noncons = 0, other = 0;
		int numWork = 0, numWorkR = 0, numStudy = 0, numStudyR = 0;
		CoordinateTransformation cT = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N);
		for(Household household:households.values())
			for(Person person:household.getPersons().values()) {
				Trip lastTrip = person.getTrips().get(person.getTrips().lastKey());
				Stage lastStage = null; 
				boolean prevModePT = false;
				if(lastTrip.getStages().size()>0){
					lastStage = lastTrip.getStage(lastTrip.getStages().lastKey());
					if(lastStage instanceof WaitStage && !(lastStage instanceof OtherBusStage))
						prevModePT = true;
				}
				boolean consistentLocation = true;
				if(lastTrip.getPurpose().equals(Trip.Purpose.HOME.text) != person.isStartHome())
					consistentLocation = false;
				for(Trip trip:person.getTrips().values()) {
					if(lastStage!=null && trip.getStages().size()>0) {
						boolean isStudent = isStudentCard(person);
						int prevTime = getSeconds(lastTrip.getEndTime());
						boolean modePT = false;
						Stage firstStage = trip.getStage(trip.getStages().firstKey());
						if(firstStage instanceof WaitStage && !(firstStage instanceof OtherBusStage))
							modePT = true;
						Coord lastCoord = cT.transform(Household.LOCATIONS.get(lastTrip.getEndPostalCode()).getCoord());
						if(consistentLocation && !lastTrip.getEndPostalCode().equals(trip.getStartPostalCode()))
							if(CoordUtils.calcEuclideanDistance(lastCoord, cT.transform(Household.LOCATIONS.get(trip.getStartPostalCode()).getCoord()))>MAX_DISTANCE) {
								consistentLocation = false;
								other++;
							}
						int choice=2;
						if(lastTrip.getPurpose().equals(Trip.Purpose.HOME.text))
							choice = 0;
						if((isStudent && lastTrip.getPurpose().equals(Trip.Purpose.EDU.text)) || (!isStudent && lastTrip.getPurpose().equals(Trip.Purpose.WORK.text)))
							choice = 1;
						if(prevModePT && modePT && consistentLocation) {
							double duration = getSeconds(trip.getStartTime())-prevTime;
							if(duration<0) {
								duration+=24*3600;
								if(choice!=0 && duration>18*3600)
									duration-=12*3600;
							}
							if(choice==1 && duration<=3600)
								duration+=12*3600;
							if(duration>MIN_DURATION) {
								if(isStudent) {
									Coord lcoord = lastCoord;
									if(lastStage instanceof StationStage) {
										TransitStopFacility station = scenario.getTransitSchedule().getFacilities().get(Id.create(((StationStage)lastStage).getEndStation(), TransitStopFacility.class));
										if(station!=null)
											lcoord = station.getCoord();
									}
									else {
										double minDistance = Double.MAX_VALUE;
										TransitLine line = scenario.getTransitSchedule().getTransitLines().get(Id.create(((PublicBusStage)lastStage).getLine(), TransitLine.class));
										if(line!=null) {
											for(TransitRoute route:line.getRoutes().values())
												for(TransitRouteStop stop:route.getStops()) {
													double distance = CoordUtils.calcEuclideanDistance(stop.getStopFacility().getCoord(), lastCoord);
													if(distance<minDistance) {
														minDistance = distance;
														lcoord = stop.getStopFacility().getCoord();
													}
												}
										}
										else
											System.out.println(((PublicBusStage)lastStage).getLine());
									}
									double minDistance = Double.MAX_VALUE;
									for(Coord coord:studyCoords) {
										double distance = CoordUtils.calcEuclideanDistance(cT.transform(coord), lcoord);
										if(distance<minDistance)
											minDistance = distance;
									}
									writerS.print(prevTime+"\t");
									writerS.print(duration+"\t");
									for(Day day:Day.values())
										if(!day.equals(Day.Sun))
											writerS.print((person.getSurveyDay().equals(day)?1:0)+"\t");
									writerS.print(minDistance+"\t");
									writerS.print((minDistance<NEAR_DISTANCE?1:0)+"\t");
									writerS.print((minDistance<VERY_NEAR_DISTANCE?1:0)+"\t");
									writerS.print(choice);
									writerS.println();
									numStudy++;
									if(choice==getTypeS(duration, prevTime).ordinal())
										numStudyR++;
									
								}
								else {
									writerW.print(prevTime+"\t");
									writerW.print(duration+"\t");
									for(Day day:Day.values())
										if(!day.equals(Day.Sun))
											writerW.print((person.getSurveyDay().equals(day)?1:0)+"\t");
									writerW.print(choice);
									writerW.println();
									numWork++;
									if(choice==getTypeW(duration, prevTime).ordinal())
										numWorkR++;
								}
							}
						}
						else {
							noncons++;
							if(!consistentLocation)
								nonconsDis++;
						}
					}
					total++;
					consistentLocation = true;
					lastTrip = trip;
					prevModePT = false;
					lastStage = null;
					if(lastTrip.getStages().size()>0) {
						lastStage = lastTrip.getStage(lastTrip.getStages().lastKey());
						if(lastStage instanceof WaitStage && !(lastStage instanceof OtherBusStage))
							prevModePT = true;
					}
				}
			}
		System.out.println(total+" "+noncons+" "+nonconsDis+" "+other);
		System.out.println(numStudyR+"/"+numStudy+", "+numWorkR+"/"+numWork);
		writerW.close();
		writerS.close();
	}
	private static boolean isStudentCard(Person person) {
		return person.getEmployment().equals("Full time student") &&
				(person.getAgeInterval().getUpperLimit()<20 ||
						(person.getAgeInterval().getUpperLimit()<25 && person.getEducation().equals("Post Secondary (JC/CI/ITE)")));
	}
	private static int getSeconds(Date time) {
		Calendar cal = new GregorianCalendar();
		cal.setTime(time);
		return cal.get(Calendar.HOUR_OF_DAY)*3600+cal.get(Calendar.MINUTE)*60+cal.get(Calendar.SECOND);
	}
	private static void writeHeaderW(PrintWriter printer) {
		printer.print("START_TIME\t");
		printer.print("DURATION\t");
		for(Day day:Day.values())
			if(!day.equals(Day.Sun))
				printer.print(day.name().toUpperCase()+"\t");
		printer.print("CHOICE");
		printer.println();
	}
	private static void writeHeaderS(PrintWriter printer) {
		printer.print("START_TIME\t");
		printer.print("DURATION\t");
		for(Day day:Day.values())
			if(!day.equals(Day.Sun))
				printer.print(day.name().toUpperCase()+"\t");
		printer.print("MIN_DISTANCE_STUDY\t");
		printer.print("NEAR_STUDY\t");
		printer.print("VERY_NEAR_STUDY\t");
		printer.print("CHOICE");
		printer.println();
	}
	public static void main3(String[] args) throws NumberFormatException, IOException, ParseException {
		Map<String, Household> households = HitsReader.readHits(args[0]);
		PrintWriter writer = new PrintWriter(new File("./data/hits/chains.txt"));
		for(Household household:households.values())
			for(Person person:household.getPersons().values()) {
				writer.print(household.getId()+","+person.getId()+",");
				if(person.isStartHome())
					writer.print(Trip.Purpose.HOME.text+",");
				else
					writer.print(person.getTrips().get(person.getTrips().lastKey()).getPurpose()+",");
				for(Trip trip:person.getTrips().values()) {
					writer.print(trip.getPurpose()+",");
				}
				writer.println();
			}
		writer.close();
	}
	public static void main2(String[] args) throws NumberFormatException, IOException, ParseException {
		Map<String, Household> households = HitsReader.readHits(args[0]);
		PrintWriter writer = new PrintWriter(new File("./data/cepas/activitiesHits.txt"));
		PrintWriter writer2 = new PrintWriter(new File("./data/cepas/ptActivitiesHits.txt"));
		writeHeaderW(writer);
		int total = 0, nonconsDis = 0, noncons = 0, other = 0;
		CoordinateTransformation cT = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N);
		for(Household household:households.values())
			for(Person person:household.getPersons().values()) {
				Trip lastTrip = person.getTrips().get(person.getTrips().lastKey());
				boolean prevModePT = false;
				for(Stage stage: lastTrip.getStages().values())
					if(stage instanceof WaitStage && !(stage instanceof OtherBusStage))
						prevModePT = true;
				boolean consistentLocation = true;
				if(lastTrip.getPurpose().equals(Trip.Purpose.HOME.text) != person.isStartHome())
					consistentLocation = false;
				for(Trip trip:person.getTrips().values()) {
					int prevTime = getSeconds(lastTrip.getEndTime());
					boolean modePT = false;
					for(Stage stage: trip.getStages().values())
						if(stage instanceof WaitStage && !(stage instanceof OtherBusStage))
							modePT = true;
					if(consistentLocation && !lastTrip.getEndPostalCode().equals(trip.getStartPostalCode()))
						if(CoordUtils.calcEuclideanDistance(cT.transform(Household.LOCATIONS.get(lastTrip.getEndPostalCode()).getCoord()), cT.transform(Household.LOCATIONS.get(trip.getStartPostalCode()).getCoord()))>MAX_DISTANCE) {
							consistentLocation = false;
							other++;
						}
					int choice=2;
					if(lastTrip.getPurpose().equals(Trip.Purpose.HOME.text))
						choice = 0;
					else if(lastTrip.getPurpose().equals(Trip.Purpose.WORK.text))
						choice = 1;
					if(prevModePT && modePT && consistentLocation) {
						double duration = getSeconds(trip.getStartTime())-prevTime;
						if(duration<0) {
							duration+=24*3600;
							if(choice!=0 && duration>18*3600)
								duration-=12*3600;
						}
						if(choice==1 && duration<=3600)
							duration+=12*3600;
						if(duration>MIN_DURATION) {
							writer.print(prevTime+"\t");
							writer.print(duration+"\t");
							for(Day day:Day.values())
								if(!day.equals(Day.Sun))
									writer.print((person.getSurveyDay().equals(day)?1:0)+"\t");
							writer.print(choice);
							writer.println();
						}
						if(choice==1)
							writer2.println("1\t"+Household.LOCATIONS.get(lastTrip.getEndPostalCode()).getCoord().getY()+"\t"+Household.LOCATIONS.get(lastTrip.getEndPostalCode()).getCoord().getX());
					}
					else {
						noncons++;
						if(!consistentLocation)
							nonconsDis++;
						else if(choice==1)
							writer2.println("0\t"+Household.LOCATIONS.get(lastTrip.getEndPostalCode()).getCoord().getY()+"\t"+Household.LOCATIONS.get(lastTrip.getEndPostalCode()).getCoord().getX());
					}
					total++;
					consistentLocation = true;
					lastTrip = trip;
					prevModePT = modePT;
				}
			}
		System.out.println(total+" "+noncons+" "+nonconsDis+" "+other);
		writer.close();
		writer2.close();
	}
	
	private static TypeActivity getTypeW(double duration, double startTime) {
		double D_H_1 = 0.884,
				D_W_1 = 0.685,
				K0 = -6.41,
				K1 = -2.79,
				K2 = 0,
				S_H_1 = 0.494,
				S_W_1 = -0.143;

		double D = duration / 3600;
		double D_5_15 = (D >= 5.0?1:0) * (D < 24.0?1:0) * (D - 5.0) + (D >= 24.0?1:0) * 19.0;
		double D_1_9 = (D >= 1.0?1:0) * (D < 9.5?1:0) * (D - 1.0) + (D >= 9.5?1:0) * 8.5;
		double S = startTime / 3600;
		double S_8_21 = (S >= 8.0?1:0) * (S < 21.5?1:0) * (S - 8.0) + (S >= 21.5?1:0) * 13.5;
		double S_7_20 = (S >= 7.0?1:0) * (S < 20.5?1:0) * (S - 7.0) + (S >= 20.5?1:0) * 13.5;


		double[] utilities = new double[TypeActivity.values().length];
		utilities[TypeActivity.HOME.ordinal()] = K0 + D_H_1 * D_5_15 + S_H_1 * S_8_21;
		utilities[TypeActivity.WORK.ordinal()] = K1 + D_W_1 * D_1_9 + S_W_1 * S_7_20;
		utilities[TypeActivity.OTHER.ordinal()] = K2;
		double[] probs = new double[TypeActivity.values().length];
		double den = 0, sumProbs = 0;
		for(double u:utilities)
			den += Math.exp(u);
		for(int i=0; i<utilities.length; i++) {
			probs[i] = Math.exp(utilities[i])/den;
			sumProbs += probs[i];
		}
		double ran = Math.random()*sumProbs, partial=0;
		int k=0;
		for(int i=0; i<probs.length; i++) {
			partial += probs[i];
			if(ran>partial)
				k++;
		}
		return TypeActivity.values()[k];
	}
	
	private static TypeActivity getTypeS(double duration, double startTime) {
		double D_H_1 = 0.722,
				D_W_1 = 1.25,
				D_W_2 = -1.97,
				K0 = -6.07,
				K1 = 1.7,
				K2 = 0,
				S_H_1 = 0.431,
				S_W_1 = -0.748;

		double D = duration / 3600;
		double D_1_16 = (D >= 1.5?1:0) * (D < 16.5?1:0) * (D - 1.5) + (D >= 16.5?1:0) * 15.0;
		double D_1_4 = (D >= 1.0?1:0) * (D < 4.5?1:0) * (D - 1.0) + (D >= 4.5?1:0) * 3.5;
		double D_7_8 = (D >= 7.5?1:0) * (D < 8.0?1:0) * (D - 7.5) + (D >= 8.0?1:0) * 0.5;
		double S = startTime / 3600;
		double S_8_24 = (S >= 8.0?1:0) * (S < 24.0?1:0) * (S - 8.0) + (S >= 24.0?1:0) * 16.0;
		double S_8_14 = (S >= 8.0?1:0) * (S < 14.5?1:0) * (S - 8.0) + (S >= 14.5?1:0) * 6.5;


		double[] utilities = new double[TypeActivity.values().length];
		utilities[TypeActivity.HOME.ordinal()] = K0 + D_H_1 * D_1_16 + S_H_1 * S_8_24;
		utilities[TypeActivity.WORK.ordinal()] = K1 + D_W_1 * D_1_4 + D_W_2 * D_7_8 + S_W_1 * S_8_14;
		utilities[TypeActivity.OTHER.ordinal()] = K2;
		double[] probs = new double[TypeActivity.values().length];
		double den = 0, sumProbs = 0;
		for(double u:utilities)
			den += Math.exp(u);
		for(int i=0; i<utilities.length; i++) {
			probs[i] = Math.exp(utilities[i])/den;
			sumProbs += probs[i];
		}
		double ran = Math.random()*sumProbs, partial=0;
		int k=0;
		for(int i=0; i<probs.length; i++) {
			partial += probs[i];
			if(ran>partial)
				k++;
		}
		return TypeActivity.values()[k];
	}

}
