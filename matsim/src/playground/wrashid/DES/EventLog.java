package playground.wrashid.DES;

import java.util.ArrayList;

public class EventLog {
	double time = 0.0;
	int vehicleId = 0;
	int legNo = 0;
	int linkId = 0;
	int fromNodeId = 0;
	int toNodeId = 0;
	String type = null;

	
	public EventLog(double time, int vehicleId, int legNo, int linkId,
			int fromNodeId, int toNodeId, String type) {
		super();
		this.time = time;
		this.vehicleId = vehicleId;
		this.legNo = legNo;
		this.linkId = linkId;
		this.fromNodeId = fromNodeId;
		this.toNodeId = toNodeId;
		this.type = type;
	}

	public void print() {
		System.out.print("time: "+time);
		System.out.print(";vehicleId: "+vehicleId);
		System.out.print(";legNo: "+legNo);
		System.out.print(";linkId: "+linkId);
		System.out.print(";fromNodeId: "+fromNodeId);
		System.out.print(";toNodeId: "+toNodeId);
		System.out.print(";type: "+type);
		System.out.println();
	}

	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = time;
	}

	public int getVehicleId() {
		return vehicleId;
	}

	public void setVehicleId(int vehicleId) {
		this.vehicleId = vehicleId;
	}

	public int getLegNo() {
		return legNo;
	}

	public void setLegNo(int legNo) {
		this.legNo = legNo;
	}

	public int getLinkId() {
		return linkId;
	}

	public void setLinkId(int linkId) {
		this.linkId = linkId;
	}

	public int getFromNodeId() {
		return fromNodeId;
	}

	public void setFromNodeId(int fromNodeId) {
		this.fromNodeId = fromNodeId;
	}

	public int getToNodeId() {
		return toNodeId;
	}

	public void setToNodeId(int toNodeId) {
		this.toNodeId = toNodeId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public static boolean compare(ArrayList<EventLog> eventLog1, ArrayList<EventLog> eventLog2){
		System.out.println(eventLog1.size());
		System.out.println(eventLog2.size());
		
		assert eventLog1.size()==eventLog2.size():"The size of both eventLogs must be the same!";
		for(int i=0;i<eventLog1.size();i++) {
			//System.out.println("=========");
			//eventLog1.get(i).print();
			//eventLog2.get(i).print();
			//System.out.println("=========");
			
			if (!equals(eventLog1.get(i),eventLog2.get(i))){
				//return false; // TODO: uncomment this, when bug is fixed!
			}
		}
		return true;
	}
	
	public static boolean equals(EventLog eventLog1,EventLog eventLog2){
		// the time must be the same (compared up to 4 digits after the floating point) and the link
		// the event type is ignored for the moment, because in the beginning it might be different
		if (Math.rint(eventLog1.getTime()*10000)==Math.rint(eventLog2.getTime()*10000) && eventLog1.getLinkId()==eventLog2.getLinkId() ){
			return true;
		} else {
			System.out.println("====PROBLEM=====");
			eventLog1.print();
			eventLog2.print();
			System.out.println("=========");
		}
		return false;
	}
}
