/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.southafrica.gauteng.roadpricingscheme;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.roadpricing.TollFactorI;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

public class SanralTollFactor_Subpopulation implements TollFactorI {
	private final Logger log = Logger.getLogger(SanralTollFactor_Subpopulation.class);

	public static final String E_TAG_ATTRIBUTE_NAME = "eTag";
	public static final String VEH_TOLL_CLASS_ATTRIB_NAME = "vehicleTollClass";

	private Scenario sc;
	
	public SanralTollFactor_Subpopulation(Scenario scenario) {
		this.sc = scenario;
	}
	
	@Override
	public double getTollFactor(Id<Person> personId, final Id<Vehicle> vehicleId, final Id<Link> linkId, final double time){		
		double timeDiscount = getTimeDiscount(time, vehicleId);
		double tagDiscount = 0.00;
		double ptDiscount = 0.00;
		double sizeFactor = 1.00;
		
		/* Determine the presence of an eTag from vehicle attributes. */  
//		if ( hasETag(vehicleId) ) {
		// yyyy Johan, this came up as a problem due to typed ids: The original method passes a vehicleId, but in the end interprets it
		// as a personId.  Such "silent conversion" are now no longer possible; they need to be explicit.  I am assuming that we can
		// as well pass the vehicleId here since, I think they are the same anyways.  kai, sep'14
		
		if ( hasETag(personId) ) {
			tagDiscount = 0.483 ;
		} 
		
		/* Determine toll class from vehicle type. */
		VehicleType type = sc.getVehicles().getVehicles().get(vehicleId).getType();
		if(type.getId().toString().equalsIgnoreCase("A2")){
			/* Nothing, it stays 1.00. */
		} else if(type.getId().toString().equalsIgnoreCase("B")){
			sizeFactor = 2.5;
		} else if(type.getId().toString().equalsIgnoreCase("C")){
			sizeFactor = 5.0;
		} else{
			log.error("Unknown vehicle class for toll. Assuming private car 'A2'");
		}
		
		/* Determine public transit status from vehicle Id. Currently (Jan '14)
		 * the subpopulations, including bus and taxi, has a prefix in its
		 * vehicle Id. */
		if(isPt(vehicleId)){
			ptDiscount = 1.0;
		}
		
		return getDiscountEligibility(linkId) ? sizeFactor*(1 - Math.min(1.0, timeDiscount + tagDiscount + ptDiscount)) : sizeFactor;		
	}


	@SuppressWarnings("static-method") // may become truly non-static later. kai, mar'14
	private boolean isPt(final Id<Vehicle> vehicleId) {
		return vehicleId.toString().startsWith("bus") || 
		   vehicleId.toString().startsWith("taxi");
	}


	
	/**
	 * Updated 2014/02/07 (jwjoubert).
	 * 
	 * @param time
	 * @return
	 * @see <a href="http://sanral.ensight-cdn.com/content/37038_19-11_TransE-TollCV01~1.pdf">Government Gazette,  Vol 581, No. 37038, page 13.</a>
	 */
	private  double getTimeDiscount(double time, Id<Vehicle> vehicleId){
		/* First get the real time of day. */
		double timeOfDay = time;
		while(timeOfDay > 86400){
			timeOfDay -= 86400;
		}
		
		Assert.assertNotNull( this.sc );
		Assert.assertNotNull( this.sc.getVehicles() );
		Assert.assertNotNull( this.sc.getVehicles().getVehicles() );
		if( this.sc.getVehicles().getVehicles().get(vehicleId)==null ) {
			Logger.getLogger(this.getClass()).warn("vehicleId: " + vehicleId ) ;
		}
		VehicleType type = this.sc.getVehicles().getVehicles().get(vehicleId).getType();
		
		if(type.getId().toString().equalsIgnoreCase("A1") || 
				type.getId().toString().equalsIgnoreCase("A2")){
			/* Weekday discounts - Class A vehicles */
			if(timeOfDay < 5*3600){ // 00:00 - 05:00
				return 0.25;
			} else if(timeOfDay < 6*3600){ // 05:00 - 06:00
				return 0.10;
			} else if(timeOfDay < 10*3600){ // 06:00 - 10:00
				return 0.00;
			} else if(timeOfDay < 14*3600){ // 10:00 - 14:00
				return 0.05;
			} else if(timeOfDay < 18*3600){ // 14:00 - 18:00
				return 0.00;
			} else if(timeOfDay < 23*3600){ // 18:00 - 23:00
				return 0.10;
			} else{ // 23:00 - 00:00
				return 0.25;
			}					
		} else if(type.getId().toString().equalsIgnoreCase("B") || 
				type.getId().toString().equalsIgnoreCase("C")){
			/* Weekday discounts - Class B & C vehicles */
			if(timeOfDay < 5*3600){ // 00:00 - 05:00
				return 0.30;
			} else if(timeOfDay < 6*3600){ // 05:00 - 06:00
				return 0.25;
			} else if(timeOfDay < 10*3600){ // 06:00 - 10:00
				return 0.00;
			} else if(timeOfDay < 14*3600){ // 10:00 - 14:00
				return 0.20;
			} else if(timeOfDay < 18*3600){ // 14:00 - 18:00
				return 0.00;
			} else if(timeOfDay < 23*3600){ // 18:00 - 23:00
				return 0.25;
			} else{ // 23:00 - 00:00
				return 0.30;
			}		
		} else{
			throw new RuntimeException("Don't know how to get time-of-day-discount for vehicles of type " + type.getId().toString());
		}
	}
	
	/**
	 * Check if the link is an existing toll booth, or a new toll gantry. Only
	 * new gantries are granted discounts. 
	 * @param linkId
	 * @return
	 */
	private static boolean getDiscountEligibility(Id<Link> linkId){
		if(NoDiscountLinks.getList().contains(linkId)){
			return false;
		} else{
			return true;
		}
	}
	
	private Boolean hasETag(Id<Person> personId) {
		Object o3 = this.sc.getPopulation().getPersonAttributes().getAttribute(personId.toString(), E_TAG_ATTRIBUTE_NAME);
		Boolean tag = false;
		if(o3 instanceof Boolean){
			tag = (Boolean)o3;
		} else{
			throw new RuntimeException("Expected an eTag availability of type `Boolean', but it was `" + o3.getClass().toString() + "'. Returning NULL");
		}
		return tag;
	}


}
