/**
 * 
 */
package playground.mzilske.freight.vrp;

import java.util.ArrayList;
import java.util.Map;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

/**
 * Implements the Single-Depot Clark&Wright (parallel version) algorithm described at 
 * http://web.mit.edu/urban_or_book/www/book/chapter6/6.4.12.html.
 * The algorithm is based on a SYMMETRIC costMatrix, thus just the lower triangular matrix is applied.
 * @author schroeder
 *
 */
public class ClarkAndWright {

	private final static Logger logger = Logger.getLogger(ClarkAndWright.class);
	private final static String TYPE = "Clark&Wright"; 
	private final VRP vrp;
	private PriorityQueue<Saving> savings;
	private Tours vehicleTours = new Tours();
	
	public ClarkAndWright(final VRP vrp, TourFactory tourFactory){
		this.vrp = vrp;
		int iniDimension = vrp.getNodes().values().size();
		int initialCapacity = iniDimension*iniDimension;			
		savings = new PriorityQueue<Saving>(initialCapacity,new DescendingOrderSavingComparator());
	}
	
	public void construct() {
		computeSavings();
		buildTours();
		putSolutionToVRP();
	}

	public String getType() {
		return TYPE;
	}
	
	private void computeSavings(){
		logger.info("compute savings");
		Costs costMatrix = vrp.getCosts();
		for(Node from : vrp.getNodes().values()){
			for(Node to : vrp.getNodes().values()){
				if(from.equals(to)){
					continue;
				}
				if(costMatrix.getCost(from,to) != null){
					Node depot = vrp.getNodes().get(vrp.getDepotId());
					double costSaving = costMatrix.getCost(depot,from) + 
						costMatrix.getCost(to,depot) - costMatrix.getCost(from,to);
					if(costSaving>0){
						Saving saving = new Saving(from.getId(),to.getId(),costSaving);	
						savings.add(saving);
					}
				}
			}
		}
	}
	
	private void buildTours(){
		logger.info("build tours");
		while(savingsListNotEmpty() && notAllDemandPointsAssigned()){
			Saving saving = savings.poll();
			Id from = saving.getOrigin();
			Id to = saving.getDestination();
			if(bothDemandPointsNotAssigned(from, to)){
				createNewTourIfConstraintsNotViolated(from,to);
				continue;
			}
			else if(bothDemandPointsAlreadyAssigned(from, to)){
				mergeToursIfConstraintsNotViolated(from,to);
				continue;
			}
			else if(atLeastOneNodeIsAlreadyAssignedToATour()){
				if(demandPointAlreadyAssigned(from)){
					VehicleTour tour = getTourWith(from);
					Node fromNode = vrp.getNodes().get(from);
					if(tour.nodeIsAtEnd(fromNode)){
						verifyAndInsertNodeAtEnd(tour, to);
					}
					else{
						doNothing();
					}
				}
				else{
					VehicleTour tour = getTourWith(to);
					Node toNode = vrp.getNodes().get(to);
					if(tour.nodeIsAtBeginning(toNode)){
						verifyAndInsertNodeAtBeginning(tour,from);
					}
				}
				continue;
			}
		}
		if(notAllDemandPointsAssigned()){
			createAndAddShuttleTours();
		}
	}

	private void putSolutionToVRP() {
		logger.info("set vrp-solution");
		VRPSolution solution = new VRPSolution(vehicleTours.getTours());
		vrp.setSolution(solution);
	}
	
	private void verifyAndInsertNodeAtEnd(VehicleTour tour, Id stoppToAdd) {
		NodeInsertionTourBuilder tourBuilder = new NodeInsertionTourBuilder(vrp.getCosts());
		tourBuilder.setTour(tour);
		Node nodeToBeInserted = vrp.getNodes().get(stoppToAdd);
		tourBuilder.insertNodeAtEnd(nodeToBeInserted);
		VehicleTourImpl newTour = tourBuilder.build();
		if(vrp.getConstraints().tourDoesNotViolateConstraints(newTour,vrp.getCosts())){
			vehicleTours.removeTour(tour);
			vehicleTours.addTour(newTour);
		}
		else {
			doNothing();
		}
	}

	private void verifyAndInsertNodeAtBeginning(VehicleTour tour, Id stoppToAdd) {
		Node nodeToBeInserted = vrp.getNodes().get(stoppToAdd);
		NodeInsertionTourBuilder tourBuilder = new NodeInsertionTourBuilder(vrp.getCosts());
		tourBuilder.setTour(tour);
		tourBuilder.insertNodeAtBeginning(nodeToBeInserted);
		VehicleTourImpl newTour = tourBuilder.build();
		if(vrp.getConstraints().tourDoesNotViolateConstraints(newTour,vrp.getCosts())){
			vehicleTours.removeTour(tour);
			vehicleTours.addTour(newTour);
		}
		else {
			doNothing();
		}
	}

