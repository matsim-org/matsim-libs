/* *********************************************************************** *
 * project: org.matsim.*
 * FhEmissions.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
 *                                                                         
 * *********************************************************************** */
package playground.fhuelsmann.emission.objects;

/** 
 *  @author friederike *
 * Hot value class has the array value, which contains the KM, Velocity , EFA etc..
 * in order to read the data, you should use getValue()[index] the index is the column index.
 * for example .getValue()[3] returns the value of the velocity.
 * Hotvalue is used in the Hashmap in the class HefaHot
 *
 *
 *Component	RoadCat	TrafficSit	Gradient	IDSubsegment	Subsegment	Technology	SizeClasse	EmConcept	KM	%OfSubsegment	V	V_0%	V_100%	EFA	EFA_0%	EFA_100%	V_weighted	V_weighted_0%	V_weighted_100%	EFA_weighted	EFA_weighted_0%	EFA_weighted_100%
FC		RUR/MW/80/Freeflow	0%	111100	PC petrol <1,4L <ECE	petrol (4S)	<1,4L	PC-P-Euro-0	50000,00	1,00	82,80			45,03								
FC		RUR/MW/80/Freeflow	0%	111101	PC petrol <1,4L ECE-15'00	petrol (4S)	<1,4L	PC-P-Euro-0	50000,00	1,00	82,80			45,03								
FC		RUR/MW/80/Freeflow	0%	111102	PC petrol <1,4L ECE-15'01/02	petrol (4S)	<1,4L	PC-P-Euro-0	50000,00	1,00	82,80			45,03								
FC		RUR/MW/80/Freeflow	0%	111103	PC petrol <1,4L ECE-15'03	petrol (4S)	<1,4L	PC-P-Euro-0	50000,00	1,00	82,80			45,03								
FC		RUR/MW/80/Freeflow	0%	111104	PC petrol <1,4L ECE-15'04	petrol (4S)	<1,4L	PC-P-Euro-0	50000,00	1,00	82,80			44,58								
FC		RUR/MW/80/Freeflow	0%	111105	PC petrol <1,4L AGV82 (CH)	petrol (4S)	<1,4L	PC-P-Euro-0	50000,00	1,00	82,80			44,58								
FC		RUR/MW/80/Freeflow	0%	111106	PC petrol <1,4L conv other concepts	petrol (4S)	<1,4L	PC-P-Euro-0	50000,00	1,00	82,80			44,58								
FC		RUR/MW/80/Freeflow	0%	111107	PC petrol <1,4L Ucat	petrol (4S)	<1,4L	PC-P-Euro-0	50000,00	1,00	82,80			44,58								
FC		RUR/MW/80/Freeflow	0%	111112	PC petrol <1,4L PreEuro 3WCat <1987	petrol (4S)	<1,4L	PC-P-Euro-0	50000,00	1,00	82,80			44,14								
FC		RUR/MW/80/Freeflow	0%	111113	PC petrol <1,4L PreEuro 3WCat 1987-90	petrol (4S)	<1,4L	PC-P-Euro-0	50000,00	1,00	82,80			43,27								
FC		RUR/MW/80/Freeflow	0%	111110	PC petrol <1,4L Euro-1	petrol (4S)	<1,4L	PC-P-Euro-1	50000,00	1,00	82,80			41,58								
FC		RUR/MW/80/Freeflow	0%	111120	PC petrol <1,4L Euro-2	petrol (4S)	<1,4L	PC-P-Euro-2	50000,00	1,00	82,80			39,54								
4	5	6	7	8	9	10	11	12	13	14	15	16	17	18	19	20	21	22	23	24	25	26
 */

public class HbefaWarmEmissionFactorsDetailed {

	private String[] value;
	
	public HbefaWarmEmissionFactorsDetailed(String[] value){
		super();
		setValue(value);
	}

	public String[] getValue() {
		return value;
	}

	public void setValue(String[] value) {
		this.value = value;
	}
	public double getV(){
		try{
			String[] num = value[0].split(",");
			String newNumber = num[0] + "." + num[1] ;
			return Double.valueOf(newNumber);
		}catch(Exception e){
			System.out.println("V " + e);
		}
		return Double.valueOf(value[0]);
	}
	
	public double getEFA(){
		try{
			String[] num = value[1].split(",");
			String newNumber = num[0] + "." + num[1] ;
			return Double.valueOf(newNumber);
		}
		catch(Exception e){
			System.out.println("EFA " +  e);
			}
		return Double.valueOf(value[1]);
	}
}