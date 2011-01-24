package playground.mzilske.freight.vrp;

import org.matsim.api.core.v01.Id;

public class Saving {

	private Double saving;
	private Id origin;
	private Id destination;
	
	public Saving(Id origin, Id destination, Double saving) {
		this.destination = destination;
		this.origin = origin;
		this.saving = saving;
	}
	
	public Double getSaving() {
		return saving;
	}
	
	public Id getOrigin() {
		return origin;
	}
	public Id getDestination() {
		return destination;
	}
	
	public String toString(){
		return "saving={"+origin+","+destination+","+saving+"}";
	}
	
}
