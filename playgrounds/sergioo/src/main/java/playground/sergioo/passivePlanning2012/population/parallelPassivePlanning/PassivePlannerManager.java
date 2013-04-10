package playground.sergioo.passivePlanning2012.population.parallelPassivePlanning;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;

import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.definitions.SinglePlannerAgent;
import playground.sergioo.passivePlanning2012.core.utils.misc.Counter;

public class PassivePlannerManager extends Thread implements BeforeMobsimListener, AfterMobsimListener {

	//Classes
	private class ParallelPassivePlanners extends Thread {

		//Classes
		private class PlanningInfo {
		
			//Attributes
			private SinglePlannerAgent planner;
			private Id startFacilityId;
			private Id endFacilityId;
		
			//Constructors
			public PlanningInfo(SinglePlannerAgent planner, Id startFacilityId,
					Id endFacilityId) {
				super();
				this.planner = planner;
				this.startFacilityId = startFacilityId;
				this.endFacilityId = endFacilityId;
			}
		
		}
		
		//Attributes
		private final List<PlanningInfo> planners = new LinkedList<PlanningInfo>();
		private Counter counter;
		private boolean mobSimEnds = false;
		
		//Constructors
		public ParallelPassivePlanners(Counter counter) {
			this.counter = counter;
		}
		public int getNumPlanners() {
			return planners.size();
		}
		public void addPlanner(SinglePlannerAgent planner, Id startFacilityId, Id endFacilityId) {
			planners.add(new PlanningInfo(planner, startFacilityId, endFacilityId));
			counter.incCounter();
		}
		//Methods
		public void setMobSimEnds() {
			mobSimEnds = true;
		}
		@Override
		public void run() {
			AtomicInteger i=new AtomicInteger();
			while(true) {
				if(!planners.isEmpty()) {
					if(i.get()==planners.size())
						i.set(0);
					PlanningInfo plannerInfo = planners.get(i.getAndIncrement());
					double endTime = ((Activity)plannerInfo.planner.getPlan().getPlanElements().get(plannerInfo.planner.getPlanElementIndex()-1)).getEndTime();
					endTime += ((Leg)plannerInfo.planner.getPlan().getPlanElements().get(plannerInfo.planner.getPlanElementIndex())).getTravelTime();
					double nowCopy=now;
					if(nowCopy>endTime || plannerInfo.planner.planLegActivityLeg(nowCopy, plannerInfo.startFacilityId, endTime, plannerInfo.endFacilityId)) {
						planners.remove(plannerInfo);
						i.decrementAndGet();
						counter.decCounter();
					}
					if(nowCopy>endTime)
						plannerInfo.planner.advanceToNextActivity(nowCopy);
				} else
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				if(mobSimEnds)
					return;
 			}
		}

	}
	
	//Attributes
	private final ParallelPassivePlanners[] parallelPlanners;
	private int maxPlanners = Integer.MAX_VALUE;
	private Counter counter;
	private double now;
	
	//Methods
	public PassivePlannerManager(int numThreads) {
		if(numThreads<1)
			numThreads = 1;
		parallelPlanners = new ParallelPassivePlanners[numThreads];
		counter =  new Counter("[PassivePlanners] handled person # ");
	}
	public void setMaxPlanners(int maxPlanners) {
		this.maxPlanners = maxPlanners;
	}
	public boolean addPlanner(SinglePlannerAgent planner, Id startFacilityId, Id endFacilityId) {
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
			lessPlanners.addPlanner(planner, startFacilityId, endFacilityId);
			return true;
		}
	}
	public void setTime(double time) {
		now = time;
	}
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		for(ParallelPassivePlanners planners:parallelPlanners)
			planners.setMobSimEnds();
	}
	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		for(int i=0; i<parallelPlanners.length; i++) {
			parallelPlanners[i] = new ParallelPassivePlanners(counter);
			parallelPlanners[i].setDaemon(true);
			parallelPlanners[i].start();
		}
	}

}
