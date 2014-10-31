package pedCA.agents;

import java.util.ArrayList;

public class Population {
	private ArrayList<Agent> pedestrians;
	
	public Population(){
		pedestrians = new ArrayList<Agent>();
	}
	
	public void addPedestrian(Agent pedestrian){
		pedestrians.add(pedestrian);
	}
	
	public void remove(Agent pedestrian){
		pedestrians.remove(pedestrian);
	}
	
	public Agent getPedestrian(int index){
		return pedestrians.get(index);
	}
	
	public ArrayList<Agent> getPedestrians(){
		return pedestrians;
	}

	public int size(){
		return pedestrians.size();
	}
	
	public boolean isEmpty() {
		return pedestrians.size()==0;
	}
}
