/* *********************************************************************** *
 * project: org.matsim.*
 * Wardrop.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.christoph.analysis.wardrop;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;

public class Wardrop {

	private static final Logger log = Logger.getLogger(Wardrop.class);
	
	//protected NetworkLayer network;
	protected Network network;
	protected Population population;
	
	protected WardropZones wardropZones;
	protected ActTimesCollector actTimesCollector;
	
	protected boolean useLengthCorrection = false;
	
	// Trips[from Zone][to Zone]
	// Zones are numbered from the top left to the bottom right
	protected Trips[][] zoneMatrix;
		
	// parameters for getResults()
	int timeStep = 60 * 60;	// [sec]
	int minTrips = 5;
	double startTime = 3600*4; // [sec]
	double endTime = 3600*36; // [sec]
	double minCellDistance = 30000.0; // [m]
	
	public Wardrop(Network network, Population population)
	{
		this.network = network;
		this.population = population;
		
		this.wardropZones = new CircularWardropZones(network);
		this.wardropZones.createMapping();
	}
	
	public void createZoneMatrix()
	{
		// create new ZoneMatrix
		int zones = wardropZones.getZonesCount();
		
//		log.info("Zone size: " + (xMax - xMin)/zonesX + "x" + (yMax - yMin)/zonesY);
		
		zoneMatrix = new Trips[zones][zones];
		
		for(int i = 0; i < zones; i++)
		{
			for(int j = 0; j < zones; j++)
			{
				zoneMatrix[i][j] = new Trips();
			}
//			if (i % 100 == 0) log.info("Created " + i + "/" + zonesX*zonesY + " rows of the Zone Matrix");
		}
	}
	
	public void resetZoneMatrix()
	{
		if (zoneMatrix != null)
		{		
			int zones = wardropZones.getZonesCount();
		
			for(int i = 0; i < zones; i++)
			{
				for(int j = 0; j < zones; j++)
				{
					zoneMatrix[i][j].reset();
				}
			}
		}
		else
		{
			createZoneMatrix();
		}
	}
	
	public void fillMatrixFromEventFile(String file)
	{
		log.info("Resetting Zone Matrix");
		resetZoneMatrix();

		log.info("Processing Events");
		processEvents(file);
		
		log.info("Getting Trips from Events");
		getTripsFromEvents();
		
	}
	
	public void fillMatrixViaActTimesCollectorObject(ActTimesCollector actTimesCollector)
	{
		this.actTimesCollector = actTimesCollector;
		fillMatrixViaActTimesCollectorObject();
	}
	
	public void fillMatrixViaActTimesCollectorObject()
	{
		log.info("Resetting Zone Matrix");
		resetZoneMatrix();
		
		log.info("Getting Trips from Events");
		getTripsFromEvents();
	}
	
	public void fillMatrixFromPlans()
	{		
		for (Person person : population.getPersons().values()) 
		{
			Plan plan = person.getSelectedPlan();
			
			ArrayList<Activity> acts = new ArrayList<Activity>();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					acts.add((Activity) pe);
				}
			}
			
			// The last Act is only the end of a leg and not the beginning of a new one!
			for(int i = 0; i < acts.size() - 1; i++)
			{
				Activity startAct = acts.get(i);
				Activity endAct = acts.get(i + 1);
				
				Coord startCoord = startAct.getCoord();
				Coord endCoord = endAct.getCoord();
				
				double startTime = startAct.getEndTime();
				double endTime = endAct.getStartTime();
				
				List<Integer> startZones = wardropZones.getZones(startCoord);
				List<Integer> endZones = wardropZones.getZones(endCoord);
				
				for (int startZone : startZones)
				{
					for (int endZone : endZones)
					{
						Trips trip = zoneMatrix[startZone][endZone];
						trip.addTrip(startTime, endTime, startCoord, endCoord);
					}
				}
				
				/*
				int startZone = getZone(startCoord);
				int endZone = getZone(endCoord);
				
				Trips trip = zoneMatrix[startZone][endZone];

				trip.addTrip(startTime, endTime, startCoord, endCoord);
				*/		
			}
			
		}
	}
	
	// gets all ActStart- and ActEndEvents from a given Eventsfile
	protected void processEvents(String file)
	{
		// Instance which takes over line by line of the events file
		// and throws events of added types
		EventsManagerImpl events = new EventsManagerImpl();
		
		// An example of an events handler which takes
		// "LinkLeaveEvents" to calculate total volumes per link of the network
		actTimesCollector = new ActTimesCollector();
		
		actTimesCollector.setNetwork(this.network);
		actTimesCollector.setPopulation(this.population);
		
		actTimesCollector.setStartTime(startTime);
		actTimesCollector.setEndTime(endTime);
		
		// register the handler to the events object
		events.addHandler(actTimesCollector);
		
		// Reader to read events line by line and passes it over to the events object
		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
//		reader.readFile("C:\\events.txt.gz");
		reader.readFile(file);
	
	}	// processEvents	
	
	
	// gets all Trips from a given ActTimesCollector
	protected void getTripsFromEvents()
	{
		double sum = 0.0;
		
		int correct = 0;
		int tripcounter = 0;
		int wrong = 0;
		
		if (actTimesCollector != null)
		{
			for(Person person:this.population.getPersons().values())
			{
				Id id = person.getId();
				ArrayList<Double> startTimes = (ArrayList<Double>) actTimesCollector.getStartTimes(id);
				ArrayList<Double> endTimes = (ArrayList<Double>) actTimesCollector.getEndTimes(id);
				ArrayList<Coord> startCoords = (ArrayList<Coord>) actTimesCollector.getStartCoords(id);
				ArrayList<Coord> endCoords = (ArrayList<Coord>) actTimesCollector.getEndCoords(id);

				// trying to correct "wrong" data
				// looks like the person has started a trip but didn't finish it so delete start time
				if (endTimes.size() == startTimes.size() + 1)
				{
					// remove last item in List
					endTimes.remove(endTimes.size()-1);
				}
					
				
				if (endTimes.size() != startTimes.size())
				{
					wrong++;
//					log.warn("Found more ending Activities than starting ones!");
				}
				else
				{
					correct++;
					for(int i = 0; i < startTimes.size(); i++)
					{
						List<Integer> startZones = wardropZones.getZones(startCoords.get(i));
						List<Integer> endZones = wardropZones.getZones(endCoords.get(i));
						
						for (int startZone : startZones)
						{
							for (int endZone : endZones)
							{
								tripcounter++;
								
								Trips trips = zoneMatrix[startZone][endZone];

								double tripDuration = startTimes.get(i) - endTimes.get(i);
								
								sum = sum + tripDuration;
								
								// without length correction
								if (!useLengthCorrection)
								{
									trips.addTrip(endTimes.get(i), startTimes.get(i), endCoords.get(i), startCoords.get(i));
								}

								// with correction
								else
								{
									double correctedDuration = distanceCorrection(endCoords.get(i), startCoords.get(i), endZone, startZone, tripDuration);
									trips.addTrip(endTimes.get(i), endTimes.get(i) + correctedDuration, endCoords.get(i), startCoords.get(i));
								}
							}
						}
/*						
						tripcounter++;
											
						int startZone = getZone(startCoords.get(i));
						int endZone = getZone(endCoords.get(i));
						
						Trips trips = zoneMatrix[startZone][endZone];

						double tripDuration = startTimes.get(i) - endTimes.get(i);
						
						sum = sum + tripDuration;
						
						// without length correction
						if (!useLengthCorrection)
						{
							trips.addTrip(endTimes.get(i), startTimes.get(i), endCoords.get(i), startCoords.get(i));
						}

						// with correction
						else
						{
							double correctedDuration = distanceCorrection(endCoords.get(i), startCoords.get(i), endZone, startZone, tripDuration);
							trips.addTrip(endTimes.get(i), endTimes.get(i) + correctedDuration, endCoords.get(i), startCoords.get(i));
						}
*/						
					}	// for int i
				}	// if correct
			}	// for every Person

			log.info("Found " + Trip.getTripCounter() + " Trips");
		
			log.info("Mean Plan Duration: " + sum / correct );
			log.info("Mean Trip Duration: " + sum / tripcounter );
			
			log.info("Correct: " + correct);
			log.info("Wrong: " + wrong);
			
//			log.info("Zone length in x direction: " + (xMax - xMin) / zonesX);
//			log.info("Zone length in y direction: " + (yMax - yMin) / zonesY);
			
		}	// if (actTimesCollector != null
		
		else
		{
			log.warn("ActTimesCollector Object was not initialized!");
		}
		
	}	// getTrips
	
	protected double distanceCorrection(Coord start, Coord end, int startZone, int endZone, double tripDuration)
	{	
		if (startZone == endZone) return tripDuration;
		
		Coord startCentre = wardropZones.getZoneCentre(startZone);
		Coord endCentre = wardropZones.getZoneCentre(endZone);
		//Coord startCentre = getZoneCentre(startZone);
		//Coord endCentre = getZoneCentre(endZone);
		
		// distance between the centres of the traffic cells
		double referenceLength = ((CoordImpl)startCentre).calcDistance(endCentre);
	
		// shortest distance between start and endpoint of a trip
		double length = ((CoordImpl)start).calcDistance(end); 
		
		//return (tripDuration * length / referenceLength);
		return (tripDuration * referenceLength / length);
		
		
	} // distanceCorrection
	
	public void getResults()
	{	
		// format the output strings
		NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
		nf.setMaximumFractionDigits(1);
		nf.setGroupingUsed(false);
		
		NumberFormat nf2 = NumberFormat.getInstance(Locale.ENGLISH);
		nf2.setMaximumFractionDigits(0);
		nf2.setGroupingUsed(false);
		
		/*
		 * Write some information about the used parameters...
		 */
		log.info("Time Step Length: " + Time.writeTime(timeStep));
		log.info("Minimum number of Trips between to Traffic Cells: " + minTrips);
		log.info("Minimum Disctance between two Traffic Cells: " + minCellDistance);
		log.info("Start Time of Analysis: " + Time.writeTime(startTime));
		log.info("End Time of Analysis: " + Time.writeTime(endTime));
//		log.info("Number of Zones in X Direction: " + zonesX);
//		log.info("Number of Zones in Y Direction: " + zonesY);
		
		if (useLengthCorrection) log.info("Use Trip Length Correction");
		else log.info("Don't use Trip Length Correction");
		
		
		int validCombinations = 0;
		int sumTrips = 0;
		double sumStandardDeviation = 0.0;
		
		for (double time = startTime; time < endTime; time = time + timeStep)
		{
			for(int i = 0; i < zoneMatrix.length; i++)
			{
				for(int j = 0; j < zoneMatrix.length; j++)
				{
					Trips trips = zoneMatrix[i][j];
					
					boolean calcTripResults = true;
					
					/*
					 * If it's traffic within one cell, there is no need to calc a wardrop equilibrium.
					 */
					if (i == j)
					{
						calcTripResults = false;
					}
					
					/*
					 *  Check if the total sum of trips is higher than the minimum number within a time step.
					 *  If not - skip it!
					 */
					if (calcTripResults && trips.getIds().size() < minTrips)
					{
						calcTripResults = false;
					}
					
					/*
					 *  Check if the distance between the centres of the current cells is big enough.
					 *  If not - skip it!
					 */
					//double cellDistance = ((CoordImpl)getZoneCentre(i)).calcDistance(getZoneCentre(j));
					double cellDistance = ((CoordImpl)wardropZones.getZoneCentre(i)).calcDistance(wardropZones.getZoneCentre(j));
					if (calcTripResults &&  cellDistance < minCellDistance)
					{
						calcTripResults = false;
					}
					
					if (calcTripResults)
					{
						//double[] results = zoneMatrix[i][j].calcMedianTripDuration(time, time + timeStep);
						double[] results = calcMedianTripDuration(trips, time, time + timeStep);
						
						// Needed amount of trips between two cells
						if (results[2] > minTrips) 
						{
							String outString = "Found " + nf2.format(results[2]) + " trips";
							outString = outString + " from Zone " + i;
							outString = outString + " to Zone " + j;
							outString = outString + " (middle distance " + nf.format(cellDistance) + ")";
							outString = outString + ", mean is " + nf.format(results[0]) + " (100.0)";
							outString = outString + ", standard deviation is " + nf.format(results[1]);
							outString = outString + " (" + nf.format(results[1]/results[0]*100) + ")";
							outString = outString + ", starting time from " + Time.writeTime(time);
							outString = outString + " to " + Time.writeTime(time + timeStep);
							
							log.info(outString);
							//log.info("Found " + results[2] + " trips, mean is " + results[0] + ", standard deviation is " + results[1]);
							validCombinations++;
							
							sumTrips = sumTrips + (int)results[2]; //Integer.valueOf(String.valueOf(results[2]));
							
							sumStandardDeviation = sumStandardDeviation + results[1] / results[0] * 100;
						}
					}
				}
			}
		}

		log.info("Found " + validCombinations + " valid Combinations!");
		log.info("Mean Normalized Number of Trips: " + nf.format(sumTrips * 1.0 / validCombinations));
		log.info("Mean Normalized Standard Deviation: " + nf.format(sumStandardDeviation / validCombinations) );
		
	}	// getResults()
	
	/*
	 * Start- and endTime specify the time, within a Trip has to start to be used for the calculation.
	 * 
	 * double[0] = mean
	 * double[1] = standard deviation
	 * double[2] = number of trips within the specified time
	 */
	public double[] calcMedianTripDuration(Trips trips, double startTime, double endTime)
	{
		List<Double> startTimes = trips.getStartTimes();
		List<Double> endTimes = trips.getEndTimes();
		
		double[] results = new double[3];
		
		// set default values
		results[0] = 0.0;
		results[1] = 0.0;
		results[2] = 0.0;
		
		double mean;
		double standarddeviation;
		
		ArrayList<Double> durations = new ArrayList<Double>();
		
		/*
		 *  Check if the startTimes has been initialized.
		 *  If there are no trips stored, it's not to save memory!
		 */
		if (startTimes != null)
		{
			for(int i = 0; i < startTimes.size(); i++)
			{
				// if trip started within the given time slot (means that the activity ended within the given time slot)
				//if (endTimes.get(i) > startTime && endTimes.get(i) < endTime)
				if (startTimes.get(i) >= startTime && startTimes.get(i) < endTime)
				{
					//counter++;
					//durations = durations + ( endTimes.get(i) - startTimes.get(i) );
					//durations.add(startTimes.get(i) - endTimes.get(i));
					durations.add(endTimes.get(i) - startTimes.get(i));
				}
			}
		}

		results[2] = durations.size();
		
		//if (counter == 0) return 0.0;
		if (durations.size() == 0) return results;
		if (durations.size() == 1)
		{
			results[0] = durations.get(0);
			return results;
		}
		
		double sum = 0.0;
		for (Double duration : durations) 
		{
			sum = sum + duration;
		}
		
		mean = sum / durations.size();
		results[0] = mean;
		
		double diffsum = 0.0;
		for(Double duration : durations)
		{
			diffsum = diffsum + Math.pow((duration - mean), 2); 
		}
		
		standarddeviation = Math.sqrt((1.0 / (durations.size() - 1)) * diffsum);
		results[1] = standarddeviation;
		
		//return durations / counter;
		return results;
	}

	public double calcMeanLinksPerTrip()
	{
		int linkCounter = 0;
		int legCounter = 0;
		int PersonCounter = 0;
		
		for (Person person : population.getPersons().values()) 
		{
			Plan plan = person.getSelectedPlan();
						
			//ArrayList<BasicLegImpl> legs = new ArrayList<BasicLegImpl>();

			for (PlanElement planElement : plan.getPlanElements())
			{
				if (planElement instanceof Leg)
				{
					Leg leg = (Leg) planElement;
					
					if (leg.getRoute() instanceof NetworkRoute)
					{
						//BasicRouteImpl route = (BasicRouteImpl) leg.getRoute();
						NetworkRoute route = (NetworkRoute) leg.getRoute();
						linkCounter = linkCounter + route.getLinkIds().size();
						legCounter++;
					}
					else
					{
						log.error("Expected Route to be from Class NetworkRoute but found Class: " + leg.getRoute().getClass());
					}
				}
			}

			PersonCounter++;
		}
		
		// format the output strings
		NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
		nf.setMaximumFractionDigits(1);
		nf.setGroupingUsed(false);
		
		NumberFormat nf2 = NumberFormat.getInstance(Locale.ENGLISH);
		nf2.setMaximumFractionDigits(0);
		nf2.setGroupingUsed(false);
		
		log.info("Found " + linkCounter + " Trips!");
		log.info("Mean Links per Leg: " + nf.format(linkCounter * 1.0 / legCounter) );
		log.info("Mean Links per Person: " + nf.format(linkCounter * 1.0 / PersonCounter) );
		log.info("Mean Legs per Person: " + nf.format(legCounter * 1.0 / PersonCounter) );
		
		return linkCounter * 1.0 / legCounter;
	}
	
	/*
	 * Internal data structure with information about a trip between two Activities.
	 */
	static class Trip
	{
		static long TripIdCounter = 0;
		
		double startTime;
		double endTime;
		Coord startCoord;
		Coord endCoord;
		long id;
		
		public Trip(double startTime, double endTime, Coord startCoord, Coord endCoord)
		{
			this.startTime = startTime;
			this.endTime = endTime;
			this.startCoord = startCoord;
			this.endCoord = endCoord;
			
			this.id = getNewId();
		}
		
		static long getTripCounter()
		{
			return TripIdCounter;
		}
		
		static long getNewId()
		{
			TripIdCounter++;
			return TripIdCounter;
		}

		public double getStartTime() {
			return startTime;
		}

		public void setStartTime(double startTime) {
			this.startTime = startTime;
		}

		public double getEndTime() {
			return endTime;
		}

		public void setEndTime(double endTime) {
			this.endTime = endTime;
		}

		public Coord getStartCoord() {
			return startCoord;
		}

		public void setStartCoord(Coord startCoord) {
			this.startCoord = startCoord;
		}

		public Coord getEndCoord() {
			return endCoord;
		}

		public void setEndCoord(Coord endCoord) {
			this.endCoord = endCoord;
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}
		
	}	// class Trip
	
	/*
	 * Internal data structure with information about all trip between two Traffic Cells.
	 */	
	static class Trips
	{
		static long TripIdCounter = 0;
		static Map<Long, Trip> trips = new TreeMap<Long, Trip>();

		List<Long> Ids;
		
		public Trips()
		{
		}
		
		// Initialize the ArrayList only then, if there are really stored some Trips!
		protected void init()
		{
			Ids = new ArrayList<Long>();
			
			trimmArrayLists();
		}
		
		public void reset()
		{
			if (Ids != null)
			{
				for(Long id : Ids)
				{			
					trips.remove(id);
				}
				Ids = null;
		
			}
		}
		
		public void addTrip(double startTime, double endTime, Coord startCoord, Coord endCoord)
		{	
			if (Ids == null) init();
	
			Trip trip = new Trip(startTime, endTime, startCoord, endCoord);
			
			trips.put(trip.getId(), trip);
			Ids.add(trip.getId());

			trimmArrayLists();
		}
		
		public List<Double> getStartTimes()
		{
			if (Ids == null) return new ArrayList<Double>();
			ArrayList<Double> times = new ArrayList<Double>();
			
			for(Long id:Ids)
			{
				times.add(trips.get(id).getStartTime());
			}
			
			return times;
		}
		
		public List<Double> getEndTimes()
		{
			if (Ids == null) return new ArrayList<Double>();
			ArrayList<Double> times = new ArrayList<Double>();
			
			for(Long id:Ids)
			{
				times.add(trips.get(id).getEndTime());
			}
			
			return times;
		}

		public List<Coord> getStartCoords()
		{
			if (Ids == null) return  new ArrayList<Coord>();
			ArrayList<Coord> coords = new ArrayList<Coord>();
			
			for(Long id:Ids)
			{
				coords.add(trips.get(id).getStartCoord());
			}
			
			return coords;
		}
		
		public List<Coord> getEndCoords()
		{
			if (Ids == null) return  new ArrayList<Coord>();
			ArrayList<Coord> coords = new ArrayList<Coord>();
			
			for(Long id:Ids)
			{
				coords.add(trips.get(id).getEndCoord());
			}
			
			return coords;
		}
		
		public List<Long> getIds()
		{
			if (Ids == null) return  new ArrayList<Long>();
			else return Ids;
		}
		
		// should free some memory
		public void trimmArrayLists()
		{
			((ArrayList<Long>)Ids).trimToSize();
		}
			
	}	// class Trips

	public ActTimesCollector getActTimesCollector() {
		return actTimesCollector;
	}

	public void setActTimesCollector(ActTimesCollector actTimesCollector) {
		this.actTimesCollector = actTimesCollector;
	}

	public boolean isUseLengthCorrection() {
		return useLengthCorrection;
	}

	public void setUseLengthCorrection(boolean useLengthCorrection) {
		this.useLengthCorrection = useLengthCorrection;
	}
	
}