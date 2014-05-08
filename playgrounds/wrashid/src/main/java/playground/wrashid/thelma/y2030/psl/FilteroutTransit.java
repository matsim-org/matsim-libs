package playground.wrashid.thelma.y2030.psl;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;


public class FilteroutTransit {

	public static void main(String[] args) {
		Matrix parkingTimesEC = GeneralLib
				.readStringMatrix("A:/for marina/26. april 2012/parkingTimesAndEnergyConsumptionCH.txt");

		HashMap<String, Id> agentIds = new HashMap();
		
		for (int i = 0; i < parkingTimesEC.getNumberOfRows(); i++) {
			String actType = parkingTimesEC.getString(i, 4);
			if (actType.equalsIgnoreCase("tta")) {
				agentIds.put(parkingTimesEC.getString(i, 0), null);
			}
		}	
		
		int i=1;
		while (i<parkingTimesEC.getNumberOfRows()){ 
			String agentId = parkingTimesEC.getString(i, 0);
			if (agentIds.containsKey(agentId)) {
				parkingTimesEC.deleteRow(i);
			} else {
				i++;
			}
		}	
	
		parkingTimesEC.writeMatrix("A:/for marina/27. april 2012/parkingTimesAndEnergyConsumptionCH.txt");
		
	}
	
	 

}
