/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package playground.southafrica.sandboxes.kai.freight;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.TimeWindow;

class MethodsToGenerateServices {
	private MethodsToGenerateServices() {} // only static methods, do not instantiate

	static void createServicesMethod1(Collection<CarrierService> services) {
		for ( int ii=0 ; ii<10 ; ii++ ) { // go through shipments to generate
			// service IDs correspond to ii:
			Id<CarrierService> id = Id.create(ii, CarrierService.class) ; 
	
			// yy this must become the linkID where the good needs to go. 
			Id<Link> locationLinkId = null ; 
	
			// objects are constructed by builders:
			CarrierService.Builder builder = CarrierService.Builder.newInstance(id, locationLinkId) ;
			
			// yy this is how much will be requested (relative to truck vehicle size):
			builder.setCapacityDemand(1) ; 
	
			// set time window (if applicable)
			builder.setServiceStartTimeWindow(TimeWindow.newInstance(8.*3600., 9.*3600. )) ; 
	
			// "build" the service object, and add it to the container:
			services.add(builder.build());
		}
	}
	
	static void createServicesMethod2(Collection<CarrierService> services) {
		for ( int ii=0 ; ii<10 ; ii++ ) { // go through shipments to generate
			// service IDs correspond to ii:
			Id<CarrierService> id = Id.create(ii, CarrierService.class) ; 
	
			// yy this must become the linkID where the good needs to go. 
			Id<Link> locationLinkId = null ; 
	
			// objects are constructed by builders:
			CarrierService.Builder builder = CarrierService.Builder.newInstance(id, locationLinkId) ;
			
			// yy this is how much will be requested (relative to truck vehicle size):
			builder.setCapacityDemand(1) ; 
	
			// set time window (if applicable)
			builder.setServiceStartTimeWindow(TimeWindow.newInstance(8.*3600., 9.*3600. )) ; 
	
			// "build" the service object, and add it to the container:
			services.add(builder.build());
		}
	}
	
	

}
