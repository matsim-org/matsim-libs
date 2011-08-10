package playground.gregor.grips.helper;


public class ProtoLink {

	private String id;
	private double freespeed;
	private double numOfLanes;
	private double capacity;
	private double length;
	private String fromNodeId;
	private String toNodeId;

	public String getId() {
		return this.id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public double getFreespeed() {
		return this.freespeed;
	}
	public void setFreespeed(double freespeed) {
		this.freespeed = freespeed;
	}
	public double getNumOfLanes() {
		return this.numOfLanes;
	}
	public void setNumOfLanes(double numOfLanes) {
		this.numOfLanes = numOfLanes;
	}
	public double getCapacity() {
		return this.capacity;
	}
	public void setCapacity(double capacity) {
		this.capacity = capacity;
	}
	public double getLength() {
		return this.length;
	}
	public void setLength(double length) {
		this.length = length;
	}
	public String getFromNodeId() {
		return this.fromNodeId;
	}
	public void setFromNodeId(String fromNodeId) {
		this.fromNodeId = fromNodeId;
	}
	public String getToNodeId() {
		return this.toNodeId;
	}
	public void setToNodeId(String toNodeId) {
		this.toNodeId = toNodeId;
	}

}
