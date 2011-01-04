package playground.mmoyo.zz_archive.TransitSimulation;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.vis.otfvis.OTFVisMobsimFeature;
import org.matsim.core.controler.Controler;

public class MMoyoTransitControler extends Controler {
	boolean launchOTFDemo=false;
	private Config config;
	
	public MMoyoTransitControler(final ScenarioImpl scenario, boolean launchOTFDemo){
		super(scenario);
		this.config = scenario.getConfig();
		this.setOverwriteFiles(true);   
		this.launchOTFDemo = launchOTFDemo;
	}
	
	@Override
	protected void runMobSim() {
		QSim sim = new QSim(this.scenarioData, this.events);
		sim.addFeature(new OTFVisMobsimFeature(sim));
		sim.getTransitEngine().setUseUmlaeufe(true);
		sim.run();
		/*
		TransitQueueSimulation sim = new TransitQueueSimulation(this.scenarioData, this.events);
		sim.startOTFServer("livesim");
		new OnTheFlyClientQuad("rmi:127.0.0.1:4019:" + "livesim").start();
		sim.run();
		*/
	}

	@Override
	public PlanAlgorithm createRoutingAlgorithm(final PersonalizableTravelCost travelCosts, final PersonalizableTravelTime travelTimes) {
		return new MMoyoPlansCalcTransitRoute(this.config.plansCalcRoute(), this.network, travelCosts, travelTimes,
				this.getLeastCostPathCalculatorFactory(), this.scenarioData.getTransitSchedule(), new TransitConfigGroup());
	}
	
	public static void main(final String[] args) {
		if (args.length > 0) {
			ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(args[0]); //load from configFile
			ScenarioImpl scenario = scenarioLoader.getScenario();
			scenarioLoader.loadScenario();
			new MMoyoTransitControler(scenario, true).run();
		} 
	}
	
}
