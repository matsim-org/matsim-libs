package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import java.io.IOException;
import java.util.HashMap;

import lpsolve.LpSolveException;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.OptimizationException;
import org.matsim.api.core.v01.Id;

import playground.wrashid.lib.obj.LinkedListValueHashMap;


/**
 * this class handles regulation up and down. For every agent, it calculates, if rescheduling or keeping its current schedule is more profitable.
 * If rescheduling has a higher utility for the agent, he reschedules the rest of his day and decreases the stochastic hub load 
 * 
 * <ul>
 * <li> reschedule according to V2G loads if possible
 * <li> save revenues from V2G fro everz agent in LinkedList agentV2GRevenue
 * </ul>
 * 
 * @author Stella
 *
 */
public class V2G {
	
	private DecentralizedSmartCharger mySmartCharger;
	//private LinkedListValueHashMap<Id, Double> agentV2GRevenue = new LinkedListValueHashMap<Id, Double>(); 
	private HashMap<Id, Double> agentV2GRevenue = new HashMap<Id, Double>(); 
	
	public Schedule answerScheduleAfterElectricSourceInterval;
	
	public V2G(DecentralizedSmartCharger mySmartCharger){
		
		this.mySmartCharger=mySmartCharger;
		
	}
	
	
	
	public  HashMap<Id, Double> getAgentV2GRevenues(){
		return agentV2GRevenue;
	}
	
	public void addRevenueToAgentFromV2G(double revenue, Id agentId){
		if (agentV2GRevenue.containsKey(agentId)){
			double valueBefore= agentV2GRevenue.get(agentId);
			//update value
			agentV2GRevenue.put(agentId,valueBefore+revenue);
			
		}else{
			agentV2GRevenue.put(agentId,revenue);
		}		
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
		
		costComparisonVehicle(agentId, 
				electricSourceInterval, 
				agentParkingDrivingSchedule, 
				compensation, 
				ev, 
				type,
				lpev,
				lpphev,
				 batterySize,batteryMin,batteryMax,
				currentSOC);
		
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
			currentSOC=currentSOC - joules;
			
			costComparisonVehicle(agentId, 
					electricSourceInterval, 
					agentParkingDrivingSchedule, 
					compensation, 
					ev, 
					type,
					lpev,
					lpphev,
					 batterySize,batteryMin,batteryMax,
					currentSOC);
			
	}



	/**
	 * checks if agent is at Hub during this load interval
	 * for every interval, where the agent is at the specified hub, rescheduling is attempted and if successful the hub load is reduced
	 * 
	 * 
	 * @param agentId
	 * @param currentStochasticLoadInterval
	 * @param agentParkingDrivingSchedule
	 * @param compensation
	 * @param joules
	 * @param ev
	 * @param type
	 * @param lpev
	 * @param lpphev
	 * @param batterySize
	 * @param batteryMin
	 * @param batteryMax
	 * @param hub
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 * @throws LpSolveException
	 * @throws IOException
	 * @throws OptimizationException
	 */
	public void regulationUpHubLoad(Id agentId, 
			LoadDistributionInterval currentStochasticLoadInterval, 
			Schedule agentParkingDrivingSchedule, 
			double compensation,
			double joules,
			boolean ev,
			String type,
			LPEV lpev,
			LPPHEV lpphev,
			double batterySize,
			double batteryMin,
			double batteryMax,
			int hub) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException, OptimizationException{
		
			
			Schedule relevantAgentParkingAndDrivingScheduleAtHub= 
				findAndReturnAgentScheduleWithinLoadIntervalWhichIsAtSpecificHub(hub, 
				agentParkingDrivingSchedule,
				currentStochasticLoadInterval);
		
			if(relevantAgentParkingAndDrivingScheduleAtHub.getNumberOfEntries()>0){
				
				for(int i=0; i<relevantAgentParkingAndDrivingScheduleAtHub.getNumberOfEntries();i++){
					
					TimeInterval t= relevantAgentParkingAndDrivingScheduleAtHub.timesInSchedule.get(i);
					
					LoadDistributionInterval electricSourceInterval= 
						new LoadDistributionInterval(t.getStartTime(), 
								t.getEndTime(), 
								currentStochasticLoadInterval.getPolynomialFunction(), 
								currentStochasticLoadInterval.isOptimal());
					
					
					double currentSOC=getSOCAtTime(agentParkingDrivingSchedule, electricSourceInterval.getEndTime());
					currentSOC=currentSOC - joules;
					
					costComparisonHubload(agentId, 
							electricSourceInterval, 
							agentParkingDrivingSchedule, 
							compensation,
							joules,
							ev,
							type,
							lpev,
							lpphev,
							batterySize,
							batteryMin,
							batteryMax,
							hub,
							currentSOC);
					
				}
				
	
			}
		
	}



