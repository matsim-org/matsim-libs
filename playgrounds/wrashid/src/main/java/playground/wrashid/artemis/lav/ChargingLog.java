package playground.wrashid.artemis.lav;


import java.util.ArrayList;
import java.util.LinkedList;

import org.matsim.contrib.parking.lib.GeneralLib;


public class ChargingLog {
	LinkedList<ChargingLogRow> chargingLog=new LinkedList<ChargingLogRow>();
	
	public void addChargingLog(String linkId, String agentId, double startChargingTime, double endChargingTime, double startSOC,
			double endSOC) {
		ChargingLogRow chargingLogRow = new ChargingLogRow(linkId, agentId, startChargingTime, endChargingTime, startSOC, endSOC);
		chargingLog.add(chargingLogRow);
	}

	public void writeLogToFile(String fileName){
		ArrayList<String> list=new ArrayList<String>();
		
		list.add("linkId\tagentId\tstartChargingTime\tendChargingTime\tstartSOCInJoule\tendSOCInJoule");
		
		for (ChargingLogRow row:chargingLog){
			list.add(row.toString());
		}
		
		GeneralLib.writeList(list, fileName);
	}
	
}

class ChargingLogRow {
	String linkId;
	String agentId;
	double startChargingTime;
	double endChargingTime;
	double startSOCInJoule;
	double endSOCInJoule;

	public ChargingLogRow(String linkId, String agentId, double startChargingTime, double endChargingTime, double startSOCInJoule,
			double endSOCInJoule) {
		this.linkId = linkId;
		this.agentId = agentId;
		this.startChargingTime = GeneralLib.projectTimeWithin24Hours(startChargingTime);
		this.endChargingTime = GeneralLib.projectTimeWithin24Hours(endChargingTime);
		this.startSOCInJoule = startSOCInJoule;
		this.endSOCInJoule = endSOCInJoule;
	}

	public String toString(){
		return linkId+ "\t" + agentId+ "\t" + startChargingTime + "\t" + endChargingTime + "\t" + startSOCInJoule + "\t" + endSOCInJoule;
	}
	
}
