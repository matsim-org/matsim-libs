package playground.sergioo.hits2012Scheduling.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.core.utils.collections.Tuple;

import playground.sergioo.hits2012.HitsReader;
import playground.sergioo.hits2012.Household;
import playground.sergioo.hits2012.Person;
import playground.sergioo.hits2012.Trip;
import playground.sergioo.hits2012.Trip.Purpose;

public class TestDurations {
	
	private static List<String> FLEX_ATIVITIES = new ArrayList<>(Arrays.asList(new String[]{Trip.Purpose.EAT.text,
			Trip.Purpose.ERRANDS.text, Trip.Purpose.REC.text, Trip.Purpose.SHOP.text, Trip.Purpose.SOCIAL.text}));

	public static void main(String[] args) throws NumberFormatException, IOException, ParseException {
		Map<String, Household> households = HitsReader.readHits(args[0]);
		PrintWriter writer = new PrintWriter("./data/hits/allvsactivity.txt");
		for(Household household:households.values())
			for(Person person:household.getPersons().values()) {
				List<Tuple<String, Tuple<Double, Double>>> previousActivities = new ArrayList<Tuple<String, Tuple<Double, Double>>>();
				List<Tuple<String, Tuple<Double, Double>>> followingActivities = new ArrayList<Tuple<String, Tuple<Double, Double>>>();
				Trip lastTrip = person.getTrips().get(person.getTrips().lastKey());
				int prevTime = 0;
				if(lastTrip.getPurpose().equals(Trip.Purpose.HOME.text) == person.isStartHome())
					prevTime = getSeconds(lastTrip.getEndTime())-24*3600;
				String prevAct = "";
				if(person.isStartHome())
					prevAct=Purpose.HOME.text;
				boolean previous = true, following = false;
				int beginTime = Integer.MAX_VALUE, endTime = Integer.MIN_VALUE;
				Set<String> acts = new HashSet<>(); 
				for(Trip trip:person.getTrips().values()) {
					if(previous && !prevAct.isEmpty()) {
						previousActivities.add(new Tuple<>(prevAct,new Tuple<>((double)getSeconds(trip.getStartTime()), (double)getSeconds(trip.getStartTime())-prevTime)));
						if(FLEX_ATIVITIES.contains(trip.getPurpose())) {
							previous = false;
							beginTime = getSeconds(trip.getStartTime());
						}
					}
					else if(following) {
						if(!prevAct.isEmpty())
							followingActivities.add(new Tuple<>(prevAct,new Tuple<>((double)getSeconds(trip.getStartTime()), (double)getSeconds(trip.getStartTime())-prevTime)));
					}
					else if(!previous && !FLEX_ATIVITIES.contains(trip.getPurpose())) {
						System.out.print(prevAct+"-");
						int duration = getSeconds(trip.getStartTime())-prevTime;
						if(duration>=0)
							acts.add(prevAct+","+duration+",");
						following = true;
						endTime = getSeconds(trip.getEndTime());
					}
					else if(!previous) {
						System.out.print(prevAct+"-");
						int duration = getSeconds(trip.getStartTime())-prevTime;
						if(duration>=0)
							acts.add(prevAct+","+duration+",");
					}
					prevAct = trip.getPurpose();
					int prevTimeP = getSeconds(trip.getEndTime());
					if(prevTimeP<prevTime)
						prevTimeP+=24*3600;
					prevTime = prevTimeP;
				}
				if(following) {
					System.out.println("]");
					int duration = endTime-beginTime;
					if(duration>=0)
						for(String act:acts)
							writer.println(act+(endTime-beginTime));
				}
			}
		writer.close();
	}
	private static int getSeconds(Date time) {
		Calendar cal = new GregorianCalendar();
		cal.setTime(time);
		return cal.get(Calendar.HOUR_OF_DAY)*3600+cal.get(Calendar.MINUTE)*60+cal.get(Calendar.SECOND);
	}

}
