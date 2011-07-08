package playground.wrashid.sschieffer.DSC.LP;

import java.io.IOException;

import lpsolve.LpSolveException;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.DrivingInterval;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.ParkingInterval;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.Schedule;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.TimeInterval;

public class EnergyFromEngineCheckPHEV {

	private int reductionOfSOCStartingAtIntervalI;
	private double lastIntervalI, reductionOfSOC, energyFromEngine;
	
	private Schedule workingSchedule;
	private boolean status;// true= above 0= still energy from battery	
	private boolean iterate;
	
	private double totalBelowZero, totalBelowSinceLastZero;
		
	private double [] solution;	
	private double [] statusJoule;	
	private double [] actualJoule;	
	
	public EnergyFromEngineCheckPHEV(){
		
	}
	
	
	public double getReductionOfSOC(){
		return reductionOfSOC;
	}
	
	public int getReductionOfSOCStartingAtInterval(){
		return reductionOfSOCStartingAtIntervalI;
	}
	
	public boolean isIterate(){
		return iterate;
	}
	
	public Schedule getWorkingSchedule(){
		return workingSchedule;
	}
	
	public double getTotalEnergyFromEngine(){
		return energyFromEngine;
	}
	
	public void checkStatus(double statusJoule){
		if(statusJoule>=0){
			status=true;
			}else{
				status=false;
			}
	}
	
	public void setlastIntervalI(int lastIntervalI){
		this.lastIntervalI=lastIntervalI;
	}
	
	
	
	/**
	 * add energy from charging or subtract energy driven
	 * @param t
	 */
	public void updateStatusJoule(TimeInterval t, int i){
		if(t.isParking()){
			
			ParkingInterval thisP= (ParkingInterval)t;
			actualJoule[1+i]=actualJoule[i]+thisP.getChargingSpeed()*solution[1+i];// charging speed * time= joules
			
			statusJoule[1+i]=statusJoule[i]+thisP.getChargingSpeed()
						*solution[1+i];// charging speed * time= joules
		}else{
			DrivingInterval thisD = (DrivingInterval)t;
			actualJoule[1+i]=actualJoule[i]+(-1)*(thisD).getTotalConsumption();
			statusJoule[1+i]=statusJoule[i]+(-1)*(thisD).getTotalConsumption();
		}
	}
	
	
	
	public void newReductionOfSOCStartingAtIntervalI(int i, double totalBelowZero){
		if (i>lastIntervalI){
			reductionOfSOCStartingAtIntervalI=i;
			reductionOfSOC=totalBelowZero;
			lastIntervalI=workingSchedule.getNumberOfEntries();}	
	}
	
	
	/**
	 * 
	 * @param s
	 * @param pos
	 * @param energyFromEngine
	 * @param thisD
	 * @param precedingP
	 */
	public void updatePrecedingParkingAndDrivingInterval(Schedule s, int pos, double energyFromEngine, DrivingInterval thisD, ParkingInterval precedingP){
		
		double chargingTime=energyFromEngine/
		( precedingP).getChargingSpeed();
		
		//s.printSchedule();
		s.reduceFollowingParkingBy( pos, chargingTime);
		//s.printSchedule();
		s.addExtraConsumptionDriving( pos, energyFromEngine);
		
	}
	
	/**
	 * find following parking interval important to get the charging speed in updatePrecedingParkingAndDrivingInterval
	 * in which now less charging is necessary
	 * @param i
	 * @return
	 */
	public ParkingInterval  findReferenceChargingSpeedParkingInterval(int i, Schedule workingSchedule){
		ParkingInterval followingP;//only need it for charging speed
		if(i<workingSchedule.getNumberOfEntries()-1){
			followingP= (ParkingInterval)workingSchedule.timesInSchedule.get(i+1);
			
		}else{
			//otherwise parking is first interval on next day
			followingP= (ParkingInterval)workingSchedule.timesInSchedule.get(0);
			
		}
		return followingP;
	}
	
	
	
	public void run(Schedule s, 
			double [] solution, 
			int reductionOfSOCStartingAtIntervalI		
			) throws LpSolveException, IOException{
		
		this.solution=solution;
		workingSchedule=s.cloneSchedule();
		setlastIntervalI(reductionOfSOCStartingAtIntervalI);
		iterate=false;
		
		totalBelowZero=0;
		totalBelowSinceLastZero=0;
				
		energyFromEngine=0;			
				
		statusJoule=new double[solution.length];
		actualJoule=new double[solution.length];
		
		statusJoule[0]=solution[0];
		actualJoule[0]=solution[0];
		
		checkStatus(statusJoule[0]);
		
		
		for(int i=0; i<workingSchedule.getNumberOfEntries(); i++){
			
			updateStatusJoule(workingSchedule.timesInSchedule.get(i), i);
			
			// before + and still + //nothing
			
			// before + and now -
			if (status==true && statusJoule[i+1]<0){
				// going below zero
				double newEnergy=Math.abs(statusJoule[i+1]);
				energyFromEngine+=newEnergy;
				actualJoule[i+1]=actualJoule[i+1]+newEnergy;
				checkStatus(statusJoule[i+1]);
								
				totalBelowZero+=newEnergy;
				totalBelowSinceLastZero+=newEnergy;
				
				newReductionOfSOCStartingAtIntervalI(i,totalBelowZero);
			
				if(workingSchedule.timesInSchedule.get(i).isDriving()){
					updatePrecedingParkingAndDrivingInterval(workingSchedule, 
							i,newEnergy, 
							(DrivingInterval)workingSchedule.timesInSchedule.get(i), 
							findReferenceChargingSpeedParkingInterval(i, workingSchedule));
				}
				
			}else{
				// before - and still -
				if (status==false && statusJoule[i+1]<0){
					double lastJoule=statusJoule[i];
					double newEnergy=Math.abs(statusJoule[1+i]-lastJoule);
					
					iterate=true;
					
					if(statusJoule[1+i]<lastJoule){
						// took more energy from engine
						energyFromEngine+=newEnergy;
						
						totalBelowZero+=newEnergy;
						totalBelowSinceLastZero+=newEnergy;
						
						newReductionOfSOCStartingAtIntervalI(i, totalBelowZero);
							
						if(workingSchedule.timesInSchedule.get(i).isDriving()){
							updatePrecedingParkingAndDrivingInterval(workingSchedule, 
									i, newEnergy,
									(DrivingInterval)workingSchedule.timesInSchedule.get(i), 
									findReferenceChargingSpeedParkingInterval(i, workingSchedule));
						}
						
					}else{
						// start recharging battery from 0!!!
						actualJoule[i+1]=actualJoule[i+1]+newEnergy;					
					}
					
				}else{
					// before - and now positive
					if (status==false && statusJoule[i+1]>=0){
						double lastJoule=statusJoule[i];
						double newEnergy=Math.abs(statusJoule[1+i]-lastJoule);
						
						actualJoule[i+1]=actualJoule[i+1]+newEnergy;
						statusJoule[i+1]=statusJoule[i+1]+newEnergy+totalBelowSinceLastZero;
						
						// start recharging battery from 0!!!
						iterate=true;
						status=true;
						totalBelowSinceLastZero=0;
					}
				}
			}
			
		}	
		
	}
	
	
	
	
	
	
	
}
