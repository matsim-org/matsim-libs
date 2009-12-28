package playground.anhorni.crossborder;

import org.matsim.api.core.v01.Id;

public class MyLink {
	
	private Id id;
	private double volume;


	public MyLink(){}
	
	public MyLink(Id id, double volume) {
		this.id=id;
		this.volume=volume;
	}
	
	public Id getId() {
		return id;
	}
	public void setId(Id id) {
		this.id = id;
	}
	public double getVolume() {
		return volume;
	}
	public void setVolume(double volume) {
		this.volume = volume;
	}
}
