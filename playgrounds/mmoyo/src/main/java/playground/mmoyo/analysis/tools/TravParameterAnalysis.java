package playground.mmoyo.analysis.tools;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.mmoyo.PTRouter.LogicFactory;
import playground.mmoyo.PTRouter.PTRouter;
import playground.mmoyo.PTRouter.PTValues;

/**shows the average result values (travelTime, distance, number of transfers) of the whole population with different travel time and distance coefficients*/
public class TravParameterAnalysis {

	private List<Path> pathListA = new ArrayList<Path>();
	private List<Path> pathListB = new ArrayList<Path>();
	List<PopulationResult> populationResultList = new ArrayList<PopulationResult>();
	
	public TravParameterAnalysis(final ScenarioImpl scenario){
		
		///iterate with all coefficient values
		for (double x= 0; x<=1.01; x = x + 0.05 ){
			DecimalFormat twoDForm = new DecimalFormat("#.##");
			PTValues.timeCoefficient = Double.valueOf(twoDForm.format(x));
			PTValues.distanceCoefficient= Math.abs(Double.valueOf(twoDForm.format(1-x)));
			PTValues.scenarioName =  "dist" + PTValues.distanceCoefficient + "_time" + PTValues.timeCoefficient ; 

			System.out.println("\nScenario:" + PTValues.scenarioName);
			routePopulation(scenario);
		}
		
		System.out.println("Time Coefficient\tDistance Coefficient\tTimeAvg\tDistanceAvg\tTransfers\tDetTransfer\tWalkDistance");
		for (PopulationResult popResult : populationResultList){
			System.out.println(PTValues.timeCoefficient + "\t+" + PTValues.distanceCoefficient + "\t+" + popResult.getTimeAvg() + "\t+" + popResult.getDistanceAvg() + "\t+" + popResult.getTransferNum() + "\t+" + popResult.getDetTransferNum() + "\t+" + popResult.getWalkDistanceAvg());
		} 
	}
	
	public List<Path> routePopulation(ScenarioImpl scenario){
		LogicFactory logicFactory = new LogicFactory (scenario.getTransitSchedule());
		NetworkLayer logicNet = logicFactory.getLogicNet();
		PTRouter ptRouter = new PTRouter(logicNet);
		
		List<Path> pathList = new ArrayList<Path>();
		int numPlans=0;
		PopulationResult populationResult= new PopulationResult();

		for (Person person: scenario.getPopulation().getPersons().values()) {
			//if ( true ) {
			//PersonImpl person = population.getPersons().get(new IdImpl("905449")); // 5228308   5636428  2949483 
 			System.out.println(PTValues.timeCoefficient + " " + (numPlans++) + " id:" + person.getId());
			Plan plan = person.getPlans().get(0);

			boolean first =true;
			ActivityImpl lastAct = null;
			ActivityImpl thisAct= null;
			
			//double startTime=0;
			//double duration=0;
			
			for (PlanElement pe : plan.getPlanElements()) {
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
	private List<ConnectionResult> connectionResultList = new ArrayList<ConnectionResult>();
	private int connectionNumber=0;
	
	private double travelDistance =0;
	private double travelTime =0;
	private double transfers =0;
	private double detTransfers =0;
	private double walkDistance=0;
	
	
	public PopulationResult(){
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