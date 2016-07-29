package playground.dhosse.gap.scenario.population.utils;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.TransportMode;

import playground.dhosse.gap.Global;
import playground.dhosse.scenarios.generic.population.HashGenerator;
import playground.dhosse.scenarios.generic.population.io.mid.MiDPersonGroupData;

public class EgapPopulationUtilsV2 {
	
		static Map<String, MiDPersonGroupData> personGroupData = new HashMap<>();
		
		/**
		 * 
		 * Creates person groups according to MiD survey data. Each person group consists of
		 * <ul>
		 * <li>age segment of ten years</li>
		 * <li>sex of the members</li>
		 * <li>legs per person and day</li>
		 * </ul>
		 * 
		 */
		public static Map<String,MiDPersonGroupData> createMiDPersonGroups(){
			
			MiDPersonGroupData students = new MiDPersonGroupData(6, 17);
			students.setLegsPerPersonAndDay(2.81);
			setPercentages(students);
			EgapPopulationUtilsV2.personGroupData.put(HashGenerator.generateAgeGroupHash(6, 17), students);
			
			MiDPersonGroupData adults = new MiDPersonGroupData(18, 65);
			students.setLegsPerPersonAndDay(3.44);
			setPercentages(adults);
			EgapPopulationUtilsV2.personGroupData.put(HashGenerator.generateAgeGroupHash(18, 65), adults);
			
			MiDPersonGroupData pensioners = new MiDPersonGroupData(66, 100);
			pensioners.setLegsPerPersonAndDay(2.29);
			setPercentages(pensioners);
			EgapPopulationUtilsV2.personGroupData.put(HashGenerator.generateAgeGroupHash(66, 100), pensioners);
			
			return EgapPopulationUtilsV2.personGroupData;
			
		}
		
		/**
		 * Sets the percentages for employment, driving license possession and car availability for a given person group
		 * of an MiD survey.
		 * 
		 * @param pgd person group data
		 */
		private static void setPercentages(MiDPersonGroupData pgd){
			
			if(pgd.getAX() < 18){
				
				pgd.setpEmployment(0.0);
				pgd.setpLicense(0.0351053159);
				pgd.setpCarAvail(0.8974358974);	
				
				pgd.setpWorkLegs(0.0);
				pgd.setpEducationLegs(.29);
				pgd.setpShopLegs(0.07);
				pgd.setpLeisureLegs(0.41);
				pgd.setpOtherLegs(0.23);
				
			} else if(pgd.getAX() >= 18 && pgd.getAX() < 66){
				
				pgd.setpEmployment(0.8056500518);
				pgd.setpLicense(0.9726371838);
				pgd.setpCarAvail(0.7813260341);
				
				pgd.setpWorkLegs(0.28);
				pgd.setpEducationLegs(0.01);
				pgd.setpShopLegs(0.2);
				pgd.setpLeisureLegs(0.28);
				pgd.setpOtherLegs(0.23);
				
			} else{
				
				pgd.setpEmployment(0.0);
				pgd.setpLicense(0.8580889309);
				pgd.setpCarAvail(0.815876516);
				
				pgd.setpWorkLegs(0.0);
				pgd.setpEducationLegs(0.0);
				pgd.setpShopLegs(0.32);
				pgd.setpLeisureLegs(0.41);
				pgd.setpOtherLegs(0.27);
				
			}
				
			
		}
		
		public static String getLegModeForAgeGroupAndActType(int age, boolean license, boolean carAvail, String actType){
			
			if(age < 18){
				
				if(actType.equals(Global.ActType.education)){
					
					return getLegMode(0.24, 0.05, 0.23, 0.03, 0.45, license, carAvail);
					
				} else if(actType.equals(Global.ActType.shop)){
					
					return getLegMode(0.22, 0.12, 0.41, 0.22, 0.03, license, carAvail);
					
				} else if(actType.equals(Global.ActType.leisure)){
					
					return getLegMode(0.32, 0.12, 0.4, 0.08, 0.03, license, carAvail);
					
				} else{
					
					return getLegMode(0.26, 0.04, 0.6, 0.08, 0.03, license, carAvail);
					
				}
				
			} else if(age >= 18 && age < 66){
				
				if(actType.equals(Global.ActType.work)){
					
					return getLegMode(0.06, 0.05, 0.05, 0.78, 0.06, license, carAvail);
					
				} else if(actType.equals(Global.ActType.education)){
					
					return getLegMode(0.06, 0.11, 0.02, 0.56, 0.26, license, carAvail);
					
				} else if(actType.equals(Global.ActType.shop)){
					
					return getLegMode(0.17, 0.07, 0.11, 0.64, 0.01, license, carAvail);
					
				} else if(actType.equals(Global.ActType.leisure)){
					
					return getLegMode(0.31, 0.07, 0.17, 0.41, 0.04, license, carAvail);
					
				} else{
					
					return getLegMode(0.18, 0.06, 0.1, 0.64, 0.02, license, carAvail);
					
				}
				
			} else{
				
				if(actType.equals(Global.ActType.shop)){
					
					return getLegMode(0.2, 0.04, 0.15, 0.59, 0.02, license, carAvail);
					
				} else if(actType.equals(Global.ActType.leisure)){
					
					return getLegMode(0.32, 0.08, 0.21, 0.37, 0.02, license, carAvail);
					
				} else{
					
					return getLegMode(0.25, 0.03, 0.18, 0.48, 0.06, license, carAvail);
					
				}
				
			}
			
		}
		
		private static String getLegMode(double thresholdWalk, double thresholdBike, double thresholdRide, double thresholdCar, double thresholdPt,
				boolean license, boolean carAvail){
			
			double p = Global.random.nextDouble();

			if(!license || !carAvail){
				p *= (1 - thresholdCar);
			}
			
			double accumulatedWeight = thresholdWalk;
			
			if(p <= accumulatedWeight){
				
				return TransportMode.walk;
				
			} else{
				
				accumulatedWeight += thresholdBike;
				
				if(p <= accumulatedWeight){
					
					return TransportMode.bike;
					
				} else{
					
					accumulatedWeight += thresholdRide;
					
					if(p <= accumulatedWeight){
						
						return TransportMode.ride;
						
					} else{
						
						if(license && carAvail){
							
							accumulatedWeight += thresholdCar;
							
							if(p <= accumulatedWeight){
								
								return TransportMode.car;
								
							} else{
								
								return TransportMode.pt;
								
							}
							
						} else{
							
							return TransportMode.pt;
							
						}
						
					}
					
				}
				
			}
			
		}
		
}
