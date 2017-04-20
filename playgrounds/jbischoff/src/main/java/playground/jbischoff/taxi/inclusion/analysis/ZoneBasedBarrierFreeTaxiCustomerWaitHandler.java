/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxi.inclusion.analysis;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.taxi.run.*;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;

import com.vividsolutions.jts.geom.*;

import playground.jbischoff.utils.JbUtils;


public class ZoneBasedBarrierFreeTaxiCustomerWaitHandler implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler {

	private int numberOfTrips;
	private double totalWaitingTime;
	private Map<Id<Person>, Double> personsTaxiCallTime;
	private Map<Id<Person>, String> personZone = new HashMap<>();
	private final Network network;
	private final Map<String,Geometry> zones;
	private Map<String,double[]> zoneWaitTimes = new TreeMap<>();
	private Map<String,int[]> zoneDepartures = new TreeMap<>();
	private final boolean onlyBarrierFreeRequests;
	private final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.DHDN_GK4, "EPSG:25833");
	private double overallAverageWaitTime = 0;
	
		public ZoneBasedBarrierFreeTaxiCustomerWaitHandler(Network network,Map<String,Geometry> zones, boolean onlyBarrierFreeRequests) {
			this.numberOfTrips = 0;
			this.totalWaitingTime = 0.0;
			this.personsTaxiCallTime = new HashMap<Id<Person>, Double>();
			this.network = network;
			this.zones = zones;
			initializeZoneMaps();
			this.onlyBarrierFreeRequests = onlyBarrierFreeRequests;
			
		}
	
	    private void initializeZoneMaps() {
	    	for (String zoneId : zones.keySet()){
	    		zoneWaitTimes.put(zoneId, new double[24]);
	    		zoneDepartures.put(zoneId, new int[24]);
	    		
	    	}
		}

		@Override
	    public void reset(int iteration){
	    }
	    
	    @Override
	    public void handleEvent(PersonDepartureEvent event){
	    	
	        if (!event.getLegMode().equals(TaxiModule.TAXI_MODE))
	            return;
	        if (onlyBarrierFreeRequests){
	        	if (!event.getPersonId().toString().startsWith("hc_")){
	        		return;
	        	}
	        } else
	        	if (event.getPersonId().toString().startsWith("hc_")){
	        		return;
	        	}
	        String zoneId = getZoneForLinkId(event.getLinkId());
	        if (zoneId!=null){
	        	this.personsTaxiCallTime.put(event.getPersonId(), event.getTime());
	        	this.personZone.put(event.getPersonId(), zoneId);
	        }
	        
	    }

	    @Override
	    public void handleEvent(PersonEntersVehicleEvent event){
	        if (!this.personsTaxiCallTime.containsKey(event.getPersonId()))
	            return;
	        double callTime = this.personsTaxiCallTime.get(event.getPersonId());
	        double waitingTime = event.getTime() - callTime;
	        if (this.personZone.containsKey(event.getPersonId())){
	        	String zoneId = this.personZone.remove(event.getPersonId());
	        	int hour = JbUtils.getHour(callTime);
	        	this.zoneDepartures.get(zoneId)[hour]++;
	        	
	        	
	        	this.zoneWaitTimes.get(zoneId)[hour]+=waitingTime;
	        	
	        	
	        }
	        
	        this.totalWaitingTime += waitingTime;
	        
	        this.numberOfTrips += 1;
	        this.personsTaxiCallTime.remove(event.getPersonId());
	    }

	    public double getAvgWaitingTime(){
	    	return (this.totalWaitingTime)/(this.numberOfTrips);
	    }
	    
	    public void writeCustomerStats(String fileDir){
	    	try {
	            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fileDir+"waitStats.txt")));
	            bw.write("total number of customer's taxi trips: \t" + this.numberOfTrips);
	            bw.newLine();
	            bw.write("total waiting time (h) : \t" + (this.totalWaitingTime / 3600));
	            bw.newLine();
	            bw.write("average waiting time per trip (s) : \t " + getAvgWaitingTime());
	            bw.flush();
	            bw.close();
	        }
	        catch (IOException e) {
	            System.err.println("Could not create File" + fileDir);
	            e.printStackTrace();
	        }
	    	String fileName = fileDir + "zoneStats."+(this.onlyBarrierFreeRequests?"_bf":"_NonBF")+".csv";
	    	writeZoneStats(fileName);
	    }
	    
	    private void writeZoneStats(String filename) {
			BufferedWriter bw = IOUtils.getBufferedWriter(filename);
			Locale.setDefault(Locale.US);
			DecimalFormat df = new DecimalFormat( "####0.00" );
			try {
				bw.write("Zone;");
				for (int i = 3; i<24; i++){
				bw.write(i+" trips;"+i+" avWt;");	
				}
				bw.write("total;averageWait");
				bw.newLine();
				double totalWait[] = new double[24];
				int totalTrips[] = new int[24];
				for (Entry<String, int[]> e : this.zoneDepartures.entrySet()){
					double[] waitTimes = this.zoneWaitTimes.get(e.getKey());
					bw.write(e.getKey()+";");
					double allTrips = 0.;
					double allWait = 0.;
					for (int i = 4; i<24; i++){
						double waitTime = waitTimes[i];
						int trips = e.getValue()[i];
						double averageWaitTime = waitTime/trips;
						if (trips == 0) averageWaitTime = 0;
						bw.write(trips+";"+df.format(averageWaitTime)+";");
						allTrips+=trips;
						allWait+=waitTime;
						totalWait[i]+=waitTime;
						totalTrips[i]+=trips;
						
					} 
					
					double allAv = allWait/allTrips;
					if (allTrips == 0) allAv = 0;
					bw.write(df.format(allTrips)+";"+df.format(allAv));
					bw.newLine();
					
				}
				bw.write("allZones;");
				double allHoursCustomers = 0;
				double allHoursWait = 0;
				for (int i = 6; i<24; i++){
					double waitTime = totalWait[i];
					int trips = totalTrips[i];
					double averageWaitTime = waitTime/trips;
					if (trips == 0) averageWaitTime = 0;
					allHoursWait += waitTime;
					allHoursCustomers += trips;
					bw.write(trips+";"+df.format(averageWaitTime)+";");
					} 
				this.overallAverageWaitTime = allHoursWait/allHoursCustomers;
				bw.flush();
				bw.close();
				BufferedWriter csvt = IOUtils.getBufferedWriter(filename+"t");
				csvt.write("\"String\"");
				for (int i = 0; i<21;i++) csvt.write(",\"Real\"");
				csvt.flush();
				csvt.close();
				
			} catch (IOException e) {
				// TODO: handle exception
			}
			
		}

		String getZoneForLinkId(Id<Link> linkId){
	    	Coord linkCoord = ct.transform( network.getLinks().get(linkId).getCoord());
	    	Point linkPoint = MGC.coord2Point(linkCoord);
	    	for (Entry<String,Geometry> e : zones.entrySet()){
	    		if (e.getValue().contains(linkPoint))
	    			return e.getKey();
	    	}
	    	
	    	return null;
	    	
	    }
		
		/**
		 * @return the overallAverageWaitTime
		 */
		public double getOverallAverageWaitTime() {
			return overallAverageWaitTime;
		}
	    
}
