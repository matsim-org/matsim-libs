package playground.gregor.sim2d_v2.calibration;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;

import playground.gregor.pedvis.PedVisPeekABot;
import playground.gregor.sim2d_v2.calibration.scenario.PhantomEvents;
import playground.gregor.sim2d_v2.calibration.scenario.PhantomPopulationLoader;
import playground.gregor.sim2d_v2.controller.Controller2D;
import playground.gregor.sim2d_v2.simulation.Sim2D;
import playground.gregor.sim2d_v2.simulation.Sim2DEngine;

public class CalibrationController2D extends Controller2D {


	private PedVisPeekABot vis;
	private PhantomEvents phantomEvents;
	private Validator validator;
	private final List<Double> allDiffs = new ArrayList<Double>();
	private final  List<Double>  allAi = new ArrayList<Double>();

	public CalibrationController2D(String[] args) {
		super(args);
	}

	@Override
	protected void loadData() {
		super.loadData();
		this.sim2dConfig.setEnableCircularAgentInterActionModule("false");
		this.sim2dConfig.setEnableCollisionPredictionAgentInteractionModule("true");
		this.sim2dConfig.setEnableCollisionPredictionEnvironmentForceModule("true");
		this.sim2dConfig.setEnableCollisionPredictionPhantomAgentIneractionModule("false");
		this.sim2dConfig.setEnableDrivingForceModule("true");
		this.sim2dConfig.setEnableEnvironmentForceModule("false");
		this.sim2dConfig.setEnablePathForceModule("true");
		this.sim2dConfig.setPhantomPopulationEventsFile("/Users/laemmel/devel/dfg/phantomEvents.xml.gz");
		this.sim2dConfig.setCalibrationMode("true");
		this.sim2dConfig.setAi(40);
		this.phantomEvents = new PhantomPopulationLoader(this.sim2dConfig.getPhantomPopulationEventsFile()).getPhantomPopulation();
		this.validator = new Validator();
		this.events.addHandler(this.validator);

		this.config.getQSimConfigGroup().setEndTime(32440);

		//		this.vis = new PedVisPeekABot(2);
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
		this.phantomEvents.setCalibrationAgentId(new IdImpl(0));
		this.validator.setCalibrationAgentEvents(this.phantomEvents.getCalibrationAgentEvents());
		this.validator.setSim2D(sim);
		this.validator.setCalibrationAgentId(new IdImpl(0));
		e.setPhantomPopulationEvents(this.phantomEvents);
		e.setCalibrationAgentId(new IdImpl(0));
		e.setValidator(this.validator);
		sim.setIterationNumber(getIterationNumber());

		sim.run();
		this.allDiffs.add(this.validator.getAndResetAllDiff());
		this.allAi .add(this.sim2dConfig.getAi());
		System.err.println("=================================================");
		for (int i = 0; i <this.allAi.size(); i++) {
			System.err.println("diff:" + this.allDiffs.get(i) + "  Ai:" + this.allAi.get(i));
		}
		System.err.println("=================================================");
		this.sim2dConfig.setAi(this.sim2dConfig.getAi()+1);
	}

	public static void main(String [] args) {
		Controler controller = new CalibrationController2D(args);
		controller.run();
	}

}
