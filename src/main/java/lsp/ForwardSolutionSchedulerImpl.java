package lsp;

import java.util.ArrayList;

import lsp.resources.LSPResource;
import lsp.shipment.LSPShipment;

/* package-private */ class ForwardSolutionSchedulerImpl implements SolutionScheduler {

	private class ResourceNeighbours{
		private ArrayList<LSPResource> predecessors;
		private ArrayList<LSPResource> successors;
		private LSPResource resource;
				
		private ResourceNeighbours(LSPResource resource) {
			this.resource = resource;
			this.predecessors = new ArrayList<LSPResource>();
			this.successors = new ArrayList<LSPResource>();
		}
		
		private void addPredecessor(LSPResource resource) {
			this.predecessors.add(resource);
		}
	
		private void addSuccessor(LSPResource resource) {
			this.successors.add(resource);
		}
	}
	
	private LSP lsp;
	private ArrayList<LSPResource> sortedResourceList;
	private ArrayList<ResourceNeighbours> neighbourList;
	private int bufferTime;
	
	ForwardSolutionSchedulerImpl() {
		this.sortedResourceList = new ArrayList<LSPResource>();
		this.neighbourList = new ArrayList<ResourceNeighbours>();
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
