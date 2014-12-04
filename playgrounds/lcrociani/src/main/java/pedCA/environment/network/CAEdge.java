package pedCA.environment.network;

public class CAEdge {
	private CANode n1;
	private CANode n2;
	private double length;
	
	public CAEdge(CANode n1, CANode n2, double ffDistance){
		this.n1 = n1;
		this.n2 = n2;
		this.length = ffDistance;
	}

	public double getLength(){
		return length;
	}
	
	public CANode getN1(){
		return n1;
	}
	
	public CANode getN2(){
		return n2;
	}
	
	@Override
	public String toString(){
		String result = super.toString()+"\n";
		result += "FROM: "+n1.getCoordinates()+" to "+n2.getCoordinates() +"\n";
		result += "LENGTH: "+length;
		return result;
	}
}
