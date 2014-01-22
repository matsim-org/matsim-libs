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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class SanralTollFactor implements TollFactorI {
	private final  int carStartId = 0;
	private final  int carEndId = 157517;
	private final  int comIntraStartId = 1100000;
	private final  int comIntraEndId = 1199999;
	private final  int comInterInOutStartId = 1200000;
	private final  int comInterInOutEndId = 1299999;
	private final  int comInterOutInStartId = 1300000;
	private final  int comInterOutInEndId = 1399999;
	private final  int busStartId = 2000000;
	private final  int busEndId = 2000209;
	private final  int taxiStartId = 3000000;
	private final  int taxiEndId = 3008133;
	private final  int extStartId = 4000000;
	private final  int extEndId = 4020181;
	
	/* (non-Javadoc)
	 * @see playground.southafrica.gauteng.roadpricingscheme.TollFactorI#typeOf(org.matsim.api.core.v01.Id)
	 */
    @Override
	public  SanralTollVehicleType typeOf ( Id idObj ) {
		long id = Long.parseLong(idObj.toString());

		if (id < carStartId + (carEndId - carStartId)*TagPenetration.CAR) { 
			/* It is a private vehicle with a tag. */
			return SanralTollVehicleType.carWithTag ;
		} else if (id <= carEndId) {
			/* It is a private car without a tag. */
			return SanralTollVehicleType.carWithoutTag ;
		} else if(id < comIntraStartId + fractionCommercialClassAIntra*(comIntraEndId - comIntraStartId)*TagPenetration.COMMERCIAL ||
				id < comInterInOutStartId + fractionCommercialClassAInter*(comInterInOutEndId - comInterInOutStartId)*TagPenetration.COMMERCIAL ||
				id < comInterOutInStartId + fractionCommercialClassAInter*(comInterOutInEndId - comInterOutInStartId)*TagPenetration.COMMERCIAL ){
			/* It is a Class A commercial vehicle with a tag. */
			return SanralTollVehicleType.commercialClassAWithTag;
		} else if(id < comIntraStartId + fractionCommercialClassAIntra*(comIntraEndId - comIntraStartId) ||
				id < comInterInOutStartId + fractionCommercialClassAInter*(comInterInOutEndId - comInterInOutStartId) ||
				id < comInterOutInStartId + fractionCommercialClassAInter*(comInterOutInEndId - comInterOutInStartId) ){
			/* It is a Class A commercial vehicle without a tag. */
			return SanralTollVehicleType.commercialClassAWithoutTag;
		} else if(id < (comIntraStartId + fractionCommercialClassAIntra*(comIntraEndId - comIntraStartId)) + fractionCommercialClassBIntra*(comIntraEndId - comIntraStartId)*TagPenetration.COMMERCIAL ||
				  id < (comInterInOutStartId + fractionCommercialClassAInter*(comInterInOutEndId - comInterInOutStartId)) + fractionCommercialClassBInter*(comInterInOutEndId - comInterInOutStartId)*TagPenetration.COMMERCIAL ||
				  id < (comInterOutInStartId + fractionCommercialClassAInter*(comInterOutInEndId - comInterOutInStartId)) + fractionCommercialClassBInter*(comInterOutInEndId - comInterOutInStartId)*TagPenetration.COMMERCIAL ){
			/* It is a Class B commercial vehicle with a tag. */
			return SanralTollVehicleType.commercialClassBWithTag;
		} else if(id < (comIntraStartId + fractionCommercialClassAIntra*(comIntraEndId - comIntraStartId)) + fractionCommercialClassBIntra*(comIntraEndId - comIntraStartId) ||
				id < (comInterInOutStartId + fractionCommercialClassAInter*(comInterInOutEndId - comInterInOutStartId)) + fractionCommercialClassBInter*(comInterInOutEndId - comInterInOutStartId) ||
				id < (comInterOutInStartId + fractionCommercialClassAInter*(comInterOutInEndId - comInterOutInStartId)) + fractionCommercialClassBInter*(comInterOutInEndId - comInterOutInStartId) ){
			/* It is a Class B commercial vehicle without a tag. */
			return SanralTollVehicleType.commercialClassBWithoutTag;
		} else if(id < (comIntraStartId + (fractionCommercialClassAIntra + fractionCommercialClassBIntra)*(comIntraEndId - comIntraStartId)) + fractionCommercialClassCIntra*(comIntraEndId - comIntraStartId)*TagPenetration.COMMERCIAL ||
				id < (comInterInOutStartId + (fractionCommercialClassAInter + fractionCommercialClassBInter)*(comInterInOutEndId - comInterInOutStartId)) + fractionCommercialClassCInter*(comInterInOutEndId - comInterInOutStartId)*TagPenetration.COMMERCIAL ||
				id < (comInterOutInStartId + (fractionCommercialClassAInter + fractionCommercialClassBInter)*(comInterOutInEndId - comInterOutInStartId)) + fractionCommercialClassCInter*(comInterOutInEndId - comInterOutInStartId)*TagPenetration.COMMERCIAL ){
			/* It is a Class C commercial vehicle with a tag. */
			return SanralTollVehicleType.commercialClassCWithTag;
		} else if(id <= comIntraEndId ||
				  id <= comInterInOutEndId ||
				  id <= comInterOutInEndId){
			/* It is a Class C commercial vehicle without a tag. */
			return SanralTollVehicleType.commercialClassCWithoutTag;
		} else if (id < busStartId + (busEndId - busStartId)*TagPenetration.BUS) {
			/* It is a bus with a tag. */
			return SanralTollVehicleType.busWithTag ;
		} else if (id <= busEndId){
			/* It is a bus without a tag. */
			return SanralTollVehicleType.busWithoutTag ;
		} else if (id < taxiStartId + (taxiEndId - taxiStartId)*TagPenetration.TAXI){
			/* It is a minibus taxi with a tag. */
			return SanralTollVehicleType.taxiWithTag ;
		} else if (id <= taxiEndId) {
			/* It is a minibus taxi without a tag. */
			return SanralTollVehicleType.taxiWithoutTag ;
		} else if (id < extStartId + (extEndId - extStartId)*TagPenetration.EXTERNAL){
			/* It is an external (light) vehicle with a tag. */
			return SanralTollVehicleType.extWithTag ;
		} else {
			/* It is an external (light) vehicle without a tag.*/
			return SanralTollVehicleType.extWithoutTag ;
		}

	}
    
    
	
	/* From the counting station data I've inferred that the split between 
	 * Class B and C is about 50:50, assuming that `Short' represents Class B, 
	 * and `Medium' and `Long' combined represent Class C vehicles. */
	private final  double fractionCommercialClassAIntra = 0.50;
	private final  double fractionCommercialClassBIntra = 0.30;
	private final  double fractionCommercialClassCIntra = 0.20;

	private final  double fractionCommercialClassAInter = 0.30;
	private final  double fractionCommercialClassBInter = 0.40;
	private final  double fractionCommercialClassCInter = 0.30;
	
	
	private  double getTollFactor(final Id vehicleId, final Id linkId, final double time){		
		double timeDiscount = getTimeDiscount(time);
		double tagDiscount = 0.00;
		double ptDiscount = 0.00;
		double sizeFactor = 1.00;
		
		switch( typeOf( vehicleId) ) {
		case carWithTag:
			tagDiscount = 0.25;
			break ;
		case carWithoutTag:
			// nothing
			break ;
		case commercialClassAWithTag:
			tagDiscount = 0.25;
			break;
		case commercialClassAWithoutTag:
			// nothing
			break ;
		case commercialClassBWithTag:
			sizeFactor = 2.5;
			tagDiscount = 0.25;
			break ;
		case commercialClassBWithoutTag:
			sizeFactor = 2.5;
			break ;
		case commercialClassCWithTag:
			sizeFactor = 5;
			tagDiscount = 0.25;
			break ;
		case commercialClassCWithoutTag:
			sizeFactor = 5;
			break ;
		case busWithTag:
			sizeFactor = 2.5;
			tagDiscount = 0.25;
			ptDiscount = 0.55;
			break ;
		case busWithoutTag:
			sizeFactor = 2.5;
			ptDiscount = 0.55;
			break ;
		case taxiWithTag:
			tagDiscount = 0.25;
			ptDiscount = 0.30;
			break ;
		case taxiWithoutTag:
			ptDiscount = 0.30;
			break ;
		case extWithTag:
			tagDiscount = 0.25;
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
	
	
	private  double getTimeDiscount(double time){
		/* First get the real time of day. */
		double timeOfDay = time;
		while(timeOfDay > 86400){
			timeOfDay -= 86400;
		}
		
		/* Weekday discounts */
		if(timeOfDay < 18000){ // 00:00 - 05:00
			return 0.20;
		} else if(timeOfDay < 21600){ // 05:00 - 06:00
			return 0.10;
		} else if(timeOfDay < 64800){ // 06:00 - 18:00
			return 0.00;
		} else if(timeOfDay < 82800){ // 18:00 - 23:00
			return 0.10;
		} else{ // 23:00 - 00:00
			return 0.20;
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
}
