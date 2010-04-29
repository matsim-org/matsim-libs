package playground.andreas.bln.ana.events2counts;

import org.matsim.api.core.v01.Id;

public class StopCountBox{
	Id stopId;
	String realName;
	int[] accessCount = new int[35];
	int[] egressCount = new int[35];	 
	
	public StopCountBox(Id stopId, String realName){
		this.stopId = stopId;
		this.realName = realName;
	}
	
	public String getHeader(){
		StringBuffer string = new StringBuffer();
		string.append("# Id; ");
		for (int i = 0; i < this.accessCount.length; i++) {
			string.append("Einstieg " + i + " - " + (i+1) + " Uhr; Ausstieg " + i + " - " + (i+1) + " Uhr; Besetzung " + i + " - " + (i+1) + " Uhr; ");
		}
		return string.toString();
	}
}
