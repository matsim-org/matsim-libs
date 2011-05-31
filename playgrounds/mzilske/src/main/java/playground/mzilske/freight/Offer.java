package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;

public class Offer {

	private Double duration;
	private Double price;
	private Id carrierId;

	public Id getCarrierId() {
		return this.carrierId;
	}

	public Double getDuration() {
		return this.duration;
	}
	
	public Double getPrice() {
		return price;
	}

	public void setCarrierId(Id id) {
		this.carrierId = id;
	}

	public void setDuration(Double i) {
		this.duration = i;
	}

	public void setPrice(double d) {
		this.price = d;
	}
	
	public String toString(){
		return "[carrierId="+carrierId+"][price="+price+"][duration="+duration+"]";
	}

}
