package playground.dhosse.gap.scenario.mid;

import java.util.ArrayList;
import java.util.List;

import playground.dhosse.gap.Global;
import playground.dhosse.gap.scenario.mid.MiDCSVReader.MiDData;
import playground.dhosse.gap.scenario.mid.MiDCSVReader.MiDWaypoint;

/**
 * 
 * This is a wrapper class to store MiD data concerning a specific person group.
 * The group is defined by a lower and an upper bound for the age and a sex and provides data about
 * <ul>
 * <li>percentage of employed people</li>
 * <li>average legs per person and day</li>
 * <li>percentage of driving license owners</li>
 * <li>percentage of persons having cars available</li>
 * </ul>
 * 
 * @author dhosse
 *
 */
public class MiDPersonGroupData {
	
	//boundaries for age class
	private final int lowerBoundAge;
	private final int upperBoundAge;
	private final int sex;
	private List<MiDData> data;
	
	//average number of legs per person and day
	private double legsPerPersonAndDay;
	//percentage of employed persons within the group
	private double pEmployment;
	//percentage of persons in possession of a driving license
	private double pLicense;
	//percentage of persons having a driving license AND a car available
	//DOES NOT MEAN: car availability over all persons!!!
	private double pCarAvail;
	
	private MiDStatsContainer home;
	private MiDStatsContainer work;
	private MiDStatsContainer education;
	private MiDStatsContainer shop;
	private MiDStatsContainer leisure;
	private MiDStatsContainer other;
	
	/**
	 * 
	 * @param a0 the lower bound for the age class
	 * @param aX the upper bound for the age class
	 * @param sex
	 */
	public MiDPersonGroupData(int a0, int aX, int sex){
		
		this.lowerBoundAge = a0;
		this.upperBoundAge = aX;
		this.sex = sex;
		this.data = new ArrayList<>();
		
		this.home = new MiDStatsContainer();
		this.work = new MiDStatsContainer();
		this.education = new MiDStatsContainer();
		this.shop = new MiDStatsContainer();
		this.leisure = new MiDStatsContainer();
		this.other = new MiDStatsContainer();
		
	}
	
	public void setLegsPerPersonAndDay(double d){
		
		this.legsPerPersonAndDay = d;
		
	}
	
	public double getLegsPerPersonAndDay(){
		
		return this.legsPerPersonAndDay;
		
	}
	
	public int getA0(){
		
		return this.lowerBoundAge;
		
	}
	
	public int getAX(){
		
		return this.upperBoundAge;
		
	}
	
	public int getSex(){
		
		return this.sex;
		
	}
	
	public double getpEmployment() {
		return pEmployment;
	}

	public double getpLicense() {
		return pLicense;
	}

	public double getpCarAvail() {
		return pCarAvail;
	}

	public void setpEmployment(double pEmployment) {
		this.pEmployment = pEmployment;
	}

	public void setpLicense(double pLicense) {
		this.pLicense = pLicense;
	}

	public void setpCarAvail(double pCarAvail) {
		this.pCarAvail = pCarAvail;
	}
	
	public void addData(MiDData data){
		this.data.add(data);
	}
	
	public void addData(List<MiDData> data){
		
		if(data != null){

			for(MiDData d : data){
				
				for(MiDWaypoint wp : d.getWayPoints()){
					
					if(wp.purpose.equals(Global.ActType.home.name())){
						if(wp.distance != Double.NEGATIVE_INFINITY){
							this.home.handleDistance(wp.distance);
						}
						if(wp.duration != Double.NEGATIVE_INFINITY){
							this.home.handleDuration(wp.duration);
						}
						if(wp.startTime != Double.NEGATIVE_INFINITY){
							this.home.handleStartTime(wp.startTime);
						}
						if(wp.endTime != Double.NEGATIVE_INFINITY){
							this.home.handleEndTime(wp.endTime);
						}
					} else if(wp.purpose.equals(Global.ActType.work.name())){
						if(wp.distance != Double.NEGATIVE_INFINITY){
							this.work.handleDistance(wp.distance);
						}
						if(wp.duration != Double.NEGATIVE_INFINITY){
							this.work.handleDuration(wp.duration);
						}
						if(wp.startTime != Double.NEGATIVE_INFINITY){
							this.work.handleStartTime(wp.startTime);
						}
						if(wp.endTime != Double.NEGATIVE_INFINITY){
							this.work.handleEndTime(wp.endTime);
						}
					} else if(wp.purpose.equals(Global.ActType.education.name())){
						if(wp.distance != Double.NEGATIVE_INFINITY){
							this.education.handleDistance(wp.distance);
						}
						if(wp.duration != Double.NEGATIVE_INFINITY){
							this.education.handleDuration(wp.duration);
						}
						if(wp.startTime != Double.NEGATIVE_INFINITY){
							this.education.handleStartTime(wp.startTime);
						}
						if(wp.endTime != Double.NEGATIVE_INFINITY){
							this.education.handleEndTime(wp.endTime);
						}
					} else if(wp.purpose.equals(Global.ActType.shop.name())){
						if(wp.distance != Double.NEGATIVE_INFINITY){
							this.shop.handleDistance(wp.distance);
						}
						if(wp.duration != Double.NEGATIVE_INFINITY){
							this.shop.handleDuration(wp.duration);
						}
						if(wp.startTime != Double.NEGATIVE_INFINITY){
							this.shop.handleStartTime(wp.startTime);
						}
						if(wp.endTime != Double.NEGATIVE_INFINITY){
							this.shop.handleEndTime(wp.endTime);
						}
					} else if(wp.purpose.equals(Global.ActType.leisure.name())){
							if(wp.distance != Double.NEGATIVE_INFINITY){
								this.leisure.handleDistance(wp.distance);
							}
							if(wp.duration != Double.NEGATIVE_INFINITY){
								this.leisure.handleDuration(wp.duration);
							}
							if(wp.startTime != Double.NEGATIVE_INFINITY){
								this.leisure.handleStartTime(wp.startTime);
							}
							if(wp.endTime != Double.NEGATIVE_INFINITY){
								this.leisure.handleEndTime(wp.endTime);
							}
						} else if(wp.purpose.equals(Global.ActType.other.name())){
							if(wp.distance != Double.NEGATIVE_INFINITY){
								this.other.handleDistance(wp.distance);
							}
							if(wp.duration != Double.NEGATIVE_INFINITY){
								this.other.handleDuration(wp.duration);
							}
							if(wp.startTime != Double.NEGATIVE_INFINITY){
								this.other.handleStartTime(wp.startTime);
							}
							if(wp.endTime != Double.NEGATIVE_INFINITY){
								this.other.handleEndTime(wp.endTime);
							}
						}
				}
				
				this.data.add(d);
				
			}
			
		}
		
	}
	
	public List<MiDData> getData(){
		
		return this.data;
		
	}

	public MiDStatsContainer getHomeStats() {
		return home;
	}

	public MiDStatsContainer getWorkStats() {
		return work;
	}

	public MiDStatsContainer getEducationStats() {
		return education;
	}

	public MiDStatsContainer getShopStats() {
		return shop;
	}
	
	public MiDStatsContainer getLeisureStats(){
		return leisure;
	}
	
	public MiDStatsContainer getOtherStats(){
		return other;
	}

}
