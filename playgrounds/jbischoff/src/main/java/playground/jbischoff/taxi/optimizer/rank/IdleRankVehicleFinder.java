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

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.Vehicle;
import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import playground.jbischoff.energy.charging.DepotArrivalDepartureCharger;
import playground.michalm.taxi.model.TaxiRequest;
import playground.michalm.taxi.optimizer.TaxiUtils;
import playground.michalm.taxi.schedule.*;
/**
 * 
 * 
 * 
 * @author jbischoff
 *
 */

public class IdleRankVehicleFinder
{
    private final VrpData data;
    private final boolean straightLineDistance;
	private DepotArrivalDepartureCharger depotarrivaldeparturecharger;
	private boolean IsElectric;
	private boolean useChargeOverTime;
	Random rnd;


    public IdleRankVehicleFinder(VrpData data, boolean straightLineDistance)
    {
        this.data = data;
        this.straightLineDistance = straightLineDistance;
        this.IsElectric = false;
        this.useChargeOverTime = false;
        this.rnd = new Random(7);
        System.out.println("Using Straight Line Distance:" + this.straightLineDistance);
    }
    public void addDepotArrivalCharger(DepotArrivalDepartureCharger depotArrivalDepartureCharger){
    	this.depotarrivaldeparturecharger = depotArrivalDepartureCharger;
    	this.IsElectric = true;
    }
    

    public void setUseChargeOverTime(boolean useChargeOverDistance) {
		this.useChargeOverTime = useChargeOverDistance;
	}
    
	private boolean hasEnoughCapacityForTask(Vehicle veh){
    		Id vid = new IdImpl(veh.getName());
    		return this.depotarrivaldeparturecharger.isChargedForTask(vid);
    }
	
	private double getVehicleSoc(Vehicle veh){
		Id vid = new IdImpl(veh.getName());
		return this.depotarrivaldeparturecharger.getVehicleSoc(vid);
	}
    
    
    public Vehicle findClosestVehicle(TaxiRequest req)
    
    
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
             
             Collections.shuffle(data.getVehicles(),rnd);
             for (Vehicle veh : data.getVehicles()) {
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
          Collections.shuffle(data.getVehicles(),rnd);
          
          for (Vehicle veh : data.getVehicles()) {
        	  if (!TaxiUtils.isIdle(TaxiSchedules.getSchedule(veh), data.getTime(), true)) continue;
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
        Collections.shuffle(data.getVehicles(),rnd);
        
        for (Vehicle veh : data.getVehicles()) {
      	  if (!TaxiUtils.isIdle(TaxiSchedules.getSchedule(veh), data.getTime(), true)) continue;
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
        Collections.shuffle(data.getVehicles(),rnd);
    	  Vehicle bestVeh = null;
          double bestDistance = 1e9;
          for (Vehicle veh : data.getVehicles()) {
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
  

    private double calculateDistance(TaxiRequest req, Vehicle veh)
    {
        Schedule<TaxiTask> sched = TaxiSchedules.getSchedule(veh);
        int time = data.getTime();
        Vertex departVertex;

        if (!TaxiUtils.isIdle(sched, time, true)) {
            return Double.MAX_VALUE;
        }

        TaxiTask currentTask = sched.getCurrentTask();

        switch (currentTask.getTaxiTaskType()) {
            case WAIT_STAY:
                departVertex = ((StayTask)currentTask).getVertex();
                break;

            case CRUISE_DRIVE:// only CRUISE possible
                throw new IllegalStateException();// currently, no support for vehicle diversion

            default:
                throw new IllegalStateException();
        }

        return distance(departVertex, req.getFromVertex(), time);
    }


    private double distance(Vertex fromVertex, Vertex toVertex, int departTime)
    {
        if (straightLineDistance) {
            double deltaX = toVertex.getX() - fromVertex.getX();
            double deltaY = toVertex.getY() - fromVertex.getY();

            // this is a SQUARED distance!!! (to avoid unnecessary Math.sqrt() call)
            return deltaX * deltaX + deltaY * deltaY;
        }
        else {
        	System.out.println("using free flow dist");
            return data.getVrpGraph().getArc(fromVertex, toVertex).getCostOnDeparture(departTime);
        }
    }

}
