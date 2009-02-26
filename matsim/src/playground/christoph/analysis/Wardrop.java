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

package playground.christoph.analysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.basic.v01.BasicActImpl;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Population;

public class Wardrop {

	protected NetworkLayer network;
	protected Population population;
	
	protected Trips[][] zoneMatrix;
	
	protected double xMin;
	protected double xMax;
	protected double yMin;
	protected double yMax;
	protected int zonesX;
	protected int zonesY;
	
	
	public void createZoneMatrix()
	{
		// create new ZoneMatrix
		int zones = zonesX*zonesY;
		
		zoneMatrix = new Trips[zones][zones];
		
		for(int i = 0; i < zones; i++)
		{
			for(int j = 0; j < zones; j++)
			{
				zoneMatrix[i][j] = new Trips();
			}
		}
		
		for (Person person : population.getPersons().values()) 
		{
			Plan plan = person.getSelectedPlan();
			
			ArrayList<BasicActImpl> acts = new ArrayList<BasicActImpl>();
			
			Iterator<BasicActImpl> actIter = plan.getIteratorAct();
			while (actIter.hasNext())
			{
				acts.add(actIter.next());				
			}
			
			// The last Act is only the end of a leg and not the beginning of a new one!
			for(int i = 0; i < acts.size() - 1; i++)
			{
				BasicActImpl startAct = acts.get(i);
				BasicActImpl endAct = acts.get(i + 1);
				
				Coord startCoord = startAct.getCoord();
				Coord endCoord = endAct.getCoord();
				
				double startTime = startAct.getEndTime();
				double endTime = endAct.getStartTime();
				
				int startZone = getZone(startCoord);
				int endZone = getZone(endCoord);
				
				Trips trip = zoneMatrix[zones][zones];

				trip.addTrip(startTime, endTime, startCoord, endCoord);
/*				
				Trip trip = new Trip();
				trip.startCoord = startCoord;
				trip.endCoord = endCoord;
				trip.startTime = startTime;
				trip.endTime = endTime;
				
				// inc counter
				int value = zoneMatrix[startZone][endZone];
				value++;
				zoneMatrix[startZone][endZone] = value;
*/			
			}
			
		}
	}
	
	public int getZone(Link link)
	{
		return getZone(link.getCenter());
	}
	
	public int getZone(Node node)
	{
		return getZone(node.getCoord());
	}
	
	public int getZone(Coord coord)
	{
		return 0;
	}

	/*
	 * Internal data structure with information about a trip between two Activities.
	 */
	class Trips
	{
		List<Double> startTimes;
		List<Double> endTimes;
		List<Coord> startCoords;
		List<Coord> endCoords;
		
//		double startTime;
//		double endTime;
//		Coord startCoord;
//		Coord endCoord;
		
		public Trips()
		{
			startTimes = new ArrayList<Double>();
			endTimes = new ArrayList<Double>();
			startCoords = new ArrayList<Coord>();
			endCoords = new ArrayList<Coord>();
		}
		
		public void addTrip(double startTime, double endTime, Coord startCoord, Coord endCoord)
		{
			startTimes.add(startTime);
			endTimes.add(endTime);
			startCoords.add(startCoord);
			endCoords.add(endCoord);
		}
		
		/*
		 * Start- and endTime specify the time, within a Trip has to start to be used for the calculation.
		 * 
		 * double[0] = mean
		 * double[1] = standarddeviation
		 * double[2] = number of trips within the specified time
		 */
		public double[] calcMedianTripDuration(double startTime, double endTime)
		{
			double[] results = new double[3];
			
			// set default values
			results[0] = 0.0;
			results[1] = 0.0;
			results[2] = 0.0;
			
			//int counter = 0;
			//double durations = 0.0;
			double mean;
			double standarddeviation;
			
			ArrayList<Double> durations = new ArrayList<Double>();
			
			for(int i = 0; i < startTimes.size(); i++)
			{
				// if trip started within the given time slot
				if (startTimes.get(i) > startTime && startTimes.get(i) < endTime)
				{
					//counter++;
					//durations = durations + ( endTimes.get(i) - startTimes.get(i) );
					durations.add(endTimes.get(i) - startTimes.get(i));
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
			
			standarddeviation = Math.sqrt(1 / (durations.size() - 1) * diffsum);
			results[1] = standarddeviation;
			
			//return durations / counter;
			return results;
		}
		
	}
	
}
