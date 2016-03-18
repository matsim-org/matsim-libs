package playground.dhosse.scenarios.generic.population.io.mid;

import java.util.LinkedList;

public class MiDPlan {

	private LinkedList<MiDPlanElement> planElements;
	private int dayOfTheWeek;
	
	public MiDPlan(){
		
		this.planElements = new LinkedList<>();
		
	}

	public LinkedList<MiDPlanElement> getPlanElements() {
		
		return this.planElements;
		
	}

	public int getDayOfTheWeek() {
		
		return this.dayOfTheWeek;
		
	}

	public void setDayOfTheWeek(int dayOfTheWeek) {
		
		this.dayOfTheWeek = dayOfTheWeek;
		
	}
	
}