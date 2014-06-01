package playground.vbmh.vmParking;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Writes a file with parking related events; The start() should be called before each iteration, the end() should be called at the end of 
 * each iteration to write the file.
 * At the moments there are events for:
 * -Parking lot is fully occupied
 * -Parking lot is available again
 * -Agent can not park
 * -EV / NEV parks (containins information on state of charge)
 * -EV / NEV leaves parking (containins information on state of charge)
 * -EV runs out of Battery 
 * 
 * 
 * 
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */


public class ParkHistoryWriter {
	static String filename;
	static File file;
	static FileWriter fwriter;
	static LinkedList<String> output;

	public void start(String filename) {
		output = new LinkedList<String>();
		this.filename = filename;
		
		
	}
	
	public void end(){
		
		ParkHistoryWriter.file = new File(this.filename);
		try {
			ParkHistoryWriter.fwriter=new FileWriter(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for(String text : output){
			try {
				ParkHistoryWriter.fwriter.write(text);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		try {
			ParkHistoryWriter.fwriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	void schreiben(String text){
		output.add(text);
		
	}
	
	public void addParkingOccupied(Parking parking, String time, String person){
		schreiben("<parkevent time="+time+" Parkingid="+parking.id+" Parkingtype="+parking.type+" eventtype=occupied last_person="+person+">\n");
	}
	public void addParkingAvailible(Parking parking, String time){
		schreiben("<parkevent time="+time+" Parkingid="+parking.id+" Parkingtype="+parking.type+" eventtype=availible>\n");
	}
	public void addAgentNotParked(String time, String person){
		schreiben("<parkevent time="+time+" eventtype=agent_not_parked person="+person+">\n");
	}

	public void addAgentNotParkedWithinDefaultDistance(String time, String person){
		schreiben("<parkevent time="+time+" eventtype=agent_not_parked_within_default_distance person="+person+">\n");
	}
	
	public void addNEVParked(String time, String person, String parking, double score, String parkingType, String spotType){
		schreiben("<parkevent time="+time+" eventtype=NEV_parked person="+person+" parking="+parking+" spot_score="+score+" parkingType="+parkingType+" spotType="+spotType+">\n");
	}
	
	public void addEVParked(String time, String person, String parking, double score, String parkingType, String spotType, String stateOfChargePercent){
		schreiben("<parkevent time="+time+" eventtype=EV_parked person="+person+" parking="+parking+" spot_score="+score+" parkingType="+parkingType+" spotType="+spotType+" stateOfChargePercent="+stateOfChargePercent+">\n");
	}
	
	public void addNEVLeft(String time, String person, String parking, String parkingType, String spotType){
		schreiben("<parkevent time="+time+" eventtype=NEV_left person="+person+" parking="+parking+" parkingType="+parkingType+" spotType="+spotType+">\n");
	}

	
	public void addEVLeft(String time, String person, String parking, String parkingType, String spotType, String stateOfChargePercent){
		schreiben("<parkevent time="+time+" eventtype=EV_left person="+person+" parking="+parking+" parkingType="+parkingType+" spotType="+spotType+" stateOfChargePercent="+stateOfChargePercent+">\n");
	}
	
	public void addEVOutOfBattery(String time, String person){
		schreiben("<parkevent time="+time+" eventtype=ev_out_of_battery person="+person+">\n");
	}
	
	public void addEVChoseWrongSpot(String time, String person, double bestScore){
		schreiben("<parkevent time="+time+" eventtype=ev_chose_nonEV_spot_instead_of_sufficient_charge person="+person+" best_spot_score="+bestScore+">\n");
	}
	
	public void addAgentHasToCharge(String time, String person){
		schreiben("<parkevent time="+time+" eventtype=Agent_looking_for_parking_has_to_charge person="+person+">\n");
	}


}
