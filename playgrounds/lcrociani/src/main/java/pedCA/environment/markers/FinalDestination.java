package pedCA.environment.markers;

import java.util.ArrayList;

import matsimConnector.environment.TransitionArea;
import pedCA.environment.grid.GridPoint;
import pedCA.environment.network.Coordinates;

public class FinalDestination extends TacticalDestination  {

	private static final long serialVersionUID = 1L;
	//for the transition area
	private int rotation = -1;
	private GridPoint environmentRef;
	private TransitionArea transitionArea;
	
	public FinalDestination(Coordinates coordinates, ArrayList<GridPoint> cells) {
		super(coordinates, cells, false);
		calculateRotationAndRef();
	}

	//TODO test!!!
	private void calculateRotationAndRef() {
		if (getCells().size()>1){
			GridPoint first = getCells().get(0);
			GridPoint second = getCells().get(1);
			GridPoint last = getCells().get(getCells().size()-1);
			if (first.getX()==second.getX() && first.getX() == 0){
				rotation = 0;
				environmentRef = first;
			}
			else if(first.getX()==second.getX() && first.getX() != 0){ //equal x but at the end of the environment grid
				rotation = 180;
				environmentRef = last; 
			}
			else if(first.getY()==second.getY() && first.getY() == 0){
				rotation = 90;
				environmentRef = last;
			}
			else if(first.getY()==second.getY() && first.getX() != 0) {//equal y but at the end of the environment grid
				rotation = 270;
				environmentRef = first;
			}		
		}else{
			environmentRef = getCells().get(0); 
			if(environmentRef.getX()==0)
				rotation = 0;
			else
				rotation = 180;				
		}
	}
	
	public GridPoint getEnvironmentRef(){
		return environmentRef;
	}
	
	public int getRotation(){
		return rotation;
	}

	//TODO CLEAN THIS
	public TransitionArea getTransitionArea() {
		return transitionArea;
	}

	public void setTransitionArea(TransitionArea transitionArea) {
		this.transitionArea = transitionArea;
	}
}
