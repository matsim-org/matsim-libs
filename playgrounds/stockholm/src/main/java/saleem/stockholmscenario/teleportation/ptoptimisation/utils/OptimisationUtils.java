package saleem.stockholmscenario.teleportation.ptoptimisation.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class OptimisationUtils {
	public ArrayList<Double> sortTimes(ArrayList<Double> times){
		Collections.sort(times, new Comparator<Double>() {

	        public int compare(Double a, Double b) {
	            return (int)(a - b);
	        }
	    });
		return times;
	}
//	public ArrayList<Double> getRatioOfTimes(ArrayList<Double> times){//Returns a ratio of departure times
//		times=sortTimes(times);
//		int i = 0;ArrayList<Double> ratios = new ArrayList<Double>();
//		if(times.size()<2){
//			ratios.add(1.0);//Assuming ratio one for single departure
//			return ratios;
//		}
//		double totaldiff = times.get(times.size()-1)-times.get(0);
//		 while(i<times.size()-1){
//			 double diff = times.get(i+1)-times.get(i);
//			 double ratio = diff/totaldiff;
//			 ratios.add(ratio);
//			 if(i+1==times.size()-1){
//				 ratios.add(ratio);//Add for the last element the same ratio as second last element.
//			 }
//			 i++;
//			 
//		 }
//		return ratios;
//	}
//	public ArrayList<Double> getRatioOfTimesIfAdded(ArrayList<Double> origratios, ArrayList<Double> origtimes, ArrayList<Double> changedtimes){//Returns a ratio of departure times
//		double ratio=0;double totalratio=0;
//		int i = 0;ArrayList<Double> updatedratios = new ArrayList<Double>();
//		if(changedtimes.size()<=2){
//			return updatedratios;
//		}
//		int j=0;
//		 while(j<changedtimes.size()){
//			 ratio=origratios.get(i);
//			 ratio=origratios.get(i)*((origtimes.size()-1))/(changedtimes.size()-1);//the -1 is for ensuring that ratios are one less than departures.
//			 updatedratios.add(ratio);
//			 totalratio=totalratio+ratio;
//			 if(origtimes.get(i).doubleValue()==changedtimes.get(j).doubleValue()){
//				 i=(i==origtimes.size()-1)?i:++i;
//			 }
//			 j++;
//		 }
//		 totalratio-=ratio;//Removing the last ratio
//		 for(j=0;j<updatedratios.size();j++){
//				updatedratios.set(j, updatedratios.get(j)/totalratio);//Normalise according to total ratio to make the total ratio one again
//		 }
//		return updatedratios;
//	}
//	public ArrayList<Double> getRatioOfTimesIfRemoved(ArrayList<Double> origratios, ArrayList<Double> origtimes, ArrayList<Double> changedtimes){//Returns a ratio of departure times
//		double ratio=0;
//		double totalratio=0;
//		int i = 0;ArrayList<Double> updatedratios = new ArrayList<Double>();
//		if(changedtimes.size()<=2){
//			return updatedratios;
//		}
//		int j=0;
//		while(j<changedtimes.size()){
//			 if(origtimes.get(i).doubleValue()==changedtimes.get(j).doubleValue()){
//				 ratio=origratios.get(i);
//				 ratio=origratios.get(i)*((origtimes.size()-1))/(changedtimes.size()-1);//the -1 is for ensuring that ratios are one less than departures.
//				 updatedratios.add(ratio);
//				 totalratio=totalratio+ratio;
//				 j++;
//			 }
//			 i++;
//		 }
//		totalratio-=ratio;//Removing the last ratio
//		for(j=0;j<updatedratios.size();j++){
//			updatedratios.set(j, updatedratios.get(j)/totalratio);//Normalise according to total ratio to make the total ratio one again
//		}
//		return updatedratios;
//	}
//	@SuppressWarnings("unused")
//	public ArrayList<Double> rearrangeTimes(ArrayList<Double> origtimes, ArrayList<Double> changedtimes){
//		origtimes=sortTimes(origtimes);
//		changedtimes=sortTimes(changedtimes);
//		ArrayList<Double> updatedtimes = new ArrayList<Double>();
//		ArrayList<Double> updatedratios = new ArrayList<Double>();
//		ArrayList<Double> origratios = getRatioOfTimes(origtimes);
//		if(origtimes.size()==changedtimes.size()){//No times added or removed
//			return origtimes;
//		}
//		else if (origtimes.size()<changedtimes.size()){//New times added
//			updatedratios = getRatioOfTimesIfAdded(origratios,origtimes, changedtimes);
//		}
//		else{//Times removed
//			updatedratios = getRatioOfTimesIfRemoved(origratios,origtimes, changedtimes);
//		}
//		updatedtimes = rearrangeTimesPerUpdatedRatios(updatedratios,origtimes, changedtimes );
//		return sortTimes(updatedtimes);
//		
//	}
//	public ArrayList<Double> rearrangeTimesPerUpdatedRatios(ArrayList<Double> updatedratios,ArrayList<Double> origtimes,  ArrayList<Double> changedtimes){//Returns a ratio of departure times
//		ArrayList<Double> updatedtimes = new ArrayList<Double>();
//		int i=1;//Skip first time value
//		double ratio=0, time=0;
//		if(changedtimes.size()<=2){//If its just one departure
//			return changedtimes;
//		}
//		double changeddiff = changedtimes.get(changedtimes.size()-1)-changedtimes.get(0);
//		time =  changedtimes.get(0);
//		updatedtimes.add(changedtimes.get(0));//First element as it is
//		updatedtimes.add(changedtimes.get(changedtimes.size()-1));//Last element as it is
//		 while(i<changedtimes.size()-1){
//			ratio=updatedratios.get(i-1);
//			time=time+ratio*changeddiff;
//			time=Math.round(time);//Round to seconds, for unit testing 
//			updatedtimes.add(time);
//			i++;
//		 }
//		return updatedtimes;
//	}
	
}
