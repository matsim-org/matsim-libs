package lsp;

import java.util.ArrayList;

import lsp.shipment.LSPShipment;

/**
 * ....
 * Macht 3 Schritte:
 * 1.) the LSPShipments are handed over to the first {@link LogisticsSolutionElement} of their {@link LogisticsSolution}
 * 2.) the neighbors, i.e. the predecessors and successors of all {@link LSPResource}s are determined
 * 3.) the Resources are brought into the right sequence according to the algorithm.
 *
 * When traversing this list of {@link LSPResource}s, the operations in
 * each {@link LSPResource} are scheduled individually by calling their {@link LSPResourceScheduler}.
 */
/* package-private */ class ForwardSolutionSchedulerImpl implements SolutionScheduler {

	/**
	 *  The relationship between different {@link LSPResource}s allows to handle various supply
	 *  structures that the {@link LSP} might decide to maintain. Thus, a {@link LSPResource} can have several
	 *  successors or predecessors or can be used by several different {@link LogisticsSolution}s.
	 *  The neighborhood structure among the {@link LSPResource}s is stored in instances of the class
	 *  {@link ResourceNeighbours} which contain references on the considered {@link LSPResource}  and on the set
	 *  of immediate successors respective predecessors. As the result of this step, a collection of
	 *  {@link ResourceNeighbours} called neighborList is created that contains the neighbors of all
	 *  {@link LSPResource}s in the plan of the considered {@link LSP}.
	 */
	private static class ResourceNeighbours{
		// internal data structure, try to ignore when looking from outside.  kai/kai, jan'22

		private final ArrayList<LSPResource> predecessors;
		private final ArrayList<LSPResource> successors;
		private final LSPResource resource;
				
		private ResourceNeighbours(LSPResource resource) {
			this.resource = resource;
			this.predecessors = new ArrayList<>();
			this.successors = new ArrayList<>();
		}
		
		private void addPredecessor(LSPResource resource) {
			this.predecessors.add(resource);
		}
	
		private void addSuccessor(LSPResource resource) {
			this.successors.add(resource);
		}
	}
	
	private LSP lsp;

	/**
	 * The Resources are brought into the right sequence according to the algorithm.
	 * The result of this algorithm is a list of Resources that is later
	 * traversed from the front to the back, i.e. starting with the entry at index 0. In the algorithm,
	 * this list is called sortedResourceList.
	 */
	private final ArrayList<LSPResource> sortedResourceList;
	/**
	 * The determination of the neighborhood  structure among the Resources resulted in the neighborList.
	 */
	private final ArrayList<ResourceNeighbours> neighbourList;
	private int bufferTime;
	
	ForwardSolutionSchedulerImpl() {
		this.sortedResourceList = new ArrayList<>();
		this.neighbourList = new ArrayList<>();
	}
	
	
	@Override
	public void scheduleSolutions() {
		insertShipmentsAtBeginning();
		setResourceNeighbours();
		sortResources();
		for(LSPResource resource : sortedResourceList ) {
			resource.schedule(bufferTime);
		}

	}

	@Override
	public void setLSP(LSP lsp) {
		this.lsp = lsp;
	}

	private void setResourceNeighbours() {
		// internal data structure, try to ignore when looking from outside.  kai/kai, jan'22
		neighbourList.clear();
		for(LSPResource resource : lsp.getResources()) {
			ResourceNeighbours neighbours = new ResourceNeighbours(resource);
			for(LogisticsSolutionElement element : resource.getClientElements()) {
				LogisticsSolutionElement predecessor = element.getPreviousElement();
				LSPResource previousResource = predecessor.getResource();
				neighbours.addPredecessor(previousResource);
				LogisticsSolutionElement successor = element.getNextElement();
				LSPResource nextResource = successor.getResource();
				neighbours.addSuccessor(nextResource);
			}
		neighbourList.add(neighbours);
		}	
	}
	
	private void sortResources() {
		sortedResourceList.clear();
		while(!neighbourList.isEmpty()) {
			for(ResourceNeighbours neighbours : neighbourList) {
				if(allPredecessorsAlreadyScheduled(neighbours) && noSuccessorAlreadyScheduled(neighbours)) {
					sortedResourceList.add(neighbours.resource);
					neighbourList.remove(neighbours);
				}
			}
		}
	}

	private boolean allPredecessorsAlreadyScheduled(ResourceNeighbours neighbours) {
		if(neighbours.predecessors.isEmpty()) {
			return true;
		}
			
		for(LSPResource predecessor : neighbours.predecessors) {
			if(!sortedResourceList.contains(predecessor)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean noSuccessorAlreadyScheduled(ResourceNeighbours neighbours) {
		if(neighbours.successors.isEmpty()) {
			return true;
		}
		
		for(LSPResource successor : neighbours.successors) {
			if(! sortedResourceList.contains(successor)) {
				return true;
			}
		}
		return false;
	}

	private void insertShipmentsAtBeginning() {
		for(LogisticsSolution solution : lsp.getSelectedPlan().getSolutions()) {
			LogisticsSolutionElement firstElement = getFirstElement(solution);
			for(LSPShipment shipment : solution.getShipments() ) {
				firstElement.getIncomingShipments().addShipment(shipment.getPickupTimeWindow().getStart(), shipment );
			}	
		}
	}

	private LogisticsSolutionElement getFirstElement(LogisticsSolution solution){
		for(LogisticsSolutionElement element : solution.getSolutionElements()){
			if(element.getPreviousElement() == null){
				return element;
			}
			
		}
		return null;
	}


	@Override
	public void setBufferTime(int bufferTime) {
		this.bufferTime = bufferTime;
	}
}
