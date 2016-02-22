package playground.sergioo.cepasWeek2015;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;

public class CepasWeekReader {
	
	private enum Day implements Serializable {
		MONDAY,
		TUESDAY,
		WEDNESDAY,
		THURSDAY,
		FRIDAY,
		SATURDAY,
		SUNDAY;
	}
	private enum TypeActivity implements Serializable {
		HOME,
		WORK,
		OTHER;
	}
	private static String toString(char[] array) {
		String string = "";
		for(int i=0; i<array.length; i++)
			string += array[i];
		return string;
	}
	private static class Trip implements Serializable {
		
		/**
		 * Serial version
		 */
		private static final long serialVersionUID = 1L;
		
		private Day day;
		private double startTime;
		private double duration;
		private char[] startStop;
		private char[] endStop;
		/*private double[] startLocation;
		private double[] endLocation;*/
		private int transfer;
		public Trip(Day day, double startTime, double duration,
				/*double[] startLocation, double[] endLocation*/String startStopS, String endStopS, int transfer) {
			super();
			this.day = day;
			this.startTime = startTime;
			this.duration = duration;
			char[] startStop = new char[startStopS.length()];
			for(int i=0; i<startStop.length; i++)
				startStop[i] = startStopS.charAt(i);
			this.startStop = startStop;
			char[] endStop = new char[endStopS.length()];
			for(int i=0; i<endStop.length; i++)
				endStop[i] = endStopS.charAt(i);
			this.endStop = endStop;
			/*this.startLocation = startLocation;
			this.endLocation = endLocation;*/
			this.transfer = transfer;
		}
		public Trip(String tripLine) {
			String[] parts = tripLine.split(SEPARATOR);
			this.day = Day.values()[Integer.parseInt(parts[0])];
			this.startTime = Double.parseDouble(parts[1]);
			this.duration = Double.parseDouble(parts[2]);
			char[] startStop = new char[parts[3].length()];
			for(int i=0; i<startStop.length; i++)
				startStop[i] = parts[3].charAt(i);
			this.startStop = startStop;
			char[] endStop = new char[parts[4].length()];
			for(int i=0; i<endStop.length; i++)
				endStop[i] = parts[4].charAt(i);
			this.endStop = endStop;
			/*this.startLocation = new double[]{Double.parseDouble(parts[3]), Double.parseDouble(parts[4])};
			this.endLocation = new double[]{Double.parseDouble(parts[5]), Double.parseDouble(parts[6])};*/
			this.transfer = Integer.parseInt(parts[/*7*/5]);
		}
		
		@Override
		public String toString() {
			String startStopS = "";
			for(int i=0; i<startStop.length; i++)
				startStopS += startStop[i];
			String endStopS = "";
			for(int i=0; i<endStop.length; i++)
				endStopS += endStop[i];
			return day.ordinal()+SEPARATOR+startTime+SEPARATOR+duration+SEPARATOR+/*startLocation[0]+SEPARATOR+startLocation[1]+SEPARATOR+endLocation[0]+SEPARATOR+endLocation[1]*/startStopS+SEPARATOR+endStopS+SEPARATOR+transfer;
		}
		
	}
	
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
		
		public Activity(Day day, double startTime, double duration, char[] stop/*double[] location*/) {
			super();
			this.day = day;
			this.startTime = startTime;
			this.duration = duration;
			this.stop = stop;
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
			this.type = TypeActivity.values()[Integer.parseInt(parts[/*5*/4])];
		}
		
