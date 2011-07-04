package playground.gregor.multidestpeds.denistyestimation;

import org.matsim.core.api.experimental.events.EventsManager;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class DensityEstimatorFactory {

	private final EventsManager events;

	public DensityEstimatorFactory(EventsManager events) {
		this.events = events;
	}

	public NNGaussianKernelEstimator createDensityEstimator() {
		NNGaussianKernelEstimator ret = new NNGaussianKernelEstimator();
		ret.addGroupId("r");
		ret.addGroupId("g");
		ret.setResolution(0.1);

		Coordinate c1 = new Coordinate(386407,5819493);
		Coordinate c2 = new Coordinate(386430,5819513);
		ret.setEnvelope(new Envelope(c1,c2));

		ret.setLambda(1);
		ret.setMinDist(0.3);
		ret.setEventsManager(this.events);
		return ret;
	}
}
