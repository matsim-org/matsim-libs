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


public class Park_History_Writer {
	String filename;
	static File file;
	static FileWriter fwriter;
	

	public void start(String filename) {
		this.filename = filename;
		Park_History_Writer.file = new File(this.filename);
		try {
			Park_History_Writer.fwriter=new FileWriter(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void end(){
		try {
			Park_History_Writer.fwriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	void schreiben(String text){
		try {
			Park_History_Writer.fwriter.write(text);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void add_parking_occupied(Parking parking, String time, String person){
		schreiben("<parkevent time="+time+" Parkingid="+parking.id+" Parkingtype="+parking.type+" eventtype=occupied last_person="+person+">\n");
	}
	public void add_parking_availible(Parking parking, String time){
		schreiben("<parkevent time="+time+" Parkingid="+parking.id+" Parkingtype="+parking.type+" eventtype=availible>\n");
	}
	public void add_agent_not_parked(String time, String person){
		schreiben("<parkevent time="+time+" eventtype=agent_not_parked person="+person+">\n");
	}
}
