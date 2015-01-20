package playground.nmviljoen.network;

public class NmvNode {
	public String intID;
	public String id;
	public double X;
	public double Y;

	public NmvNode(String intID, String id, double X, double Y) {
		this.intID = intID;
		this.id = id;
		this.X = X;
		this.Y = Y;
	}
	
	public String toString() {
		return "id: " + id + " ("+ X + ";"+ Y + ")";
	}   
	
	public String getId(){
		return this.id;
	}
	
	public String getXAsString(){
		return Double.toString(X);
	}
	
	public String getYAsString(){
		return Double.toString(Y);
	}

}