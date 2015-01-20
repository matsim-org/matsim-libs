package playground.nmviljoen.network;

public class NmvLink implements Comparable<NmvLink>{
	String id;
	double weight;//capacity in this case
	double transProb;
	
	public NmvLink(String id, double weight, double transProb) {
		this.id = id;
		this.weight = weight;
		this.transProb = transProb;
	}
	
	public NmvLink(String id, double weight){
		this.id = id;
		this.weight = weight;
		this.transProb = -99;
	}
	
	public String getId(){
		return id;
	}
	
	public double getWeight(){
		return weight;
	}
	
	public void setTransProb(double newTransProb){
		transProb = newTransProb;
	}
	
	public double getTransProb(){
		return transProb;
	}
	
	public String toString() {
		return "id:" + id + " (weight: " + weight + ")";
	}

	@Override
	public int compareTo(NmvLink o) {
		return this.getId().compareTo(o.getId());
	}
	

}
