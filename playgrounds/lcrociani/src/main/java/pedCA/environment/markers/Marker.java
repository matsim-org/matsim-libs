package pedCA.environment.markers;

import java.io.Serializable;
import java.util.ArrayList;

import pedCA.environment.grid.GridPoint;

public abstract class Marker implements Serializable{
	
	private static final long serialVersionUID = 1L;
	protected ArrayList <GridPoint> cells;
	//TODO link to matsim node here
	
	public Marker (ArrayList<GridPoint> cells){
		this.cells = cells;
	}

	public ArrayList<GridPoint> getCells(){
		return cells;
	}
	
	public int size(){
		return cells.size();
	}
	
	public GridPoint get(int i){
		return cells.get(i);
	}
	
}
