package playground.mmoyo.Validators;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.mmoyo.utils.DataLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**validates that facilities of a transit schedule stop have the same coordinate (as real world stops do) */ 
public class StopCoordinateValidator {

	private void run(final TransitSchedule schedule){
		/**organize stop data in a map*/
		final String point = ".";
		Map <String, List<Id>> stopIdMap = new TreeMap <String, List<Id>>();
		for (TransitStopFacility stopFacility : schedule.getFacilities().values()){
			String strId = stopFacility.getId().toString();
			int pIndex= strId.indexOf(point);
			String root = strId.substring(0, pIndex);
			
			//System.out.println(root + ":   " + strId);
			if (!stopIdMap.containsKey(root)){
				List<Id> idList = new ArrayList<Id>();
				stopIdMap.put(root, idList);
			} 
			stopIdMap.get(root).add(stopFacility.getId());
		}
		
		for(Entry<String, List<Id>> entry: stopIdMap.entrySet() ){
			String root = entry.getKey(); 
			List<Id> idList = entry.getValue();

			TransitStopFacility lastStop= null;
			for (int i=0; i<idList.size();i++){
				TransitStopFacility stop = schedule.getFacilities().get(idList.get(i));
				if (i>0){
					if(!stop.getCoord().equals(lastStop.getCoord())){
						System.out.println(root+ " different coordinate!");
					}
				}
				lastStop = stop;
			}
		}
	}

	public static void main(String[] args) {
		String scheduleFile = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
		DataLoader dataLoader = new DataLoader ();
		TransitSchedule schedule = dataLoader.readTransitSchedule(scheduleFile);
		new StopCoordinateValidator().run(schedule);
	}
}
