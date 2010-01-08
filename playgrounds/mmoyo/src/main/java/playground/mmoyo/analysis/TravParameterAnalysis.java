package playground.mmoyo.analysis;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.mmoyo.PTRouter.LogicFactory;
import playground.mmoyo.PTRouter.PTRouter;
import playground.mmoyo.PTRouter.PTValues;

/**shows the average result values (travelTime, distance, number of transfers) of the whole population with different travel time and distance coefficients*/
public class TravParameterAnalysis {
	private PopulationImpl population;
	private List<PopulationResult> populationResultList = new ArrayList<PopulationResult>();
	
	private List<Path> pathListA = new ArrayList<Path>();
	private List<Path> pathListB = new ArrayList<Path>();
	
	public TravParameterAnalysis(final String plansFile, final LogicFactory logicFactory){
		NetworkLayer logicNet = logicFactory.getLogicNet();
		
		this.population = new PopulationImpl();
		MatsimPopulationReader plansReader = new MatsimPopulationReader(this.population, logicFactory.getLogicNet());
		plansReader.readFile(plansFile);
	
	
		///iterate with all coefficient values
		for (double i=0; i<=100 ; i++ ){
			double x = i/100;  
			PTRouter ptRouter = new PTRouter(logicNet,  x , (1-x), 60);
			routePopulation(x , ptRouter);	
		}
		
		//a unique coefficient value
		/*
		double timeCoefficient = 1;
		double distanceCoefficient = 0;
		double transferPenalty = 0;
		PTRouter ptRouter = new PTRouter(logicNet, timeCoefficient, distanceCoefficient, transferPenalty);
		pathListA =routePopulation(timeCoefficient , ptRouter);

		transferPenalty = 60;
		PTRouter ptRouter2 = new PTRouter(logicNet, timeCoefficient, distanceCoefficient, transferPenalty);
		pathListB =routePopulation(timeCoefficient , ptRouter2);
		System.out.println(pathListA.equals(pathListB));
		
		int differences=0;
		int  size= pathListA.size();
		for (int i=0; i< size-1; i++){
			if (pathListA.get(i) != pathListB.get(i))differences++; 
		}
		System.out.println("size:" + size + " diferences:" + differences);
		///
		*/
	
		
		System.out.println("Time Coefficient\tDistance Coefficient\tTimeAvg\tDistanceAvg\tTransfers\tDetTransfer\tWalkDistance");
		for (PopulationResult r : populationResultList){
			System.out.println(r.getTimeCoef() + "\t+" + r.getDistanceCoef() + "\t+" + r.getTimeAvg() + "\t+" + r.getDistanceAvg() + "\t+" + r.getTransferNum() + "\t+" + r.getDetTransferNum() + "\t+" + r.getWalkDistanceAvg());
			//r.getTransferPenalty()
		} 
		
	}
	
	public List<Path> routePopulation(double timeCoefficient, final PTRouter ptRouter){
		List<Path> pathList = new ArrayList<Path>();
		
		int numPlans=0;
		
		PopulationResult populationResult= new PopulationResult(ptRouter.getTimeCoeficient(), ptRouter.getDistanceCoeficient(), ptRouter.getTransferPenalty());
		
		for (Person person: population.getPersons().values()) {
			//if ( true ) {
			//PersonImpl person = population.getPersons().get(new IdImpl("905449")); // 5228308   5636428  2949483 
 			System.out.println(timeCoefficient + " " + (numPlans++) + " id:" + person.getId());
			Plan plan = person.getPlans().get(0);

			boolean first =true;
			ActivityImpl lastAct = null;
			ActivityImpl thisAct= null;
			
			//double startTime=0;
			//double duration=0;
			
			for (PlanElement pe : plan.getPlanElements()) {   		//temporarily commented in order to find only the first leg
			//for	(int elemIndex=0; elemIndex<3; elemIndex++){            //jun09  finds only
				//PlanElement pe= plan.getPlanElements().get(elemIndex);  //jun09  the first trip
				
				if (pe instanceof ActivityImpl){
					thisAct= (ActivityImpl) pe;
					if (!first) {
						Coord lastActCoord = lastAct.getCoord();
			    		Coord actCoord = thisAct.getCoord();
						//trips++;
			    		double distanceToDestination = CoordUtils.calcDistance(lastActCoord, actCoord);
			    		double distToWalk= PTValues.FIRST_WALKRANGE;
			    		if (distanceToDestination<= distToWalk){
			    			//inWalkRange++;
			    		}else{
				    		//startTime = System.currentTimeMillis();
				    		Path path = ptRouter.findPTPath(lastActCoord, actCoord, lastAct.getEndTime());
				    		
				    		//duration= System.currentTimeMillis()-startTime;
				    		if(path!=null){
					    		if (path.nodes.size()>1){
					    			//found++;
					    			populationResult.addPath(person.getId(), path);
					    			
					    			//18 sep
					    			pathList.add(path);
					    			
					    			//System.out.println("travelTime:" + path.travelTime);		    			
					    			//durations.add(duration);
				    			}else{
				    				//lessThan2Node++;
				    			}
				    		}else{
				    			//nulls++;
				    		}
			    		}
					}
		
					lastAct = thisAct;
					first=false;
				}
			}
		}//for person

		System.out.println("average Time:" + populationResult.getTimeAvg() + "   average distance:" + populationResult.getDistanceAvg() + "    transfers:" + populationResult.getTransferNum() + "  WalkDistance:" + populationResult.getWalkDistanceAvg());
		populationResultList.add(populationResult);
		return pathList;
	}

}
	
