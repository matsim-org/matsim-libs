package playground.dziemke.cemdapMatsimCadyts;

public class CommuterRelation {

	private int from;
	private String fromName;
	private int to;
	private String toName;
	private int quantity;
		
	public CommuterRelation(int from, String fromName, int to, String toName, int quantity) {
		this.from = from;
		this.fromName = fromName;
		this.to = to;
		this.toName = toName;
		this.quantity = quantity;
	}

	public int getFrom() {
		return this.from;
	}

	public void setFrom(int from) {
		this.from = from;
	}
	
	public String getFromName() {
		return this.fromName;
	}

	public void setFromName(String fromName) {
		this.fromName = fromName;
	}

	public int getTo() {
		return this.to;
	}

	public void setTo(int to) {
		this.to = to;
	}
	
	public String getToName() {
		return this.toName;
	}

	public void setToName(String toName) {
		this.toName = toName;
	}

	public int getQuantity() {
		return this.quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

}