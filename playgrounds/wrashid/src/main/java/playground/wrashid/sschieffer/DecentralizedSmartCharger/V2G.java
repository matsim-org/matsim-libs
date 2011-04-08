package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import java.io.IOException;

import lpsolve.LpSolveException;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.OptimizationException;
import org.matsim.api.core.v01.Id;

import playground.wrashid.lib.obj.LinkedListValueHashMap;

public class V2G {
	
	DecentralizedSmartCharger mySmartCharger;
	
	public V2G(DecentralizedSmartCharger mySmartCharger){
		
		this.mySmartCharger=mySmartCharger;
		
	}
	
	
	
	
	
	
	/**
	 * calculates if continuation of current plan is cheaper or rescheduling feasible and cheaper
	 * if rescheduling is the better option, the new schedule is stored in the agentParkingAndDrivingSchedules in DEcentralizedSmartCharger
	 * and the agentCharginCosts are also updated for the agent
	 * 
	 * @param agentId
	 * @param electricSourceInterval
	 * @param agentParkingDrivingSchedule
	 * @param compensation
	 * @param ev
	 * @param type
	 * @param lpev
	 * @param lpphev
	 * @param batterySize
	 * @param batteryMin
	 * @param batteryMax
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 * @throws LpSolveException
	 * @throws IOException
	 * @throws OptimizationException
	 */
	public void regulationUp(Id agentId, 
			LoadDistributionInterval electricSourceInterval, 
			Schedule agentParkingDrivingSchedule, 
			double compensation,
			double joules,
			boolean ev,
			String type,
			LPEV lpev,
			LPPHEV lpphev,
			double batterySize,
			double batteryMin,
			double batteryMax) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException, OptimizationException{
		
			
			
			double currentSOC=getSOCAtTime(agentId, agentParkingDrivingSchedule, electricSourceInterval.getEndTime());
			currentSOC=currentSOC-joules;
			
			Schedule secondHalf= cutScheduleAtTimeSecondHalf(agentId, 
					agentParkingDrivingSchedule, 
					electricSourceInterval.getEndTime());
						
			secondHalf.setStartingSOC(currentSOC);
			Schedule answerScheduleAfterElectricSourceInterval=null;
			
			double costKeeping=mySmartCharger.calculateChargingCostForAgentSchedule(secondHalf);
			double costReschedule=0;
			//***********************************
			//EV
			//***********************************
			if(ev){
				answerScheduleAfterElectricSourceInterval= lpev.solveLPReschedule(secondHalf, agentId, batterySize, batteryMin, batteryMax, type,currentSOC);
				
				if(answerScheduleAfterElectricSourceInterval==null){
					System.out.println("Reschedule was not possible!");
					costReschedule= 10000000000000000000000000000.0;
				}else{
					costReschedule=mySmartCharger.calculateChargingCostForAgentSchedule(answerScheduleAfterElectricSourceInterval)-compensation;
				}
				
				
			}else{
				//***********************************
				//PHEV
				//***********************************
				
				answerScheduleAfterElectricSourceInterval= lpev.solveLPReschedule(secondHalf, agentId, batterySize, batteryMin, batteryMax, type,currentSOC);
				if(answerScheduleAfterElectricSourceInterval==null){
					answerScheduleAfterElectricSourceInterval = lpphev.solveLPReschedule(secondHalf, agentId, batterySize, batteryMin, batteryMax, type, currentSOC);
				}
				if(answerScheduleAfterElectricSourceInterval==null){
					System.out.println("Reschedule was not possible!");
					costReschedule= 10000000000000000000000000000.0;
				}else{
					costReschedule=mySmartCharger.calculateChargingCostForAgentSchedule(answerScheduleAfterElectricSourceInterval)-compensation;
				}
				
			}
		
			if(costKeeping>costReschedule){
				//reschedule
				answerScheduleAfterElectricSourceInterval= mySmartCharger.myChargingSlotDistributor.distribute(agentId, answerScheduleAfterElectricSourceInterval);
				
				//merge with old schedule
				Schedule rescheduledFirstHalf= cutChargingScheduleAtTime(agentParkingDrivingSchedule, electricSourceInterval.getStartTime());
				
				Schedule rescheduledPart= cutChargingScheduleAtTime(rescheduledFirstHalf, electricSourceInterval.getEndTime());
				
				// TODO maybe?
				//TODO
				//TODO 
				//in rescheduled Part add joules... or somehow remember that u did this?
				
				rescheduledFirstHalf.mergeSchedules(rescheduledPart);
				rescheduledFirstHalf.mergeSchedules(answerScheduleAfterElectricSourceInterval);
				
				agentParkingDrivingSchedule=rescheduledFirstHalf;
				
				mySmartCharger.getAllAgentParkingAndDrivingSchedules().put(agentId, agentParkingDrivingSchedule);
				
				mySmartCharger.getChargingCostsForAgents().put(
						agentId, 
						(mySmartCharger.getChargingCostsForAgents().getValue(agentId)-costKeeping+costReschedule)
						);
				
			}else{
				// for how long parking AND NOT Charging --> joules to hub level above - add to stochastic Load
				// parking and charging --> joules is too much demand - chargingSpeed is limit
				// 
				// 
				// TODO
				// TODO
				// TODO
				// TODO
				// TODO
				// 
			}		
		
	}
	
	
	
	
	/**
	 * CUts the schedule at given time and returns schedule for the time from 0-time
	 * @param agentParkingDrivingSchedule
	 * @param time
	 * @return
	 * @throws IllegalArgumentException 
	 * @throws FunctionEvaluationException 
	 * @throws MaxIterationsExceededException 
	 */
	public Schedule cutScheduleAtTime(Id id, Schedule agentParkingDrivingSchedule, double time) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		Schedule firstHalf=new Schedule();
		int interval= agentParkingDrivingSchedule.timeIsInWhichInterval(time); 
		