class ConnectionResult{
	Id agentId;
	double distance=0;
	int transfers=0;
	int detTransfers=0;
	double walkDistance =0;
	double walkTime =0;
	double tCost=0;
	double tTime =0;

	public ConnectionResult(final Id agentId, final Path path){
		this.agentId = agentId;
		this.tTime = path.travelTime;
		this.tCost = path.travelCost;
		
		for (Link link : path.links){
			distance += link.getLength();

			String type  = ((LinkImpl)link).getType();  
			if (type.equals(PTValues.DETTRANSFER_STR)){    //type.equals("Egress") || type.equals("Access") || <- we don't want to count the access and egress walk time until the radius search be defined.
				walkDistance += link.getLength();
				detTransfers++;
			}else if (type.equals(PTValues.TRANSFER_STR)){
				transfers++;
			}
		}	
	}

	public Id getAgentId(){
		return this.agentId;
	}
	
	public double getDistance() {
		return this.distance;
	}

	public int getTransfers() {
		return this.transfers;
	}
	public int getDetTransfers() {
		return this.detTransfers;
	}
	public double getWalkDistance() {
		return this.walkDistance;
	}
	public double getWalkTime() {
		return this.walkTime;
	}
	public double getTravelCost() {
		return this.tCost;
	}
	public double getTravelTime() {
		return this.tTime;
	}
}
	
class PopulationResult {
	private double distanceCoef=0;
	private double timeCoef=0;
	private double transferPenalty=0;
	private List<ConnectionResult> connectionResultList = new ArrayList<ConnectionResult>();
	private int connectionNumber=0;
	
	private double travelDistance =0;
	private double travelTime =0;
	private double transfers =0;
	private double detTransfers =0;
	private double walkDistance=0;
	
	
	public PopulationResult(final double timeCoef, final double distanceCoef, final double transferPenalty){
		this.timeCoef = timeCoef;
		this.distanceCoef = distanceCoef;
		this.transferPenalty =  transferPenalty;
	}
		
	public void addPath(final Id id, final Path path){
		ConnectionResult connectionResult = new ConnectionResult(id, path);
		this.connectionResultList.add(connectionResult);
		connectionNumber = connectionResultList.size(); 
		
		travelTime 	   += connectionResult.getTravelTime();
		travelDistance += connectionResult.getDistance();
		transfers 	   += connectionResult.getTransfers();
		detTransfers   += connectionResult.getDetTransfers();
		walkDistance   += connectionResult.getWalkDistance();
	}

	public double getDistanceCoef() {
		return distanceCoef;
	}
	
	public double getTimeCoef() {
		return timeCoef;
	}			
	
	public double getTransferPenalty() {
		return this.transferPenalty;
	}
	
	public List<ConnectionResult> getConnectionResultList() {
		return this.connectionResultList;
	}
	
	public double getDistanceAvg() {
		return this.travelDistance / connectionNumber; 
	}
	
	public double getTimeAvg() {
		return this.travelTime / connectionNumber; 
	}
	
	public double getTransferNum() {
		return this.transfers / connectionNumber;
	}
	
	public double getDetTransferNum() {
		return this.detTransfers / connectionNumber;
	}
	
	public double getWalkDistanceAvg() {
		return this.walkDistance / connectionNumber;
	}
}	