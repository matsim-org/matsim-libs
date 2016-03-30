/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.jjoubert.projects.taxiInflation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.utilities.Header;

/**
 * Class to read and process the GPS waypoints as captured my Isaac Masilela.
 *   
 * @author jwjoubert
 */
public class GpsHandler {
	private final static Logger LOG = Logger.getLogger(GpsHandler.class);
	private final double DISTANCE_FACTOR = 1.35;
	private final double DEFAULT_CAPACITY = 15;
	private CoordinateTransformation ct;
	private List<Trip> trips = new ArrayList<GpsHandler.Trip>();
	private List<String> outputRecordsDemand = new ArrayList<String>();
	private List<String> outputRecordsSupply = new ArrayList<String>();
	private Map<String, List<Coord>> transactions = new TreeMap<String, List<Coord>>();
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(GpsHandler.class.toString(), args);

		String inputFile = args[0];
		String outputFileDemand = args[1];
		String outputFileSupply = args[2];
		String outputFileTransactions = args[3];
		String fromCRS = args[4];
		String toCRS = args[5];
		
		GpsHandler gh = new GpsHandler(fromCRS, toCRS);
		gh.parseInput(inputFile);
		gh.processTrips();
		gh.writeDemandOutputRecordsToFile(outputFileDemand);
		gh.writeSupplyOutputRecordsToFile(outputFileSupply);
		gh.writeTransactions(outputFileTransactions);
		
