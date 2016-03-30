package playground.dhosse.gap.scenario.population.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.TransportMode;

import playground.dhosse.gap.Global;

public class LegModeCreator {

//	private static final double meanDistanceBike = 3786.879756;
//	private static final double meanDistanceCar = 14560.53314;
//	private static final double meanDistancePt = 10000;
//	private static final double meanDistanceWalk = 1276.443998;

	private static final double meanDistanceBike = 3768;
	private static final double meanDistanceCar = 14560;
	private static final double meanDistancePt = 8000;
	private static final double meanDistanceWalk = 1276;
	
	public static String getLegModeForDistance(double distance, int age, int sex){
		
		return getLegModeForDistance(distance, false, false, age, sex);
		
	}
	
	public static String getLegModeForDistance(double distance, boolean carAvail, int age, int sex){
		
		return getLegModeForDistance(distance, carAvail, false, age, sex);
		
	}
	
	public static String getLegModeForDistance(double distance, boolean carAvail, boolean hasLicense, int age, int sex){

		Map<String, Double> modeShares = getModeShare(age, sex);
		
		
		Map<String, Double> weightsMap = new TreeMap<>();
		double accumulatedWeight = 0.;
		
		double pBike = Math.exp(-distance/meanDistanceBike)/meanDistanceBike;
		weightsMap.put(TransportMode.bike, pBike * modeShares.get(TransportMode.bike));
		accumulatedWeight += pBike * modeShares.get(TransportMode.bike);
		
		if(carAvail){
			if(hasLicense){
				double pCar = Math.exp(-distance/meanDistanceCar)/meanDistanceCar;
				weightsMap.put(TransportMode.car, pCar * modeShares.get(TransportMode.car));
				accumulatedWeight += pCar * modeShares.get(TransportMode.car);
			}
		}
		
		double pRide = Math.exp(-distance/meanDistanceCar)/meanDistanceCar;
		weightsMap.put(TransportMode.ride, pRide * modeShares.get(TransportMode.ride));
		accumulatedWeight += pRide * modeShares.get(TransportMode.ride);
		
		double pPt = Math.exp(-distance/meanDistancePt)/meanDistancePt;
		weightsMap.put(TransportMode.pt, pPt * modeShares.get(TransportMode.pt));
		accumulatedWeight += pPt * modeShares.get(TransportMode.pt);
		
		double pWalk = Math.exp(-distance/meanDistanceWalk)/meanDistanceWalk;
		weightsMap.put(TransportMode.walk, pWalk * modeShares.get(TransportMode.walk));
		accumulatedWeight += pWalk * modeShares.get(TransportMode.walk);
		
		double random = Global.random.nextDouble() * accumulatedWeight;
		
		double weight = 0.;
		double max = 0.;
		String selectedMode = "";
		
		for(Entry<String, Double> entry : weightsMap.entrySet()){
			
			if(entry.getValue() > max){
				max = entry.getValue();
				selectedMode = entry.getKey();
			}
//			weight += entry.getValue();
//			if(random <= weight){
//				return entry.getKey();
//			}
			
		}
		
		return selectedMode;
		
	}
	
	public static double getProbaForDistance(String mode, double distance){
		
		double mean = 0.;
		
		if(mode.equals(TransportMode.bike)){
			
			mean = meanDistanceBike;
			
		} else if(mode.equals(TransportMode.car) || mode.equals(TransportMode.ride)){
			
			mean = meanDistanceCar;
			
		} else if(mode.equals(TransportMode.pt)){
			
			mean = meanDistancePt;
			
		} else if(mode.equals(TransportMode.walk)){
			
			mean = meanDistanceWalk;
			
		}
		
		return Math.exp(-distance/mean)/mean;
		
	}
	
	public static String getMode(int age, int sex){
		
		Map<String, Double> shares = getModeShare(age, sex);
		
		double p = Global.random.nextDouble();
		
		
		double weight = 0.;
		for(Entry<String, Double> mode : shares.entrySet()){
			
			weight += mode.getValue();
			
			if(p <= weight) return mode.getKey();
			
		}
		
		return "";
		
	}
	