	/**
	 * attempts to charge agent battery from grid stochastic load,
	 * - if agent contract allows regulation down 
	 * - if he is parking at the time
	 * - and if his schedule allows
	 * he will charge, consequently the hub load will be reduced.
	 *
	 * 
	 * @param agentId
	 * @param currentStochasticLoadInterval
	 * @param agentParkingDrivingSchedule
	 * @param compensation
	 * @param joules
	 * @param ev
	 * @param type
	 * @param lpev
	 * @param lpphev
	 * @param batterySize
	 * @param batteryMin
	 * @param batteryMax
	 * @param hub
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 * @throws LpSolveException
	 * @throws IOException
	 * @throws OptimizationException
	 */
	public void regulationDownHubLoad(Id agentId, 
			LoadDistributionInterval currentStochasticLoadInterval, 
			Schedule agentParkingDrivingSchedule, 
			double compensation,
			double joules,
			boolean ev,
			String type,
			LPEV lpev,
			LPPHEV lpphev,
			double batterySize,
			double batteryMin,
			double batteryMax,
			int hub) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException, OptimizationException{
		
		//*****************************
		//*****************************
		// CHECK WHICH PARTS AGENT IS AT HUB hub FIRST
			Schedule relevantAgentParkingAndDrivingScheduleAtHub= 
				findAndReturnAgentScheduleWithinLoadIntervalWhichIsAtSpecificHub(hub, 
				agentParkingDrivingSchedule,
				currentStochasticLoadInterval);
		
			if(relevantAgentParkingAndDrivingScheduleAtHub.getNumberOfEntries()>0){
				
				
				for(int i=0; i<relevantAgentParkingAndDrivingScheduleAtHub.getNumberOfEntries();i++){
					
					TimeInterval t= relevantAgentParkingAndDrivingScheduleAtHub.timesInSchedule.get(i);
					
					LoadDistributionInterval electricSourceInterval= 
						new LoadDistributionInterval(t.getStartTime(), 
								t.getEndTime(), 
								currentStochasticLoadInterval.getPolynomialFunction(), 
								currentStochasticLoadInterval.isOptimal());
					
					//*****************************
					//*****************************
	
					double currentSOC=getSOCAtTime(agentParkingDrivingSchedule, electricSourceInterval.getEndTime());
					
					currentSOC=currentSOC+joules;
					//*****************************
					//*****************************
					
					costComparisonHubload(agentId, 
							electricSourceInterval, 
							agentParkingDrivingSchedule, 
							compensation,
							joules,
							ev,
							type,
							lpev,
							lpphev,
							batterySize,
							batteryMin,
							batteryMax,
							hub,
							currentSOC);
					
					
				}
				
	
			}
			
		
	}



