package playground.jjoubert.CommercialTraffic;

import com.vividsolutions.jts.geom.MultiPolygon;

/**
 * A class to represent Geospatial Analysis Platform (GAP) mesozones,
 * but specifically for the Gauteng area. 
 * 
 * ... Could consider changing the code to rather use the more generic
 *     SAZone. 
 * 
 * @author johanwjoubert
 *
 */
public class GapZone {

	private final int gapID;
	private int minorActivityCount;
	private int majorActivityCount;
	private final MultiPolygon gapPolygon;
	private Integer[] activityCount;
	private Integer[] activityDuration;
	
	/**
	 * Constructs a new Geospatial Analysis Platform (GAP) mesozone.
	 *
	 * @param ID of type {@code int}, the GAP_ID as established by the CSIR
	 * @param poly of type {@code MultiPolygon},	the polygon of the mesozone
	 */
	public GapZone(int ID, MultiPolygon poly){
		this.gapID = ID;
		this.gapPolygon = poly;
		this.minorActivityCount = 0;
		this.majorActivityCount = 0;
		this.activityCount = new Integer[24];
		this.activityDuration = new Integer[24];
		for(int i = 0; i < this.activityCount.length; i++ ){
			this.activityCount[i] = 0;
			this.activityDuration[i] = 0;
		}	
	}

	public int getMinorActivityCount() {
		return minorActivityCount;
	}

	public void setMinorActivityCount(int minorCount) {
		this.minorActivityCount = minorCount;
	}

	public int getMajorActivityCount() {
		return majorActivityCount;
	}

	public void setMajorActivityCount(int majorCount) {
		this.majorActivityCount = majorCount;
	}

	public int getGapID() {
		return gapID;
	}

	public MultiPolygon getGapPolygon() {
		return gapPolygon;
	}
	
	public void incrementActivityCount(int hour){
		this.activityCount[hour]++;
	}
	
	public void increaseActivityDuration(int hour, int duration){
		this.activityDuration[hour] += duration;
	}
	
	public int getActivityCount(int hour){
		return this.activityCount[hour];
	}

	public int getActivityDuration(int hour){
		return this.activityDuration[hour];
	}

	/**
	 * The {@code activityDuration} field previously merely
	 * contained the sum of the activity durations. This method
	 * thus divides the 'total' duration by the number of
	 * activities.  
	 */
	public void calculateAverageDuration(){
		for (int i = 0; i < this.activityDuration.length; i++ ) {
			if(this.activityCount[i] > 0){
				double x = ((double) this.activityDuration[i] ) / 
						   ((double) this.activityCount[i] ); 
				this.activityDuration[i] = (int)x;
			} else{
				this.activityDuration[i] = 0;
			}
		}
	}

}
