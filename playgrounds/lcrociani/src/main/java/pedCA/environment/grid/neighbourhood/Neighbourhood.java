package pedCA.environment.grid.neighbourhood;

import java.util.ArrayList;

import pedCA.environment.grid.GridPoint;

public class Neighbourhood {
	private ArrayList<GridPoint> neighbourhood;
	
	public Neighbourhood(){
		neighbourhood = new ArrayList<GridPoint>();
	}
	
	public Neighbourhood(ArrayList<GridPoint> neighbourhood){
		this.neighbourhood = neighbourhood;
	}
	
	public void add(GridPoint gp){
		neighbourhood.add(gp);
	}
	
	public GridPoint get(int i){
		return neighbourhood.get(i);
	}
	
	public int size(){
		return neighbourhood.size();
	}	
	
	public ArrayList<GridPoint> getObjects(){
		return neighbourhood;
	}
}
