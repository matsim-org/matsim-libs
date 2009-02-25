package playground.jjoubert.CommercialTraffic;

import java.util.ArrayList;
import java.util.GregorianCalendar;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * A class to represent a chain of commercial vehicle activities.
 * Each chain to start and end with a major activity.
 * 
 * @author johanwjoubert
 *
 */
public class Chain {
	private GregorianCalendar start;
	private ArrayList<Activity> activities;
	private int duration; // in MINUTES
	private int distance;
	private boolean inGauteng;
	
	// Constructor
	/**
	 * Creates an empty chain.
	 */
	public Chain(){
		this.start = null;
		this.activities = new ArrayList<Activity>();
		this.inGauteng = false;
	}
	

	/**
	 * Calculates the duration of the activity (in MINUTES) from the 
	 * end of the first major activity until the start of the second
	 * major activity, i.e. from when the vehicle switches on until
	 * it is switched off again.
	 */
	public void setDuration(){
		if(this.activities.size() > 0){
			this.duration = (int) ( ( (this.activities.get( this.activities.size() - 1).getStartTime().getTimeInMillis() ) -
							( this.start.getTimeInMillis() ) ) / (1000 * 60) );
		} else{
			this.duration = 0;
		}
	}
	
	/**
	 * Calculates the distance of the chain. Since activity locations
	 * should be expressed in the UTM35S coordinate system for South
	 * Africa, distance is expressed in meters.
	 */
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

		this.distance = (int)(dist);
	}

	public GregorianCalendar getDayStart() {
		return start;
	}

	/**
	 *  Sets the start time of the chain.
	 *  
	 * @param dayStart of type <code>GregorianCalendar<code>
	 */
	public void setDayStart(GregorianCalendar dayStart) {
		this.start = dayStart;
	}

	public int getDuration() {
		return duration;
	}

	public int getDistance() {
		return distance;
	}

	/** 
	 * Returns the Gauteng status of the chain: <code>true<code> if
	 * any of the activities of the chain (major or minor) occur in
	 * Gauteng, <code>false<code> otherwise.
	 * 
	 * @return inGauteng of type <code>boolean<code>
	 */
	public boolean isInGauteng() {
		return inGauteng;
	}

	/**
	 * Sets whether the chain has an activity in Gauteng or not.
	 * 
	 * @param inGauteng of type <code>boolean<code>
	 */
	public void setInGauteng(boolean inGauteng) {
		this.inGauteng = inGauteng;
	}
	
	/**
	 * Adds an activity to the chain.
	 * 
	 * @param activity
	 */
	public void addActivity(Activity activity){
		this.activities.add(activity);
	}
	
	/**
	 * Returns the array list of activities currently assigned to the chain.
	 * 
	 * @return 	ArrayList < Activity > 
	 */
	public ArrayList<Activity> getActivities(){
		return this.activities;
	}
	
	

}
