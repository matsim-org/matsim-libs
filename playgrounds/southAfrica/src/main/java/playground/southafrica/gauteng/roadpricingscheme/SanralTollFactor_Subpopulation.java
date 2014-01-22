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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;

public class SanralTollFactor_Subpopulation implements TollFactorI {
	private final Logger log = Logger.getLogger(SanralTollFactor_Subpopulation.class);
	private Scenario sc;
	
	public SanralTollFactor_Subpopulation(Scenario scenario) {
		this.sc = scenario;
	}
	
	
	private  double getTollFactor(final Id vehicleId, final Id linkId, final double time){		
		double timeDiscount = getTimeDiscount(time);
		double tagDiscount = 0.00;
		double ptDiscount = 0.00;
		double sizeFactor = 1.00;
		
		switch( typeOf( vehicleId) ) {
		case carWithTag:
			tagDiscount = 0.483;
			break ;
		case carWithoutTag:
			// nothing
			break ;
		case commercialClassAWithTag:
			tagDiscount = 0.483;
			break;
		case commercialClassAWithoutTag:
			// nothing
			break ;
		case commercialClassBWithTag:
			sizeFactor = 2.5;
			tagDiscount = 0.483;
			break ;
		case commercialClassBWithoutTag:
			sizeFactor = 2.5;
			break ;
		case commercialClassCWithTag:
			sizeFactor = 5;
			tagDiscount = 0.483;
			break ;
		case commercialClassCWithoutTag:
			sizeFactor = 5;
			break ;
		case busWithTag:
			sizeFactor = 2.5;
			tagDiscount = 0.483;
			ptDiscount = 0.55;
			break ;
		case busWithoutTag:
			sizeFactor = 2.5;
			ptDiscount = 0.55;
			break ;
		case taxiWithTag:
			tagDiscount = 0.483;
			ptDiscount = 0.30;
			break ;
		case taxiWithoutTag:
			ptDiscount = 0.30;
			break ;
		case extWithTag:
			tagDiscount = 0.483;
			break ;
		case extWithoutTag:
			// nothing
			break ;
		}
		return getDiscountEligibility(linkId) ? sizeFactor*(1 - Math.min(1.0, timeDiscount + tagDiscount + ptDiscount)) : sizeFactor;
		
	}
	
	/* (non-Javadoc)
	 * @see playground.southafrica.gauteng.roadpricingscheme.TollFactorI#getTollFactor(org.matsim.api.core.v01.population.Person, org.matsim.api.core.v01.Id, double)
	 */
	@Override
	public  double getTollFactor(final Person person, final Id linkId, final double time) {
		// yyyyyy aaarrrrgh ... (assuming vehId = personId).  kai, mar'12
		Id vehicleId = person.getId() ;
		return getTollFactor(vehicleId, linkId, time);
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

	/**
	 * Checks the subpopulation attributes and assigns the vehicle type 
	 * accordingly.
	 */
	@Override
	public SanralTollVehicleType typeOf(Id idObj) {
		/* Check subpopulation. */
		Object o1 = this.sc.getPopulation().getPersonAttributes().getAttribute(idObj.toString(), sc.getConfig().plans().getSubpopulationAttributeName());
		String subpopulation;
		if(o1 instanceof String){
			subpopulation = (String)o1;
		} else{
			log.error("Expected a subppulation description of type `String', but it was `" + o1.getClass().toString() + "'. Returning NULL");
			return null;
		}
		
		/* Check vehicle type. */
		Object o2 = this.sc.getPopulation().getPersonAttributes().getAttribute(idObj.toString(), "vehicleTollClass");
		String vehicleType;
		if(o2 instanceof String){
			vehicleType = (String)o2;
		} else{
			log.error("Expected a vehicle type description of type `String', but it was `" + o2.getClass().toString() + "'. Returning NULL");
			return null;
		}
		
		/* Check availability of eTag. */
		Object o3 = this.sc.getPopulation().getPersonAttributes().getAttribute(idObj.toString(), "eTag");
		Boolean tag = false;
		if(o3 instanceof Boolean){
			tag = (Boolean)o3;
		} else{
			log.error("Expected an eTag availability of type `Boolean', but it was `" + o3.getClass().toString() + "'. Returning NULL");
			return null;
		}
	
		/* Identify correct vehicle type. */
		if(subpopulation.equalsIgnoreCase("car")){
			return tag ? SanralTollVehicleType.carWithTag : SanralTollVehicleType.carWithoutTag;

		} else if(subpopulation.equalsIgnoreCase("commercial")){
			if(vehicleType.equalsIgnoreCase("A2")){
				return tag ? SanralTollVehicleType.commercialClassAWithTag : SanralTollVehicleType.commercialClassAWithoutTag;
			} else if(vehicleType.equalsIgnoreCase("B")){
				return tag ? SanralTollVehicleType.commercialClassBWithTag : SanralTollVehicleType.commercialClassBWithoutTag;
			} else if(vehicleType.equalsIgnoreCase("C")){
				return tag ? SanralTollVehicleType.commercialClassCWithTag : SanralTollVehicleType.commercialClassCWithoutTag;
			} else{
				throw new RuntimeException("Unknown vehicle type " + vehicleType);
			}
			
		} else if(subpopulation.equalsIgnoreCase("bus")){
			return tag ? SanralTollVehicleType.busWithTag : SanralTollVehicleType.busWithoutTag;
			
		} else if(subpopulation.equalsIgnoreCase("taxi")){
			return tag ? SanralTollVehicleType.taxiWithTag : SanralTollVehicleType.taxiWithoutTag;
			
		} else if(subpopulation.equalsIgnoreCase("ext")){
			return tag ? SanralTollVehicleType.extWithTag : SanralTollVehicleType.extWithoutTag;
			
		} else{
			throw new RuntimeException("Unknown subpopulation type " + subpopulation);
		}
	}

}
