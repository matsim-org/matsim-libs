package lsp;

import lsp.functions.LSPInfo;
import lsp.replanning.LSPReplanner;
import lsp.resources.LSPResource;
import lsp.scoring.LSPScorer;
import lsp.controler.LSPSimulationTracker;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.handler.EventHandler;

import java.util.ArrayList;
import java.util.Collection;

public class LSPUtils{
	public static LSPPlan createLSPPlan(){
		return new LSPPlanImpl();
	}
	public static SolutionScheduler createForwardSolutionScheduler(){
		return new ForwardSolutionSchedulerImpl();
	}
	public static WaitingShipments createWaitingShipments(){
		return new WaitingShipmentsImpl();
	}
	private LSPUtils(){} // do not instantiate
	public static class LSPBuilder{
		Id<LSP> id;
		SolutionScheduler solutionScheduler;
		LSPPlan initialPlan;
		Collection<LSPResource> resources;
		LSPScorer scorer;
		LSPReplanner replanner;



		public static LSPBuilder getInstance(){
			return new LSPBuilder();
		}

		private LSPBuilder(){
			this.resources = new ArrayList<LSPResource>();

		}

		public LSPBuilder setSolutionScheduler( SolutionScheduler solutionScheduler ){
			this.solutionScheduler = solutionScheduler;
			return this;
		}

		public LSPBuilder setSolutionScorer( LSPScorer scorer ){
			this.scorer = scorer;
			return this;
		}

		public LSPBuilder setReplanner( LSPReplanner replanner ){
			this.replanner= replanner;
			return this;
		}


		public LSPBuilder setInitialPlan( LSPPlan plan ){
			this.initialPlan = plan;
			for(LogisticsSolution solution : plan.getSolutions()) {
				for(LogisticsSolutionElement element : solution.getSolutionElements()) {
					if(!resources.contains(element.getResource())) {
						resources.add(element.getResource());
					}
				}
			}
			return this;
		}

		public LSPBuilder setId( Id<LSP> id ){
			this.id = id;
			return this;
		}

		public LSP build(){
			return new LSPImpl(this);
		}

	}

	public static class LogisticsSolutionBuilder{
		Id<LogisticsSolution> id;
		Collection<LogisticsSolutionElement> elements;
		Collection<LSPInfo> solutionInfos;
		Collection<EventHandler> eventHandlers;
		Collection<LSPSimulationTracker>trackers;

		public static LogisticsSolutionBuilder newInstance( Id<LogisticsSolution>id ){
			return new LogisticsSolutionBuilder(id);
		}

		private LogisticsSolutionBuilder( Id<LogisticsSolution> id ){
			this.elements = new ArrayList<LogisticsSolutionElement>();
			this.solutionInfos = new ArrayList<LSPInfo>();
			this.eventHandlers = new ArrayList<EventHandler>();
			this.trackers = new ArrayList<LSPSimulationTracker>();
			this.id = id;
		}

		public LogisticsSolutionBuilder addSolutionElement( LogisticsSolutionElement element ){
			elements.add(element);
			return this;
		}

		public LogisticsSolutionBuilder addInfo( LSPInfo info ) {
			solutionInfos.add(info);
			return this;
		}

		public LogisticsSolutionBuilder addEventHandler( EventHandler handler ) {
			eventHandlers.add(handler);
			return this;
		}

		public LogisticsSolutionBuilder addTracker( LSPSimulationTracker tracker ) {
			trackers.add(tracker);
			return this;
		}

		public LogisticsSolution build(){
			//linkSolutionElements(elements);
			return new LogisticsSolutionImpl(this);
		}

		/*private void linkSolutionElements(Collection<LogisticsSolutionElement> solutionElements){

			LogisticsSolutionElement previousElement = null;
			LogisticsSolutionElement currentElement = null;


			for(LogisticsSolutionElement element : solutionElements){
				if((previousElement == null) && (currentElement == null)){
					previousElement = element;
				}
				else{
					currentElement = element;
					previousElement.setNextElement(currentElement);
					currentElement.setPreviousElement(previousElement);
					previousElement = currentElement;
				}
			}
		}*/
	}

	public static class LogisticsSolutionElementBuilder{
		Id<LogisticsSolutionElement>id;
		LSPResource resource;
		WaitingShipments incomingShipments;
		WaitingShipments outgoingShipments;

		public static LogisticsSolutionElementBuilder newInstance( Id<LogisticsSolutionElement>id ){
			return new LogisticsSolutionElementBuilder(id);
		}

		private LogisticsSolutionElementBuilder( Id<LogisticsSolutionElement>id ){
			this.id = id;
			this.incomingShipments = createWaitingShipments();
			this.outgoingShipments = createWaitingShipments();
		}


		public LogisticsSolutionElementBuilder setResource( LSPResource resource ){
			this.resource = resource;
			return this;
		}

		public LogisticsSolutionElement build(){
			return new LogisticsSolutionElementImpl(this);
		}
	}
}
