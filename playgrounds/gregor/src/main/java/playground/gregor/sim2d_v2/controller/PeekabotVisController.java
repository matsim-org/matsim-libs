package playground.gregor.sim2d_v2.controller;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.population.algorithms.PlanAlgorithm;


import playground.gregor.multidestpeds.densityestimation.DensityEstimatorFactory;
import playground.gregor.multidestpeds.densityestimation.NNGaussianKernelEstimator;
import playground.gregor.pedvis.PedVisPeekABot;
import playground.gregor.sim2d_v2.helper.UTurnRemover;
import playground.gregor.sims.msa.MSATravelTimeCalculatorFactory;

public class PeekabotVisController extends Controller2D{

	private PedVisPeekABot vis;

	public PeekabotVisController(Scenario sc) {
		super(sc);
		setTravelTimeCalculatorFactory(new MSATravelTimeCalculatorFactory());
	}

	@Override
	protected void loadData() {
		super.loadData();
		this.vis = new PedVisPeekABot(10,this.scenarioData);
		//		this.vis.setOffsets(386128,5820182);
		this.vis.setOffsets(getNetwork());
		this.vis.setFloorShapeFile(this.getSim2dConfig().getFloorShapeFile());
		this.vis.drawNetwork(this.network);
		this.events.addHandler(this.vis);

		NNGaussianKernelEstimator est = new DensityEstimatorFactory(this.events,this.scenarioData).createDensityEstimator();
		this.events.addHandler(est);
		this.addControlerListener(this.vis);

		this.addControlerListener(new UTurnRemover(this.scenarioData));

	}

	public static void main(String[] args) {
		String configFile = args[0];
		Config c = ConfigUtils.loadConfig(configFile);
		c.addQSimConfigGroup(new QSimConfigGroup());
		c.getQSimConfigGroup().setEndTime( 9*3600 + 20* 60);

		Scenario sc = ScenarioUtils.createScenario(c);
		((NetworkFactoryImpl)sc.getNetwork().getFactory()).setRouteFactory("walk2d", new LinkNetworkRouteFactory());
		ScenarioUtils.loadScenario(sc);


		Controler controller = new PeekabotVisController(sc);
		controller.run();

	}

}
