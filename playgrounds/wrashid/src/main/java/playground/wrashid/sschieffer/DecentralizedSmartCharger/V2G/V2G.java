package playground.wrashid.sschieffer.DecentralizedSmartCharger.V2G;

import java.io.IOException;
import java.util.HashMap;

import lpsolve.LpSolveException;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.OptimizationException;
import org.matsim.api.core.v01.Id;

import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.DecentralizedSmartCharger;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.DrivingInterval;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.LPEV;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.LPPHEV;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.LoadDistributionInterval;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.ParkingInterval;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.Schedule;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.TimeInterval;


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
	private HashMap<Id, V2GAgentStats> agentV2GStatistic = new HashMap<Id, V2GAgentStats>(); 
	
	public Schedule answerScheduleAfterElectricSourceInterval;
	private double averageV2GRevenueEV=0;
	private double averageV2GRevenuePHEV=0;
	private double averageV2GRevenueAllAgents=0;
	
	private double totalRegulationUp=0;
	private double totalRegulationUpEV=0;
	private double totalRegulationUpPHEV=0;
	private double totalRegulationDown=0;
	private double totalRegulationDownEV=0;
	private double totalRegulationDownPHEV=0;
	
	public V2G(DecentralizedSmartCharger mySmartCharger){
		this.mySmartCharger=mySmartCharger;
		initializeAgentStats();
		
	}
	
	public void initializeAgentStats(){
		for (Id id: mySmartCharger.vehicles.getKeySet()){
			agentV2GStatistic.put(id, new V2GAgentStats());
		}
	}
	
	public double getAgentV2GRevenues(Id id){
		return agentV2GStatistic.get(id).getRevenueV2G();
	}
	
	public double getAgentTotalJouleV2GUp(Id id){
		return agentV2GStatistic.get(id).getTotalJoulesUp();
	}
	
	public double getAgentTotalJouleV2GDown(Id id){
		return agentV2GStatistic.get(id).getTotalJoulesDown();
	}
	
	public double getAverageV2GRevenueAgent(){
		return averageV2GRevenueAllAgents;
	}
	
	public double getAverageV2GRevenueEV(){
		return averageV2GRevenueEV;
	}
	
	public double getAverageV2GRevenuePHEV(){
		return averageV2GRevenuePHEV;
	}
	
	
	public double getTotalRegulationUp(){
		return totalRegulationUp;
	}
	
	public double getTotalRegulationUpEV(){
		return totalRegulationUpEV;
	}
	
	public double getTotalRegulationUpPHEV(){
		return totalRegulationUpPHEV;
	}
	
	public double getTotalRegulationDown(){
		return totalRegulationDown;
	}
	
	public double getTotalRegulationDownEV(){
		return totalRegulationDownEV;
	}
	
	public double getTotalRegulationDownPHEV(){
		return totalRegulationDownPHEV;
	}
	
	public void calcV2GRevenueStats(){
		//EV
		int totalEV=mySmartCharger.getAllAgentsWithEV().size();
		for(int i=0; i<totalEV; i++){
			Id agentId= mySmartCharger.getAllAgentsWithEV().get(i);
			
			averageV2GRevenueEV+=getAgentV2GRevenues(agentId);
			averageV2GRevenueAllAgents+=getAgentV2GRevenues(agentId);
			
			totalRegulationDown+=getAgentTotalJouleV2GDown(agentId);
			totalRegulationDownEV+=getAgentTotalJouleV2GDown(agentId);
			
			totalRegulationUp+=getAgentTotalJouleV2GUp(agentId);
			totalRegulationUpEV+=getAgentTotalJouleV2GUp(agentId);
		}
		averageV2GRevenueEV=averageV2GRevenueEV/totalEV;
		
		//PHEV
		int totalPHEV=mySmartCharger.getAllAgentsWithPHEV().size();
		for(int i=0; i<totalPHEV; i++){
			Id agentId= mySmartCharger.getAllAgentsWithPHEV().get(i);
			averageV2GRevenuePHEV+=getAgentV2GRevenues(agentId);
			averageV2GRevenueAllAgents+=getAgentV2GRevenues(agentId);
			
			totalRegulationDown+=getAgentTotalJouleV2GDown(agentId);
			totalRegulationDownPHEV+=getAgentTotalJouleV2GDown(agentId);
			
			totalRegulationUp+=getAgentTotalJouleV2GUp(agentId);
			totalRegulationUpPHEV+=getAgentTotalJouleV2GUp(agentId);
		}
		averageV2GRevenuePHEV=averageV2GRevenuePHEV/totalPHEV;
		
		//TOTAL
		averageV2GRevenueAllAgents=averageV2GRevenueAllAgents/(totalPHEV+totalEV);
		
		
	}
	
	
	
	public void addRevenueToAgentFromV2G(double revenue, Id agentId){		
		agentV2GStatistic.get(agentId).addRevenueV2G(revenue);		
	}
	
	public void addJoulesUpDownToAgentStats(double joulesUpDown, Id agentId){		
		
		if (joulesUpDown>0){
			agentV2GStatistic.get(agentId).addJoulesUp(joulesUpDown);
		}else{
			agentV2GStatistic.get(agentId).addJoulesDown(joulesUpDown);
		}
	
	}
	
	
	
	public void regulationUpDownVehicleLoad(Id agentId, 
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
		// joules>0 
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
				currentSOC, joules);
		
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
	public void regulationUpDownHubLoad(Id agentId, 
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
					// joules >0
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
			double currentSOC,
			double joules) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException, OptimizationException{
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
					costReschedule,
					joules);
			
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
					costReschedule,
					joules);
			
			// e.g. stochastic load on net is -3500
			// thus car discharges 3500 to balance the net
			// thus -3500 will be reduced by (-3500)= 0
			reduceHubLoadByGivenLoadInterval(hub, electricSourceInterval);
			
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
		
		Schedule relevantAgentParkingAndDrivingSchedule= 
			agentParkingDrivingSchedule.cutScheduleAtTime(currentStochasticLoadInterval.getEndTime());
		
		relevantAgentParkingAndDrivingSchedule= 
			relevantAgentParkingAndDrivingSchedule.cutScheduleAtTimeSecondHalf(currentStochasticLoadInterval.getStartTime());
		
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
			double costReschedule,
			double joules) throws MaxIterationsExceededException, OptimizationException, FunctionEvaluationException, IllegalArgumentException{
		
		//UPDATE CHARGING COSTS 
		addRevenueToAgentFromV2G(costKeeping-costReschedule, agentId);
		
		// add joules to V2G up or down
		addJoulesUpDownToAgentStats(joules, agentId);
		
		// change agents charging costs
		mySmartCharger.getChargingCostsForAgents().put(
				agentId, 
				(mySmartCharger.getChargingCostsForAgents().get(agentId)-costKeeping+costReschedule)
				);
		
		
		// distribute new found required charging times for agent
		answerScheduleAfterElectricSourceInterval=updateChargingSchedule(agentId, 
				answerScheduleAfterElectricSourceInterval,
				electricSourceInterval);
		
		
		//UPDATE AGENTPARKINGADNDRIVINGSCHEDULE
		updateAgentDrivingParkingSchedule(agentId, 
				answerScheduleAfterElectricSourceInterval,
				agentParkingDrivingSchedule,
				electricSourceInterval);
		
	}
	
	/**
	 * creates and saves new charging schedule 
	 * 
	 * @param agentId
	 * @param answerScheduleAfterElectricSourceInterval
	 * @param electricSourceInterval
	 * @throws MaxIterationsExceededException
	 * @throws OptimizationException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 */
	public Schedule updateChargingSchedule(Id agentId, 
			Schedule answerScheduleAfterElectricSourceInterval,
			LoadDistributionInterval electricSourceInterval	) throws MaxIterationsExceededException, OptimizationException, FunctionEvaluationException, IllegalArgumentException{
		
		//UPDATE CHARGING SCHEDULE
		
		// determine the part which remains (first half )
		Schedule oldChargingSchedule= mySmartCharger.getAllAgentChargingSchedules().get(agentId);
		Schedule oldRemainingPartChargingSchedule= oldChargingSchedule.cutChargingScheduleAtTime(
				electricSourceInterval.getEndTime());
		
		if(DecentralizedSmartCharger.debug){
			if(oldRemainingPartChargingSchedule.getNumberOfEntries()>0){
				System.out.println("old remaining part of charging schedule of agent "+ agentId.toString());
				oldRemainingPartChargingSchedule.printSchedule();
			}
		}
		
		// determine the new second half
		Schedule newChargingSecondHalf= mySmartCharger.myChargingSlotDistributor.distribute(agentId, 
				answerScheduleAfterElectricSourceInterval);
		if(DecentralizedSmartCharger.debug){
			System.out.println("new rescheduled part of charging schedule of agent "+ agentId.toString());
			newChargingSecondHalf.printSchedule();
		}
		oldRemainingPartChargingSchedule.mergeSchedules(newChargingSecondHalf);
		mySmartCharger.getAllAgentChargingSchedules().put(agentId, oldRemainingPartChargingSchedule);
		
		
		
		return newChargingSecondHalf;
				
	}
	
	
	
	/**
	 * 
	 * @param agentId
	 * @param answerScheduleAfterElectricSourceInterval
	 * @param agentParkingDrivingSchedule
	 * @param electricSourceInterval
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 */
	public void updateAgentDrivingParkingSchedule(Id agentId, 
			Schedule answerScheduleAfterElectricSourceInterval,
			Schedule agentParkingDrivingSchedule,
			LoadDistributionInterval electricSourceInterval) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		Schedule rescheduledFirstHalf= agentParkingDrivingSchedule.cutScheduleAtTime( electricSourceInterval.getEndTime());
		
		rescheduledFirstHalf.mergeSchedules(answerScheduleAfterElectricSourceInterval);
		
		agentParkingDrivingSchedule=rescheduledFirstHalf;
		
		mySmartCharger.getAllAgentParkingAndDrivingSchedules().put(agentId, agentParkingDrivingSchedule);
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
		
		Schedule agentVehicleSource= mySmartCharger.myHubLoadReader.agentVehicleSourceMappingAfter.get(agentId);
		
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
		
		mySmartCharger.myHubLoadReader.agentVehicleSourceMappingAfter.put(agentId, 
				agentVehicleSource
				);
		
	}

	


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
		
		
		Schedule hubLoadSchedule= mySmartCharger.myHubLoadReader.stochasticHubLoadDistributionAfter.get(i);
		
		PolynomialFunction negativePolynomialFunc= 
			new PolynomialFunction(electricSourceInterval.getPolynomialFunction().getCoefficients().clone());
		negativePolynomialFunc=negativePolynomialFunc.negate();
		
		
		LoadDistributionInterval negativeElectricSourceInterval= new LoadDistributionInterval(
				electricSourceInterval.getStartTime(),
				electricSourceInterval.getEndTime(),
				negativePolynomialFunc,						 
				!electricSourceInterval.isOptimal());
		
		hubLoadSchedule.addLoadDistributionIntervalToExistingLoadDistributionSchedule(negativeElectricSourceInterval);
		
		mySmartCharger.myHubLoadReader.stochasticHubLoadDistributionAfter.put(i, hubLoadSchedule);
		
		
	}
	
	
	
	
	public void attributeSuperfluousVehicleLoadsToGridIfPossible(Id agentId, 
			Schedule agentParkingDrivingSchedule, 
			LoadDistributionInterval electricSourceInterval) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		Schedule agentDuringLoad= 
			agentParkingDrivingSchedule.cutScheduleAtTimeSecondHalf(electricSourceInterval.getStartTime());
		
		agentDuringLoad= agentDuringLoad.cutScheduleAtTime(electricSourceInterval.getEndTime());
		
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
					
					
					Schedule hubSchedule=mySmartCharger.myHubLoadReader.stochasticHubLoadDistribution.get(hubId);
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
					if(DecentralizedSmartCharger.debug){
						System.out.println("Reschedule was not possible!");
					}
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
		Schedule cutSchedule=agentParkingDrivingSchedule.cutScheduleAtTime(time);
		
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
		Schedule half= agentParkingDrivingSchedule.cutScheduleAtTime (time);
		
		reAssignJoulesToSchedule(half, id);
		return half;
	}
	
	
	
	public Schedule cutScheduleAtTimeSecondHalfAndReassignJoules (Id id, Schedule agentParkingDrivingSchedule, double time) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		Schedule half= agentParkingDrivingSchedule.cutScheduleAtTimeSecondHalf ( time);
		
		reAssignJoulesToSchedule(half, id);
		return half;
	}






	private void reAssignJoulesToSchedule(Schedule half, Id id) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		half.clearJoules();
		
		DecentralizedSmartCharger.myAgentTimeReader.getJoulesForEachParkingInterval(id, half);
	}



	
		
	
	
}
