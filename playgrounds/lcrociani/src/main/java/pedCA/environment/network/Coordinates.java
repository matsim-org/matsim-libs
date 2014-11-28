package pedCA.environment.network;

import java.io.Serializable;

import matsimConnector.utility.Constants;
import pedCA.environment.grid.GridPoint;

public class Coordinates implements Serializable{
	
	private static final long serialVersionUID = 1L;
	private double x;
	private double y;
	
	public Coordinates(GridPoint gp){
		this.x=gp.getX()*Constants.CA_CELL_SIDE;
		this.y=gp.getY()*Constants.CA_CELL_SIDE;
	}
	
	public Coordinates(double x, double y){
		this.x=x;
		this.y=y;
	}
	
	public double getX() {
		return x;
	}
	public void setX(double x) {
		this.x = x;
	}
	public double getY() {
		return y;
	}
	public void setY(double y) {
		this.y = y;
	}
	
	public String toString(){
		return "("+x+","+y+")";
	}
	
}
