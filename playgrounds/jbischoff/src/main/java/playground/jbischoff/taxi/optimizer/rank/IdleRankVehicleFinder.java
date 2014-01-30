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

import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;

import playground.jbischoff.energy.charging.RankArrivalDepartureCharger;
import playground.michalm.taxi.model.TaxiRequest;
import playground.michalm.taxi.optimizer.TaxiUtils;
import playground.michalm.taxi.optimizer.immediaterequest.*;
import playground.michalm.taxi.schedule.TaxiSchedules;
/**
 * 
 * 
 * 
 * @author jbischoff
 *
 */

public class IdleRankVehicleFinder
    implements VehicleFinder
{
    private final MatsimVrpContext context;
    private final VrpPathCalculator calculator;
    private final boolean straightLineDistance;
	private RankArrivalDepartureCharger rankArrivaldeparturecharger;
	private boolean IsElectric;
	private boolean useChargeOverTime;
	Random rnd;


    public IdleRankVehicleFinder(MatsimVrpContext context, VrpPathCalculator calculator, boolean straightLineDistance)
    {
        this.context = context;
        this.calculator = calculator;
        this.straightLineDistance = straightLineDistance;
        this.IsElectric = false;
        this.useChargeOverTime = false;
        this.rnd = new Random(7);
        System.out.println("Using Straight Line Distance:" + this.straightLineDistance);
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
    public Vehicle findVehicle(TaxiRequest req)
    {
    	
    	if(this.useChargeOverTime) {
    		
//    		return findHighestChargedIdleVehicleDistanceSort(req);
    		return findBestChargedVehicle(req);
//    		return findHighestChargedIdleVehicle(req);
    	
    	}
    	else return findClosestFIFOVehicle(req);
    	
    }
      
    
    private Vehicle findBestChargedVehicle(TaxiRequest req){
       	  Vehicle bestVeh = null;
             double bestDistance = 1e9;
             
             Collections.shuffle(context.getVrpData().getVehicles(),rnd);
             for (Vehicle veh : context.getVrpData().getVehicles()) {
             	if (this.IsElectric)
             		if (!this.hasEnoughCapacityForTask(veh)) continue;
             	
                 double distance = calculateDistance(req, veh);
                 
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
          
          for (Vehicle veh : context.getVrpData().getVehicles()) {
        	  if (!TaxiUtils.isIdle(veh, context.getTime(), true)) continue;
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
        
        for (Vehicle veh : context.getVrpData().getVehicles()) {
      	  if (!TaxiUtils.isIdle(veh, context.getTime(), true)) continue;
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
      		if (this.calculateDistance(req, veh)<this.calculateDistance(req, bestVeh)){
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
          for (Vehicle veh : context.getVrpData().getVehicles()) {
          	if (this.IsElectric)
          		if (!this.hasEnoughCapacityForTask(veh)) continue;
          	
              double distance = calculateDistance(req, veh);
              
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
    
    
    private double calculateDistance(TaxiRequest req, Vehicle veh){
        return IdleVehicleFinder.calculateDistance(req, veh, context.getTime(), calculator,
                straightLineDistance);
    }
}
