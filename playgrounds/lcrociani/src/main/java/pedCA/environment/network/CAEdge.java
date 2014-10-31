package pedCA.environment.network;

import pedCA.utility.Distances;

public class CAEdge {
	private CANode n1;
	private CANode n2;
	private double length;
	
	public CAEdge(CANode n1, CANode n2){
		this.n1 = n1;
		this.n2 = n2;
		calculateLength();
	}

	private void calculateLength() {
		//TODO Take the distance from the floor field
		length = Distances.EuclideanDistance(n1.getCoordinates(), n2.getCoordinates());
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
