package playground.dhosse.gap.scenario.population.utils;

import java.util.HashMap;
import java.util.Map;

import playground.dhosse.gap.Global;
import playground.dhosse.scenarios.generic.population.HashGenerator;
import playground.dhosse.scenarios.generic.population.io.mid.MiDPersonGroupData;

/**
 * Some utilities for creating the population of the eGAP scenario.
 * 
 * @author dhosse
 *
 */
public class EgapPopulationUtils {
	
	//age distribution
	private static final double thresholdAge0_9 = 0.1083;
	private static final double thresholdAge10_19 = 0.1635;
	private static final double thresholdAge20_29 = 0.0892;
	private static final double thresholdAge30_39 = 0.098;
	private static final double thresholdAge40_49 = 0.2113;
	private static final double thresholdAge50_59 = 0.1566;
	private static final double thresholdAge60_69 = 0.1053;
	private static final double thresholdAge70_79 = 0.0559;
	
	private static final double thresholdsAge[] = {0.1083, 0.1635, 0.0892, 0.098, 0.2113, 0.1566, 0.1053, 0.0559, 0.0119};
	
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
			EgapPopulationUtils.personGroupData.put(HashGenerator.generatePersonGroupHash(age, 0), male);
			
			MiDPersonGroupData female = new MiDPersonGroupData(age, age + 9, 1);
			female.setLegsPerPersonAndDay(setLegsPerPersonAndDay(age, 1));
			setPercentages(female);
			EgapPopulationUtils.personGroupData.put(HashGenerator.generatePersonGroupHash(age, 1), female);
			
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
			
