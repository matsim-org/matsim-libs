package playground.anhorni.Nelson;

public class Route {
	
	private int number;
	
	private double length;
	private double riseAv;
	private double riseMax;
	private double bikeAv;
	private double tLights;
	private double PS;
	
	
	public Route(int number) {
		this.number = number;
	}
		
	public int getNumber() {
		return number;
	}
	public void setId(int number) {
		this.number = number;
	}
	public double getLength() {
		return length;
	}
	public void setLength(double length) {
		this.length = length;
	}
	public double getRiseAv() {
		return riseAv;
	}
	public void setRiseAv(double riseAv) {
		this.riseAv = riseAv;
	}
	public double getRiseMax() {
		return riseMax;
	}
	public void setRiseMax(double riseMax) {
		this.riseMax = riseMax;
	}
	public double getBikeAv() {
		return bikeAv;
	}
	public void setBikeAv(double bikeAv) {
		this.bikeAv = bikeAv;
	}
	public double getTLights() {
		return tLights;
	}
	public void setTLights(double lights) {
		tLights = lights;
	}
	public double getPS() {
		return PS;
	}
	public void setPS(double ps) {
		PS = ps;
	}
}
