/**
 * 
 */
package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.api.Offer;

/**
 * @author stefan
 *
 */
public class TSPOffer implements Offer{
	
	private Id tspId;
	
	private double price;

	public Id getId() {
		return tspId;
	}

	public void setId(Id tspId) {
		this.tspId = tspId;
	}

	public Double getPrice() {
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
