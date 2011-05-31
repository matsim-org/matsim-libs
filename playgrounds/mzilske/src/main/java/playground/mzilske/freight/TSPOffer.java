/**
 * 
 */
package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;

/**
 * @author stefan
 *
 */
public class TSPOffer {
	
	private Id tspId;
	
	private double price;

	public Id getTspId() {
		return tspId;
	}

	public void setTspId(Id tspId) {
		this.tspId = tspId;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}
	
	@Override
	public String toString() {
		return "[tspId=" + tspId + "][price=" + price + "]";
	}

}
