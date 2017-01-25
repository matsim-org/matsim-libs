/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.algorithm.scheduler.vehreqpath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.av.drt.TaxibusRequest;
import org.matsim.contrib.av.drt.TaxibusRequest.TaxibusRequestStatus;
import org.matsim.contrib.dvrp.data.Requests;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;


public class TaxibusDispatch 
{
    public final Vehicle vehicle;
    public final Set<TaxibusRequest> requests;
    public final ArrayList<VrpPathWithTravelData> path;
 
    private double earliestNextDeparture = 0;
    
    double twMax;
    
    public TaxibusDispatch(Vehicle vehicle, TaxibusRequest request, VrpPathWithTravelData path)
    {
    	this.requests = new LinkedHashSet<>();
    	this.path = new ArrayList<>();
    	this.requests.add(request);
        this.vehicle = vehicle;
        this.path.add(path);
        this.earliestNextDeparture = Math.max(request.getT0(), path.getArrivalTime());
        
        
    }
    
    public TaxibusDispatch(Vehicle vehicle, VrpPathWithTravelData path)
    {
    	this.requests = new LinkedHashSet<>();
    	this.path = new ArrayList<>();
        this.vehicle = vehicle;
        this.path.add(path);
        this.earliestNextDeparture =  path.getArrivalTime();
        
        
    }
    public void addRequests(Collection<TaxibusRequest> requests){
    	this.requests.addAll(requests);
    }
    public void addRequest(TaxibusRequest request){
    	this.requests.add(request);
    }
    
    public void addRequestAndPath(TaxibusRequest request, VrpPathWithTravelData path){
    	this.requests.add(request);
//    	System.out.println(requests);
    	this.path.add(path);
    	this.earliestNextDeparture = Math.max(request.getT0(), path.getArrivalTime());
    }
    
    public void addPath(VrpPathWithTravelData path){
    	this.path.add(path);
    	this.earliestNextDeparture = Math.max(this.earliestNextDeparture, path.getArrivalTime());
    }
    
    
    public double getEarliestNextDeparture() {
    	
		return earliestNextDeparture;
	}
    
    public VrpPathWithTravelData getLastPathAdded(){
    	return this.path.get(path.size()-1);
    }
    
    public void removeLastPathAdded(){
    	this.path.remove(path.size()-1);
    }
    public void setTwMax(double twMax) {
		this.twMax = twMax;
	}
    public double getTwMax() {
		return twMax;
	}
    
    
    
		
	public void failIfAnyRequestNotUnplanned(){
		for (TaxibusRequest request : this.requests){
			if (request.getStatus() != TaxibusRequestStatus.UNPLANNED) {
	            throw new IllegalStateException();
	        }
		}
	}
	
	public void failIfRequestNotUnplannedOrDispatched(){
		for (TaxibusRequest request : this.requests){
			 if (request.getStatus() != TaxibusRequestStatus.UNPLANNED){
				 if	(request.getStatus() != TaxibusRequestStatus.DISPATCHED) {
				Logger.getLogger(getClass()).error(request.toString() + " S: "+request.getStatus() );
	            throw new IllegalStateException();
	        }}
		}
	}
	
    public TreeSet<TaxibusRequest> getPickUpsForLink(Link link){
    	TreeSet<TaxibusRequest> beginningRequests = new TreeSet<>(Requests.ABSOLUTE_COMPARATOR);
    	for (TaxibusRequest req : this.requests){
    		if (req.getFromLink().equals(link)){
    			
    			if(req.getPickupTask()==null){
    			beginningRequests.add(req);
    			}
    		}
    	}
    	
    	return beginningRequests.isEmpty() ? null : beginningRequests ;
    }
    
    public TreeSet<TaxibusRequest> getDropOffsForLink(Link link){
    	TreeSet<TaxibusRequest> endingRequests = new TreeSet<>(Requests.ABSOLUTE_COMPARATOR);
    	for (TaxibusRequest req : this.requests){
    		if (req.getToLink().equals(link)){
    			
    			endingRequests.add(req);
    		}
    	}
    	
    	return endingRequests.isEmpty() ? null : endingRequests ;
    }
    
    
    
    
    
    
    
    
    
    
    
}