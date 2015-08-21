package playground.dhosse.cl.counts;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

class CsData {
	
	private Map<String, ArrayList<Integer>> volumePerTime = new TreeMap<String, ArrayList<Integer>>(); //Map<time, volumes[]>
	
//	CsData(String time){
//		this.volumePerTime.put(time, null);
//	}
	
	Map<String, ArrayList<Integer>> getVolumePerTime() {
		return volumePerTime;
	}

	void addTimeAndVolume(String time, Integer vol){
		volumePerTime.put(time, new ArrayList<Integer>());
		volumePerTime.get(time).add(vol);
	}
	
	void addVolume(String time,Integer vol){
		this.volumePerTime.get(time).add(vol);
	}
	
	Integer calcVolumePerTime(String time){
		Integer sum = 0;
		for (int i=0; i<this.volumePerTime.get(time).size(); i++){
			sum += this.volumePerTime.get(time).get(i);
		}
		return sum;
	}
	
	Boolean containsTime(String time){
		if (this.volumePerTime.containsKey(time)){
			return true;
		} else
		return false;
	}
}
