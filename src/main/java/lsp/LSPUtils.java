package lsp;

import lsp.replanning.LSPReplanner;
import lsp.resources.Resource;
import lsp.scoring.LSPScorer;
import org.matsim.api.core.v01.Id;

import java.util.ArrayList;
import java.util.Collection;

public class LSPUtils{
	public static LSPPlan createLSPPlan(){
		return new LSPPlanImpl();
	}
	private LSPUtils(){} // do not instantiate
	public static class LSPBuilder{
		Id<LSP> id;
		SolutionScheduler solutionScheduler;
		LSPPlan initialPlan;
		Collection<Resource> resources;
		LSPScorer scorer;
		LSPReplanner replanner;



		public static LSPBuilder getInstance(){
			return new LSPBuilder();
		}

		private LSPBuilder(){
			this.resources = new ArrayList<Resource>();

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
}
