package playground.wrashid.PHEV.Utility;

import java.util.Iterator;
import java.util.PriorityQueue;



public class EnergyConsumptionSamples{
	
	private PriorityQueue<AverageSpeedEnergyConsumption> queue=new PriorityQueue<AverageSpeedEnergyConsumption>();
	private AverageSpeedEnergyConsumption zeroSpeedConsumption=new AverageSpeedEnergyConsumption(0,0);
	
	public void add(AverageSpeedEnergyConsumption averageConsumption){
		queue.add(averageConsumption);
	}
	
	
	public void clearSamples(){
		queue.clear();
	}
	
	// returns for a given speed the energy consumption
	// precondition: use this method only, if at least one average consumption added to sample
	public double getInterpolatedEnergyConsumption(double speed, double distance){
		assert(queue.size()>0);
		
		Iterator<AverageSpeedEnergyConsumption> iter=queue.iterator();
		while (iter.hasNext()){
			AverageSpeedEnergyConsumption averageConsumption=iter.next();
			
			// if speed is equal to a sample speed
			if (averageConsumption.getSpeed()==speed){
				return averageConsumption.getEnergyConsumption()*distance;
			}
			// only if smaller than first sample speed
			if (averageConsumption.getSpeed()>speed){
				return getInterpolatedValue(zeroSpeedConsumption,averageConsumption,speed)*distance;
			}
			
			AverageSpeedEnergyConsumption previousConsumption=null;
			
			while (averageConsumption.getSpeed()<speed && iter.hasNext()){
				previousConsumption=averageConsumption;
				averageConsumption=iter.next();
			}
			
			if (iter.hasNext()){
				// if there are more elements in the list, then interpolate at that point
				return getInterpolatedValue(previousConsumption,averageConsumption,speed)*distance;
			} else {
				// if last element, then interpolat last and first point
				return getInterpolatedValue(zeroSpeedConsumption,averageConsumption,speed)*distance;
			}
			
		}
		
		// this case should never happen (if precondition fulfilled)
		assert(false);
		return 0;
	}
	
	// gives for the speed an interpolated energy
	// precondition: speed of consumption1 is smaller than consumption2 (not equal!)
	// note: energyConsumption of consumption1 can be large than that of consumption2
	// note: speed can be bigger than both consumption1 and consumption2 speed (but not smaller than consumption 1)
	public static double getInterpolatedValue(AverageSpeedEnergyConsumption consumption1, AverageSpeedEnergyConsumption consumption2, double speed){
		assert(consumption1.getSpeed()<consumption2.getSpeed());
		
		double differenceSpeed=consumption2.getSpeed()-consumption1.getSpeed();
		double interpolationFactor=(speed-consumption1.getSpeed())/differenceSpeed;
		
		double differenceEnergyConsumption=consumption2.getEnergyConsumption()-consumption1.getEnergyConsumption();
		
		double result=consumption1.getEnergyConsumption()+interpolationFactor*differenceEnergyConsumption;
		return result;
	}
	
}