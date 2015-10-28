package playground.sergioo.hits2012;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.sergioo.hits2012.Person.Day;
import playground.sergioo.hits2012.stages.WaitStage;

public class FilesGenerator {

	private static final double MAX_DISTANCE = 1000;
	private static final double MIN_DURATION = 45*60;

	/**
	 * 
	 * @param args[0] day of the week, "all" for 
	 * @throws NumberFormatException
	 * @throws IOException
	 * @throws ParseException
	 */
	public static void main(String[] args) throws NumberFormatException, IOException, ParseException {
		Map<String, Household> households = HitsReader.readHits(args[0], args[1]);
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
					if(stage instanceof WaitStage)
						prevModePT = true;
				boolean consistentLocation = true;
				if(lastTrip.getPurpose().equals(Trip.Purpose.HOME.text) != person.isStartHome())
					consistentLocation = false;
				for(Trip trip:person.getTrips().values()) {
					int prevTime = getSeconds(lastTrip.getEndTime());
					boolean modePT = false;
					for(Stage stage: trip.getStages().values())
						if(stage instanceof WaitStage)
							modePT = true;
					if(consistentLocation && !lastTrip.getEndPostalCode().equals(trip.getStartPostalCode()))
						if(CoordUtils.calcDistance(cT.transform(Household.LOCATIONS.get(lastTrip.getEndPostalCode()).getCoord()), cT.transform(Household.LOCATIONS.get(trip.getStartPostalCode()).getCoord()))>MAX_DISTANCE) {
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

}
