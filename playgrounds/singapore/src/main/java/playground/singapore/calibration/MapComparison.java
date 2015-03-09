package playground.singapore.calibration;

import java.util.SortedMap;
import java.util.TreeMap;

public class MapComparison {

	public static SortedMap<Integer, Double[]> createDiffMap(final SortedMap<Integer, Integer[]> map1, final SortedMap<Integer, Integer[]> map2){
		SortedMap<Integer, Double[]> diffMap = new TreeMap<Integer, Double[]>();
		for(Integer key:map1.keySet()){
			Double[] diff = new Double[map1.get(key).length];
			double sum1=0.0;
			double sum2=0.0;
			for(int l:map1.get(key)){
				sum1 += l;
			}
			for(int l:map2.get(key)){
				sum2 += l;
			}

			for(int i=0;i<map1.get(key).length;i++){				

				if(sum1!=0 && sum2!=0){
					diff[i] = ((double) map1.get(key)[i]/sum1 - (double) map2.get(key)[i]/sum2)*100;

				}
				else{
					diff[i]=0.0;
				}

			}
			diffMap.put(key, diff);
		}
		return diffMap;		
	}

	public static SortedMap<Integer, Double[]> createCumulativeValuesMap(final BenchmarkDataReader dataSet){
		SortedMap<Integer, Double[]> cumulativeValuesMap = new TreeMap<Integer, Double[]>();
		Integer previousKey = null;
		double sum1=dataSet.getTotalTrips();
		for(Integer key:dataSet.getDataMap().keySet()){	
			Double[] cumSum = new Double[dataSet.getDataMap().get(key).length];
			for(int i=0;i<dataSet.getDataMap().get(key).length;i++){
				if(previousKey==null){
					cumSum[i] = (double) dataSet.getDataMap().get(key)[i]/sum1;
				}
				else{
					cumSum[i] = cumulativeValuesMap.get(previousKey)[i] + (double) dataSet.getDataMap().get(key)[i]/sum1;
				}
			}
			previousKey=key;	
			cumulativeValuesMap.put(key, cumSum);
		}			
		return cumulativeValuesMap;
	}


	public static SortedMap<Integer, Double[]> createCumulativeDiffMap(final SortedMap<Integer, Integer[]> map1, final SortedMap<Integer, Integer[]> map2){
		SortedMap<Integer, Double[]> cumulativeDiffMap = new TreeMap<Integer, Double[]>();
		double[][] cumulativeMap1 = new double[map1.keySet().size()][map1.get((map1.firstKey())).length];
		double[][] cumulativeMap2 = new double[map2.keySet().size()][map2.get((map2.firstKey())).length];	

		double sum1=0.0;
		double sum2=0.0;


		//Calculate total number of trips
		for(Integer key:map1.keySet()){
			for(Integer trips:map1.get(key)){
				sum1 = sum1 + trips;		
			}
		}
		for(Integer key:map2.keySet()){
			for(Integer trips:map2.get(key)){
				sum2 = sum2 + trips;		
			}
		}

		//Calculate cumulative mode share maps
		int r=0;
		for(Integer key:map1.keySet()){
			for(int c=0;c<cumulativeMap2[r].length;c++){
				if(r==0){
					cumulativeMap1[r][c] = (double) map1.get(key)[c]/sum1;
				}
				else{
					cumulativeMap1[r][c] = cumulativeMap1[r-1][c] + (double) map1.get(key)[c]/sum1;
				}
			}
			r++;
		}

		r=0;
		for(Integer key:map2.keySet()){	
			for(int c=0;c<cumulativeMap2[r].length;c++){
				if(r==0){
					cumulativeMap2[r][c] = (double) map2.get(key)[c]/sum2;
				}
				else{
					cumulativeMap2[r][c] = cumulativeMap2[r-1][c] + (double) map2.get(key)[c]/sum2;
				}	
			}
			r++;
		}




		r=0;
		for(Integer key:map1.keySet()){			
			Double[] cumDiff = new Double[map1.get(key).length];
			for(int c=0;c<cumulativeMap1[r].length;c++){
				cumDiff[c] = (cumulativeMap1[r][c] - cumulativeMap2[r][c])*100;
				//System.out.println(cumulativeMap1[r][c]+" - "+cumulativeMap2[r][c]+" = "+cumDiff[c]);
			}
			cumulativeDiffMap.put(key, cumDiff);
			r++;
		}

		return cumulativeDiffMap;		
	}



}
