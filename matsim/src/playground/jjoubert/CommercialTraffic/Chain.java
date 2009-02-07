package playground.jjoubert.CommercialTraffic;

import java.util.ArrayList;

public class Chain {
	public long dayStart;
	public ArrayList<Activity> activities;
	public int duration;
	
	// Constructor
	public Chain(){
		this.dayStart = 0;
		this.activities = new ArrayList<Activity>();
	}
	
	// Methods
	public void calcDuration(){
		if(this.activities.size() > 0){
			this.duration = ( (int) (this.activities.get( this.activities.size() - 1).getStartTime() ) ) +
							( (int) (this.activities.get( this.activities.size() - 1).getDuration() ) ) -
							( (int) (this.activities.get(0).getStartTime() ) );
		} else{
			this.duration = 0;
		}
	}

}
