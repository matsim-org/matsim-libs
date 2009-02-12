package playground.jjoubert.CommercialTraffic;

import java.util.ArrayList;
import java.util.GregorianCalendar;

import com.vividsolutions.jts.geom.Coordinate;

public class Chain {
	public GregorianCalendar start;
	public ArrayList<Activity> activities;
	public int duration; // in MINUTES
	public int distance;
	public boolean inGauteng;
	
	// Constructor
	public Chain(){
		this.start = null;
		this.activities = new ArrayList<Activity>();
		this.inGauteng = false;
	}
	
	// Methods
	public void setDuration(){
		if(this.activities.size() > 0){
			this.duration = (int) ( ( (this.activities.get( this.activities.size() - 1).getStartTime().getTimeInMillis() ) -
							( this.start.getTimeInMillis() ) ) / (1000 * 60) );
		} else{
			this.duration = 0;
		}
	}
	
	public void setDistance(){
		
		Coordinate c1 = this.activities.get(0).getLocation().getCoordinate();
		Coordinate c2 = this.activities.get(1).getLocation().getCoordinate();
		
		double dist = c1.distance(c2);

		Coordinate dummy = c2;
		for(int i = 2; i < this.activities.size(); i++){
			c1 = dummy;
			c2 = this.activities.get(i).getLocation().getCoordinate();
			dist += c1.distance(c2);
			dummy = c2;
		}

		this.distance = (int)(dist*Vehicle.DISTANCE_THRESHOLD);
	}

	public GregorianCalendar getDayStart() {
		return start;
	}

	public void setDayStart(GregorianCalendar dayStart) {
		this.start = dayStart;
	}

	public int getDuration() {
		return duration;
	}

	public int getDistance() {
		return distance;
	}

	public boolean isInGauteng() {
		return inGauteng;
	}

	public void setInGauteng(boolean inGauteng) {
		this.inGauteng = inGauteng;
	}
	
	

}