	private static Map<String, Double> getModeShare(int age, int sex){
		
		Map<String, Double> modeShares = new HashMap<>();
		
		if(sex == 0){
			
			if(age < 10){
				
				modeShares.put(TransportMode.walk, 0.3712);
				modeShares.put(TransportMode.bike, 0.0544);
				modeShares.put(TransportMode.car, 0.0);
				modeShares.put(TransportMode.ride, 0.4988);
				modeShares.put(TransportMode.pt, 0.0757);
				
			} else if(age >= 10 && age < 20){
				
				modeShares.put(TransportMode.walk, 0.2035);
				modeShares.put(TransportMode.bike, 0.149);
				modeShares.put(TransportMode.ride, 0.2935);
				modeShares.put(TransportMode.car, 0.1239);
				modeShares.put(TransportMode.pt, 0.2301);
				
			} else if(age >= 20 && age < 30){
				
				modeShares.put(TransportMode.walk, 0.1009);
				modeShares.put(TransportMode.bike, 0.0467);
				modeShares.put(TransportMode.ride, 0.1103);
				modeShares.put(TransportMode.car, 0.6972);
				modeShares.put(TransportMode.pt, 0.0449);
				
			} else if(age >= 30 && age < 40){
				
				modeShares.put(TransportMode.walk, 0.1452);
				modeShares.put(TransportMode.bike, 0.0477);
				modeShares.put(TransportMode.ride, 0.083);
				modeShares.put(TransportMode.car, 0.6888);
				modeShares.put(TransportMode.pt, 0.0353);
				
			} else if(age >= 40 && age < 50){
				
				modeShares.put(TransportMode.walk, 0.1469);
				modeShares.put(TransportMode.bike, 0.0603);
				modeShares.put(TransportMode.ride, 0.0594);
				modeShares.put(TransportMode.car, 0.6986);
				modeShares.put(TransportMode.pt, 0.0348);
				
			} else if(age >= 50 && age < 60){
				
				modeShares.put(TransportMode.walk, 0.1393);
				modeShares.put(TransportMode.bike, 0.079);
				modeShares.put(TransportMode.ride, 0.0602);
				modeShares.put(TransportMode.car, 0.6775);
				modeShares.put(TransportMode.pt, 0.0439);
				
			} else if(age >= 60 && age < 70){
				
				modeShares.put(TransportMode.walk, 0.2279);
				modeShares.put(TransportMode.bike, 0.0689);
				modeShares.put(TransportMode.ride, 0.0442);
				modeShares.put(TransportMode.car, 0.6237);
				modeShares.put(TransportMode.pt, 0.0353);
				
			} else if(age >= 70 && age < 80){
				
				modeShares.put(TransportMode.walk, 0.2093);
				modeShares.put(TransportMode.bike, 0.0498);
				modeShares.put(TransportMode.ride, 0.1163);
				modeShares.put(TransportMode.car, 0.5847);
				modeShares.put(TransportMode.pt, 0.0399);
				
			} else{
				
				modeShares.put(TransportMode.walk, 0.2326);
				modeShares.put(TransportMode.bike, 0.0);
				modeShares.put(TransportMode.ride, 0.186);
				modeShares.put(TransportMode.car, 0.5581);
				modeShares.put(TransportMode.pt, 0.0233);
				
			}
			
		} else{
			
			if(age < 10){
				
				modeShares.put(TransportMode.walk, 0.3527);
				modeShares.put(TransportMode.bike, 0.0441);
				modeShares.put(TransportMode.car, 0.0);
				modeShares.put(TransportMode.ride, 0.4965);
				modeShares.put(TransportMode.pt, 0.0162);
				
			} else if(age >= 10 && age < 20){
				
				modeShares.put(TransportMode.walk, 0.2211);
				modeShares.put(TransportMode.bike, 0.0501);
				modeShares.put(TransportMode.ride, 0.3533);
				modeShares.put(TransportMode.car, 0.178);
				modeShares.put(TransportMode.pt, 0.1975);
				
			} else if(age >= 20 && age < 30){
				
				modeShares.put(TransportMode.walk, 0.1935);
				modeShares.put(TransportMode.bike, 0.0327);
				modeShares.put(TransportMode.ride, 0.1771);
				modeShares.put(TransportMode.car, 0.5123);
				modeShares.put(TransportMode.pt, 0.0845);
				
			} else if(age >= 30 && age < 40){
				
				modeShares.put(TransportMode.walk, 0.2365);
				modeShares.put(TransportMode.bike, 0.0469);
				modeShares.put(TransportMode.ride, 0.1264);
				modeShares.put(TransportMode.car, 0.5596);
				modeShares.put(TransportMode.pt, 0.0307);
				
			} else if(age >= 40 && age < 50){
				
				modeShares.put(TransportMode.walk, 0.1554);
				modeShares.put(TransportMode.bike, 0.0633);
				modeShares.put(TransportMode.ride, 0.1234);
				modeShares.put(TransportMode.car, 0.6434);
				modeShares.put(TransportMode.pt, 0.0144);
				
			} else if(age >= 50 && age < 60){
				
				modeShares.put(TransportMode.walk, 0.2045);
				modeShares.put(TransportMode.bike, 0.0985);
				modeShares.put(TransportMode.ride, 0.1515);
				modeShares.put(TransportMode.car, 0.5101);
				modeShares.put(TransportMode.pt, 0.0354);
				
			} else if(age >= 60 && age < 70){
				
				modeShares.put(TransportMode.walk, 0.3011);
				modeShares.put(TransportMode.bike, 0.0637);
				modeShares.put(TransportMode.ride, 0.222);
				modeShares.put(TransportMode.car, 0.367);
				modeShares.put(TransportMode.pt, 0.0462);
				
			} else if(age >= 70 && age < 80){
				
				modeShares.put(TransportMode.walk, 0.36);
				modeShares.put(TransportMode.bike, 0.08);
				modeShares.put(TransportMode.ride, 0.23);
				modeShares.put(TransportMode.car, 0.3);
				modeShares.put(TransportMode.pt, 0.03);
				
			} else{
				
				modeShares.put(TransportMode.walk, 0.1875);
				modeShares.put(TransportMode.bike, 0.0);
				modeShares.put(TransportMode.ride, 0.4375);
				modeShares.put(TransportMode.car, 0.3438);
				modeShares.put(TransportMode.pt, 0.0313);
				
			}
			
		}
		
		return modeShares;
		
	}

}
