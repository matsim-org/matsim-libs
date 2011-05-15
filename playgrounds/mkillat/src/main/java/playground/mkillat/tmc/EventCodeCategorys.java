package playground.mkillat.tmc;

public class EventCodeCategorys {

	int eventCode;
	String flowCapacityChange;
	String freespeedChange;
	String lanesChange;
	double factor;
	
	
	
	public EventCodeCategorys(int eventCode,String flowCapacityChange, 
			String freespeedChange, String lanesChange, double factor  ){
		this.eventCode = eventCode;
		this.flowCapacityChange = flowCapacityChange;
		this.freespeedChange = freespeedChange;
		this.lanesChange = lanesChange;
		this.factor = factor;
	}



	@Override
	public String toString() {
		return "EventCodeCategorys [eventCode=" + eventCode
				+ ", flowCapacityChange=" + flowCapacityChange
				+ ", freespeedChange=" + freespeedChange + ", lanesChange="
				+ lanesChange + ", factor=" + factor + "]";
	}
	
	
	
}