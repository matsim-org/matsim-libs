package playground.wrashid.bsc.vbmh.vm_parking;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Writes a file with parking related events. At the moments there are events for:
 * -Parking lot is fully occupied
 * -Parking lot is available again
 * -Agent can not park
 * 
 * The start() should be called before each iteration, the end() should be called at the end of 
 * each iteration to close the file stream.
 * 
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */


public class ParkHistoryWriter {
	String filename;
	static File file;
	static FileWriter fwriter;
	

	public void start(String filename) {
		this.filename = filename;
		ParkHistoryWriter.file = new File(this.filename);
		try {
			ParkHistoryWriter.fwriter=new FileWriter(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void end(){
		try {
			ParkHistoryWriter.fwriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	void schreiben(String text){
		try {
			ParkHistoryWriter.fwriter.write(text);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
}
