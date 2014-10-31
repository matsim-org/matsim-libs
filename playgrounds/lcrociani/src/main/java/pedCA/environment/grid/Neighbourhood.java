package pedCA.environment.grid;

import java.util.ArrayList;

public class Neighbourhood {
	private ArrayList<GridPoint> neighbourhood;
	
	public Neighbourhood(){
		neighbourhood = new ArrayList<GridPoint>();
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
}
