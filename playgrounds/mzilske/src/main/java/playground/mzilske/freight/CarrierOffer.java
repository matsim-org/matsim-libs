package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;

public class CarrierOffer implements Offer {

	private Double duration;
	private Double price;
	private Id carrierId;

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.Offer#getId()
	 */
	@Override
	public Id getId() {
		return this.carrierId;
	}

	public Double getDuration() {
		return this.duration;
	}
	
	/* (non-Javadoc)
	 * @see playground.mzilske.freight.Offer#getPrice()
	 */
	@Override
	public Double getPrice() {
		return price;
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.Offer#setId(org.matsim.api.core.v01.Id)
	 */
	@Override
	public void setId(Id id) {
		this.carrierId = id;
	}

	public void setDuration(Double i) {
		this.duration = i;
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.Offer#setPrice(double)
	 */
	@Override
	public void setPrice(double d) {
		this.price = d;
	}
	
	public String toString(){
		return "[carrierId="+carrierId+"][price="+price+"][duration="+duration+"]";
	}

}