			if(pgd.getSex() == 0){
				
				pgd.setpWorkLegs(0.);
				pgd.setpEducationLegs(.2648);
				pgd.setpShopLegs(0.0449);
				pgd.setpLeisureLegs(0.3428);
				pgd.setpOtherLegs(0.3475);
				
			} else{
				
				pgd.setpWorkLegs(0.);
				pgd.setpEducationLegs(.2993);
				pgd.setpShopLegs(0.0371);
				pgd.setpLeisureLegs(0.3573);
				pgd.setpOtherLegs(0.3063);
				
			}
			
		} else if(pgd.getA0() >= 10 && pgd.getAX() < 20){
			
			pgd.setpEmployment(0.9899856938);
			pgd.setpLicense(0.2253218884);
			pgd.setpCarAvail(0.8063492063);
			
			if(pgd.getSex() == 0){
				
				pgd.setpWorkLegs(0.0619);
				pgd.setpEducationLegs(.292);
				pgd.setpShopLegs(0.0693);
				pgd.setpLeisureLegs(0.413);
				pgd.setpOtherLegs(0.1637);
				
			} else{
				
				pgd.setpWorkLegs(0.0501);
				pgd.setpEducationLegs(.2629);
				pgd.setpShopLegs(0.0876);
				pgd.setpLeisureLegs(0.4214);
				pgd.setpOtherLegs(0.178);
				
			}
			
		} else if(pgd.getA0() >= 20 && pgd.getAX() < 30){
			
			pgd.setpEmployment(0.9312638581);
			pgd.setpLicense(0.9567627494);
			pgd.setpCarAvail(0.8505214368);
			
			if(pgd.getSex() == 0){
				
				pgd.setpWorkLegs(0.2168);
				pgd.setpEducationLegs(.0561);
				pgd.setpShopLegs(0.0991);
				pgd.setpLeisureLegs(0.2953);
				pgd.setpOtherLegs(0.3327);
				
			} else{
				
				pgd.setpWorkLegs(0.2752);
				pgd.setpEducationLegs(.0518);
				pgd.setpShopLegs(0.1417);
				pgd.setpLeisureLegs(0.3215);
				pgd.setpOtherLegs(0.2098);
				
			}
			
		} else if(pgd.getA0() >= 30 && pgd.getAX() < 40){
			
			pgd.setpEmployment(0.8368725869);
			pgd.setpLicense(0.9951737452);
			pgd.setpCarAvail(0.9291949564);
			
			if(pgd.getSex() == 0){
				
				pgd.setpWorkLegs(0.3091);
				pgd.setpEducationLegs(.0041);
				pgd.setpShopLegs(0.1473);
				pgd.setpLeisureLegs(0.2801);
				pgd.setpOtherLegs(0.2593);
				
			} else{
				
				pgd.setpWorkLegs(0.1715);
				pgd.setpEducationLegs(0.);
				pgd.setpShopLegs(0.213);
				pgd.setpLeisureLegs(0.3375);
				pgd.setpOtherLegs(0.278);
				
			}
			
		} else if(pgd.getA0() >= 40 && pgd.getAX() < 50){
			
			pgd.setpEmployment(0.8701566364);
			pgd.setpLicense(0.9855729596);
			pgd.setpCarAvail(0.9297365119);
			
			if(pgd.getSex() == 0){
				
				pgd.setpWorkLegs(0.2479);
				pgd.setpEducationLegs(0.);
				pgd.setpShopLegs(0.1511);
				pgd.setpLeisureLegs(0.2521);
				pgd.setpOtherLegs(0.3489);
				
			} else{
				
				pgd.setpWorkLegs(0.1538);
				pgd.setpEducationLegs(0.);
				pgd.setpShopLegs(0.2516);
				pgd.setpLeisureLegs(0.2067);
				pgd.setpOtherLegs(0.3878);
				
			}
			
		} else if(pgd.getA0() >= 50 && pgd.getAX() < 60){
			
			pgd.setpEmployment(0.7721837634);
			pgd.setpLicense(0.9704216488);
			pgd.setpCarAvail(0.9260700389);
			
			if(pgd.getSex() == 0){
				
				pgd.setpWorkLegs(0.2673);
				pgd.setpEducationLegs(0.0025);
				pgd.setpShopLegs(0.1568);
				pgd.setpLeisureLegs(0.2427);
				pgd.setpOtherLegs(0.3262);
				
			} else{
				
				pgd.setpWorkLegs(0.1692);
				pgd.setpEducationLegs(0.);
				pgd.setpShopLegs(0.25);
				pgd.setpLeisureLegs(0.3119);
				pgd.setpOtherLegs(0.2689);
				
			}
			
		} else if(pgd.getA0() >= 60 && pgd.getAX() < 70){
			
			pgd.setpEmployment(0.1798631476);
			pgd.setpLicense(0.9169110459);
			pgd.setpCarAvail(0.8880597015);
			
			if(pgd.getSex() == 0){
				
				pgd.setpWorkLegs(0.0424);
				pgd.setpEducationLegs(.0018);
				pgd.setpShopLegs(0.2898);
				pgd.setpLeisureLegs(0.3498);
				pgd.setpOtherLegs(0.3163);
				
			} else{
				
				pgd.setpWorkLegs(0.0769);
				pgd.setpEducationLegs(0.);
				pgd.setpShopLegs(0.2681);
				pgd.setpLeisureLegs(0.3604);
				pgd.setpOtherLegs(0.2945);
				
			}
			
		} else if(pgd.getA0() >= 70 && pgd.getAX() < 80){
			
			pgd.setpEmployment(0.0319361277);
			pgd.setpLicense(0.8363273453);
			pgd.setpCarAvail(0.9021479714);
			
			if(pgd.getSex() == 0){
				
				pgd.setpWorkLegs(0.0066);
				pgd.setpEducationLegs(0.);
				pgd.setpShopLegs(0.299);
				pgd.setpLeisureLegs(0.4319);
				pgd.setpOtherLegs(0.2625);
				
			} else{
				
				pgd.setpWorkLegs(0.015);
				pgd.setpEducationLegs(0.);
				pgd.setpShopLegs(0.355);
				pgd.setpLeisureLegs(0.33);
				pgd.setpOtherLegs(0.3);
				
			}
			
		} else{
			
			pgd.setpEmployment(0.);
			pgd.setpLicense(0.7466666667);
			pgd.setpCarAvail(0.7142857143);
			
			if(pgd.getSex() == 0){
				
				pgd.setpWorkLegs(0.0233);
				pgd.setpEducationLegs(0.);
				pgd.setpShopLegs(0.3023);
				pgd.setpLeisureLegs(0.3953);
				pgd.setpOtherLegs(0.2791);
				
			} else{
				
				pgd.setpWorkLegs(0.);
				pgd.setpEducationLegs(0.);
				pgd.setpShopLegs(0.3125);
				pgd.setpLeisureLegs(0.5938);
				pgd.setpOtherLegs(0.0938);
				
			}
			
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
		
		double rnd = Global.random.nextDouble();
		
		if(rnd <= thresholdAge0_9){
			
			return (int) (0 + Global.random.nextDouble() * 9);
			
		} else if(rnd > thresholdAge0_9 && rnd <= thresholdAge10_19){
			
			return (int) (10 + Global.random.nextDouble() * 9);
			
		} else if(rnd > thresholdAge10_19 && rnd <= thresholdAge20_29){
			
			return (int) (20 + Global.random.nextDouble() * 9);
			
		} else if(rnd > thresholdAge20_29 && rnd <= thresholdAge30_39){
			
			return (int) (30 + Global.random.nextDouble() * 9);
			
		} else if(rnd > thresholdAge30_39 && rnd <= thresholdAge40_49){
			
			return (int) (40 + Global.random.nextDouble() * 9);
			
		} else if(rnd > thresholdAge40_49 && rnd <= thresholdAge50_59){
			
			return (int) (50 + Global.random.nextDouble() * 9);
			
		} else if(rnd > thresholdAge50_59 && rnd <= thresholdAge60_69){
			
			return (int) (60 + Global.random.nextDouble() * 9);
			
		} else if(rnd > thresholdAge60_69 && rnd <= thresholdAge70_79){
			
			return (int) (70 + Global.random.nextDouble() * 9);
			
		} else {
			
			return (int) (80 + Global.random.nextDouble() * 9);
			
		}
		
	}
	
	public static int setAge(int lowerBound, int upperBound){
		
		int startIndex = lowerBound / 10;
		int endIndex = upperBound / 10;
		
		double totalThreshold = 1.;
		
		for(int i = 0; i < startIndex; i++){
			
			totalThreshold -= thresholdsAge[i];
			
		}
		
		for(int i = endIndex + 1; i < thresholdsAge.length; i++){
			
			totalThreshold -= thresholdsAge[i];
			
		}

		double rnd = Global.random.nextDouble() * totalThreshold;
		
		double accumulatedWeight = 0.;
		
		for(int i = startIndex; i < endIndex; i++){
			
			accumulatedWeight += thresholdsAge[i];
			
			if(accumulatedWeight >= rnd){
				
				int age = (int) (i * 10 + Global.random.nextDouble() * 9);
				
				return age < lowerBound ? lowerBound : age; 
				
			}
			
		}
		
		return lowerBound;
		
	}
	
	/**
	 * Sets an agent's sex according to MiD survey data. 
	 * 
	 * @param age the agent's age
	 * @return An integer representing the sex of the agent (0 = male, 1 = female)
	 */
	public static int setSex(int age){
		
		double rnd = Global.random.nextDouble();
		
		if(age < 10){
			
			if(rnd <= 0.4175){
				
				return 0;
				
			} else{
				
				return 1;
				
			}
			
		} else if(age >= 10 && age < 20){
			
			if(rnd <= 0.4941){
				
				return 0;
				
			} else{
				
				return 1;
				
			}
			
		} else if(age >= 20 && age < 30){
			
			if(rnd <= 0.5408){
				
				return 0;
				
			} else{
				
				return 1;
				
			}
			
		} else if(age >= 30 && age < 40){
			
			if(rnd <= 0.4688){
				
				return 0;
				
			} else{
				
				return 1;
				
			}
			
		} else if(age >= 40 && age < 50){
			
			if(rnd <= 0.4873){
				
				return 0;
				
			} else{
				
				return 1;
				
			}
			
		} else if(age >= 50 && age < 60){
			
			if(rnd <= 0.5037){
				
				return 0;
				
			} else{
				
				return 1;
				
			}
			
		} else if(age >= 60 && age < 70){
			
			if(rnd <= 0.5309){
				
				return 0;
				
			} else{
				
				return 1;
				
			}
			
		} else if(age >= 70 && age < 80){
			
			if(rnd <= 0.5753){
				
				return 0;
				
			} else{
				
				return 1;
				
			}
			
		} else{
			
			if(rnd <= 0.5806){
				
				return 0;
				
			} else{
				
				return 1;
				
			}
			
		}
		
	}
	
}