	/**
	 * calculates the costs for keeping or rescheduling the plan
	 * accordingly, the charging schedule is changed and the stochastic loads reduced
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
	 * @param currentSOC
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 * @throws LpSolveException
	 * @throws IOException
	 * @throws OptimizationException
	 */
	public void costComparisonVehicle(Id agentId, 
			LoadDistributionInterval electricSourceInterval, 
			Schedule agentParkingDrivingSchedule, 
			double compensation, // money
			boolean ev, //yes or no
			String type,
			LPEV lpev,
			LPPHEV lpphev,
			double batterySize,
			double batteryMin,
			double batteryMax,
			double currentSOC) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException, OptimizationException{
		//try reschedule
		Schedule secondHalf= cutScheduleAtTimeSecondHalfAndReassignJoules(agentId, 
				agentParkingDrivingSchedule, 
				electricSourceInterval.getEndTime());
					
		secondHalf.setStartingSOC(currentSOC);
		answerScheduleAfterElectricSourceInterval=null;
		
		
		double costKeeping=mySmartCharger.calculateChargingCostForAgentSchedule(agentId, secondHalf);
		
		double costReschedule;
		if (currentSOC>=batterySize*batteryMin && currentSOC<=batterySize*batteryMax){
			costReschedule= calcCostForRescheduling(
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
		}else{
			costReschedule=Double.MAX_VALUE;
		}
			
		
		if(costKeeping>costReschedule){
			
			//reschedule
			reschedule(agentId, 
					answerScheduleAfterElectricSourceInterval,
					agentParkingDrivingSchedule,
					electricSourceInterval,
					costKeeping,
					costReschedule);
			
			// joules>0 positive function which needs to be reduced
			reduceAgentVehicleLoadsByGivenLoadInterval(
					agentId, 
					electricSourceInterval);
			
			
		
		}else{
			// no regulation
			attributeSuperfluousVehicleLoadsToGridIfPossible(agentId, 
					agentParkingDrivingSchedule, 
					electricSourceInterval);
			
		}
	}
	
	
	
	public void costComparisonHubload(Id agentId, 
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
			double batteryMax,
			int hub,
			double currentSOC) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException, OptimizationException{
		
		Schedule secondHalf= cutScheduleAtTimeSecondHalfAndReassignJoules(agentId, 
				agentParkingDrivingSchedule, 
				electricSourceInterval.getEndTime());
					
		secondHalf.setStartingSOC(currentSOC);
		answerScheduleAfterElectricSourceInterval=null;
		
		/*System.out.println("schedule before rescheduling");
		agentParkingDrivingSchedule.printSchedule();*/
		
		double costKeeping=mySmartCharger.calculateChargingCostForAgentSchedule(agentId, secondHalf);
		
		
		double costReschedule=0;
		if (currentSOC>=batterySize*batteryMin && currentSOC<=batterySize*batteryMax){
			costReschedule= calcCostForRescheduling(
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
		}else{
			costReschedule=Double.MAX_VALUE;
		}
		
							
		if(costKeeping>costReschedule){
			
			//reschedule
			reschedule(agentId, 
					answerScheduleAfterElectricSourceInterval,
					agentParkingDrivingSchedule,
					electricSourceInterval,
					costKeeping,
					costReschedule);
			
			// e.g. stochastic load on net is -3500
			// thus car discharges 3500 to balance the net
			// thus -3500 will be reduced by (-3500)=>0
			reduceHubLoadByGivenLoadInterval(hub, electricSourceInterval);
			
		}else{
			// nothing load remains in hub and no rescheduling
		}
	}
	
	/**
	 * goes through the part of agents schedule which is during  currentStochasticLoadInterval,
	 * a schedule will be returned of all overlapping segments that are at the specified hub 
	 * 
	 * @param hub
	 * @param agentParkingDrivingSchedule
	 * @param currentStochasticLoadInterval
	 * @return
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 */
	public Schedule findAndReturnAgentScheduleWithinLoadIntervalWhichIsAtSpecificHub(int hub, 
			Schedule agentParkingDrivingSchedule,
			LoadDistributionInterval currentStochasticLoadInterval) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		Schedule relevantAgentParkingAndDrivingScheduleAtHub=new Schedule();
		
		Schedule relevantAgentParkingAndDrivingSchedule= cutScheduleAtTime(
				agentParkingDrivingSchedule, 
				currentStochasticLoadInterval.getEndTime());
		
		relevantAgentParkingAndDrivingSchedule= cutScheduleAtTimeSecondHalf(
				relevantAgentParkingAndDrivingSchedule, 
				currentStochasticLoadInterval.getStartTime());
		
		for(int i=0; i<relevantAgentParkingAndDrivingSchedule.getNumberOfEntries(); i++){
			
			TimeInterval t= relevantAgentParkingAndDrivingSchedule.timesInSchedule.get(i);
			if(t.isParking()
					&& 
					mySmartCharger.myHubLoadReader.getHubForLinkId( ((ParkingInterval) t ).getLocation())==hub){
				relevantAgentParkingAndDrivingScheduleAtHub.addTimeInterval(t);
			}
		}
		
		return relevantAgentParkingAndDrivingScheduleAtHub;
	}
	
	
	/**
	 * <ul>
	 * <li>updates revenue of agent in addRevenueToAgentFromV2G
	 * <li>reassembles updated agentParkingandDriving Schedule from original Schedule,
	 * the rescheduled part during the electric stochastic load
	 * and the newly scheduled part after the electric stochastic load;
	 * <li>saves the reassembled schedule back into AgentParkingAndDrivingSchedules
	 * <li> updates total charging costs in ChargingCostsForAgents
	 * </ul>
	 * 
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
		
		
		addRevenueToAgentFromV2G(costKeeping-costReschedule, agentId);
//		
		mySmartCharger.getAllAgentChargingSchedules().put(agentId, 
				mySmartCharger.myChargingSlotDistributor.distribute(agentId, answerScheduleAfterElectricSourceInterval));
		
		
	
		Schedule rescheduledFirstHalf= cutScheduleAtTime(agentParkingDrivingSchedule, electricSourceInterval.getStartTime());

		Schedule rescheduledPart= cutScheduleAtTime(agentParkingDrivingSchedule, electricSourceInterval.getEndTime());
		rescheduledPart=cutScheduleAtTimeSecondHalf(rescheduledPart, electricSourceInterval.getStartTime());
	
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
	
	
	
	
	/**
	 * adds the passed electricSourceInterval to the current stochastic agent vehicle load
	 * in myHubLoadReader.agentVehicleSourceMapping.getValue(agentId)	
	 * 
	 * @param agentId
	 * @param electricSourceInterval
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 */
	public void reduceAgentVehicleLoadsByGivenLoadInterval(
			Id agentId, 
			LoadDistributionInterval electricSourceInterval) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		// -3500-(-3500)=0
		
		
		Schedule agentVehicleSource= mySmartCharger.myHubLoadReader.agentVehicleSourceMapping.getValue(agentId);
		
		PolynomialFunction negativePolynomialFunc= 
			new PolynomialFunction(electricSourceInterval.getPolynomialFunction().getCoefficients().clone());
		negativePolynomialFunc=negativePolynomialFunc.negate();
		
		LoadDistributionInterval negativeElectricSourceInterval= new LoadDistributionInterval(
				electricSourceInterval.getStartTime(),
				electricSourceInterval.getEndTime(),
				negativePolynomialFunc,			
				!electricSourceInterval.isOptimal());
		
		agentVehicleSource.addLoadDistributionIntervalToExistingLoadDistributionSchedule(
				negativeElectricSourceInterval);
		
		mySmartCharger.myHubLoadReader.agentVehicleSourceMapping.put(agentId, 
				agentVehicleSource
				);
		
	}

	
	/*public void reduceAgentVehicleLoadsByGivenLoadInterval(
			Id agentId, 
			LoadDistributionInterval electricSourceInterval) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		
		// 3500-(3500)=0
		Schedule agentVehicleSource= mySmartCharger.myHubLoadReader.agentVehicleSourceMapping.getValue(agentId);
		
		
		agentVehicleSource.addLoadDistributionIntervalToExistingLoadDistributionSchedule(electricSourceInterval);
		
		mySmartCharger.myHubLoadReader.agentVehicleSourceMapping.put(agentId, 
				agentVehicleSource
				);
		
	}*/

