package playground.gregor.sim2d_v2.calibration;


import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;

import playground.gregor.pedvis.PedVisPeekABot;
import playground.gregor.sim2d_v2.calibration.scenario.PhantomPopulationLoader;
import playground.gregor.sim2d_v2.controller.Controller2D;
import playground.gregor.sim2d_v2.simulation.Sim2D;
import playground.gregor.sim2d_v2.simulation.Sim2DEngine;

public class CalibrationController2D extends Controller2D {


	private Queue<Event> phantomPopulation;
	private PedVisPeekABot vis;

	public CalibrationController2D(String[] args) {
		super(args);
	}

	@Override
	protected void loadData() {
		super.loadData();
		this.sim2dConfig.setEnableCircularAgentInterActionModule("false");
		this.sim2dConfig.setEnableCollisionPredictionAgentInteractionModule("false");
		this.sim2dConfig.setEnableCollisionPredictionEnvironmentForceModule("true");
		this.sim2dConfig.setEnableCollisionPredictionPhantomAgentIneractionModule("true");
		this.sim2dConfig.setEnableDrivingForceModule("true");
		this.sim2dConfig.setEnableEnvironmentForceModule("false");
		this.sim2dConfig.setEnablePathForceModule("true");
		this.sim2dConfig.setPhantomPopulationEventsFile("/Users/laemmel/devel/dfg/phantomEvents.xml.gz");
		this.phantomPopulation = new PhantomPopulationLoader(this.sim2dConfig.getPhantomPopulationEventsFile()).getPhantomPopulation();


		//		this.vis = new PedVisPeekABot(1);
		//		Link l = this.network.getLinks().get(new IdImpl(0));
		//		this.vis.setOffsets(l.getCoord().getX(), l.getCoord().getY());
		//		this.vis.setFloorShapeFile(this.getSim2dConfig().getFloorShapeFile());
		//		this.vis.drawNetwork(this.network);
		//		this.events.addHandler(this.vis);
	}

	@Override
	protected void runMobSim() {
		Sim2D sim = new Sim2D(this.events, this.scenario2DData);
		Sim2DEngine e = sim.getSim2DEngine();
		e.enablePhantomPopulation(new ConcurrentLinkedQueue<Event>(this.phantomPopulation));
		sim.setIterationNumber(getIterationNumber());
		sim.run();
	}

	public static void main(String [] args) {
		Controler controller = new CalibrationController2D(args);
		controller.run();
	}

}