		for(int i=0; i<=interval-1; i++){
			firstHalf.addTimeInterval(agentParkingDrivingSchedule.timesInSchedule.get(i));
			
		}
		
		//last interval To Be Cut
		TimeInterval lastInterval= agentParkingDrivingSchedule.timesInSchedule.get(interval);
		if (lastInterval.isDriving()){
			
			firstHalf.addTimeInterval(new DrivingInterval(lastInterval.getStartTime(), 
					time, 
					((DrivingInterval)lastInterval).getConsumption() * (time- lastInterval.getStartTime())/lastInterval.getIntervalLength()
					));
		}else{
			
				
				ParkingInterval p= new ParkingInterval(lastInterval.getStartTime(), 
						time, 
						((ParkingInterval)lastInterval).getLocation() 
						);
				p.setParkingOptimalBoolean(((ParkingInterval)lastInterval).isInSystemOptimalChargingTime());
				
				
				p.setChargingSchedule(
						cutChargingScheduleAtTime( p.getChargingSchedule(),
								time
						));
				
				double totalTimeChargingInP= p.getChargingSchedule().getTotalTimeOfIntervalsInSchedule();
				p.setRequiredChargingDuration(totalTimeChargingInP);
					
				
				firstHalf.addTimeInterval(p);
				
			
		}
		
		firstHalf.clearJoules();
		
		DecentralizedSmartCharger.myAgentTimeReader.getJoulesForEachParkingInterval(id, firstHalf);
		
