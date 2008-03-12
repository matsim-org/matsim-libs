package playground.anhorni.crossborder;

import org.matsim.utils.identifiers.IdI;

public class MyLink {
	
	private IdI id;
	private double volume;


	public MyLink(){}
	
	public MyLink(IdI id, double volume) {
		this.id=id;
		this.volume=volume;
	}
	
	public IdI getId() {
		return id;
	}
	public void setId(IdI id) {
		this.id = id;
	}
	public double getVolume() {
		return volume;
	}
	public void setVolume(double volume) {
		this.volume = volume;
	}
}
