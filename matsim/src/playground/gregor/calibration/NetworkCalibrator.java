package playground.gregor.calibration;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.api.population.PopulationBuilder;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.queuesim.SimulationTimer;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationBeforeCleanupEvent;
import org.matsim.core.mobsim.queuesim.listener.QueueSimulationBeforeCleanupListener;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class NetworkCalibrator {
	
	private final static double WIDTH = 10.;
	private final static double PERS_WIDTH = 0.71;
	private final static double CAP = 10. * 0.5;
	private final static double FREESPEED = 0.5;
	private final static double CELLSIZE = 0.26;
	private final static double PERSONS = 20000;
	private final static double CRITERION = 36*60 +26;
	private Link l1;
	
	private void run() {
		
		Scenario sc = new ScenarioImpl();
		createScenario(sc,FREESPEED);
		createConfig(sc);
		
		Network net = sc.getNetwork();
		
		Optimizer op = new Optimizer(net,this.l1);
		System.out.println("Num of its:" + op.getLastIt());
		sc.getConfig().controler().setLastIteration(op.getLastIt());
				
		
		Controler c = new Controler(sc);
		c.getQueueSimulationListener().add(op);
		c.setOverwriteFiles(true);
		c.run();
	}
	

	private void createConfig(Scenario sc) {
		Config c = sc.getConfig();
		c.global().setCoordinateSystem(TransformationFactory.ATLANTIS);
		c.simulation().setStartTime(0);
		c.simulation().setSnapshotPeriod(60);
		c.simulation().setSnapshotFormat("otfvis");
		c.controler().setOutputDirectory("../../outputs/calibration");
		c.charyparNagelScoring().addParam("activityType_0", "h");
		c.charyparNagelScoring().addParam("activityTypicalDuration_0", "3600");
//		c.charyparNagelScoring().addParam("activityMinimalDuration_0", "12.");
		c.charyparNagelScoring().addParam("activityPriority_0", "1.");
		c.charyparNagelScoring().addParam("activityType_1", "w");
		c.charyparNagelScoring().addParam("activityTypicalDuration_1", "3600");
//		c.charyparNagelScoring().setBrainExpBeta(10);
//		c.charyparNagelScoring().setEarlyDeparture(0);
//		c.charyparNagelScoring().setLateArrival(0);
//		c.charyparNagelScoring().setTraveling(-6);
//		c.charyparNagelScoring().setWaiting(-6);
//		c.charyparNagelScoring().setPerforming(0);
		c.strategy().addParam("ModuleProbability_1", "1.");
		c.strategy().addParam("Module_1", "ReRoute");
		c.strategy().setMaxAgentPlanMemorySize(1);
		
	}

	private void createScenario(Scenario sc, double fs) {
		NetworkLayer net = (NetworkLayer)sc.getNetwork();
		net.setCapacityPeriod(1);
		net.setEffectiveCellSize(CELLSIZE);
		net.setEffectiveLaneWidth(0.71);
		Node n0 = net.createNode(new IdImpl(0),new CoordImpl(0,0));
		Node n1 = net.createNode(new IdImpl(1),new CoordImpl(50,0));
		Node n2 = net.createNode(new IdImpl(2),new CoordImpl(150,0));
		Node n3 = net.createNode(new IdImpl(3),new CoordImpl(200,0));
		Link l0 = net.createLink(new IdImpl(0), n0, n1, 50, fs, 20000,PERSONS/(50/CELLSIZE));
		this.l1 = net.createLink(new IdImpl(1), n1, n2, 100, fs, CAP, WIDTH/PERS_WIDTH);
		Link l2 = net.createLink(new IdImpl(3), n2, n3, 50, fs, 20000, PERSONS/(50/CELLSIZE));
		
		Population pop = sc.getPopulation();
		PopulationBuilder pb = pop.getPopulationBuilder();
		for (int i = 0; i < PERSONS; i++) {
			Person p = pb.createPerson(new IdImpl(i));
			Plan plan = pb.createPlan(p);
			p.addPlan(plan);
			Activity a0 = pb.createActivityFromLinkId("h", l0.getId());
			a0.setEndTime(0);
			plan.addActivity(a0);
			Leg l = pb.createLeg(TransportMode.car);
			plan.addLeg(l);
			Activity a1 = pb.createActivityFromLinkId("w", l2.getId());
			plan.addActivity(a1);
			pop.addPerson(p);
		}
		
	}

	private static class Optimizer implements QueueSimulationBeforeCleanupListener {

		Logger log = Logger.getLogger(Optimizer.class);
		private ArrayList<Param> params;
		int it = 0;
		private final NetworkLayer net;
		private Link link;
		private int lastIt;
		public Optimizer(Network net, Link link) {
			this.net = (NetworkLayer) net;
			this.link = link;
			init();
		}
		
		public int getLastIt() {
			return this.lastIt;
		}
		
		private void init() {
			this.lastIt = -1;
			this.params = new ArrayList<Param>();
			for (double fs = 0.5; fs <= 2.; fs += 0.1) {
				for (double fc = 0.5; fc <= 2; fc += 0.1) {
					for (double cs = .26; cs <= .26; cs += 0.1) {
						Param p = new Param();
						p.cs = cs;
						p.fc = fc;
						p.fs = fs;
						this.params.add(p);
						this.lastIt++;
					}
				}
			}
			
		}

		public void notifySimulationBeforeCleanup(
				QueueSimulationBeforeCleanupEvent e) {
			double time = SimulationTimer.getTime();
			this.log.info("IT:" + this.it + "TimeDiff: " + (CRITERION - time) + this.params.get(this.it));
			this.it++;
			if (this.it >= this.params.size()) {
				return;
			}
			Param p = this.params.get(this.it);
			this.net.setEffectiveCellSize(p.cs);
			LinkImpl l = (LinkImpl) this.link;
			this.net.removeLink(this.link);
			this.link = this.net.createLink(l.getId(), l.getFromNode(), l.getToNode(), 100, p.fs, p.fc*WIDTH, WIDTH/PERS_WIDTH);
			
		}
		
	}
	
	private static class Param {
		double fs;
		double fc;
		double cs;
		
		@Override
		public String toString() {
			return " fs: " + this.fs + " fc: " + this.fc + " cs: " + this.cs;
		}
	}
	
	public static void main(String args []) {
		
		
		

		
		new NetworkCalibrator().run();
		
		
	}



}