		return firstHalf;
	}
	
	
	
	
	public Schedule cutChargingScheduleAtTime(Schedule chargingSchedule, double time){
		Schedule newCharging= new Schedule();
		for(int i=0; i<chargingSchedule.getNumberOfEntries(); i++){
			if(time < chargingSchedule.timesInSchedule.get(i).getStartTime()){
				//add full interval
				newCharging.addTimeInterval(chargingSchedule.timesInSchedule.get(i));
				
			}
			if(time > chargingSchedule.timesInSchedule.get(i).getStartTime() && time <= chargingSchedule.timesInSchedule.get(i).getEndTime()){
				//end within this interval
				newCharging.addTimeInterval(new ChargingInterval(chargingSchedule.timesInSchedule.get(i).getStartTime(), time));
			}
			
		}
		return newCharging;
		
	}
	
	
	
	
	public Schedule cutScheduleAtTimeSecondHalf (Id id, Schedule agentParkingDrivingSchedule, double time) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		Schedule secondHalf=new Schedule();
		
		int interval= agentParkingDrivingSchedule.timeIsInWhichInterval(time); 
		
		//add first
		TimeInterval firstInterval= agentParkingDrivingSchedule.timesInSchedule.get(interval);
		if (firstInterval.isDriving()){
			
			secondHalf.addTimeInterval(new DrivingInterval(time, firstInterval.getEndTime(), 
					((DrivingInterval)firstInterval).getConsumption() * (firstInterval.getEndTime()-time )/firstInterval.getIntervalLength()
					));
		}else{
			
				
				ParkingInterval p= new ParkingInterval(time, firstInterval.getEndTime(), 
						((ParkingInterval)firstInterval).getLocation() 
						);
				p.setParkingOptimalBoolean(((ParkingInterval)firstInterval).isInSystemOptimalChargingTime());
				
				
				p.setChargingSchedule(
						cutChargingScheduleAtTimeSecondHalf( p.getChargingSchedule(),
								time
						));
				
				double totalTimeChargingInP= p.getChargingSchedule().getTotalTimeOfIntervalsInSchedule();
				p.setRequiredChargingDuration(totalTimeChargingInP);
					
				
				secondHalf.addTimeInterval(p);
				
			
		}
		
		
		for(int i=interval; i<=agentParkingDrivingSchedule.getNumberOfEntries(); i++){
			secondHalf.addTimeInterval(agentParkingDrivingSchedule.timesInSchedule.get(i));
			
		}
		
		
		secondHalf.clearJoules();
		
		DecentralizedSmartCharger.myAgentTimeReader.getJoulesForEachParkingInterval(id, secondHalf);
				
		return secondHalf;
		
	}
	
	
	
	
	
	
	
	public Schedule cutChargingScheduleAtTimeSecondHalf(Schedule chargingSchedule, double time){
		Schedule newCharging= new Schedule();
		for(int i=0; i<chargingSchedule.getNumberOfEntries(); i++){
			if(time > chargingSchedule.timesInSchedule.get(i).getEndTime()){
				//add full interval
				newCharging.addTimeInterval(chargingSchedule.timesInSchedule.get(i));
				
			}
			if(time > chargingSchedule.timesInSchedule.get(i).getStartTime() && time <= chargingSchedule.timesInSchedule.get(i).getEndTime()){
				//end within this interval
				newCharging.addTimeInterval(new ChargingInterval(time, chargingSchedule.timesInSchedule.get(i).getEndTime()));
			}
			
		}
		return newCharging;
		
	}
	
	
	
	public double getSOCAtTime(Id id, Schedule agentParkingDrivingSchedule, double time) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		Schedule cutSchedule=cutScheduleAtTime(id, agentParkingDrivingSchedule, time);
		
		double SOCAtStart=agentParkingDrivingSchedule.getStartingSOC();
		
		for(int i=0; i<=cutSchedule.getNumberOfEntries(); i++){
			TimeInterval t= cutSchedule.timesInSchedule.get(i);
			if(t.isDriving()){
				SOCAtStart=SOCAtStart - ((DrivingInterval) t).getConsumption();
				
			}else{
				//Parking
				Schedule chargingSchedule= ((ParkingInterval)t).getChargingSchedule();
				if(chargingSchedule!=null){
					double chargingSpeed=((ParkingInterval)t).getChargingSpeed();
					
					SOCAtStart += chargingSpeed*chargingSchedule.getTotalTimeOfIntervalsInSchedule();
				}
				
			}
			
		}
		
		
		
		return SOCAtStart;
		
	}
	
	
		
	
	
}
