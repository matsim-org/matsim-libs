package playground.wrashid.bsc.vbmh.vm_parking;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Park_History_Writer {
	String filename;
	static File file;
	static FileWriter fwriter;
	

	public void start(String filename) {
		this.filename = filename;
		this.file = new File(this.filename);
		try {
			this.fwriter=new FileWriter(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void end(){
		try {
			this.fwriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	void schreiben(String text){
		try {
			this.fwriter.write(text);
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
