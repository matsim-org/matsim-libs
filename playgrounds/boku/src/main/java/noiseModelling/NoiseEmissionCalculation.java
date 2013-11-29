package noiseModelling;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;

public class NoiseEmissionCalculation {

	//compute Lcar, Lhdv per hour and link, equation (12) and (13) Euronoise Paper, sum up to Lme
	//based on Map <Id,double[][]> linkId2hour2vehicles = new TreeMap <Id, double[][]> from NoiseHandler
	public Map <Id,Map<Double,Double>> calLmeCarHdvHour (Map <Id,Map<Double,double[]>> linkId2hour2vehicles){ 
			
		Map<Id,Map<Double,Double>> linkId2Hour2NoiseEmissions = new TreeMap<Id,Map<Double,Double>> ();

		for(Entry < Id, Map<Double , double[]> > entry : linkId2hour2vehicles.entrySet()){
			Id linkId = entry.getKey();
			Map<Double, double[]> hour2Infos = entry.getValue(); //hour,freespeed,total,HDV
			
			Map<Double,Double> hour2Noise = new TreeMap<Double,Double>() ;
			//double[] noise = null;
			
			for (Entry <Double , double[]> element : hour2Infos.entrySet()){
				double hour = element.getKey();
				double [] hourInfos = element.getValue();
				// we collect now the parameter
				double maxSpeed = hourInfos [0]; //freespeed
				double total = 100.0 * hourInfos [1]; //cars+hdv
				double hdv = 100.0 * hourInfos [2]; //HDV
				//double total = car + hdv;
						
				double p = hdv/total*100; //share HDV
				// do the calculation
				//double l_car = 27.7+(10*Math.log10(1.0 + Math.pow(0.02 * maxSpeed, 3.0)));
				//double l_lkw = 23.1 + (12.5 * Math.log10(maxSpeed)) ;
				double l_car = (10*Math.log10((1-p/100)*total))+27.7+(10*Math.log10(1.0 + Math.pow(0.02 * maxSpeed, 3.0)));
				double l_hdv = (10*Math.log10((p/100)*total))+23.1 + (12.5 * Math.log10(maxSpeed)) ;
				double lme = (10*Math.log10(Math.pow(10, l_car/10.0)+Math.pow(10, l_hdv/10.0)));
				//noise[0] = l_car;
				//noise[1] = l_hdv;
				//noise[2] = lme;
				hour2Noise.put(hour, lme);			
			}
			linkId2Hour2NoiseEmissions.put(linkId,hour2Noise);
		}
		return linkId2Hour2NoiseEmissions;
	}
	
	/*FH Code, slightly modified by Regine
	 algorithm to calculate noise emissions 
	public Map<Id,Map<String,Double>> calculate_lme (Map<Id,Map<String, double[]>> linkInfos){ 
		/*Methode calculate_lme, return value linkInfos, used in NoiseTool
		 * linkInfos is computed in handler.getlinkId2timePeriod2TrafficInfo()
		 * contains Id, Timeperiod String, double[3]: freespeed, totalvehicles, HDV
		Map<Id,Map<String,Double>> linkId2Time2NoiseEmissions = new TreeMap<Id,Map<String,Double>> ();
		
		for(Entry < Id, Map<String , double[]> > entry : linkInfos.entrySet()){
			Id linkId = entry.getKey();
			Map<String, double[]> timeperiodeToInfos = entry.getValue();
			
			Map<String,Double> timeperiodeToResult = new TreeMap<String,Double>() ;
			
			for (Entry <String , double[]> element : timeperiodeToInfos.entrySet()){
				String timeperiode = element.getKey();
				double [] timeperiodeInfos = element.getValue();
				// we collect now the parameter
				double maxSpeed = timeperiodeInfos [0]; //freespeed
				double total = 10.0 * timeperiodeInfos [1]; //totalvehicles
				double heavy = 10.0 * timeperiodeInfos [2]; //HDV
				
				double p = heavy/total*100; //share HDV
				double DTV = total;
				// do the calculation
				double l_pkw = 27.7+(10*Math.log10(1.0 + Math.pow(0.02 * maxSpeed, 3.0)));
				double l_lkw = 23.1 + (12.5 * Math.log10(maxSpeed)) ;
				double D = l_lkw-l_pkw ;
				double Dv = l_pkw - 37.3 + 10* Math.log10((100.0 + (Math.pow(10.0, (D/10.0))-1)*p ) / (100.0+8.23*p));
				double lm = calc_lm(timeperiode , DTV, p);
				double lme = lm + Dv ;
				timeperiodeToResult.put(timeperiode, lme);			
			}
			linkId2Time2NoiseEmissions.put(linkId,timeperiodeToResult);
		}
		addBasicValue(linkId2Time2NoiseEmissions);
		return linkId2Time2NoiseEmissions;
	}
	end FH code*/
	
	// calculation of LDEN nach VBUS

	public Map<Id,Double> cal_lden (Map<Id,Map<Double,Double>> linkId2timePeriod2lden){		
		Map<Id,Double> linkId2lden = new TreeMap <Id,Double> ();
		
		for (Entry <Id,Map<Double,Double>> entry : linkId2timePeriod2lden.entrySet()){
			Id linkId = entry.getKey();
			Map<Double,Double> hour2lden = entry.getValue();
			double sum = 0.0;				
			for (Entry <Double , Double> element : hour2lden.entrySet()){
				double hour = element.getKey();
				double lme = element.getValue();
				sum = sum + calcValueForLden(hour, lme);	
			}
			double lden = 10.0 * (Math.log10((1.0/24.0)*sum)) ;
			linkId2lden.put(linkId, lden);		
		}
		return linkId2lden;
	}
	
	private double calcValueForLden(Double hour , double lme){
		//den folgenden Befehl kann man ganz sicher eleganter ausdrücken, wie?
		if (hour.equals(6.0) || hour.equals(7.0) || hour.equals(8.0) || hour.equals(9.0) || hour.equals(10.0) || hour.equals(11.0) || hour.equals(12.0) || hour.equals(13.0) || hour.equals(14.0) || hour.equals(15.0) || hour.equals(16.0) || hour.equals(17.0)){
			return 12.0*(Math.pow(10.0, lme/10.0));
		}
		
		else if (hour.equals(18.0) || hour.equals(19.0) || hour.equals(20.0) || hour.equals(21.0)){
			return 4.0*(Math.pow(10.0, (lme+5.0)/10.0));
		}
		
		else {
			return 8.0*(Math.pow(10.0,(lme+10.0)/10.0));
		}	
	}

}
