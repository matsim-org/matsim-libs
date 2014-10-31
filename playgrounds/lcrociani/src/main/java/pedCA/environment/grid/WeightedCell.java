package pedCA.environment.grid;

import pedCA.environment.grid.GridPoint;

public class WeightedCell{
	public int x;
	public int y;
	public double p;
	
	public WeightedCell(GridPoint gp, double p){
		this.x = gp.getX();
		this.y = gp.getY();
		this.p = p;		
	}
	
	public String toString(){
		return "("+x+","+y+","+p+")";
	}
}