		@Override
		public String toString() {
			String stopS = "";
			for(int i=0; i<stop.length; i++)
				stopS += stop[i];
			return day.ordinal()+SEPARATOR+startTime+SEPARATOR+duration+SEPARATOR+/*location[0]+SEPARATOR+location[1]*/stopS+SEPARATOR+type.ordinal();
		}
		
	}
	
	private static final File STOPS_FILE = new File("./stops.dat");
	private static final File TRIPS_FILE = new File("./tripsCEPAS.txt");
	private static final File ACTIVITIES_FILE = new File("./activitiesCEPAS.txt");
	private static final File VECTORS_FILE = new File("./vectorsCEPAS.txt");
	private static final String SEPARATOR = ",";
	private static final double MAX_DISTANCE = 1000;
	private static final double DAY_IN_SECONDS = 24*3600;
	private static final double HOUR_IN_SECONDS = 3600;
	private static final double MINUTE_IN_SECONDS = 60;
	
	private static Map<String, double[]> stops = new HashMap<>();
	private static Map<String, List<Trip>> trips = new HashMap<>();
	private static Map<String, List<Activity>> activities = new HashMap<>();

	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException, NoConnectionException {
		//if(!ACTIVITIES_FILE.exists()) {
			DataBaseAdmin dataBaseAux  = new DataBaseAdmin(new File("./input/dBProperties/spatialDB.properties"));
			if(!STOPS_FILE.exists()) {
				String stopsTable = "m_calibration.spatial_transit_stops";
				ResultSet stopsRes = dataBaseAux.executeQuery("SELECT * FROM "+stopsTable);
				while(stopsRes.next()) {
					String id=stopsRes.getString("stop_id");
					boolean isBusStop = true;
					try {
						Integer.parseInt(id);
					} catch(NumberFormatException e) {
						isBusStop = false;
					}
					double[] coord = new double[]{stopsRes.getDouble("stop_lon"), stopsRes.getDouble("stop_lat")};
					if(isBusStop)
						stops.put(id, coord);
					else
						stops.put("STN "+stopsRes.getString("stop_name"), coord);
				}
				stopsRes.close();
				ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(STOPS_FILE));
				oos.writeObject(stops);
				oos.close();
			} else {
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(STOPS_FILE));
				stops = (Map<String, double[]>) ois.readObject();
				ois.close();
			}
			System.out.println("Stops ready");
			if(!TRIPS_FILE.exists()) {
				String[] tables = new String[]{"a_lta_ezlink_week.trips11042011",
											   /*"a_lta_ezlink_week.trips12042011",
											   "a_lta_ezlink_week.trips13042011",
											   "a_lta_ezlink_week.trips14042011",
											   "a_lta_ezlink_week.trips15042011",
											   "a_lta_ezlink_week.trips16042011",
											   "a_lta_ezlink_week.trips17042011"*/};
				Set<String> noStops = new HashSet<>();
				Calendar cal = new GregorianCalendar();
				for(int i=0; i<1;) {
					ResultSet dailyRes = dataBaseAux.executeQuery("SELECT card_id,ride_start_time,ride_time,boarding_stop_stn,alighting_stop_stn,transfer_number FROM "+tables[i]+" ORDER BY card_id, ride_start_time");
					System.out.println(++i);
					String currentCard = null;
					List<Trip> currentCardTrips = null;
					int n=0;
					while(dailyRes.next()) {
						String card = dailyRes.getString("card_id");
						if(!card.equals(currentCard)) {
							currentCardTrips = trips.get(card);
							if(currentCardTrips==null) {
								currentCardTrips = new ArrayList<>();
								trips.put(card, currentCardTrips);
							}
							currentCard = card;
						}
						cal.setTime(new Date(dailyRes.getTime("ride_start_time").getTime()));
						double secs = cal.get(Calendar.HOUR_OF_DAY)*HOUR_IN_SECONDS+cal.get(Calendar.MINUTE)*MINUTE_IN_SECONDS+cal.get(Calendar.SECOND);
						if(stops.get(dailyRes.getString("boarding_stop_stn"))==null)
							noStops.add(dailyRes.getString("boarding_stop_stn"));
						else if(stops.get(dailyRes.getString("alighting_stop_stn"))==null)
							noStops.add(dailyRes.getString("alighting_stop_stn"));
						else
							currentCardTrips.add(new Trip(Day.values()[i-1], secs, dailyRes.getDouble("ride_time"), dailyRes.getString("boarding_stop_stn"), dailyRes.getString("alighting_stop_stn"), /*stops.get(dailyRes.getString("boarding_stop_stn")), stops.get(dailyRes.getString("alighting_stop_stn")), */dailyRes.getInt("transfer_number")));
						n++;
					}
					System.out.println(n+" "+trips.size());
				}
				/*for(String noStop:noStops)
					System.out.println(noStop);*/
				PrintWriter writer = new PrintWriter(TRIPS_FILE);
				for(Entry<String, List<Trip>> cardTrips:trips.entrySet()) {
					writer.println(cardTrips.getKey());
					writer.println(cardTrips.getValue().size());
					for(Trip trip:cardTrips.getValue())
						writer.println(trip.toString());
				}
				writer.close();
				/*ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(TRIPS_FILE));
				oos.writeObject(trips);
				oos.close();*/
			} else {
				BufferedReader reader = new BufferedReader(new FileReader(TRIPS_FILE));
				String line = reader.readLine();
				while(line!=null) {
					String key = line;
					int numTrips = Integer.parseInt(reader.readLine());
					List<Trip> cardTrips = trips.get(key);
					if(cardTrips==null)
						cardTrips = new ArrayList<>();
					for(int i=0; i<numTrips; i++)
						cardTrips.add(new Trip(reader.readLine()));
					trips.put(key, cardTrips);
					line = reader.readLine();
				}
				reader.close();
				/*ObjectInputStream ois = new ObjectInputStream(new FileInputStream(TRIPS_FILE));
				trips = (Map<String, List<Trip>>) ois.readObject();
				ois.close();*/
			}
			System.out.println("Trips ready");
			int[] tran = new int[3600*27];
			for(List<Trip> cardTrips:trips.values())
				for(Trip trip:cardTrips) {
					tran[(int) trip.startTime]++;
					tran[(int) (trip.startTime+trip.duration*MINUTE_IN_SECONDS)]--;
				}
			int[] sum = new int[3600*27];
			for(int i=1; i<3600*27; i++)
				sum[i] = sum[i-1]+tran[i];
			PrintWriter prin = new PrintWriter("./data/numberPT.txt");
			for(int s:sum)
				prin.println(s);
			prin.close();
			/*int max=0;
			for(List<Trip> cardTrips:trips.values()) {
				int numTrips = 0;
				for(Trip trip:cardTrips)
					if(trip.transfer==0)
						numTrips++;
				if(numTrips>max)
					max = numTrips;
			}
			int[] nums = new int[max+1];
			for(Entry<String, List<Trip>> cardTrips:trips.entrySet()) {
				int numTrips = 0;
				for(Trip trip:cardTrips.getValue())
					if(trip.transfer==0)
						numTrips++;
				nums[numTrips]++;
			}*/
			CoordinateTransformation cT = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N);
			for(Entry<String, List<Trip>> cardTripsE:trips.entrySet()) {
				List<Trip> cardTrips = cardTripsE.getValue();
				if(cardTrips.size()>0) {
					Trip lastTrip = cardTrips.get(cardTrips.size()-1);
					Day day = lastTrip.day;
					double prevTime = (lastTrip.startTime + lastTrip.duration*MINUTE_IN_SECONDS) - (Day.values().length - day.ordinal())*DAY_IN_SECONDS;
					List<Activity> activitiesList = new ArrayList<>();
					for(Trip trip:cardTrips) {
						double time = trip.day.ordinal()*DAY_IN_SECONDS + trip.startTime;
						Coord locA = new Coord(stops.get(toString(lastTrip.endStop))[0], stops.get(toString(lastTrip.endStop))[1]);
						Coord locB = new Coord(stops.get(toString(trip.startStop))[0], stops.get(toString(trip.startStop))[1]);
						double distance = CoordUtils.calcEuclideanDistance(cT.transform(locA), cT.transform(locB));
						if(trip.transfer==0 && distance<MAX_DISTANCE) {
							activitiesList.add(new Activity(day, lastTrip.startTime + lastTrip.duration*MINUTE_IN_SECONDS, time-prevTime, /*lastTrip.endLocation*/lastTrip.endStop));
						}
						day = trip.day;
						prevTime = time + trip.duration*MINUTE_IN_SECONDS;
						lastTrip = trip;
					}
					activities.put(cardTripsE.getKey(), activitiesList);
					cardTrips.clear();
				}
			}
			trips.clear();
			setActivityTypes();
			PrintWriter writer = new PrintWriter(ACTIVITIES_FILE);
			for(Entry<String, List<Activity>> cardActivities:activities.entrySet()) {
				writer.println(cardActivities.getKey());
				writer.println(cardActivities.getValue().size());
				for(Activity activity:cardActivities.getValue())
					writer.println(activity.toString());
			}
			writer.close();
		//} else {
			BufferedReader reader = new BufferedReader(new FileReader(ACTIVITIES_FILE));
			String line = reader.readLine();
			while(line!=null) {
				String key = line;
				int numActivities = Integer.parseInt(reader.readLine());
				List<Activity> cardActivities = new ArrayList<>();
				for(int i=0; i<numActivities; i++)
					cardActivities.add(new Activity(reader.readLine()));
				activities.put(key, cardActivities);
				line = reader.readLine();
			}
			reader.close();
			/*ObjectInputStream ois = new ObjectInputStream(new FileInputStream(TRIPS_FILE));
			trips = (Map<String, List<Trip>>) ois.readObject();
			ois.close();*/
		//}
		System.out.println("Activities ready");
		Collection<double[]> vectors = new ArrayList<>();
		for(List<Activity> activitiesList:activities.values()) {
			double[] vector = new double[2*Day.values().length];
			for(Day day:Day.values())
				vector[2*day.ordinal()] = -DAY_IN_SECONDS;
			Day day = null; 
			for(Activity activity:activitiesList) {
				if(activity.type==TypeActivity.WORK) {
					if(activity.day!=day) {
						day = activity.day;
						vector[2*day.ordinal()] = activity.startTime;
						vector[2*day.ordinal()+1] = activity.duration;
					} else
						vector[2*day.ordinal()+1] += activity.duration;
				}
			}
			vectors.add(vector);
			activitiesList.clear();
		}
		activities.clear();
		/*PrintWriter */writer = new PrintWriter(VECTORS_FILE);
		for(double[] vector:vectors) {
			for(double value:vector)
				writer.print(value+SEPARATOR);
			writer.println();
		}
		writer.close();
		System.out.println("Vectors ready");
	}

	private static void setActivityTypes() {
		for(List<Activity> activitiesList:activities.values())
			for(Activity activity:activitiesList)
				setType(activity);
	}

	private static void setType(Activity activity) {
		double D_H_A = -14.6;
		double D_H_B = 0.271;
		double D_H_C = -3.06;
		double D_W_A = 0.152;
		double D_W_B = -0.594;
		double D_W_C = 1.34;
		double D_W_D = -0.445;
		double K0 = 13.1;
		double K1 = -40.3;
		double K2 = 0;
		double S_H_A = -1.15;
		double S_H_B = 1.17;
		double S_H_C = -1.79;
		double S_H_D = 2.43;
		double S_W_A = -1.6;
		double S_W_B = 2;
		double S_W_C = -0.0092;
		
		double one = 1;
		double D_SC = activity.duration / 3600;
		double D_L_4 = (D_SC < 4.5?1:0) * D_SC + (D_SC >= 4.5?1:0) * 4.5;
		double D_G_4_L_6 = (D_SC >= 4.5?1:0) * (D_SC < 6.5?1:0) * (D_SC - 4.5) + (D_SC >= 6.5?1:0) * 2;
		double D_G_6_L_9 = (D_SC >= 6.5?1:0) * (D_SC < 9.5?1:0) * (D_SC - 6.5) + (D_SC >= 9.5?1:0) * 3;
		double D_G_15 = (D_SC >= 15?1:0) * (D_SC - 15);
		double D_2_L_4 = D_L_4 * D_L_4;
		double S_SC = activity.startTime / 3600;
		double S_L_2 = (S_SC < 2.5?1:0) * S_SC;
		double S_G_2_L_7 = (S_SC >= 2.5?1:0) * (S_SC < 7?1:0) * S_SC + (S_SC >= 7?1:0) * 7;
		double S_L_7 = (S_SC < 7.5?1:0) * S_SC + (S_SC >= 7.5?1:0) * 7.5;
		double S_G_7_L_8 = (S_SC >= 7?1:0) * (S_SC < 8?1:0) * (S_SC - 7) + (S_SC >= 8?1:0) * 1;
		double S_G_7_L_11 = (S_SC >= 7.5?1:0) * (S_SC < 11.5?1:0) * (S_SC - 7.5) + (S_SC >= 11.5?1:0) * 4;
		double S_G_8 = (S_SC >= 8?1:0) * (S_SC - 8);
		double S_G_11_L_12 = (S_SC >= 11.5?1:0) * (S_SC < 12.5?1:0) * (S_SC - 11.5) + (S_SC >= 12.5?1:0) * 1;
		double S_G_12 = (S_SC < 12.5?1:0) * 1 + (S_SC >= 12.5?1:0) * (S_SC - 12.5 + 1);
		double S_2_G_2_L_7 = S_G_2_L_7 * S_G_2_L_7;
		double S_2_G_8 = S_G_8 * S_G_8;
		double S_E_L_7 = -2 * (5.5 - 2.5) * S_L_2 + S_2_G_2_L_7 - 2 * 5.5 * S_G_2_L_7 + (2.5 * 2.5);
		double S_E_G_8 = S_2_G_8 - 2 * 2 * S_G_8;
		
		double[] utilities = new double[TypeActivity.values().length];
		utilities[TypeActivity.HOME.ordinal()] = K0 * one + S_H_A * S_L_7 + S_H_B * S_G_7_L_11 + S_H_C * S_G_11_L_12 + ( D_H_A / ( 1 + Math.pow(Math.E, ( D_H_B * D_SC ) + D_H_C ) ) ) + ( S_H_D * Math.log(S_G_12 ) );
		utilities[TypeActivity.WORK.ordinal()] = K1 * one + D_W_A * D_2_L_4 + D_W_B * D_G_4_L_6 + D_W_C * D_G_6_L_9 + D_W_D * D_G_15 + S_W_A * S_E_L_7 + S_W_B * S_G_7_L_8 + S_W_C * S_E_G_8;
		utilities[TypeActivity.OTHER.ordinal()] = K2 * one;
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
		activity.type = TypeActivity.values()[k];
	}

}
