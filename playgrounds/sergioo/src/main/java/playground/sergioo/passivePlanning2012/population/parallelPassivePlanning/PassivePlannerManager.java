package playground.sergioo.passivePlanning2012.population.parallelPassivePlanning;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacility;

import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.PlanningEngine;
import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.definitions.SinglePlannerAgent;
import playground.sergioo.passivePlanning2012.core.utils.misc.Counter;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PassivePlannerManager extends Thread implements BeforeMobsimListener, AfterMobsimListener {

	private static final Logger log = Logger.getLogger(PassivePlannerManager.class);
	private final Lock lock = new ReentrantLock();
	
	//Classes
	public static class MobsimStatus {
		private boolean mobsimEnds = false;

		public boolean isMobsimEnds() {
			return mobsimEnds;
		}
		private void setMobsimEnds() {
			mobsimEnds = true;
		}
	}
	private class PlanningInfo {

		//Attributes
		private final SinglePlannerAgent planner;
		private final Id<ActivityFacility> startFacilityId;
		private final Id<ActivityFacility> endFacilityId;
		private final double startTime;
		private final Id<Person> agentId;

		//Constructors
		public PlanningInfo(SinglePlannerAgent planner, Id<ActivityFacility> startFacilityId,
				Id<ActivityFacility> endFacilityId, double startTime, Id<Person> agentId) {
			super();
			this.planner = planner;
			this.startFacilityId = startFacilityId;
			this.endFacilityId = endFacilityId;
			this.startTime = startTime;
			this.agentId = agentId;
		}

	}
	private class ParallelPassivePlanners extends Thread {

		//Attributes
		private List<PlanningInfo> plannersInfo = new LinkedList<PlanningInfo>();
		private Counter counter;
		private final MobsimStatus mobSimStatus = new MobsimStatus();
		private final TripRouter tripRouter;
		private org.matsim.core.utils.misc.Counter counterTotal;
		private org.matsim.core.utils.misc.Counter counterBad;

		//Constructors
		public ParallelPassivePlanners(Counter counter, org.matsim.core.utils.misc.Counter counterTotal, org.matsim.core.utils.misc.Counter counterBad, TripRouter tripRouter) {
			this.counter = counter;
			this.counterTotal = counterTotal;
			this.counterBad = counterBad;
			this.tripRouter = tripRouter;
		}
		public int getNumPlanners() {
			return plannersInfo.size();
		}
		public void addPlanner(SinglePlannerAgent planner, Id<ActivityFacility> startFacilityId, Id<ActivityFacility> endFacilityId, double startTime, Id<Person> agentId) {
			planner.setRouter(tripRouter);
			while(!lock.tryLock());
			plannersInfo.add(new PlanningInfo(planner, startFacilityId, endFacilityId, startTime, agentId));
			lock.unlock();
			counter.incCounter();
		}
		//Methods
		public void setMobSimEnds() {
			mobSimStatus.setMobsimEnds();
		}
		@Override
		public void run() {
			AtomicInteger i=new AtomicInteger();
			while(true) {
				while(!lock.tryLock());
				boolean isEmpty = plannersInfo.isEmpty();
				lock.unlock();
				if(!isEmpty) {
					while(!lock.tryLock());
					int size = plannersInfo.size();
					lock.unlock();
					if(i.get()==size)
						i.set(0);
					while(!lock.tryLock());
					PlanningInfo plannerInfo = plannersInfo.get(i.getAndIncrement());
					lock.unlock();
					if(planningEngine.containsAgentId(plannerInfo.agentId)) {
						List<PlanElement> elements = plannerInfo.planner.getPlan().getPlanElements();
						int index = plannerInfo.planner.getPlanElementIndex(); 
						double endTime = ((Activity)elements.get(index-1)).getEndTime();
						endTime += ((Leg)elements.get(index)).getTravelTime();
						now.setNow(planningEngine.getTime());
						double nowCopy = now.getNow();
						if(nowCopy>=endTime) {
							while(!lock.tryLock());
							plannersInfo.remove(plannerInfo);
							lock.unlock();
							i.decrementAndGet();
							counter.decCounter();
							counterBad.incCounter();
							if(plannerInfo.startFacilityId.equals(plannerInfo.endFacilityId)) {
								//log.warn("Agent didn't finish planning when next activity started: "+plannerInfo.planner.getPlan().getPerson().getId()+" "+(nowCopy-plannerInfo.startTime)+" "+((Leg)elements.get(index)).getTravelTime());
								plannerInfo.planner.advanceToNextActivity(nowCopy, 0);
							}
							else {
								//log.warn("Agent didn't finish planning when next activity started: "+plannerInfo.planner.getPlan().getPerson().getId()+" "+(nowCopy-plannerInfo.startTime)+" "+((Leg)elements.get(index)).getTravelTime()+", teleporting...");
								plannerInfo.planner.advanceToNextActivity(nowCopy, 24*3600);
							}
						}
						else {
							int res = plannerInfo.planner.planLegActivityLeg(plannerInfo.startTime, now, plannerInfo.startFacilityId, endTime, plannerInfo.endFacilityId, mobSimStatus);
							if(res!=2) {
								while(!lock.tryLock());
								plannersInfo.remove(plannerInfo);
								lock.unlock();
								i.decrementAndGet();
								counter.decCounter();
								counterTotal.incCounter();
								if(res==1)
									plannerInfo.planner.advanceToNextActivity(nowCopy, 0);
							}
							else
								log.warn("Not successful planning: "+plannerInfo.planner.getPlan().getPerson().getId());
						}
					}
				}
				else
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				if(mobSimStatus.isMobsimEnds())
					return;
			}
		}

	}

	public static class CurrentTime {

		private double now;

		public CurrentTime() {
			now = 0;
		}
		public double getNow() {
			return now;
		}
		private void setNow(double now) {
			this.now = now;
		}

	}

	//Attributes
	private final ParallelPassivePlanners[] parallelPlanners;
	private int maxPlanners = Integer.MAX_VALUE;
	private Counter counter;
	private CurrentTime now = new CurrentTime();
	private PlanningEngine planningEngine;
	private org.matsim.core.utils.misc.Counter counterTotal;
	private org.matsim.core.utils.misc.Counter counterBad;

	//Methods
	public PassivePlannerManager(int numThreads) {
		if(numThreads<1)
			numThreads = 1;
		parallelPlanners = new ParallelPassivePlanners[numThreads];
		counter =  new Counter("[PassivePlanners] no handled people # ");
		counterTotal = new org.matsim.core.utils.misc.Counter("[PassivePlanners] handled person # ");
		counterBad = new org.matsim.core.utils.misc.Counter("[PassivePlanners] failed person # ");
		maxPlanners = numThreads*2;
	}
	public void setMaxPlanners(int maxPlanners) {
		this.maxPlanners = maxPlanners;
	}
	public void addPlanner(SinglePlannerAgent planner, Id<ActivityFacility> startFacilityId, Id<ActivityFacility> endFacilityId, double now, Id<Person> agentId) {
		while(counter.getCounter()>=maxPlanners);
		int less = Integer.MAX_VALUE;
		ParallelPassivePlanners lessPlanners = null;
		for(ParallelPassivePlanners planners:parallelPlanners)
			if(planners.getNumPlanners()<less) {
				less = planners.getNumPlanners();
				lessPlanners = planners;
			}
		lessPlanners.addPlanner(planner, startFacilityId, endFacilityId, now, agentId);
	}
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		log.info("Passive planners report:");
		counterTotal.printCounter();
		counterTotal.reset();
		counterBad.printCounter();
		counterBad.reset();
		for(ParallelPassivePlanners planners:parallelPlanners)
			planners.setMobSimEnds();
	}
	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		for(int i=0; i<parallelPlanners.length; i++) {
			parallelPlanners[i] = new ParallelPassivePlanners(counter, counterTotal, counterBad, event.getControler().getTripRouterProvider().get());
			parallelPlanners[i].setDaemon(true);
			parallelPlanners[i].start();
		}
	}
	public void setPlanningEngine(PlanningEngine planningEngine) {
		this.planningEngine = planningEngine;
	}

}
