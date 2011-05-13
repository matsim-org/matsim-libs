package mkillat.tmc;

public class MessagesPlusFactor {

	String msId;
	String link;
	int eventCode;
	String startTime;
	String endTime;
	String flowCapacityChange;
	String freespeedChange;
	String lanesChange;
	double factor;
	
	public MessagesPlusFactor (	String msId, String link, int eventCode, String startTime, String endTime, 
								String flowCapacityChange, String freespeedChange, String lanesChange, double factor){
		this.msId = msId;
		this.link = link;
		this.eventCode = eventCode;
		this.startTime = startTime;
		this.endTime = endTime;
		this.flowCapacityChange = flowCapacityChange;
		this.freespeedChange = freespeedChange;
		this.lanesChange = lanesChange;
		this.factor = factor;
	
	}

	@Override
	public String toString() {
		return "MessagesPlusFactor [msId=" + msId + ", link=" + link
				+ ", eventCode=" + eventCode + ", startTime=" + startTime
				+ ", endTime=" + endTime + ", flowCapacityChange="
				+ flowCapacityChange + ", freespeedChange=" + freespeedChange
				+ ", lanesChange=" + lanesChange + ", factor=" + factor + "]";
	}

	
	
	
}
