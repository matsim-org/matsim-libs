package playground.dziemke.cemdapMatsimCadyts;


public class Zone2Zone {

	private int source;
	private int sink;
	private int adjacent = 0;
	private double distance;
			
	
	public Zone2Zone(int source, int sink, double distance) {
		this.source = source;
		this.sink = sink;
		this.distance = distance;
	}

	public int getSource() {
		return this.source;
	}

	public void setSource(int source) {
		this.source = source;
	}

	public int getSink() {
		return this.sink;
	}

	public void setSink(int sinik) {
		this.sink = sinik;
	}
	
	public int getAdjacent() {
		return this.adjacent;
	}

	public void setAdjacent(int adjacent) {
		this.adjacent = adjacent;
	}
	
	public double getDistance() {
		return this.distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}
	
}