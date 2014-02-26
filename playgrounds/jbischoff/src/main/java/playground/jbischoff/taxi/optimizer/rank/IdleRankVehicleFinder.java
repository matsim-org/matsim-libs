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

package playground.jbischoff.taxi.optimizer.rank;

import java.util.*;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.util.*;

import playground.jbischoff.energy.charging.RankArrivalDepartureCharger;
import playground.michalm.taxi.model.TaxiRequest;
import playground.michalm.taxi.optimizer.TaxiUtils;
import playground.michalm.taxi.optimizer.fifo.TaxiScheduler;
import playground.michalm.taxi.optimizer.query.*;
/**
 * 
 * 
 * 
 * @author jbischoff
 *
 */

public class IdleRankVehicleFinder
    implements VehicleFinder, VehicleFilter
{
    private final MatsimVrpContext context;
    private final TaxiScheduler scheduler;
	private RankArrivalDepartureCharger rankArrivaldeparturecharger;
	private boolean IsElectric;
	private boolean useChargeOverTime;
	Random rnd;


    public IdleRankVehicleFinder(MatsimVrpContext context, TaxiScheduler scheduler)
    {
        this.context = context;
        this.scheduler = scheduler;
        this.IsElectric = false;
        this.useChargeOverTime = false;
        this.rnd = new Random(7);
        System.out.println("Using distance measure: Straight line");
    }
    public void addRankArrivalCharger(RankArrivalDepartureCharger rankArrivalDepartureCharger){
    	this.rankArrivaldeparturecharger = rankArrivalDepartureCharger;
    	this.IsElectric = true;
    }
    

    public void setUseChargeOverTime(boolean useChargeOverDistance) {
		this.useChargeOverTime = useChargeOverDistance;
	}
    
	private boolean hasEnoughCapacityForTask(Vehicle veh){
    		return this.rankArrivaldeparturecharger.isChargedForTask(veh.getId());
    }
	
	private double getVehicleSoc(Vehicle veh){
		return this.rankArrivaldeparturecharger.getVehicleSoc(veh.getId());
	}
    
    
	@Override
    public Vehicle findVehicleForRequest(Iterable<Vehicle> vehicles, TaxiRequest request)
    {
    	if(this.useChargeOverTime) {
    		
//    		return findHighestChargedIdleVehicleDistanceSort(request);
    		return findBestChargedVehicle(request);
//    		return findHighestChargedIdleVehicle(request);
    	
    	}
    	else return findClosestFIFOVehicle(request);
    }
	
	@Override
	public Iterable<Vehicle> filterVehiclesForRequest(Iterable<Vehicle> vehicles,
	        TaxiRequest request)
	{
	    return Collections.singleton(findVehicleForRequest(vehicles, request));
	}
      
    
    private Vehicle findBestChargedVehicle(TaxiRequest req){
       	  Vehicle bestVeh = null;
             double bestDistance = 1e9;
             
             Collections.shuffle(context.getVrpData().getVehicles(),rnd);
             for (Vehicle veh : context.getVrpData().getVehicles()) {
             	if (this.IsElectric)
             		if (!this.hasEnoughCapacityForTask(veh)) continue;
             	
                 double distance = calculateSquaredDistance(req, veh);
                 
                 if (distance < bestDistance) {	
                     bestDistance = distance;
                     bestVeh = veh;
                 }
                 else if (distance == bestDistance){
               	         
                 	if (bestVeh == null)
                 		{
                 		bestVeh= veh;
                 		continue;           		
                 		}
                 		if (this.getVehicleSoc(veh)>this.getVehicleSoc(bestVeh)){  bestVeh= veh;
                 		}
                 		//higher charge, if distance is equal	
                 }
             }

             return bestVeh;
       }
    
    private Vehicle findHighestChargedIdleVehicle(TaxiRequest req){
     	  Vehicle bestVeh = null;
     	  double bestSoc=0;
          Collections.shuffle(context.getVrpData().getVehicles(),rnd);
          
          for (Vehicle veh : TaxiUtils.filterIdleVehicles(context.getVrpData().getVehicles())) {
        	  if (this.IsElectric)   if (!this.hasEnoughCapacityForTask(veh)) continue;
        	  double soc = this.getVehicleSoc(veh);
        	  if (soc>bestSoc){
        		  bestSoc = soc;
        		  bestVeh=veh;
        	  }
          }

    	
          return bestVeh;

    }
    
    private Vehicle findHighestChargedIdleVehicleDistanceSort(TaxiRequest req){
   	  Vehicle bestVeh = null;
   	  double bestSoc=0;
        Collections.shuffle(context.getVrpData().getVehicles(),rnd);
        
        for (Vehicle veh : TaxiUtils.filterIdleVehicles(context.getVrpData().getVehicles())) {
      	  if (this.IsElectric)   if (!this.hasEnoughCapacityForTask(veh)) continue;
      	  double soc = this.getVehicleSoc(veh);
      	  if (soc>bestSoc){
      		  bestSoc = soc;
      		  bestVeh=veh;
      	  }
      	  else if (soc == bestSoc){
      		if (bestVeh == null)
     		{
     		bestVeh= veh;
     		continue;           		
     		}
      		if (this.calculateSquaredDistance(req, veh)<this.calculateSquaredDistance(req, bestVeh)){
      			bestVeh = veh;
      		}
      	  }
        }

  	
        return bestVeh;

  }
    
    private Vehicle findClosestFIFOVehicle(TaxiRequest req){
        Collections.shuffle(context.getVrpData().getVehicles(),rnd);
    	  Vehicle bestVeh = null;
//          double bestDistance = Double.MAX_VALUE;
          double bestDistance = Double.MAX_VALUE/2;
          for (Vehicle veh : TaxiUtils.filterIdleVehicles(context.getVrpData().getVehicles())) {
          	if (this.IsElectric)
          		if (!this.hasEnoughCapacityForTask(veh)) continue;
              double distance = calculateSquaredDistance(req, veh);
              
              if (distance < bestDistance) {	
                  bestDistance = distance;
                  bestVeh = veh;
              }
              else if (distance == bestDistance){
            	         
              	if (bestVeh == null)
              		{
              		bestVeh= veh;
              		continue;           		
              		}
              		if (veh.getSchedule().getCurrentTask().getBeginTime() < bestVeh.getSchedule().getCurrentTask().getBeginTime())
              		bestVeh= veh;
              		//FIFO, if distance is equal	
              }
          }

          return bestVeh;
    }
    
    
    private double calculateSquaredDistance(TaxiRequest req, Vehicle veh)
    {
        LinkTimePair departure = scheduler.getEarliestIdleness(veh);
        Link fromLink = departure.link;
        Link toLink = req.getFromLink();

        return DistanceUtils.calculateSquaredDistance(fromLink, toLink);
    }
}
