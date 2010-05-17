package playground.andreas.bln.ana.events2counts;

import org.matsim.api.core.v01.Id;

public class StopCountBox{
	
	static int slots = 35;
	
	Id stopId;
	String realName;
	int[] accessCount = new int[StopCountBox.slots];
	int[] egressCount = new int[StopCountBox.slots];	 
	
	public StopCountBox(Id stopId, String realName){
		this.stopId = stopId;
		this.realName = realName;
	}
	
	public int getTotalAccessSum(){
		int sum = 0;		
		for (int i = 0; i < StopCountBox.slots; i++) {
			sum += this.accessCount[i];
		}
		return sum;
	}
	
	public int getTotalEgressSum(){
		int sum = 0;		
		for (int i = 0; i < StopCountBox.slots; i++) {
			sum += this.egressCount[i];
		}
		return sum;
	}
	
	public String getHeader(){
		StringBuffer string = new StringBuffer();
		string.append("# Id; Einstieg " + StopCountBox.slots + "h; Ausstieg " + StopCountBox.slots + "h; Besetzung " + StopCountBox.slots + "h; ");
		for (int i = 0; i < StopCountBox.slots; i++) {
			string.append("Einstieg " + i + " - " + (i+1) + " Uhr; Ausstieg " + i + " - " + (i+1) + " Uhr; Besetzung " + i + " - " + (i+1) + " Uhr; ");
		}
		return string.toString();
	}
}
