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
	private LinkedListValueHashMap<Id, Double> agentElectricSourcesFailureinJoules = new LinkedListValueHashMap<Id, Double>(); 
	
	public V2G(DecentralizedSmartCharger mySmartCharger){
		
		this.mySmartCharger=mySmartCharger;
		
	}
	
	
	public void regulationDownVehicleLoad(Id agentId, 
			LoadDistributionInterval electricSourceInterval, 
			Schedule agentParkingDrivingSchedule, 
			double compensation, // money
			double joules,
			boolean ev, //yes or no
			String type,
			LPEV lpev,
			LPPHEV lpphev,
			double batterySize,
			double batteryMin,
			double batteryMax) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, OptimizationException, LpSolveException, IOException{
		
		double currentSOC=getSOCAtTime(agentParkingDrivingSchedule, electricSourceInterval.getEndTime());
		
		currentSOC=currentSOC+joules;
		
		Schedule agentDuringElectricSourceInterval= cutScheduleAtTimeSecondHalf(agentParkingDrivingSchedule, electricSourceInterval.getStartTime());
		agentDuringElectricSourceInterval=cutScheduleAtTime(agentDuringElectricSourceInterval, electricSourceInterval.getEndTime());
		
		for(int i=0; i<agentDuringElectricSourceInterval.getNumberOfEntries();i++){
			if(agentDuringElectricSourceInterval.timesInSchedule.get(i).isDriving()){
				currentSOC=currentSOC- ((DrivingInterval)agentDuringElectricSourceInterval.timesInSchedule.get(i)).getConsumption();
			}
		}
		
		if(currentSOC<batterySize){
			// regulation !!!
			Schedule secondHalf= cutScheduleAtTimeSecondHalfAndReassignJoules(agentId, 
					agentParkingDrivingSchedule, 
					electricSourceInterval.getEndTime());
						
			secondHalf.setStartingSOC(currentSOC);
			Schedule answerScheduleAfterElectricSourceInterval=null;
			
			
			double costKeeping=mySmartCharger.calculateChargingCostForAgentSchedule(agentId, secondHalf);
			
			double costReschedule= calcCostForRescheduling(answerScheduleAfterElectricSourceInterval,
					secondHalf, 
					agentId, 
					batterySize, batteryMin, batteryMax, 
					type,
					currentSOC,
					ev,
					compensation,
					lpphev,
					lpev
					);
			
			
			if(costKeeping>costReschedule){
				//reschedule
				reschedule(agentId, 
						answerScheduleAfterElectricSourceInterval,
						agentParkingDrivingSchedule,
						electricSourceInterval,
						costKeeping,
						costReschedule);
			
			}else{
				// no regulation
				attributeSuperfluousVehicleLoadsToGridIfPossible(agentId, 
						agentParkingDrivingSchedule, 
						electricSourceInterval);
			}
		}else{
			// no regulation
			attributeSuperfluousVehicleLoadsToGridIfPossible(agentId, 
					agentParkingDrivingSchedule, 
					electricSourceInterval);
		}
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
	public void regulationUpVehicleLoad(Id agentId, 
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
		
			
			
			double currentSOC=getSOCAtTime(agentParkingDrivingSchedule, electricSourceInterval.getEndTime());
			currentSOC=currentSOC - Math.abs(joules);//joulesFromSource<0
			
			Schedule secondHalf= cutScheduleAtTimeSecondHalfAndReassignJoules(agentId, 
					agentParkingDrivingSchedule, 
					electricSourceInterval.getEndTime());
						
			secondHalf.setStartingSOC(currentSOC);
			Schedule answerScheduleAfterElectricSourceInterval=null;
			
			double costKeeping=mySmartCharger.calculateChargingCostForAgentSchedule(agentId, secondHalf);
			double costReschedule= calcCostForRescheduling(answerScheduleAfterElectricSourceInterval,
					secondHalf, 
					agentId, 
					batterySize, batteryMin, batteryMax, 
					type,
					currentSOC,
					ev,
					compensation,
					lpphev,
					lpev
					);
			
			
			if(costKeeping>costReschedule){
				//reschedule
				reschedule(agentId, 
						answerScheduleAfterElectricSourceInterval,
						agentParkingDrivingSchedule,
						electricSourceInterval,
						costKeeping,
						costReschedule);
				
			}else{
				
				//go over agent schedule during electric load
				//find hub and attribute if possible to Hub level above
				attributeSuperfluousVehicleLoadsToGridIfPossible(agentId, 
						agentParkingDrivingSchedule, 
						electricSourceInterval);
				
			}		
		
	}
	
	
	
	
	/**
	 * reassembles updated agentParkingandDriving Schedule from original Schedule,
	 * the rescheduled part during the electric stochastic load
	 * and the newly scheduled part after the electric stochastic load;
	 * saves the reassembled schedule back into AgentParkingAndDrivingSchedules
	 * 
	 * AND
	 * 
	 * updates charging costs in ChargingCostsForAgents
	 * 
	 * @param agentId
	 * @param answerScheduleAfterElectricSourceInterval
	 * @param agentParkingDrivingSchedule
	 * @param electricSourceInterval
	 * @param costKeeping
	 * @param costReschedule
	 * @throws MaxIterationsExceededException
	 * @throws OptimizationException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 */
	public void reschedule(Id agentId, 
			Schedule answerScheduleAfterElectricSourceInterval,
			Schedule agentParkingDrivingSchedule,
			LoadDistributionInterval electricSourceInterval,
			double costKeeping,
			double costReschedule) throws MaxIterationsExceededException, OptimizationException, FunctionEvaluationException, IllegalArgumentException{
		
		
		answerScheduleAfterElectricSourceInterval= mySmartCharger.myChargingSlotDistributor.distribute(agentId, answerScheduleAfterElectricSourceInterval);
		
		//merge with old schedule
		Schedule rescheduledFirstHalf= cutChargingScheduleAtTime(agentParkingDrivingSchedule, electricSourceInterval.getStartTime());
		
		Schedule rescheduledPart= cutChargingScheduleAtTime(rescheduledFirstHalf, electricSourceInterval.getEndTime());
		
		
		rescheduledFirstHalf.mergeSchedules(rescheduledPart);
		rescheduledFirstHalf.mergeSchedules(answerScheduleAfterElectricSourceInterval);
		
		agentParkingDrivingSchedule=rescheduledFirstHalf;
		
		mySmartCharger.getAllAgentParkingAndDrivingSchedules().put(agentId, agentParkingDrivingSchedule);
		
		// change agents charging costs
		mySmartCharger.getChargingCostsForAgents().put(
				agentId, 
				(mySmartCharger.getChargingCostsForAgents().getValue(agentId)-costKeeping+costReschedule)
				);
	}
	
	
	
	
	public void attributeSuperfluousVehicleLoadsToGridIfPossible(Id agentId, 
			Schedule agentParkingDrivingSchedule, 
			LoadDistributionInterval electricSourceInterval) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		Schedule agentDuringLoad= 
			cutScheduleAtTimeSecondHalf(agentParkingDrivingSchedule, electricSourceInterval.getStartTime());
		
		agentDuringLoad= cutScheduleAtTime(agentDuringLoad, electricSourceInterval.getEndTime());
		
		
		for(int i=0; i<agentDuringLoad.getNumberOfEntries();i++){
			TimeInterval agentInterval= agentDuringLoad.timesInSchedule.get(i);
			
			// if overlap between agentInterval && electricSource
			LoadDistributionInterval overlapAgentAndElectricSource=
				agentInterval.ifOverlapWithLoadDistributionIntervalReturnOverlap(electricSourceInterval);
			
			if(overlapAgentAndElectricSource!=null){
				
				if(agentInterval.isParking()){
					//IF PARKING
					//
					int hubId=mySmartCharger.myHubLoadReader.getHubForLinkId(
							((ParkingInterval)agentInterval).getLocation());
					
//					System.out.println("hubSchedule before:");
					
					Schedule hubSchedule=mySmartCharger.myHubLoadReader.stochasticHubLoadDistribution.getValue(hubId);
//					hubSchedule.printSchedule();
//					
					if (hubSchedule.overlapWithTimeInterval(overlapAgentAndElectricSource)){
						//if there is overlap
						
						hubSchedule.addLoadDistributionIntervalToExistingLoadDistributionSchedule(overlapAgentAndElectricSource);
						
						
					}else{
						hubSchedule.addTimeInterval(overlapAgentAndElectricSource);
						hubSchedule.sort();
					}
					
					System.out.println("hubSchedule after adding superfluous loads:");
					hubSchedule.printSchedule();
				}else{
					//IF NOT PARKING - ENERGY LOST
					//save as impossible request for Joules
					double lost= mySmartCharger.functionIntegrator.integrate(
							overlapAgentAndElectricSource.getPolynomialFunction(),
							overlapAgentAndElectricSource.getStartTime(),
							overlapAgentAndElectricSource.getEndTime());
					
					agentElectricSourcesFailureinJoules.put(agentId, Math.abs(lost));
				}
			}
			
		}
	}
	
	
	
	public double calcCostForRescheduling(Schedule answerScheduleAfterElectricSourceInterval,
			Schedule secondHalf, 
			Id agentId, 
			double batterySize, double batteryMin, double batteryMax, 
			String type,
			double currentSOC,
			boolean ev,
			double compensation,
			LPPHEV lpphev,
			LPEV lpev
			) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException{
		
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
				costReschedule=mySmartCharger.calculateChargingCostForAgentSchedule(agentId, answerScheduleAfterElectricSourceInterval)-compensation;
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
				costReschedule=mySmartCharger.calculateChargingCostForAgentSchedule(agentId, answerScheduleAfterElectricSourceInterval)-compensation;
			}
			
		}
		return costReschedule;
		
	}
	
	
	
	public double getSOCAtTime(Schedule agentParkingDrivingSchedule, double time) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		System.out.println("Schedule to compute SOC at time:"+ time);
		Schedule cutSchedule=cutScheduleAtTime(agentParkingDrivingSchedule, time);
		
		System.out.println("Schedule CUT compute SOC at time:");
		cutSchedule.printSchedule();
		double SOCAtStart=agentParkingDrivingSchedule.getStartingSOC();
		
		for(int i=0; i<cutSchedule.getNumberOfEntries(); i++){
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






	public Schedule cutScheduleAtTimeFirstHalfAndReassignJoules (Id id, Schedule agentParkingDrivingSchedule, double time) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		Schedule half= cutScheduleAtTime (agentParkingDrivingSchedule, time);
		
		reAssignJoulesToSchedule(half, id);
		return half;
	}
	
	
	
	public Schedule cutScheduleAtTimeSecondHalfAndReassignJoules (Id id, Schedule agentParkingDrivingSchedule, double time) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		Schedule half= cutScheduleAtTimeSecondHalf (agentParkingDrivingSchedule, time);
		
		reAssignJoulesToSchedule(half, id);
		return half;
	}






	/**
	 * Cuts the schedule at given time and returns schedule for the time from 0-time
	 * @param agentParkingDrivingSchedule
	 * @param time
	 * @return
	 * @throws IllegalArgumentException 
	 * @throws FunctionEvaluationException 
	 * @throws MaxIterationsExceededException 
	 */
	public Schedule cutScheduleAtTime(Schedule agentParkingDrivingSchedule, double time) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		Schedule firstHalf=new Schedule();
		int interval= agentParkingDrivingSchedule.timeIsInWhichInterval(time); 
		
		for(int i=0; i<=interval-1; i++){
			firstHalf.addTimeInterval(agentParkingDrivingSchedule.timesInSchedule.get(i));
			
		}
		
		//last interval To Be Cut
		TimeInterval lastInterval= agentParkingDrivingSchedule.timesInSchedule.get(interval);
		if (lastInterval.isDriving()){
			
			DrivingInterval d= new DrivingInterval(lastInterval.getStartTime(), 
					time, 
					((DrivingInterval)lastInterval).getConsumption() * (time- lastInterval.getStartTime())/lastInterval.getIntervalLength()
					);
			if(d.getIntervalLength()>0){
				firstHalf.addTimeInterval(d);
			}
			
		}else{
			
				
				ParkingInterval p= new ParkingInterval(lastInterval.getStartTime(), 
						time, 
						((ParkingInterval)lastInterval).getLocation() 
						);
				p.setParkingOptimalBoolean(((ParkingInterval)lastInterval).isInSystemOptimalChargingTime());
				
				if(((ParkingInterval)lastInterval).getChargingSchedule()!= null){
					p.setChargingSchedule(
							cutChargingScheduleAtTime(((ParkingInterval)lastInterval).getChargingSchedule(),
									time
							));
					double totalTimeChargingInP= p.getChargingSchedule().getTotalTimeOfIntervalsInSchedule();
					p.setRequiredChargingDuration(totalTimeChargingInP);
				}else{
					p.setRequiredChargingDuration(0);
				}
					
				if(p.getIntervalLength()>0){
					firstHalf.addTimeInterval(p);
				}
				
				
			
		}
		
		return firstHalf;
	}
	
	
	
	
	public Schedule cutScheduleAtTimeSecondHalf (Schedule agentParkingDrivingSchedule, double time) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		Schedule secondHalf=new Schedule();
		
		int interval= agentParkingDrivingSchedule.timeIsInWhichInterval(time); 
		
		//add first
		TimeInterval firstInterval= agentParkingDrivingSchedule.timesInSchedule.get(interval);
		if (firstInterval.isDriving()){
			
			DrivingInterval d= new DrivingInterval(time, firstInterval.getEndTime(), 
					((DrivingInterval)firstInterval).getConsumption() * (firstInterval.getEndTime()-time )/firstInterval.getIntervalLength()
			);
			if(d.getIntervalLength()>0){
				secondHalf.addTimeInterval(d);
			}
			
			
			
		}else{
			
				
				ParkingInterval p= new ParkingInterval(time, firstInterval.getEndTime(), 
						((ParkingInterval)firstInterval).getLocation() 
						);
				p.setParkingOptimalBoolean(((ParkingInterval)firstInterval).isInSystemOptimalChargingTime());
				
				
				if(((ParkingInterval)firstInterval).getChargingSchedule()!= null){
					p.setChargingSchedule(
							cutChargingScheduleAtTimeSecondHalf(((ParkingInterval)firstInterval).getChargingSchedule(),
									time
							));
				}else{
					p.setRequiredChargingDuration(0);
				}
				
				if(p.getIntervalLength()>0){
					secondHalf.addTimeInterval(p);
				}
							
				
				
				
			
		}
		
		
		for(int i=interval+1; i<agentParkingDrivingSchedule.getNumberOfEntries(); i++){
			secondHalf.addTimeInterval(agentParkingDrivingSchedule.timesInSchedule.get(i));
			
		}
		secondHalf.sort();		
		return secondHalf;
		
	}



	private void reAssignJoulesToSchedule(Schedule half, Id id) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		half.clearJoules();
		
		DecentralizedSmartCharger.myAgentTimeReader.getJoulesForEachParkingInterval(id, half);
	}



	public Schedule cutChargingScheduleAtTime(Schedule chargingSchedule, double time){
		Schedule newCharging= new Schedule();
		for(int i=0; i<chargingSchedule.getNumberOfEntries(); i++){
			if(time > chargingSchedule.timesInSchedule.get(i).getEndTime()){
				//add full interval
				newCharging.addTimeInterval(chargingSchedule.timesInSchedule.get(i));
				
			}
			if(time > chargingSchedule.timesInSchedule.get(i).getStartTime() && time <= chargingSchedule.timesInSchedule.get(i).getEndTime()){
				//only take first half
				newCharging.addTimeInterval(new ChargingInterval(chargingSchedule.timesInSchedule.get(i).getStartTime(), time));
			}
			
		}
		return newCharging;
		
	}
	
	
	
	
	public Schedule cutChargingScheduleAtTimeSecondHalf(Schedule chargingSchedule, double time){
		Schedule newCharging= new Schedule();
		for(int i=0; i<chargingSchedule.getNumberOfEntries(); i++){
			if(time < chargingSchedule.timesInSchedule.get(i).getStartTime()){
				//add full interval
				newCharging.addTimeInterval(chargingSchedule.timesInSchedule.get(i));
				
			}
			if(time > chargingSchedule.timesInSchedule.get(i).getStartTime() && time <= chargingSchedule.timesInSchedule.get(i).getEndTime()){
				//only take 2nd half
				newCharging.addTimeInterval(new ChargingInterval(time, chargingSchedule.timesInSchedule.get(i).getEndTime()));
			}
			
		}
		return newCharging;
		
	}
	
	
		
	
	
}