		LOG.info("Found " + gh.trips.size() + " trips.");
		Header.printFooter();
	}

	
	private void writeDemandOutputRecordsToFile(String outputFile) {
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		try{
			for(String s : this.outputRecordsDemand){
				bw.write(s);
				bw.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + outputFile);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + outputFile);
			}
		}
	}


	private void writeSupplyOutputRecordsToFile(String outputFile) {
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		try{
			for(String s : this.outputRecordsSupply){
				bw.write(s);
				bw.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + outputFile);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + outputFile);
			}
		}
	}
	
	private void writeTransactions(String outputFile){
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		try{
			bw.write("route,long,lat");
			bw.newLine();
			for(String s : transactions.keySet()){
				List<Coord> stops = transactions.get(s);
				for(Coord c : stops){
					bw.write(String.format("%s,%.8f,%.8f\n", s, c.getX(), c.getY()));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + outputFile);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + outputFile);
			}
		}
	}
	
	
	private void parseInput(String inputFile) {
		int numberOfLines = 0;
		BufferedReader br = IOUtils.getBufferedReader(inputFile);
		Trip currentTrip = new Trip();

		/* Assuming each new route starts with a String description of the
		 * route. */
		String tripName = null;
		
		try{
			String line = br.readLine(); /* Header line */
			numberOfLines++;

			while((line=br.readLine()) != null){
				numberOfLines++;
				String[] sa = line.split(",");
				if(sa.length != 11){
					/* It is a new trip. Check that the current trip is not empty. */
					if(currentTrip.records.size() > 0){
						currentTrip.setDescription(tripName);
						this.trips.add(currentTrip);
					} else{
						/* It probably is the first entry. Do nothing. */
					}
					
					/*FIXME This may be a problem... check if it overrides the Trips on the list. */
					currentTrip = new Trip();

					/* Update the trip description to the new name. */
					if(sa.length == 1){
						tripName = sa[0];
					}
				} else{
					/* Add to existing trip. */
					currentTrip.addRecord(sa);
					
					/* Just capture the location of the `transaction', 
					 * irrespective of whether commuter(s) are boarding or
					 * alighting. */
					double x = Double.parseDouble(sa[2]);
					double y = Double.parseDouble(sa[1]);
					Coord c = new Coord(x, y);
					List<Coord> list = transactions.get(tripName);
					if(list == null){
						list = new ArrayList<Coord>();
					}
					list.add(c);
					transactions.put(tripName, list);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot close " + inputFile);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + inputFile);
			}
		}
		
		/* In the end, add the final (remaining) Trip. */
		if(currentTrip.records.size() > 0){
			currentTrip.setDescription(tripName);
			this.trips.add(currentTrip);
		}
		
		LOG.info("Number of lines processed: " + numberOfLines);
	}
	
	private void processTrips(){
		for(Trip trip : this.trips){
			trip.processTrip();
			this.outputRecordsDemand.addAll(trip.outputLines);
			this.outputRecordsSupply.add(trip.outputLineTaxi);
		}
	}

	
	/**
	 * Constructor.
	 * @param fromCRS
	 * @param toCRS
	 */
	public GpsHandler(String fromCRS, String toCRS) {
		this.ct = TransformationFactory.getCoordinateTransformation(fromCRS, toCRS);
	}
	
	private class Trip{
		private double accumulatedDistance = 0.0;
		private List<Tuple<String[], Double>> records = new ArrayList<Tuple<String[], Double>>();
		private List<String> outputLines = new ArrayList<String>();
		private String outputLineTaxi;
		private Coord lastCoordinate = null;
		private String description = null;
		
		public Trip() {
		}
		
		public void setDescription(String tripName) {
			this.description = tripName;
		}

		private void addRecord(String[] sa){
			
			if(records.size() == 0){
				/* It is the first record. Set the origin coordinate*/
				double lon = Double.parseDouble(sa[2]);
				double lat = Double.parseDouble(sa[1]);
				Coord c = new Coord(lon, lat);
				lastCoordinate = ct.transform(c);
				
				records.add(new Tuple<String[], Double>(sa, new Double(0.0)));
			} else{
				double lon = Double.parseDouble(sa[2]);
				double lat = Double.parseDouble(sa[1]);
				Coord c = new Coord(lon, lat);
				Coord thisCoordinate = ct.transform(c);
				
				/* Convert distance (m) to distance (km). */
				double distance = CoordUtils.calcEuclideanDistance(lastCoordinate, thisCoordinate) / 1000;
				
				/* Add crow-fly-factor. */
				accumulatedDistance += distance * DISTANCE_FACTOR;
				
				records.add(new Tuple<String[], Double>(sa, new Double(accumulatedDistance)));
				lastCoordinate = thisCoordinate;
			}
		}
		
		/**
		 * Output records are of the following format: <code>type,peakClass,distance,costPerKilometer</code>
		 * <ul>
		 * 		<li><b>type</b> - (1) people boarding; (2) people alighting; (3) people switching from another taxi, and (4) people switching to another taxi;
		 * 		<li><b>peakClass</b> - (1) morning peak; (2) daytime off-peak; (3) afternoon peak; and (4) evening peak;
		 * 		<li><b>distance</b> - the estimated distance travelled by the person observed;
		 * 		<li><b>costPerKilometer</b> - the calculated cost per kilometer for the observed person. 
		 * </ul>
		 */
		private void processTrip(){
			LOG.info("Process trip...");
			
			/* Find the total trip distance. */
			double totalDistance = this.records.get(this.records.size()-1).getSecond();
			
			/* Find the total route cost. */
			String[] firstRecord = this.records.get(0).getFirst();
			double routeCost = Double.parseDouble(firstRecord[10]);
			
			/* Set up the vehicle occupancy data structure. */
			int numberOfPeopleAtStartOfTrip = Integer.parseInt(firstRecord[5]);
			double tripIncome = numberOfPeopleAtStartOfTrip*routeCost;
			double occupancy = numberOfPeopleAtStartOfTrip;
			double capacity = Math.max(occupancy, DEFAULT_CAPACITY);
			double accumulatedWeightedOccupancy = 0.0;
			double tripPortionStart = 0.0;
			
			int peakClass = 0;
			
			/* Process each record. */
			for(Tuple<String[], Double> tuple : this.records){
				String[] sa = tuple.getFirst();
				String timeString = sa[4];
				peakClass = getPeakClassFromTimeString(timeString);
				double accumulatedDistance = tuple.getSecond();
				
				/* TYPE 1:People getting on along the route are assumed to ride 
				 * until the end of the route. */
				int numberOfPeopleGettingOnAlongRoute = Integer.parseInt(sa[5]);
				if(numberOfPeopleGettingOnAlongRoute > 0){
					double distanceTravelled = totalDistance - accumulatedDistance;
					double uniqueCostPaidOnBoarding = Double.parseDouble(sa[10]); // This may be a `local fee'
					double costPerKilometer = uniqueCostPaidOnBoarding / distanceTravelled;
//					LOG.info("Getting on along the route: " + costPerKilometer);
					
					/* Add the observations. */
					String record = String.format("1,%d,%.2f,%.2f,%s", peakClass, distanceTravelled, costPerKilometer, this.description);
					for(int i = 0; i < numberOfPeopleGettingOnAlongRoute; i++){
						this.outputLines.add(record);
					}
					
					/* Account for the income. */
					tripIncome += numberOfPeopleGettingOnAlongRoute*uniqueCostPaidOnBoarding;
				}
				
				/* TYPE 2: People getting off along the route are assumed to 
				 * have joined at the start of the route. */
				int numberOfPeopleGettingOffAlongRoute = Integer.parseInt(sa[6]);
				if(numberOfPeopleGettingOffAlongRoute > 0){
					double distanceTravelled = accumulatedDistance;
					double costPerKilometer = routeCost / distanceTravelled;
//					LOG.info("Getting off along the route: " + costPerKilometer);
					
					/* Add the observations. */
					String record = String.format("2,%d,%.2f,%.2f,%s", peakClass, distanceTravelled, costPerKilometer, this.description);
					for(int i = 0; i < numberOfPeopleGettingOffAlongRoute; i++){
						this.outputLines.add(record);
					}
				}
				
				/* TYPE 3: People joining along the route, switching from another
				 * route. It is assumed that switching is `free' for the commuter, 
				 * and the distance is calculated as the entire journey, as they 
				 * are assumed to travel until the end. The cost the occupant 
				 * paid in the other taxi is assumed to be the same as in our 
				 * tracked taxi. */
				int numberOfPeopleSwitchingFromAnotherTaxi = Integer.parseInt(sa[7]);
				if(numberOfPeopleSwitchingFromAnotherTaxi > 0){
					double distanceTravelled = totalDistance;
					double costPerKilometer = routeCost / distanceTravelled;
//					LOG.info("Switching from another taxi: " + costPerKilometer);
					
					/* Add the observations. */
					String record = String.format("3,%d,%.2f,%.2f,%s", peakClass, distanceTravelled, costPerKilometer, this.description);
					for(int i = 0; i < numberOfPeopleSwitchingFromAnotherTaxi; i++){
						this.outputLines.add(record);
					}
					
					/* Account for income (not for commuter account), but settled
					 * among the drivers. 
					 * TODO Confirm what the real practice is in settling these
					 * amounts among the drivers. */
					tripIncome += numberOfPeopleSwitchingFromAnotherTaxi*routeCost;
				}
				
				/* TYPE 4: People leaving along the route, switching to another
				 * route. It is assumed that switching is `free', and the 
				 * distance is calculated as the entire journey. */
				int numberOfPeopleSwitchingToAnotherTaxi = Integer.parseInt(sa[8]);
				if(numberOfPeopleSwitchingToAnotherTaxi > 0){
					double distanceTravelled = totalDistance;
					double costPerKilometer = routeCost / distanceTravelled;
//					LOG.info("Switching to another taxi: " + costPerKilometer);
					
					/* Add the observations. */
					String record = String.format("4,%d,%.2f,%.2f,%s", peakClass, distanceTravelled, costPerKilometer, this.description);
					for(int i = 0; i < numberOfPeopleSwitchingToAnotherTaxi; i++){
						this.outputLines.add(record);
					}

					/* Account for income (not for commuter account), but settled
					 * among the drivers. 
					 * TODO Confirm what the real practice is in settling these
					 * amounts among the drivers. */
					tripIncome -= numberOfPeopleSwitchingToAnotherTaxi*routeCost;
				}
				
				/* Account for change in occupancy. */
				occupancy += (numberOfPeopleGettingOnAlongRoute + numberOfPeopleSwitchingFromAnotherTaxi - numberOfPeopleGettingOffAlongRoute - numberOfPeopleSwitchingToAnotherTaxi);
				capacity = Math.max(occupancy, capacity);
				double tripPortionDistance = accumulatedDistance - tripPortionStart;
				tripPortionStart = accumulatedDistance;
				/* Weigh the occupancy with the distance travelled. */
				accumulatedWeightedOccupancy += tripPortionDistance*occupancy;
			}
			
			/* Report the taxi's weighted occupancy. The output line contains 
			 * the following elements, an din this order:
			 * 1. peak class;
			 * 2. total distance;
			 * 3. weighted average occupancy (number of people);
			 * 4. weighted average occupancy (percentage of estimated capacity);
			 * 5. trip income;
			 * 6. trip description.
			 */
			outputLineTaxi = String.format("%d,%.2f,%.2f,%.2f,%.2f,%s", peakClass, totalDistance, accumulatedWeightedOccupancy / totalDistance, (accumulatedWeightedOccupancy / totalDistance)/capacity, tripIncome, this.description);
			LOG.info(" -->  " + outputLineTaxi);
			
		}
		
		/**
		 * Convert the time stamp from Basecamp, for example
		 * <br><br>
		 * <code>2013-09-11T13:34:22Z</code> <br><br>
		 * 
		 * to a predefined class of the 
		 * day, where:
		 * <ol>
		 * 		<li> morning peak, 05:00 - 09:00;
		 * 		<li> daytime off-peak, 09:00 - 15:00;
		 * 		<li> afternoon peak, 15:00 - 18:00; and
		 * 		<li> evening off-peak, 18:00 - 05:00
		 * </ol> 
		 * @param s
		 * @return
		 */
		private int getPeakClassFromTimeString(String s){
			int hour = Integer.parseInt(s.substring(11, 13));
			if(hour >= 5 && hour < 9){
				return 1;
			} else if(hour >= 9 && hour < 15){
				return 2;
			} else if(hour >= 15 && hour < 18){
				return 3;
			} else{
				return 4;
			}
		}

	
	}
	
	
}