	/**
	 * reduces the entry of myHubLoadReader.stochasticHubLoadDistribution.getValue(i)	 * 
	 * with updated HubLoadSchedule which is reduced by given electricSourceInterval
	 * 
	 * hubLoad + (-1*electricSourceInterval)
	 * @param i
	 * @param electricSourceInterval
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 */
	public void reduceHubLoadByGivenLoadInterval(
			Integer i, 
			LoadDistributionInterval electricSourceInterval) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		
		Schedule hubLoadSchedule= mySmartCharger.myHubLoadReader.stochasticHubLoadDistribution.getValue(i);
		
		PolynomialFunction negativePolynomialFunc= 
			new PolynomialFunction(electricSourceInterval.getPolynomialFunction().getCoefficients().clone());
		negativePolynomialFunc=negativePolynomialFunc.negate();
		
		
		LoadDistributionInterval negativeElectricSourceInterval= new LoadDistributionInterval(
				electricSourceInterval.getStartTime(),
				electricSourceInterval.getEndTime(),
				negativePolynomialFunc,						 
				!electricSourceInterval.isOptimal());
		
		hubLoadSchedule.addLoadDistributionIntervalToExistingLoadDistributionSchedule(negativeElectricSourceInterval);
		
		mySmartCharger.myHubLoadReader.stochasticHubLoadDistribution.put(i, hubLoadSchedule);
		
		
	}
	
	
	
	
	public void attributeSuperfluousVehicleLoadsToGridIfPossible(Id agentId, 
			Schedule agentParkingDrivingSchedule, 
			LoadDistributionInterval electricSourceInterval) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		Schedule agentDuringLoad= 
			cutScheduleAtTimeSecondHalf(agentParkingDrivingSchedule, electricSourceInterval.getStartTime());
		
		agentDuringLoad= cutScheduleAtTime(agentDuringLoad, electricSourceInterval.getEndTime());
		
		/*System.out.println("agentDuringLoad:");
		agentDuringLoad.printSchedule();
		System.out.println("electricSourceInterval:");
		electricSourceInterval.printInterval();*/
		
		for(int i=0; i<agentDuringLoad.getNumberOfEntries();i++){
			TimeInterval agentInterval= agentDuringLoad.timesInSchedule.get(i);
			
			// if overlap between agentInterval && electricSource
			LoadDistributionInterval overlapAgentAndElectricSource=
				agentInterval.ifOverlapWithLoadDistributionIntervalReturnOverlap(electricSourceInterval);
			
			if(overlapAgentAndElectricSource!=null){
				
				if(agentInterval.isParking()){
					//IF PARKING					
					int hubId=mySmartCharger.myHubLoadReader.getHubForLinkId(
							((ParkingInterval)agentInterval).getLocation());
					
					
					Schedule hubSchedule=mySmartCharger.myHubLoadReader.stochasticHubLoadDistribution.getValue(hubId);
					/*System.out.println("hubSchedule before:");
					hubSchedule.printSchedule();*/
					
					if (hubSchedule.overlapWithTimeInterval(overlapAgentAndElectricSource)){
						//if there is overlap
						
						hubSchedule.addLoadDistributionIntervalToExistingLoadDistributionSchedule(overlapAgentAndElectricSource);
												
					}else{
						hubSchedule.addTimeInterval(overlapAgentAndElectricSource);
						hubSchedule.sort();
					
					}
					
					// could be transmitted to HUb - thus reduce Vehicle Load
					reduceAgentVehicleLoadsByGivenLoadInterval(
							agentId, 
							overlapAgentAndElectricSource);
					
					/*System.out.println("hubSchedule after adding superfluous loads:");
					hubSchedule.printSchedule();*/
					
				}/*else{
					//IF NOT PARKING - ENERGY LOST
					//save as impossible request for Joules
					
					setLoadAsLost(overlapAgentAndElectricSource.getPolynomialFunction(),
							overlapAgentAndElectricSource.getStartTime(),
							overlapAgentAndElectricSource.getEndTime(),
							agentId);
					
				}*/
			}
			
		}
	}
	
	
	
	
	/*public void setLoadAsLost(PolynomialFunction func, 
			double start,
			double end,
			Id agentId) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		double lost= mySmartCharger.functionIntegrator.integrate(
				func,
				start,
				end);
		
		agentElectricSourcesFailureinJoules.put(agentId, Math.abs(lost));
		
	}*/
	
	
	public double calcCostForRescheduling(
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
				//System.out.println("Reschedule was not possible!");
				costReschedule= 100000000.0;
			}else{
				costReschedule=mySmartCharger.calculateChargingCostForAgentSchedule(agentId, answerScheduleAfterElectricSourceInterval)
								-compensation;
			}
			
			
		}else{
			//***********************************
			//PHEV
			//***********************************
			
			answerScheduleAfterElectricSourceInterval= lpev.solveLPReschedule(secondHalf, 
					agentId, batterySize, batteryMin, batteryMax, 
					type,currentSOC);
			
			if(answerScheduleAfterElectricSourceInterval==null){
				answerScheduleAfterElectricSourceInterval = lpphev.solveLPReschedule(
						secondHalf, agentId, batterySize, batteryMin, batteryMax, type, currentSOC);
				//if still no answer possible
				if(answerScheduleAfterElectricSourceInterval==null){
					System.out.println("Reschedule was not possible!");
					costReschedule= 100000000.0;
				}else{
					costReschedule=mySmartCharger.calculateChargingCostForAgentSchedule(agentId, answerScheduleAfterElectricSourceInterval)-compensation;
				}
			}else{
				costReschedule=mySmartCharger.calculateChargingCostForAgentSchedule(agentId, answerScheduleAfterElectricSourceInterval)-compensation;
			}
			
			
		}
		this.answerScheduleAfterElectricSourceInterval=answerScheduleAfterElectricSourceInterval;
		return costReschedule;
		
	}
	
	
	
	public double getSOCAtTime(Schedule agentParkingDrivingSchedule, double time) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
//		System.out.println("Schedule to compute SOC at time:"+ time);
		Schedule cutSchedule=cutScheduleAtTime(agentParkingDrivingSchedule, time);
		
//		System.out.println("Schedule CUT compute SOC at time:");
//		cutSchedule.printSchedule();
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
