/**
 * 
 */
package org.matsim.contrib.parking.parkingsearch.evaluation;


/**
 * @author schlenther
 *
 */
public final class ParkingTuple implements Comparable{
	
	private double time;
	private double occupancy;
	
	public ParkingTuple(double time, double occupancy){
		this.time = time;
		this.occupancy = occupancy;
	}
	
	public double getTime(){
		return this.time;
	}
	
	public double getOccupancy(){
		return this.occupancy;
	}

	@Override
	public int compareTo(Object o) {
		if (!(o instanceof ParkingTuple)){
			throw new RuntimeException("cannot compare ParkingTuple with another object");
		}
		else{
			ParkingTuple other = (ParkingTuple) o;
			return (other.getTime() < this.getTime()) ? 1 : (other.getTime() == this.getTime() ? 0 : -1);
		}
	}
	
	@Override
	public String toString(){
		return "" + this.time + ":" + this.occupancy;
	}
	
	
}