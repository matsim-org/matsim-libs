package playground.sergioo.hits2012Scheduling;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.sergioo.hits2012.HitsReader;
import playground.sergioo.hits2012.Household;
import playground.sergioo.hits2012.Location;
import playground.sergioo.hits2012.Person;
import playground.sergioo.hits2012.Trip;

public class DistanceDistribution {

	public static void main(String[] args) throws NumberFormatException, IOException, ParseException {
		Map<String, Household> households = HitsReader.readHits(args[0]);
		//Map<String, double[]> secondaryTrips = new HashMap<>();
		PrintWriter writer = new PrintWriter(new File("./data/cepas/distance.txt"));
		List<String> homeWork = Arrays.asList(Trip.Purpose.HOME.text, Trip.Purpose.WORK.text);
		List<String> specials = Arrays.asList(Trip.Purpose.WORK_FLEX.text, Trip.Purpose.P_U_D_O.text);
		for(Household household:households.values()) {
			Location homeLocation = household.getLocation();
			for(Person person:household.getPersons().values()) {
				Location workLocation = null;
				for(Trip trip:person.getTrips().values())
					if(workLocation==null && trip.getPurpose().equals(Trip.Purpose.WORK.text))
						workLocation = Household.LOCATIONS.get(trip.getEndPostalCode());
				if(workLocation!=null) {
					double homeWorkDistance = CoordUtils.calcEuclideanDistance(homeLocation.getCoord(), workLocation.getCoord());
					if(homeWorkDistance>0) {
						String[] homeWorkIndices = new String[person.getTrips().size()+1];
						if(person.isStartHome())
							homeWorkIndices[0] = Trip.Purpose.HOME.text;
						int n = 1;
						for(Trip trip:person.getTrips().values()) {
							homeWorkIndices[n] = trip.getPurpose();
							n++;
						}
						n = 1;
						Coord origin = null;
						for(Trip trip:person.getTrips().values()) {
							if(origin==null)
								origin = Household.LOCATIONS.get(trip.getStartPostalCode()).getCoord();
							Coord destination = Household.LOCATIONS.get(trip.getEndPostalCode()).getCoord();
							if(homeWorkIndices[n-1]!=null && !(specials.contains(homeWorkIndices[n-1]) || specials.contains(homeWorkIndices[n]))
									&& (homeWork.contains(homeWorkIndices[n-1]) && !homeWork.contains(homeWorkIndices[n]) ||
									homeWork.contains(homeWorkIndices[n]) && !homeWork.contains(homeWorkIndices[n-1]))) {
								Coord other = null;
								boolean isOrigin = true;
								String actT = "", actT2 = "";
								if(Trip.Purpose.HOME.text.equals(homeWorkIndices[n-1])) {
									other = workLocation.getCoord();
									isOrigin = false;
									actT = Trip.Purpose.HOME.text;
									actT2 = Trip.Purpose.WORK.text;
								}
								else if(Trip.Purpose.WORK.text.equals(homeWorkIndices[n-1])) {
									other = homeLocation.getCoord();
									isOrigin = false;
									actT = Trip.Purpose.WORK.text;
									actT2 = Trip.Purpose.HOME.text;
								}
								else if(Trip.Purpose.HOME.text.equals(homeWorkIndices[n])) {
									other = workLocation.getCoord();
									actT = Trip.Purpose.HOME.text;
									actT2 = Trip.Purpose.WORK.text;
								}
								else if(Trip.Purpose.WORK.text.equals(homeWorkIndices[n])) {
									other = homeLocation.getCoord();
									actT = Trip.Purpose.WORK.text;
									actT2 = Trip.Purpose.HOME.text;
								}
								String accc = "";
								if(CoordUtils.calcEuclideanDistance(origin, destination) < CoordUtils.calcEuclideanDistance(other, isOrigin?origin:destination))
									accc = actT;
								else
									accc = actT2;
								double distance = Math.min(CoordUtils.calcEuclideanDistance(origin, destination), CoordUtils.calcEuclideanDistance(other, isOrigin?origin:destination));
								writer.println(distance/homeWorkDistance+","+accc);
							}
							origin = destination;
							n++;
						}
					}
				}
			}
		}
		writer.close();
	}

}
