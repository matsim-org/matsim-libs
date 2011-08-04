package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;

public interface Offer {

	public abstract Id getId();

	public abstract Double getPrice();

	public abstract void setId(Id id);

	public abstract void setPrice(double d);

}