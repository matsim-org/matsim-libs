package playground.mkillat.tmc;

public class Message {

	String msId;
	String link;
	int eventCode;
	String startTime;
	String endTime;
	
	public Message(String msId, String link, int eventCode, String startTime, String endTime){
		this.msId = msId;
		this.link = link;
		this.eventCode = eventCode;
		this.startTime = startTime;
		this.endTime = endTime;
	}

	@Override
	public String toString() {
		return "Message [msId=" + msId + ", link=" + link + ", eventCode="
				+ eventCode + ", startTime=" + startTime + ", endTime="
				+ endTime + "]";
	}
	
}