	private void mergeToursIfConstraintsNotViolated(Id i, Id j) {
		if(demandPointsAreInDifferentTours(i,j)){
			VehicleTour tour1 = getTourWith(i);
			VehicleTour tour2 = getTourWith(j);
			Node iNode = vrp.getNodes().get(i);
			Node jNode = vrp.getNodes().get(j);
			if(tour1.nodeIsAtEnd(iNode) && tour2.nodeIsAtBeginning(jNode)){
					VehicleTourImpl newTour = mergeTours(tour1,tour2);
					if(vrp.getConstraints().tourDoesNotViolateConstraints(newTour,vrp.getCosts())){
						vehicleTours.removeTour(tour1);
						vehicleTours.removeTour(tour2);
						vehicleTours.addTour(newTour);
					}
			}
		}
	}

	private void createNewTourIfConstraintsNotViolated(Id i, Id j) {
		Node depotNode = vrp.getNodes().get(vrp.getDepotId());
		Node iNode = vrp.getNodes().get(i);
		Node jNode = vrp.getNodes().get(j);
		NodeInsertionTourBuilder tourBuilder = new NodeInsertionTourBuilder(vrp.getCosts());
		VehicleTourImpl tour = tourBuilder.createRoundTour(depotNode, iNode, jNode);
		if(vrp.getConstraints().tourDoesNotViolateConstraints(tour,vrp.getCosts())){
			vehicleTours.addTour(tour);
		}
	}
	
	private void createAndAddShuttleTours() {
		ArrayList<Id> stoppsNotYetAssigned = getNodesWithoutTour();
		for(Id id : stoppsNotYetAssigned){
			if(!id.equals(vrp.getDepotId())){
				createAndAddShuttleTour(id);
			}
		}
	}

	private void createAndAddShuttleTour(Id to) {
		Node depot = vrp.getNodes().get(vrp.getDepotId());
		Node customer = vrp.getNodes().get(to);
		NodeInsertionTourBuilder tourBuilder = new NodeInsertionTourBuilder(vrp.getCosts());
		VehicleTourImpl tour = tourBuilder.createShuttleTour(depot, customer);
		if(vrp.getConstraints().tourDoesNotViolateConstraints(tour,vrp.getCosts())){
			vehicleTours.addTour(tour);
		}
		else{
			throw new IllegalStateException("could not create shuttle tour. a job ("+tour+") exists which cannot " +
					"be assigned to a tour given the tour-constraints " + vrp.getConstraints());
		}
	}

	private ArrayList<Id> getNodesWithoutTour() {
		ArrayList<Id> stopps = new ArrayList<Id>();
		for(Node node : vrp.getNodes().values()){
			if(!vehicleTours.getNodesToTourMap().containsKey(node.getId())){
				stopps.add(node.getId());
			}
		}
		return stopps;
	}

	private boolean atLeastOneNodeIsAlreadyAssignedToATour() {
		return true;
	}

	private VehicleTourImpl mergeTours(VehicleTour tour1, VehicleTour tour2) {
		verifyToursHaveSameDepotId(tour1,tour2);
		NodeInsertionTourBuilder tourBuilder = new NodeInsertionTourBuilder(vrp.getCosts());
		VehicleTourImpl tour = tourBuilder.createTourMerge(tour1, tour2);
		return tour;
	}
	
	private void verifyToursHaveSameDepotId(VehicleTour tour1, VehicleTour tour2) {
		if(tour1.getFirst().equals(tour2.getFirst())){
			if(tour1.getLast().equals(tour2.getLast())){
				return;
			}
		}
		throw new IllegalStateException("depots are not the same");
	}

	private boolean demandPointsAreInDifferentTours(Id origin, Id destination) {
		Map<Id, VehicleTourImpl> nodesToTourMap = vehicleTours.getNodesToTourMap();
		if(nodesToTourMap.get(origin) != nodesToTourMap.get(destination)){
			return true;
		}
		return false;
	}

	private void doNothing(){
		
	}
	
	private boolean savingsListNotEmpty(){
		if(savings.isEmpty()){
			return false;
		}
		return true;
	}
	
	private boolean notAllDemandPointsAssigned(){
		if(vehicleTours.getNodesToTourMap().keySet().size() < vrp.getNodes().values().size()){
			return true;
		}
		return false;
	}
	
	private boolean demandPointAlreadyAssigned(Id id){
		if(vehicleTours.getNodesToTourMap().containsKey(id)){
			return true;
		} 
		return false;
	}
	
	private boolean bothDemandPointsAlreadyAssigned(Id firstId, Id secondId){
		if(demandPointAlreadyAssigned(firstId) && demandPointAlreadyAssigned(secondId)){
			return true;
		}
		return false;
	}
	
	private boolean bothDemandPointsNotAssigned(Id firstId,Id secondId){
		if(!demandPointAlreadyAssigned(firstId) && !demandPointAlreadyAssigned(secondId)){
			return true;
		}
		return false;
	}
	
	private VehicleTour getTourWith(Id id){
		if(vehicleTours.getNodesToTourMap().containsKey(id)){
			return vehicleTours.getNodesToTourMap().get(id);
		}
		return null;
	}
	
}
