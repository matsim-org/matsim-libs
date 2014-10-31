package pedCA.engine;

import pedCA.agents.Agent;
import pedCA.agents.Population;
import pedCA.context.Context;
import pedCA.output.Log;

public class AgentMover {
	//private final Context context;
	private final Population population;
	
	public AgentMover(Context context){
		//this.context = context;
		this.population = context.getPopulation();
	}
	
	public void step(){
		for(int index=0; index<population.size(); index++){
			Agent pedestrian = population.getPedestrian(index);
			if (pedestrian.isArrived()){
				Log.log(pedestrian.toString()+" exited.");
				moveToUniverse(pedestrian);
				index--;
			}
			else{
				pedestrian.move();
			}
		}
	}

	private void moveToUniverse(Agent pedestrian) {
		pedestrian.leavePedestrianGrid();
		population.remove(pedestrian);
	}
	
	protected Population getPopulation(){
		return population;
	}
}
