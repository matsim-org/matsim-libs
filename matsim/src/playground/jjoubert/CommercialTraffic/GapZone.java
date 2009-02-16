package playground.jjoubert.CommercialTraffic;

import com.vividsolutions.jts.geom.MultiPolygon;

public class GapZone {

	private final int gapID;
	private int minorCount;
	private int majorCount;
	private final MultiPolygon gapPolygon;
	private Integer[] activityCount;
	private Integer[] activityDuration;
	
	public GapZone(int ID, MultiPolygon poly){
		this.gapID = ID;
		this.gapPolygon = poly;
		this.minorCount = 0;
		this.majorCount = 0;
		this.activityCount = new Integer[24];
		this.activityDuration = new Integer[24];
		for(int i = 0; i < this.activityCount.length; i++ ){
			this.activityCount[i] = 0;
			this.activityDuration[i] = 0;
		}
		
	}

	public int getMinorCount() {
		return minorCount;
	}

	public void setMinorCount(int minorCount) {
		this.minorCount = minorCount;
	}

	public int getMajorCount() {
		return majorCount;
	}

	public void setMajorCount(int majorCount) {
		this.majorCount = majorCount;
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

	public void update(){
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
