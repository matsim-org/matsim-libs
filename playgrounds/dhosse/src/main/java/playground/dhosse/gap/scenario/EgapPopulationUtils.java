package playground.dhosse.gap.scenario;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.TransportMode;

import playground.dhosse.gap.scenario.mid.MiDPersonGroupData;
import playground.dhosse.utils.EgapHashGenerator;

/**
 * Some utilities for creating the population of the eGAP scenario.
 * 
 * @author dhosse
 *
 */
public class EgapPopulationUtils {
	
	//age distribution
	private static final double thresholdAge0_9 = 0.1004;
	private static final double thresholdAge10_19 = 0.1598;
	private static final double thresholdAge20_29 = 0.089;
	private static final double thresholdAge30_39 = 0.0955;
	private static final double thresholdAge40_49 = 0.205;
	private static final double thresholdAge50_59 = 0.1589;
	private static final double thresholdAge60_69 = 0.1098;
	private static final double thresholdAge70_79 = 0.0643;
	
	//transport mode distribution
	private static final double thresholdWalk = 0.2029384757;
	private static final double thresholdBike = 0.269768391;
	private static final double thresholdRide = 0.446587083;
	private static final double thresholdCar = 0.9345985104;
	
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
		
		for(int age = 0; age < 90; age += 10){
			
			MiDPersonGroupData male = new MiDPersonGroupData(age, age + 9, 0);
			male.setLegsPerPersonAndDay(setLegsPerPersonAndDay(age, 0));
			setPercentages(male);
			EgapPopulationUtils.personGroupData.put(EgapHashGenerator.generatePersonGroupHash(age, 0), male);
			
			MiDPersonGroupData female = new MiDPersonGroupData(age, age + 9, 1);
			female.setLegsPerPersonAndDay(setLegsPerPersonAndDay(age, 1));
			setPercentages(female);
			EgapPopulationUtils.personGroupData.put(EgapHashGenerator.generatePersonGroupHash(age, 1), female);
			
		}

