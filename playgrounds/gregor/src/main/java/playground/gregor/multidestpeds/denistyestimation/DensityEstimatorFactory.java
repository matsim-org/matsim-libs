package playground.gregor.multidestpeds.denistyestimation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.scenario.MyDataContainer;
import playground.gregor.sim2d_v2.simulation.floor.StaticEnvironmentDistancesField;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class DensityEstimatorFactory {

	private final EventsManager events;
	private final Scenario sc;

	public DensityEstimatorFactory(EventsManager events, Scenario sc) {
		this.events = events;
		this.sc = sc;
	}

	public NNGaussianKernelEstimator createDensityEstimator() {
		NNGaussianKernelEstimator ret = new NNGaussianKernelEstimator();
		ret.addGroupId("r");
		ret.addGroupId("g");
		ret.setResolution(.5);

		Coordinate c1 = new Coordinate(386407,5819493);
		Coordinate c2 = new Coordinate(386430,5819513);
		ret.setEnvelope(new Envelope(c1,c2));

		ret.setLambda(1);
		ret.setMinDist(0.3);
		ret.setEventsManager(this.events);

		ret.setStaticEnvironmentDistancesField(this.sc.getScenarioElement(StaticEnvironmentDistancesField.class));
		return ret;
	}
}
