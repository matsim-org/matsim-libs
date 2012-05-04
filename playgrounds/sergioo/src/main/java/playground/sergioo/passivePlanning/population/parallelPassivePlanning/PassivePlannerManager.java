package playground.sergioo.passivePlanning.population.parallelPassivePlanning;

import java.util.Collection;
import java.util.LinkedList;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import playground.sergioo.passivePlanning.core.mobsim.passivePlanning.definitions.SinglePlannerAgent;
import playground.sergioo.passivePlanning.core.utils.misc.Counter;

public class PassivePlannerManager extends Thread implements IterationStartsListener, IterationEndsListener {

	//Static Classes
	private static class ParallelPassivePlanners extends Thread {

		//Attributes
		private final Collection<SinglePlannerAgent> planners = new LinkedList<SinglePlannerAgent>();
		private Counter counter;
		private boolean isRunning;
		
		//Constructors
		public ParallelPassivePlanners(Counter counter) {
			this.counter = counter;
		}
		public int getNumPlanners() {
			return planners.size();
		}
		public void addPlanner(SinglePlannerAgent planner) {
			planners.add(planner);
			counter.incCounter();
		}
		public boolean isRunning() {
			return isRunning;
		}
		//Methods
		@Override
		public void run() {
			isRunning=true;
			while(planners.size()>0) {
				SinglePlannerAgent planner = planners.iterator().next();
				if(planner.isPlanned() || planner.planLegActivity()) {
					planners.remove(planner);
					counter.decCounter();
				}
				//TODO else
 			}
			isRunning=false;
		}

	}
	
	//Attributes
	private final ParallelPassivePlanners[] parallelPlanners;
	private int maxPlanners = Integer.MAX_VALUE;
	private Counter counter;
	private boolean continuePlanning = false;
	
	//Methods
	public PassivePlannerManager(int numThreads) {
		if(numThreads<1)
			numThreads = 1;
		parallelPlanners = new ParallelPassivePlanners[numThreads];
		counter =  new Counter("[PassicePlanners] handled person # ");
		for(int i=0; i<parallelPlanners.length; i++) {
			parallelPlanners[i] = new ParallelPassivePlanners(counter);
		}
	}
	public void setMaxPlanners(int maxPlanners) {
		this.maxPlanners = maxPlanners;
	}
	public boolean addPlanner(SinglePlannerAgent planner) {
		if(counter.getCounter()>=maxPlanners)
			return false;
		else {
			int less = Integer.MAX_VALUE;
			ParallelPassivePlanners lessPlanners = null;
			for(ParallelPassivePlanners planners:parallelPlanners)
				if(planners.getNumPlanners()<less) {
					less = planners.getNumPlanners();
					lessPlanners = planners;
				}
			lessPlanners.addPlanner(planner);
			return true;
		}
	}
	@Override
	public void run() {
		while(continuePlanning) {
			for(ParallelPassivePlanners planners:parallelPlanners)
				if(!planners.isRunning() && planners.getNumPlanners()>0)
					planners.start();
		}
	}
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		continuePlanning = true;
		start();
	}
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		continuePlanning = false;
	}

}
