package playground.gregor.sim2d.controller;


import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.Controler;

import playground.gregor.sim2d.peekabot.PeekABotClient;
import playground.gregor.sim2d.scenario.ScenarioLoader2DImpl;
import playground.gregor.sim2d.simulation.Sim2D;
import playground.gregor.sim2d.simulation.StaticForceField;

import com.vividsolutions.jts.geom.MultiPolygon;

public class Controller2D extends Controler {

	private Map<MultiPolygon,List<Link>> mps;


	private StaticForceField sff;


	private PeekABotClient peekABot;



	public Controller2D(String[] args) {
		super(args);
		this.setOverwriteFiles(true);
		this.peekABot = new PeekABotClient();


	}




	@Override
	protected void loadData() {
		if (!this.scenarioLoaded) {
			this.loader = new ScenarioLoader2DImpl(this.scenarioData);
			this.loader.loadScenario();
			this.mps = ((ScenarioLoader2DImpl)this.loader).getFloorLinkMapping();
			this.sff = ((ScenarioLoader2DImpl)this.loader).getStaticForceField();
			this.network = this.loader.getScenario().getNetwork();
			this.population = this.loader.getScenario().getPopulation();
			this.scenarioLoaded = true;
		}
	}

	@Override
	protected void runMobSim() {
		
		Sim2D sim = new Sim2D(this.network,this.mps,this.population,this.events,this.sff, this.config);
		sim.addPeekABotClient(this.peekABot, this.getIterationNumber() == 0);
		sim.run();
	}

	public static void main(String [] args){
		Controler controller = new Controller2D(args);
		controller.run();

	}

}
