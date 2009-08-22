package playground.jjoubert.CommercialTraffic;

import java.util.ArrayList;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class SAZone extends MultiPolygon{

	/**
	 * @author johanwjoubert
	 */
	
	private static final long serialVersionUID = 1L;
	private String name;
	private int minorActivityCount;
	private int majorActivityCount;
	private int level;
	private ArrayList<SAZone> polygonContainer;
	private Integer[] minorActivityCountDetail;
	private Integer[] minorActivityDurationDetail;
	private Integer[] majorActivityCountDetail;
	private Integer[] majorActivityDurationDetail;
	private Double[] speedDetail;
	private Integer[] speedCount;

	private static final int TIME_BINS = 24;
	private int timeBins;


	public SAZone(Polygon[] polygons, GeometryFactory factory, String name, int level) {
		super(polygons, factory);
		
		this.setName(name);
		this.setLevel(level);
		this.polygonContainer = new ArrayList<SAZone>();
		this.minorActivityCount = 0;
		this.majorActivityCount = 0;
		this.timeBins = TIME_BINS;
		this.minorActivityCountDetail = new Integer[this.timeBins];
		this.minorActivityDurationDetail = new Integer[this.timeBins];
		this.majorActivityCountDetail = new Integer[this.timeBins];
		this.majorActivityDurationDetail = new Integer[this.timeBins];
		this.speedDetail = new Double[this.timeBins];
		this.speedCount = new Integer[this.timeBins];
		
		// TODO Check if this is fine
		this.speedDetail = new Double[this.timeBins];
		
		for(int i = 0; i < this.timeBins; i++ ){
			this.minorActivityCountDetail[i] = 0;
			this.minorActivityDurationDetail[i] = 0;
			this.majorActivityCountDetail[i] = 0;
			this.majorActivityDurationDetail[i] = 0;
			this.speedDetail[i] = 0.0;
			this.speedCount[i] = 0;
		}	
	}
	
	public int getTimeBins() {
		return this.timeBins;
	}

	public void updateSAZoneCounts(boolean minor){
		if( minor ){
			this.setMinorActivityCount();
			this.calculateAverageDurationMinor();
		} else{
			this.setMajorActivityCount();
			this.calculateAverageDurationMajor();
		}
	}
	
	// Minor activity details
	public void incrementMinorActivityCountDetail(int hour){
		this.minorActivityCountDetail[hour]++;
	}
	
	public void increaseMinorActivityDurationDetail(int hour, int duration){
		this.minorActivityDurationDetail[hour] += duration;
	}
	
	public int getMinorActivityCountDetail(int hour){
		return this.minorActivityCountDetail[hour];
	}
	
	public void setMinorActivityCountDetail(int hour, int value){
		this.minorActivityCountDetail[hour] = value;
	}

	public int getMinorActivityDurationDetail(int hour){
		return this.minorActivityDurationDetail[hour];
	}

	/*
	 * The {@code activityDuration} field previously merely
	 * contained the sum of the activity durations. This method
	 * thus divides the 'total' duration by the number of
	 * activities.  
	 */
	private void calculateAverageDurationMinor(){
		for (int i = 0; i < this.timeBins; i++ ) {
			if(this.minorActivityDurationDetail[i] > 0){
				double x = ((double) this.minorActivityDurationDetail[i] ) / 
						   ((double) this.minorActivityCountDetail[i] ); 
				this.minorActivityDurationDetail[i] = (int)x;
			} else{
				this.minorActivityDurationDetail[i] = 0;
			}
		}
	}

	// Major activity details
	public void incrementMajorActivityCountDetail(int hour){
		this.majorActivityCountDetail[hour]++;
	}
	
	public void increaseMajorActivityDurationDetail(int hour, int duration){
		this.majorActivityDurationDetail[hour] += duration;
	}
	
	public int getMajorActivityCountDetail(int hour){
		return this.majorActivityCountDetail[hour];
	}

	public int getMajorActivityDurationDetail(int hour){
		return this.majorActivityDurationDetail[hour];
	}
	
	// Private vehicle speed details
	public Double getPrivateVehicleSpeedDetail(int i) {
		return this.speedDetail[i];
	}

	public void setPrivateVehicleSpeedDetail(int i, double d) {
		this.speedDetail[i] = d;
	}

	

	/**
	 * The {@code activityDuration} field previously merely
	 * contained the sum of the activity durations. This method
	 * thus divides the 'total' duration by the number of
	 * activities.  
	 */
	private void calculateAverageDurationMajor(){
		for (int i = 0; i < this.timeBins; i++ ) {
			if(this.majorActivityDurationDetail[i] > 0){
				double x = ((double) this.majorActivityDurationDetail[i] ) / 
						   ((double) this.majorActivityCountDetail[i] ); 
				this.majorActivityDurationDetail[i] = (int)x;
			} else{
				this.majorActivityDurationDetail[i] = 0;
			}
		}
	}
	
	

	private void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	private void setMinorActivityCount() {
		int total = 0;
		for (int thisInt : minorActivityCountDetail) {
			total += thisInt;
		}
		this.minorActivityCount = total;
	}

	public int getMinorActivityCount() {
		return minorActivityCount;
	}

	private void setMajorActivityCount() {
		int total = 0;
		for (int thisInt : majorActivityCountDetail) {
			total += thisInt;
		}
		this.majorActivityCount = total;
	}

	public int getMajorActivityCount() {
		return majorActivityCount;
	}

	public void addToPolygonContainer(SAZone newZone) {
		if(newZone.level > this.level ){ // only add zones with higher levels
			this.polygonContainer.add(newZone );
		} else{
			System.out.println("Cannot add a polygon of the same level");
		}
	}

	public ArrayList<SAZone> getPolygonContainer() {
		return polygonContainer;
	}

	private void setLevel(int level) {
		this.level = level;
	}

	public int getLevel() {
		return level;
	}
	
	public void clearSAZone(){
		this.polygonContainer = new ArrayList<SAZone>();
		this.minorActivityCount = 0;
		this.majorActivityCount = 0;		
		for(int i = 0; i < this.timeBins; i++ ){
			this.minorActivityCountDetail[i] = 0;
			this.minorActivityDurationDetail[i] = 0;
			this.majorActivityCountDetail[i] = 0;
			this.majorActivityDurationDetail[i] = 0;
			this.speedDetail[i] = 0.0;
			this.speedCount[i] = 0;
		}
		// Also, clear the polygon container
		this.polygonContainer = new ArrayList<SAZone>();
	}
	
	public void incrementSpeedCount(int i){
		this.speedCount[i]++;
	}
	
	public void addToSpeedDetail(int i, Double d){
		this.speedDetail[i] += d;
	}
	
	public void calculateAverageSpeed(){
		for(int i = 0; i < this.timeBins; i++){
			if(this.speedCount[i] > 0){
				this.speedDetail[i] /= ((double) this.speedCount[i]);
			}
		}
	}

	public Double[] getSpeedDetail() {
		return speedDetail;
	}

	public Integer[] getSpeedCount() {
		return speedCount;
	}

}