		return EgapPopulationUtils.personGroupData;
		
	}
	
	/**
	 * Sets the percentages for employment, driving license possession and car availability for a given person group
	 * of an MiD survey.
	 * 
	 * @param pgd person group data
	 */
	private static void setPercentages(MiDPersonGroupData pgd){
		
		if(pgd.getAX() < 10){
			
			pgd.setpEmployment(0.4941451991);
			pgd.setpLicense(0.);
			pgd.setpCarAvail(0.);
			
		} else if(pgd.getA0() >= 10 && pgd.getAX() < 20){
			
			pgd.setpEmployment(0.9899856938);
			pgd.setpLicense(0.2253218884);
			pgd.setpCarAvail(0.8063492063);
			
		} else if(pgd.getA0() >= 20 && pgd.getAX() < 30){
			
			pgd.setpEmployment(0.9312638581);
			pgd.setpLicense(0.9567627494);
			pgd.setpCarAvail(0.8505214368);
			
		} else if(pgd.getA0() >= 30 && pgd.getAX() < 40){
			
			pgd.setpEmployment(0.8368725869);
			pgd.setpLicense(0.9951737452);
			pgd.setpCarAvail(0.9291949564);
			
		} else if(pgd.getA0() >= 40 && pgd.getAX() < 50){
			
			pgd.setpEmployment(0.8701566364);
			pgd.setpLicense(0.9855729596);
			pgd.setpCarAvail(0.9297365119);
			
		} else if(pgd.getA0() >= 50 && pgd.getAX() < 60){
			
			pgd.setpEmployment(0.7721837634);
			pgd.setpLicense(0.9704216488);
			pgd.setpCarAvail(0.9260700389);
			
		} else if(pgd.getA0() >= 60 && pgd.getAX() < 70){
			
			pgd.setpEmployment(0.1798631476);
			pgd.setpLicense(0.9169110459);
			pgd.setpCarAvail(0.8880597015);
			
		} else if(pgd.getA0() >= 70 && pgd.getAX() < 80){
			
			pgd.setpEmployment(0.0319361277);
			pgd.setpLicense(0.8363273453);
			pgd.setpCarAvail(0.9021479714);
			
		} else{
			
			pgd.setpEmployment(0.);
			pgd.setpLicense(0.7466666667);
			pgd.setpCarAvail(0.7142857143);
			
		}
		
	}
	
	private static double setLegsPerPersonAndDay(int age, int sex){
		
		if(age < 10){
			
			if(sex == 0){
				
				return 2.7828947368;
				
			} else{
				
				return 2.7452229299;
				
			}
			
		} else if(age >= 10 && age < 20){
			
			if(sex == 0){
				
				return 2.8016528926;
				
			} else{
				
				return 2.876;
				
			}
			
		} else if(age >= 20 && age < 30){
			
			if(sex == 0){
				
				return 3.7676056338;
				
			} else{
				
				return 2.7803030303;
				
			}
			
		} else if(age >= 30 && age < 40){
			
			if(sex == 0){
				
				return 3.5703703704;
				
			} else{
				
				return 3.4842767296;
				
			}
			
		} else if(age >= 40 && age < 50){
			
			if(sex == 0){
				
				return 3.9266666667;
				
			} else{
				
				return 3.7703927492;
				
			}
			
		} else if(age >= 50 && age < 60){
		
			if(sex == 0){
				
				return 3.2398373984;
				
			} else{
				
				return 3.2592592593;
				
			}
			
		} else if(age >= 60 && age < 70){
			
			if(sex == 0){
				
				return 3.1444444444;
				
			} else{
				
				return 2.8797468354;
				
			}
			
		} else if(age >= 70 && age < 80){
			
			if(sex == 0){
				
				return 2.7363636364;
				
			} else{
				
				return 2.2727272727;
				
			}
			
		} else{
			
			if(sex == 0){
				
				return 1.5925925926;
				
			} else{
				
				return 1.2307692308;
				
			}
			
		}
		
	}
	
	/**
	 * Sets the age of an agent according to MiD survey data.
	 * 
	 * @return An integer representing the age of the agent
	 */
	public static int setAge(){
		
		double rnd = GAPMain.random.nextDouble();
		
		if(rnd <= thresholdAge0_9){
			
			return (int) (0 + GAPMain.random.nextDouble() * 9);
			
		} else if(rnd > thresholdAge0_9 && rnd <= thresholdAge10_19){
			
			return (int) (10 + GAPMain.random.nextDouble() * 9);
			
		} else if(rnd > thresholdAge10_19 && rnd <= thresholdAge20_29){
			
			return (int) (20 + GAPMain.random.nextDouble() * 9);
			
		} else if(rnd > thresholdAge20_29 && rnd <= thresholdAge30_39){
			
			return (int) (30 + GAPMain.random.nextDouble() * 9);
			
		} else if(rnd > thresholdAge30_39 && rnd <= thresholdAge40_49){
			
			return (int) (40 + GAPMain.random.nextDouble() * 9);
			
		} else if(rnd > thresholdAge40_49 && rnd <= thresholdAge50_59){
			
			return (int) (50 + GAPMain.random.nextDouble() * 9);
			
		} else if(rnd > thresholdAge50_59 && rnd <= thresholdAge60_69){
			
			return (int) (60 + GAPMain.random.nextDouble() * 9);
			
		} else if(rnd > thresholdAge60_69 && rnd <= thresholdAge70_79){
			
			return (int) (70 + GAPMain.random.nextDouble() * 9);
			
		} else {
			
			return (int) (80 + GAPMain.random.nextDouble() * 9);
			
		}
		
	}
	
	/**
	 * Sets an agent's sex according to MiD survey data. 
	 * 
	 * @param age the agent's age
	 * @return An integer representing the sex of the agent (0 = male, 1 = female)
	 */
	public static int setSex(int age){
		
		double rnd = GAPMain.random.nextDouble();
		
		if(age < 10){
			
			if(rnd <= 0.4919){
				
				return 0;
				
			} else{
				
				return 1;
				
			}
			
		} else if(age >= 10 && age < 20){
			
			if(rnd <= 0.4919){
				
				return 0;
				
			} else{
				
				return 1;
				
			}
			
		} else if(age >= 20 && age < 30){
			
			if(rnd <= 0.5182){
				
				return 0;
				
			} else{
				
				return 1;
				
			}
			
		} else if(age >= 30 && age < 40){
			
			if(rnd <= 0.4592){
				
				return 0;
				
			} else{
				
				return 1;
				
			}
			
		} else if(age >= 40 && age < 50){
			
			if(rnd <= 0.4754){
				
				return 0;
				
			} else{
				
				return 1;
				
			}
			
		} else if(age >= 50 && age < 60){
			
			if(rnd <= 0.5031){
				
				return 0;
				
			} else{
				
				return 1;
				
			}
			
		} else if(age >= 60 && age < 70){
			
			if(rnd <= 0.5325){
				
				return 0;
				
			} else{
				
				return 1;
				
			}
			
		} else if(age >= 70 && age < 80){
			
			if(rnd <= 0.5556){
				
				return 0;
				
			} else{
				
				return 1;
				
			}
			
		} else{
			
			if(rnd <= 0.5094){
				
				return 0;
				
			} else{
				
				return 1;
				
			}
			
		}
		
	}
	
	/**
	 * Sets the transport mode of an agent's leg according to the modal split surveyed in MiD.
	 * 
	 * @param traveldistance The beeline distance between the activity locations
	 * @return The string representation of the leg mode.
	 */
	
	public static String setLegModeForPerson(double traveldistance){
		
		String mode = "";
		
		do{
			
			double weight = GAPMain.random.nextDouble();
		
			if(weight <= thresholdWalk){
				
				if(traveldistance < 5000){
					
					mode = TransportMode.walk;
					
				}
				
			} else if(weight > thresholdWalk && weight <= thresholdBike){
				
				if(traveldistance < 15000){
					
					mode = TransportMode.bike;
					
				}
				
			} else if(weight > thresholdBike && weight <= thresholdRide){
				
				mode = TransportMode.ride;
				
			} else if(weight > thresholdRide && weight <= thresholdCar){
				
				mode = TransportMode.car;
				
			} else{
				
				mode = TransportMode.pt;
				
			}
			
		} while(mode.equals(""));
		
		return mode;
		
	}

}
