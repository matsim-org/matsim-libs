package playground.jbischoff.taxi.optimizer.rank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiUtils;
import playground.jbischoff.energy.charging.DepotArrivalDepartureCharger;


public class IdleRankVehicleFinder
{
    private final VrpData data;
    private final boolean straightLineDistance;
	private DepotArrivalDepartureCharger depotarrivaldeparturecharger;
	private boolean IsElectric;


    public IdleRankVehicleFinder(VrpData data, boolean straightLineDistance)
    {
        this.data = data;
        this.straightLineDistance = straightLineDistance;
        this.IsElectric = false;
    }
    public void addDepotArrivalCharger(DepotArrivalDepartureCharger depotArrivalDepartureCharger){
    	this.depotarrivaldeparturecharger = depotArrivalDepartureCharger;
    	this.IsElectric = true;
    }

    private boolean hasEnoughCapacityForTask(Vehicle veh){
    		Id vid = new IdImpl(veh.getName());
    		return this.depotarrivaldeparturecharger.isChargedForTask(vid);
    		
    	
    }
    
    public Vehicle findClosestVehicle(Request req)
    {
    	
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
            	           	
            }
        }

        return bestVeh;
    }

  

    private double calculateDistance(Request req, Vehicle veh)
    {
        Schedule sched = veh.getSchedule();
        int time = data.getTime();
        Vertex departVertex;

        if (!TaxiUtils.isIdle(veh, time, true)) {
            return Double.MAX_VALUE;
        }

        Task currentTask = sched.getCurrentTask();

        switch (currentTask.getType()) {
            case WAIT:
                departVertex = ((WaitTask)currentTask).getAtVertex();
                break;

            case DRIVE:// only CRUISE possible
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
            return data.getVrpGraph().getArc(fromVertex, toVertex).getCostOnDeparture(departTime);
        }
    }

}
