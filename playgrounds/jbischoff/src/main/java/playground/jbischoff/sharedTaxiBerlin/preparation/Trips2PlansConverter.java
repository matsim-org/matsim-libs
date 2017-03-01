/* *********************************************************************** *
* project: org.matsim.*
*                                                                         *
* *********************************************************************** *
*                                                                         *
* copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.jbischoff.sharedTaxiBerlin.preparation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.core.utils.misc.Time;

import playground.jbischoff.utils.JbUtils;

/**
* @author  jbischoff
*
*/
/**
*
*/
public class Trips2PlansConverter {

       private Map<String,Coord> stopLocations = new HashMap<>();
       private List<Trip> trips = new ArrayList<>();
       private String stopLocationsFile = "Y:/bvg-taxi/stoplocations.txt";
       private String tripLocationsFile = "Y:/bvg-taxi/departures.txt";
       private List<String> selectedTrips = new ArrayList<>();
       private Random random = MatsimRandom.getRandom();
       double sampleSize = 0.2;
       private boolean randomize = true;
       private String plansFile  = "Y:/bvg-taxi/plans"+sampleSize+"_"+randomize+".xml";
       private String tripsFile  = "Y:/bvg-taxi/trips"+sampleSize+"_"+randomize+".csv";
       private int randomAllocation_meters = 300;
       private int[] depHour = new int [24];

       
       
       public static void main(String[] args) {
       new Trips2PlansConverter().run();
       }

       private void run() {
             readStopLocations(stopLocationsFile);
             readTrips(tripLocationsFile);
             System.out.println(trips.size());
             
             Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
             createPlans(scenario.getPopulation());
             new PopulationWriter(scenario.getPopulation()).write(plansFile);
             JbUtils.collection2Text(selectedTrips,tripsFile, "time;fromStop;toStop;fromX;fromY;toX;toY");
             for (int i = 0; i<depHour.length;i++)
             {System.out.println(i +"\t"+ depHour[i]);
             }
       }

       private void createPlans(Population population) {
             int i = 0;
             for (Trip trip : trips)
             {
                    if (this.random.nextDouble()<sampleSize){
                    randomizeTrip(trip);	
                    Person p = population.getFactory().createPerson(Id.createPersonId(trip.fromLoc+"_"+trip.toLoc+"_"+(int)trip.departureTime+"_"+i));
                    i++;
                    Plan plan = population.getFactory().createPlan();
                    p.addPlan(plan);
                    Activity act = population.getFactory().createActivityFromCoord("departure", trip.fromCoord);
                    int hour = JbUtils.getHour(trip.departureTime);
                    depHour[hour]++;
                    if (trip.departureTime<18*3600){
                           trip.departureTime += 24*3600;
                    }
                    act.setEndTime(trip.departureTime);
                    plan.addActivity(act);
                    plan.addLeg(population.getFactory().createLeg("drt"));
                    Activity act2 = population.getFactory().createActivityFromCoord("arrival", trip.toCoord);
                    plan.addActivity(act2);
                    population.addPerson(p);
                    selectedTrips.add(trip.toString());
             }}
       }

       /**
	 * @param trip
	 */
	private void randomizeTrip(Trip trip) {
		double fromX = trip.fromCoord.getX() + - randomAllocation_meters + random.nextInt(2*randomAllocation_meters); 
		double fromY = trip.fromCoord.getY() + - randomAllocation_meters + random.nextInt(2*randomAllocation_meters);
		trip.fromCoord = new Coord(fromX,fromY);
		
		double toX = trip.toCoord.getX() + - randomAllocation_meters + random.nextInt(2*randomAllocation_meters); 
		double toY = trip.toCoord.getY() + - randomAllocation_meters + random.nextInt(2*randomAllocation_meters);
		trip.toCoord = new Coord(toX,toY);
		
	}

	private void readTrips(String tripLocationsFile2) {
             TabularFileParserConfig config = new TabularFileParserConfig();
        config.setDelimiterTags(new String[] {";"});
        config.setFileName(tripLocationsFile2);
        config.setCommentTags(new String[] { "#" });             
        new TabularFileParser().parse(config, new TabularFileHandler() {
                    @Override
                    public void startRow(String[] row) {
                           Trip trip = new Trip();
                           trip.departureTime = Time.parseTime(row[0]);
                           trip.fromLoc = row[1];
                           trip.toLoc = row[2];
                           trip.fromCoord = stopLocations.get(trip.fromLoc);
                           trip.toCoord = stopLocations.get(trip.toLoc);
                           trips.add(trip);
                    }
             });
       }

       private void readStopLocations(String stopLocationsFile2) {
             TabularFileParserConfig config = new TabularFileParserConfig();
        config.setDelimiterTags(new String[] {";"});
        config.setFileName(stopLocationsFile2);
        config.setCommentTags(new String[] { "#" });
        new TabularFileParser().parse(config, new TabularFileHandler() {
                    @Override
                    public void startRow(String[] row) {
                           Coord coord = new Coord( Double.parseDouble(row[1]),Double.parseDouble(row[2]));
                           stopLocations.put(row[0], coord);
                    }
             });

             
       }

}


class Trip
{

       String fromLoc;
       String toLoc;
       Coord fromCoord;
       Coord toCoord;
       double departureTime;
       
       /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
    	return (departureTime+";"+fromLoc+";"+toLoc+";"+fromCoord.getX()+";"+fromCoord.getY()+";"+toCoord.getX()+";"+toCoord.getY());
    
    }
       
}



