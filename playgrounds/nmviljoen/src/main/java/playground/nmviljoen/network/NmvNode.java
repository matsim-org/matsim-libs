package playground.nmviljoen.network;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

public class NmvNode implements Comparable<NmvNode>{
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

	@Override
	public int compareTo(NmvNode o) {
		return this.getId().compareTo(o.getId().toString());
	}
	
}