package playground.mzilske.freight.vrp;

import java.util.ArrayList;

import playground.mzilske.freight.TourBuilder;

public class NodeInsertionTourBuilder {
	
	private Costs costMatrix;	
	
	private VehicleTour tour;
	
	private VehicleTourImpl newTour;
	
	private boolean tourSet = false;
	
	private ArrayList<Node> newNodeSequence = new ArrayList<Node>();
	
	private Node nodeToBeInserted;
	
	private boolean nodeInserted = false;

	private boolean tourMerged = false;
	
	public NodeInsertionTourBuilder(Costs costMatrix) {
		super();
		this.costMatrix = costMatrix;
	}

	public void setTour(VehicleTour tour){
		this.tour = tour;
		tourSet = true;
	}
	
	public void insertNodeAtEnd(Node nodeToBeInserted){
		if(tourSet && !nodeInserted){
			double tourCost = 0.0;
			TourBuilder tourBuilder = new TourBuilder();
			tourBuilder.setStartNode(tour.getFirst());
			Node lastNode = tour.getFirst();
			for(int i=1;i<tour.getNodes().size()-1;i++){
				Node nextNode = tour.getNodes().get(i);
				tourBuilder.addNode(nextNode);
				tourCost += costMatrix.getCost(lastNode, nextNode);
				lastNode = nextNode;
			}
			tourBuilder.addNode(nodeToBeInserted);
			tourCost += costMatrix.getCost(lastNode, nodeToBeInserted);
			tourBuilder.setEndNode(tour.getLast());
			tourCost += costMatrix.getCost(nodeToBeInserted, tour.getLast());
			tourBuilder.setTourCost(tourCost);
			newTour = tourBuilder.build();
			nodeInserted = true;
		}
		else{
			throw new RuntimeException("either tour not set or node already inserted");
		}
	}
	
	public void insertNodeAtBeginning(Node nodeToBeInserted) {
		if(tourSet && !nodeInserted){
			double tourCost = 0.0;
			TourBuilder tourBuilder = new TourBuilder();
			Node depotNode = tour.getFirst();
			tourBuilder.setStartNode(depotNode);
			tourBuilder.addNode(nodeToBeInserted);
			tourCost += costMatrix.getCost(depotNode, nodeToBeInserted);
			Node lastNode = nodeToBeInserted;
			for(int i=1;i<tour.getNodes().size()-1;i++){
				Node nextNode = tour.getNodes().get(i);
				tourBuilder.addNode(nextNode);
				tourCost += costMatrix.getCost(lastNode, nextNode);
				lastNode = nextNode;
			}
			tourBuilder.setEndNode(tour.getLast());
			tourCost += costMatrix.getCost(lastNode, tour.getLast());
			tourBuilder.setTourCost(tourCost);
			newTour = tourBuilder.build();
			nodeInserted = true;
		}
		else{
			throw new RuntimeException("either tour not set or node already inserted");
		}
	}
	
	public VehicleTourImpl createTourMerge(VehicleTour tour1, VehicleTour tour2){
		double costOfNewTour = 0.0;
		Node depotNode = tour1.getNodes().get(0);
		TourBuilder tourBuilder = new TourBuilder();
		tourBuilder.setStartNode(depotNode);
		Node lastNode = depotNode;
		for(int i=1;i<tour1.getNodes().size()-1;i++){
			Node nextNode = tour1.getNodes().get(i);
			costOfNewTour += costMatrix.getCost(lastNode, nextNode);
			tourBuilder.addNode(nextNode);
			lastNode = nextNode;
		}
		for(int i=1;i<tour2.getNodes().size()-1;i++){
			Node nextNode = tour2.getNodes().get(i);
			costOfNewTour += costMatrix.getCost(lastNode, nextNode);
			tourBuilder.addNode(nextNode);
			lastNode = nextNode;
		}
		costOfNewTour += costMatrix.getCost(lastNode, depotNode);
		tourBuilder.setEndNode(depotNode);
		tourBuilder.setTourCost(costOfNewTour);
		return tourBuilder.build();
	}
	
	public VehicleTourImpl createRoundTour(Node depot, Node customer1, Node customer2){
		double costOfTour = costMatrix.getCost(depot, customer1) + 
			costMatrix.getCost(customer1, customer2) + costMatrix.getCost(customer2,depot);
		TourBuilder tourBuilder = new TourBuilder();
		tourBuilder.setStartNode(depot);
		tourBuilder.addNode(customer1);
		tourBuilder.addNode(customer2);
		tourBuilder.setEndNode(depot);
		tourBuilder.setTourCost(costOfTour);
		VehicleTourImpl tour = tourBuilder.build();
		return tour;
	}
	
	public VehicleTourImpl createShuttleTour(Node depot, Node customer){
		double costOfTour = costMatrix.getCost(depot, customer) + 
			costMatrix.getCost(customer, depot);
		TourBuilder tourBuilder = new TourBuilder();
		tourBuilder.setStartNode(depot);
		tourBuilder.addNode(customer);
		tourBuilder.setEndNode(depot);
		tourBuilder.setTourCost(costOfTour);
		return tourBuilder.build();
	}
	
	public VehicleTourImpl build(){
		if((tourSet && nodeInserted) || tourMerged){
			return newTour;
		}
		throw new RuntimeException("cannot build the tour");
	}

}
