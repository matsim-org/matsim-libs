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
import playground.wrashid.sschieffer.DecentralizedSmartCharger.ChargingInterval;
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
	private HashMap<Id, V2GAgentStats> agentV2GStatistic; 
	
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
		agentV2GStatistic = new HashMap<Id, V2GAgentStats>(); 
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
	
	
	public void addRevenueToAgentFromFeedIn(double revenue, Id agentId){		
		agentV2GStatistic.get(agentId).addRevenueFeedIn(revenue);		
	}
	
	
	public void addJouleFeedInToAgentStats(double joulesUpDown, Id agentId){		
		
		if (joulesUpDown>0){
			agentV2GStatistic.get(agentId).addJoulesFeedIn(joulesUpDown);
		}	
	}
	
	public void addRevenueToAgentFromV2G(double revenue, Id agentId){		
		agentV2GStatistic.get(agentId).addRevenueV2G(revenue);		
	}
	
	
	
	
	public void addJoulesUpDownToAgentStats(double joulesUpDown, Id agentId){		
		
		if (joulesUpDown>0){
			//joules>0 reg down
			agentV2GStatistic.get(agentId).addJoulesDown(joulesUpDown);
		}else{
			agentV2GStatistic.get(agentId).addJoulesUp(joulesUpDown);
		}
	
	}
	
	
	
	public void regulationUpDownVehicleLoad(Id agentId, 
			LoadDistributionInterval electricSourceInterval, 
			Schedule agentParkingDrivingSchedule, 
			double compensation, // money
			double joules,		
			String type			
			) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, OptimizationException, LpSolveException, IOException{
		
	
		double currentSOC=getSOCAtTime(agentParkingDrivingSchedule, electricSourceInterval.getEndTime());
		/*
		 * joules>0 defined as reg down = local energy generation = currentSOC+ joules
		 * joules<0 = reg up = local energy demand  = currentSOC + (- joules)
		 */
		currentSOC=currentSOC+joules;
		
		costComparisonVehicle(agentId, 
				electricSourceInterval, 
				agentParkingDrivingSchedule, 
				compensation,				
				type,				
				currentSOC, joules);
		
	}
	
	
	
	/**
	 * determines during which times the agent is actually parking at the hub in the given load interval
	 * calculates costs for keeping or rescheduling
	 * and then makes a cost comparison
	 * 
	 * @param agentId
	 * @param currentStochasticLoadInterval
	 * @param agentParkingDrivingSchedule
	 * @param type
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
			String type,			
			int hub) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException, OptimizationException{
		
		//*****************************
		//*****************************
		// CHECK WHICH PARTS AGENT IS AT HUB and parking
			Schedule relevantAgentParkingAndDrivingScheduleAtHub= 
				findAndReturnAgentScheduleWithinLoadIntervalWhichIsAtSpecificHub(hub, 
				agentParkingDrivingSchedule,
				currentStochasticLoadInterval);
		
			if(relevantAgentParkingAndDrivingScheduleAtHub.getNumberOfEntries()>0){
				
				for(int i=0; i<relevantAgentParkingAndDrivingScheduleAtHub.getNumberOfEntries();i++){
					
					ParkingInterval t= (ParkingInterval) relevantAgentParkingAndDrivingScheduleAtHub.timesInSchedule.get(i);
					// the overlap interval is:
					LoadDistributionInterval electricSourceInterval= 
						new LoadDistributionInterval(t.getStartTime(), 
								t.getEndTime(), 
								new PolynomialFunction(currentStochasticLoadInterval.getPolynomialFunction().getCoefficients().clone()), 
								currentStochasticLoadInterval.isOptimal());
					
					double joules= mySmartCharger.functionIntegrator.integrate(electricSourceInterval.getPolynomialFunction(), 
							electricSourceInterval.getStartTime(),
							electricSourceInterval.getEndTime());
					
					/*
					 * IN CASE JOULES IS HIGHER THAN WHAT IS FEASIBLE AT THE CONNECTION
					 */
					if (joules>t.getChargingSpeed()*electricSourceInterval.getIntervalLength()){
						electricSourceInterval= 
							new LoadDistributionInterval(t.getStartTime(), 
									t.getEndTime(), 
									new PolynomialFunction(new double []{t.getChargingSpeed()}), 
									currentStochasticLoadInterval.isOptimal());
						joules=t.getChargingSpeed()*electricSourceInterval.getIntervalLength();
					}
					//*****************************
					agentParkingDrivingSchedule.printSchedule();
					double currentSOC=getSOCAtTime(agentParkingDrivingSchedule, electricSourceInterval.getEndTime());
					
					/*
					 * if regulation up   joules<0    currentSOC+(-joules)
					 * if regulation down  joules>0   currentSOC+(+joules)
					 */
					currentSOC=currentSOC+joules;
					//*****************************
					double compensation=calculateCompensationUpDown(joules,agentId);
					
					costComparisonHubload(agentId, 
							electricSourceInterval, 							
							compensation,
							joules,							
							type,							
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
			String type,				
			double currentSOC,
			double joules) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException, OptimizationException{
		
		double batterySize= mySmartCharger.getBatteryOfAgent(agentId).getBatterySize();
		double batteryMin=mySmartCharger.getBatteryOfAgent(agentId).getMinSOC();
		double batteryMax=mySmartCharger.getBatteryOfAgent(agentId).getMaxSOC(); 
		
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
					compensation
					);
		}else{
			costReschedule=Double.MAX_VALUE;
		}
			
		// reschedule if equal - hopefully the load can be balanced elsewhere in the grid
		if(costKeeping>=costReschedule){
			
			//reschedule
			reschedule(agentId, 
					answerScheduleAfterElectricSourceInterval,					
					electricSourceInterval,
					costKeeping,
					costReschedule,
					joules);
			
			// joules>0 positive function which needs to be reduced
			reduceAgentVehicleLoadsByGivenLoadInterval(
					agentId, 
					electricSourceInterval);
			
			
		
		}else{
			
			if(joules>0){
				// reg down not possible - i.e. battery full
				// thus attribute rest to upper level
				attributeSuperfluousVehicleLoadsToGridIfPossible(agentId, 					 
						electricSourceInterval
						);	
			}else{
				// reg up = local energy demand
				// increase charging need here
				chargeMoreToAccomodateExtraVehicleLoad(agentId, 					 
						electricSourceInterval);
			}
					
			
		}
	}
	
	
	
	public void costComparisonHubload(Id agentId, 
			LoadDistributionInterval electricSourceInterval, 			
			double compensation,
			double joules,			
			String type,			
			int hub,
			double currentSOC) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException, OptimizationException{
		
		Schedule secondHalf= cutScheduleAtTimeSecondHalfAndReassignJoules(agentId, 
				mySmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentId), 
				electricSourceInterval.getEndTime());
		//secondHalf.printSchedule();
		secondHalf.setStartingSOC(currentSOC);
		answerScheduleAfterElectricSourceInterval=null;
		
		/*System.out.println("schedule before rescheduling");
		agentParkingDrivingSchedule.printSchedule();*/
		double costKeeping=mySmartCharger.calculateChargingCostForAgentSchedule(agentId, secondHalf);
		
		double batterySize= mySmartCharger.getBatteryOfAgent(agentId).getBatterySize();
		double batteryMin=mySmartCharger.getBatteryOfAgent(agentId).getMinSOC();
		double batteryMax=mySmartCharger.getBatteryOfAgent(agentId).getMaxSOC(); 
		
		double costReschedule=0;
		if (currentSOC>=batterySize*batteryMin && currentSOC<=batterySize*batteryMax){
			costReschedule= calcCostForRescheduling(
					secondHalf, 
					agentId, 
					batterySize, batteryMin, batteryMax, 
					type,
					currentSOC,					
					compensation					
					);
		}else{
			costReschedule=Double.MAX_VALUE;
		}
		if(DecentralizedSmartCharger.debug){
			System.out.println("CostKeeping"+ costKeeping + "Cost Rescheduling"+ costReschedule);	
		}
		
		// if costs are equal = also reschedule, because its good for the system
		if(costKeeping>=costReschedule){
			
			//UPDATE CHARGING COSTS 
			addRevenueToAgentFromV2G(costKeeping-costReschedule, agentId);
			
			// add joules to V2G up or down
			addJoulesUpDownToAgentStats(joules, agentId);
			
			//reschedule
			reschedule(agentId, 
					answerScheduleAfterElectricSourceInterval,
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
			relevantAgentParkingAndDrivingSchedule.cutScheduleAtTimeSecondHalf(currentStochasticLoadInterval.getStartTime(), 0.0);
		
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
			LoadDistributionInterval electricSourceInterval,
			double costKeeping,
			double costReschedule,
			double joules) throws MaxIterationsExceededException, OptimizationException, FunctionEvaluationException, IllegalArgumentException{
		
		
		// change agents charging costs
		mySmartCharger.getChargingCostsForAgents().put(
				agentId, 
				(mySmartCharger.getChargingCostsForAgents().get(agentId)-costKeeping+costReschedule)
				);
		
				
		//UPDATE AGENTPARKINGADNDRIVINGSCHEDULE WITH NEW required charging times
		// also reducing or increasing charging times during electric load
		updateAgentDrivingParkingSchedule(agentId, 
				answerScheduleAfterElectricSourceInterval,				
				electricSourceInterval,
				joules);
		
		// distribute new found required charging times for agent
		updateChargingSchedule(agentId, 
				answerScheduleAfterElectricSourceInterval,
				electricSourceInterval,
				joules);
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
	public void  updateChargingSchedule(Id agentId, 
			Schedule answerScheduleAfterElectricSourceInterval,
			LoadDistributionInterval electricSourceInterval, 
			double joules	) throws MaxIterationsExceededException, OptimizationException, FunctionEvaluationException, IllegalArgumentException{
		
		//UPDATE CHARGING SCHEDULE
		
		Schedule newChargingSchedule= mySmartCharger.myChargingSlotDistributor.distribute(
				agentId, 
				mySmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentId));
		if(DecentralizedSmartCharger.debug){	
			
			System.out.println("new  charging schedule of agent "+ agentId.toString());
			 newChargingSchedule.printSchedule();
			 System.out.println("new  parking driving schedule of agent "+ agentId.toString());
			 mySmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentId).printSchedule();	
		}
		
		mySmartCharger.getAllAgentChargingSchedules().put(agentId, newChargingSchedule);
				
				
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
			LoadDistributionInterval electricSourceInterval,
			double joules) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		if(DecentralizedSmartCharger.debug){		
			System.out.println("parking driving schedule of agent BEFORE"+ agentId.toString());
			mySmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentId).printSchedule();
		}
		
		Schedule rescheduledFirstHalf= mySmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentId).cutScheduleAtTime( electricSourceInterval.getEndTime());
		//inbetween part with new load
		Schedule rescheduledElectricLoadPart= rescheduledFirstHalf.cutScheduleAtTimeSecondHalf(electricSourceInterval.getStartTime(), 0.0);
		
		// insert new bit
		// joules/charging speed = secs
		ParkingInterval thisParking=((ParkingInterval)rescheduledElectricLoadPart.timesInSchedule.get(0));
		double newChargingLength= joules/thisParking.getChargingSpeed();
		/*
		 * if joules>0  reg down=charging,--> then new CHargingLength>0	
		 * new ChargingLength= oldLength+newLength
		 * ifjoules<0 reg up --> charging length<0
		 * new Charging Length = oldLength+(-newLength)
		 */
		thisParking.setRequiredChargingDuration( thisParking.getRequiredChargingDuration()+newChargingLength);
		
		
		//first remaining part
		rescheduledFirstHalf= rescheduledFirstHalf.cutScheduleAtTime(electricSourceInterval.getStartTime());
		
		rescheduledFirstHalf.mergeSchedules(rescheduledElectricLoadPart);
		rescheduledFirstHalf.mergeSchedules(answerScheduleAfterElectricSourceInterval);
				
		mySmartCharger.getAllAgentParkingAndDrivingSchedules().put(agentId, rescheduledFirstHalf);
		if(DecentralizedSmartCharger.debug){		
			System.out.println("parking driving schedule of agent After"+ agentId.toString());
			mySmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentId).printSchedule();
		}
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
			LoadDistributionInterval electricSourceInterval
			) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		Schedule agentDuringLoad= 
			mySmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentId).cutScheduleAtTimeSecondHalf(electricSourceInterval.getStartTime(), 0.0);
		
		agentDuringLoad= agentDuringLoad.cutScheduleAtTime(electricSourceInterval.getEndTime());	
		
		for(int i=0; i<agentDuringLoad.getNumberOfEntries();i++){
			TimeInterval agentIntervalInAgentDuringLoad= agentDuringLoad.timesInSchedule.get(i);
			if (agentIntervalInAgentDuringLoad.isParking()){
				
				// if overlap between agentInterval && electricSource
				LoadDistributionInterval overlapAgentAndElectricSource=
					agentIntervalInAgentDuringLoad.ifOverlapWithLoadDistributionIntervalReturnOverlap(electricSourceInterval);
				
				if(overlapAgentAndElectricSource!=null){
					//IF PARKING					
					// can be transmitted to HUb - thus reduce Vehicle Load 
					reduceAgentVehicleLoadsByGivenLoadInterval(
							agentId, 
							overlapAgentAndElectricSource);
					
					double joulesOfOverlap=DecentralizedSmartCharger.functionIntegrator.integrate(
							overlapAgentAndElectricSource.getPolynomialFunction(), overlapAgentAndElectricSource.getStartTime(), overlapAgentAndElectricSource.getEndTime());
					
					
					double revenue=calculateCompensationFeedIn(joulesOfOverlap, agentId);//compensationPerFeedIn 
					addRevenueToAgentFromFeedIn(revenue, agentId);
					addJouleFeedInToAgentStats(joulesOfOverlap, agentId);
										
					
					/*
					 * REDUCE HUB LOAD
					 */
					int hubId=mySmartCharger.myHubLoadReader.getHubForLinkId(
							((ParkingInterval)agentIntervalInAgentDuringLoad).getLocation());					
					
					Schedule hubSchedule=mySmartCharger.myHubLoadReader.stochasticHubLoadDistribution.get(hubId);
					//if func<0  regulation up + = --  SWITCH SIGN
					overlapAgentAndElectricSource.negatePolynomialFunc();
					if (hubSchedule.overlapWithTimeInterval(overlapAgentAndElectricSource)){			
						
						hubSchedule.addLoadDistributionIntervalToExistingLoadDistributionSchedule(overlapAgentAndElectricSource);												
					}else{
						hubSchedule.addTimeInterval(overlapAgentAndElectricSource);
						hubSchedule.sort();					
					}
					
					
					if(DecentralizedSmartCharger.debug){
						System.out.println("hubSchedule after adding superfluous loads:");
						hubSchedule.printSchedule();
					}
				}
			}							
				
		}
	}
	
	
	public void chargeMoreToAccomodateExtraVehicleLoad(Id agentId, 
			LoadDistributionInterval electricSourceInterval
			) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		Schedule agentDuringLoad= 
			mySmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentId).cutScheduleAtTimeSecondHalf(electricSourceInterval.getStartTime(), 0.0);
		
		agentDuringLoad= agentDuringLoad.cutScheduleAtTime(electricSourceInterval.getEndTime());	
		
		for(int i=0; i<agentDuringLoad.getNumberOfEntries();i++){
			TimeInterval agentIntervalInAgentDuringLoad= agentDuringLoad.timesInSchedule.get(i);
			if (agentIntervalInAgentDuringLoad.isParking()){
				
				// if overlap between agentInterval && electricSource
				LoadDistributionInterval overlapAgentAndElectricSource=
					agentIntervalInAgentDuringLoad.ifOverlapWithLoadDistributionIntervalReturnOverlap(electricSourceInterval);
				
				if(overlapAgentAndElectricSource!=null){
					//IF PARKING					
					// can be transmitted to HUb - thus reduce Vehicle Load 
					reduceAgentVehicleLoadsByGivenLoadInterval(
							agentId, 
							overlapAgentAndElectricSource);
					
					double joulesOfOverlap=DecentralizedSmartCharger.functionIntegrator.integrate(
							overlapAgentAndElectricSource.getPolynomialFunction(), overlapAgentAndElectricSource.getStartTime(), overlapAgentAndElectricSource.getEndTime());
					
					
					// increase charging costs
					// approximation of costs of charging
					double extraCost=approximateChargingCostOfVariableLoad(((ParkingInterval)agentIntervalInAgentDuringLoad), joulesOfOverlap);
												
					mySmartCharger.getChargingCostsForAgents().put(
								agentId, 
								(mySmartCharger.getChargingCostsForAgents().get(agentId)+extraCost)
								);
					
				
					/*
					 * REDUCE HUB LOAD
					 */
					int hubId=mySmartCharger.myHubLoadReader.getHubForLinkId(
							((ParkingInterval)agentIntervalInAgentDuringLoad).getLocation());					
					
					Schedule hubSchedule=mySmartCharger.myHubLoadReader.stochasticHubLoadDistributionAfter.get(hubId);
					//if func<0  regulation up + = --  SWITCH SIGN
					overlapAgentAndElectricSource.negatePolynomialFunc();
					if (hubSchedule.overlapWithTimeInterval(overlapAgentAndElectricSource)){			
						
						hubSchedule.addLoadDistributionIntervalToExistingLoadDistributionSchedule(overlapAgentAndElectricSource);												
					}else{
						hubSchedule.addTimeInterval(overlapAgentAndElectricSource);
						hubSchedule.sort();					
					}
					
					mySmartCharger.myHubLoadReader.stochasticHubLoadDistributionAfter.put(hubId, hubSchedule);
					
					if(DecentralizedSmartCharger.debug){
						System.out.println("hubSchedule after adding extra vehicle charging load:");
						hubSchedule.printSchedule();
					}
				}
			}							
				
		}
	}
	
	
	
	public double approximateChargingCostOfVariableLoad(
			ParkingInterval agentIntervalInAgentDuringLoad, double joulesOfOverlap){
		
		double pricingValueOfTime= mySmartCharger.myHubLoadReader.getValueOfPricingPolynomialFunctionAtLinkAndTime(
				agentIntervalInAgentDuringLoad.getLocation(), 
				agentIntervalInAgentDuringLoad.getStartTime());
		
		//pricingValueOfTime *1s = CHF/s*1s = 3500W
		
		return Math.abs(pricingValueOfTime*joulesOfOverlap/3500.0);
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
			double compensation		
			) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException{
		
		double costReschedule=0;
		//***********************************
		//EV
		//***********************************
		if(mySmartCharger.hasAgentEV(agentId)){
			answerScheduleAfterElectricSourceInterval= mySmartCharger.getLPEV().solveLPReschedule(secondHalf, agentId, batterySize, batteryMin, batteryMax, type,currentSOC);
			
			if(answerScheduleAfterElectricSourceInterval==null){
				//System.out.println("Reschedule was not possible!");
				costReschedule= Double.MAX_VALUE;
			}else{
				costReschedule=mySmartCharger.calculateChargingCostForAgentSchedule(agentId, answerScheduleAfterElectricSourceInterval)
								-compensation;
			}
			
			
		}else{
			//***********************************
			//PHEV
			//***********************************
			
			answerScheduleAfterElectricSourceInterval= mySmartCharger.getLPEV().solveLPReschedule(secondHalf, 
					agentId, batterySize, batteryMin, batteryMax, 
					type,currentSOC);
			
			if(answerScheduleAfterElectricSourceInterval==null){
				answerScheduleAfterElectricSourceInterval = mySmartCharger.getLPPHEV().solveLPReschedule(
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
		Schedule half= agentParkingDrivingSchedule.cutScheduleAtTimeSecondHalf ( time, 0.0);
		
		reAssignJoulesToSchedule(half, id);
		return half;
	}






	private void reAssignJoulesToSchedule(Schedule half, Id id) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		half.clearJoules();
		
		DecentralizedSmartCharger.myAgentTimeReader.getJoulesForEachParkingInterval(id, half);
	}



	public double calculateCompensationUpDown(double contributionInJoulesAgent, Id agentId){
		/*
		 * if regulation up   joules<0
		 * if regulation down  joules>0
		 */
		if (contributionInJoulesAgent>0){//down
			double compensationPerJouleRegulationDown= 
				mySmartCharger.getAgentContracts().get(agentId).compensationDown()*mySmartCharger.KWHPERJOULE;
				
			return  Math.abs(contributionInJoulesAgent*compensationPerJouleRegulationDown);
		}else{
			double compensationPerJouleRegulationUp= 
				mySmartCharger.getAgentContracts().get(agentId).compensationUp()*mySmartCharger.KWHPERJOULE;
				
			return  Math.abs(contributionInJoulesAgent*compensationPerJouleRegulationUp);
		}
		
	}
	
	
	public double calculateCompensationFeedIn(double contributionInJoulesAgent, Id agentId){
		/*
		 * if regulation up   joules<0
		 * if regulation down  joules>0
		 */
		if (contributionInJoulesAgent>0){//feedin
			double compensationPerJouleRegulationDown= 
				mySmartCharger.getAgentContracts().get(agentId).compensationUpFeedIn()*mySmartCharger.KWHPERJOULE;
				
			return  Math.abs(contributionInJoulesAgent*compensationPerJouleRegulationDown);
		}else{//charging
			
			return  0.0;
		}
		
	}
		
	
	
	
}
