package freight;

import org.matsim.api.core.v01.Id;

public class CommodityFlow {
	
	private Id from;
	
	private Id to;
	
	private int size;
	
	private Double value;

	public CommodityFlow(Id from, Id to, int size, Double value) {
		super();
		this.from = from;
		this.to = to;
		this.size = size;
		this.value = value;
	}

	public Id getFrom() {
		return from;
	}

	public Id getTo() {
		return to;
	}

	public int getSize() {
		return size;
	}

	public Double getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return "[from=" +from+ "][to=" +to+ "][size=" + size + "][value=" + value + "]";
	}

}
