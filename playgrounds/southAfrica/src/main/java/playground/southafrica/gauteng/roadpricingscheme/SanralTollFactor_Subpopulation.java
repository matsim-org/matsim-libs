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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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
	public double getTollFactor(Id personId, final Id vehicleId, final Id linkId, final double time){		
		double timeDiscount = getTimeDiscount(time);
		double tagDiscount = 0.00;
		double ptDiscount = 0.00;
		double sizeFactor = 1.00;
		
		/* Determine the presence of an eTag from vehicle attributes. */  
		Object o1 = sc.getVehicles().getVehicleAttributes().getAttribute(vehicleId.toString(), E_TAG_ATTRIBUTE_NAME);
		if (o1 instanceof Boolean) {
			tagDiscount = ((Boolean)o1) ? 0.483 : 0.00;
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
		if(vehicleId.toString().startsWith("bus") || 
		   vehicleId.toString().startsWith("taxi")){
			ptDiscount = 0.55;
		}
		
		return getDiscountEligibility(linkId) ? sizeFactor*(1 - Math.min(1.0, timeDiscount + tagDiscount + ptDiscount)) : sizeFactor;		
	}
	
	
	/**
	 * Updated 2014/01/20 (jwjoubert).
	 * 
	 * @param time
	 * @return
	 * @see <a href="http://sanral.ensight-cdn.com/content/37038_19-11_TransE-TollCV01~1.pdf">Government Gazette,  Vol 581, No. 37038, page 27.</a>
	 */
	private  double getTimeDiscount(double time){
		/* First get the real time of day. */
		double timeOfDay = time;
		while(timeOfDay > 86400){
			timeOfDay -= 86400;
		}
		
		/* Weekday discounts */
		if(timeOfDay < 5*3600){ // 00:00 - 05:00
			return 0.30;
		} else if(timeOfDay < 6*3600){ // 05:00 - 06:00
			return 0.25;
		} else if(timeOfDay < 8.5*3600){ // 06:00 - 10:00
			return 0.00;
		} else if(timeOfDay < 16*3600){ // 10:00 - 14:00
			return 0.20;
		} else if(timeOfDay < 19*3600){ // 14:00 - 18:00
			return 0.00;
		} else if(timeOfDay < 23*3600){ // 18:00 - 23:00
			return 0.25;
		} else{ // 23:00 - 00:00
			return 0.30;
		}		
	}
	
	/**
	 * Check if the link is an existing toll booth, or a new toll gantry. Only
	 * new gantries are granted discounts. 
	 * @param linkId
	 * @return
	 */
	private static boolean getDiscountEligibility(Id linkId){
		if(NoDiscountLinks.getList().contains(linkId)){
			return false;
		} else{
			return true;
		}
	}
	
	private Map<Id,SanralTollVehicleType> tollVehicleTypes = new HashMap<Id,SanralTollVehicleType>() ; 

	/**
	 * Checks the subpopulation attributes and assigns the vehicle type 
	 * accordingly.
	 */
	@Override
	public SanralTollVehicleType typeOf(Id idObj) {
		if ( tollVehicleTypes.get(idObj) != null ) {
			return tollVehicleTypes.get(idObj) ;
		}
		
		/* Check subpopulation. */
		Assert.assertNotNull(this.sc);
		Assert.assertNotNull(this.sc.getPopulation());
		Assert.assertNotNull(this.sc.getPopulation().getPersonAttributes());
		Assert.assertNotNull( idObj ) ;
		Assert.assertNotNull( sc.getConfig() );
		Assert.assertNotNull( sc.getConfig().plans() );
		Object o1 = this.sc.getPopulation().getPersonAttributes().getAttribute(idObj.toString(), sc.getConfig().plans().getSubpopulationAttributeName());
		String subpopulation;
		if(o1 instanceof String){
			subpopulation = (String)o1;
		} else{
			throw new RuntimeException("Expected a subppulation description of type `String', but it was `" + o1.getClass().toString() + "'. Returning NULL");
		}
		
		/* Check vehicle type. */
		Object o2 = this.sc.getPopulation().getPersonAttributes().getAttribute(idObj.toString(), VEH_TOLL_CLASS_ATTRIB_NAME);
		String vehicleType;
		if(o2 instanceof String){
			vehicleType = (String)o2;
		} else{
			throw new RuntimeException("Expected a vehicle type description of type `String', but it was `" + o2.getClass().toString() + "'. Returning NULL");
		}
		
		/* Check availability of eTag. */
		Object o3 = this.sc.getPopulation().getPersonAttributes().getAttribute(idObj.toString(), E_TAG_ATTRIBUTE_NAME);
		Boolean tag = false;
		if(o3 instanceof Boolean){
			tag = (Boolean)o3;
		} else{
			throw new RuntimeException("Expected an eTag availability of type `Boolean', but it was `" + o3.getClass().toString() + "'. Returning NULL");
		}
	
		/* Identify correct vehicle type. */
		if(subpopulation.equalsIgnoreCase("car")){
			final SanralTollVehicleType tvType = tag ? SanralTollVehicleType.carWithTag : SanralTollVehicleType.carWithoutTag;
			tollVehicleTypes.put( idObj, tvType ) ;
			return tvType;

		} else if(subpopulation.equalsIgnoreCase("commercial")){
			if(vehicleType.equalsIgnoreCase("A2")){
				final SanralTollVehicleType tvType = tag ? SanralTollVehicleType.commercialClassAWithTag : SanralTollVehicleType.commercialClassAWithoutTag;
				tollVehicleTypes.put( idObj, tvType ) ;
				return tvType;
			} else if(vehicleType.equalsIgnoreCase("B")){
				final SanralTollVehicleType tvType = tag ? SanralTollVehicleType.commercialClassBWithTag : SanralTollVehicleType.commercialClassBWithoutTag;
				tollVehicleTypes.put( idObj, tvType ) ;
				return tvType;
			} else if(vehicleType.equalsIgnoreCase("C")){
				final SanralTollVehicleType tvType = tag ? SanralTollVehicleType.commercialClassCWithTag : SanralTollVehicleType.commercialClassCWithoutTag;
				tollVehicleTypes.put( idObj, tvType ) ;
				return tvType;
			} else{
				throw new RuntimeException("Unknown vehicle type " + vehicleType);
			}
			
		} else if(subpopulation.equalsIgnoreCase("bus")){
			final SanralTollVehicleType tvType = tag ? SanralTollVehicleType.busWithTag : SanralTollVehicleType.busWithoutTag;
			tollVehicleTypes.put( idObj, tvType ) ;
			return tvType;
			
		} else if(subpopulation.equalsIgnoreCase("taxi")){
			final SanralTollVehicleType tvType = tag ? SanralTollVehicleType.taxiWithTag : SanralTollVehicleType.taxiWithoutTag;
			tollVehicleTypes.put( idObj, tvType ) ;
			return tvType;
			
		} else if(subpopulation.equalsIgnoreCase("ext")){
			final SanralTollVehicleType tvType = tag ? SanralTollVehicleType.extWithTag : SanralTollVehicleType.extWithoutTag;
			tollVehicleTypes.put( idObj, tvType ) ;
			return tvType;
			
		} else{
			throw new RuntimeException("Unknown subpopulation type " + subpopulation);
		}
	}

}
