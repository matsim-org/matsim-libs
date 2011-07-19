package playground.gregor.multidestpeds.densityestimation;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.gregor.sim2d_v2.simulation.floor.StaticEnvironmentDistancesField;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class DensityEstimatorFactory {

	private final EventsManager events;
	private final Scenario sc;
	private final double res;

	public DensityEstimatorFactory(EventsManager events, Scenario sc) {
		this(events,sc,1);
	}

	public DensityEstimatorFactory(EventsManager events, Scenario sc, double res) {
		this.events = events;
		this.sc = sc;
		this.res = res;
	}

	public NNGaussianKernelEstimator createDensityEstimator() {
		NNGaussianKernelEstimator ret = createBasicEstimator();
		Envelope e = getEnvelope();
		ret.setEnvelope(e);

		List<Coordinate> queryCoords = new ArrayList<Coordinate>();
		double x = e.getMinX() + this.res/2;
		for (; x < e.getMaxX(); x += this.res){
			double y = e.getMinY() + this.res/2;
			for (; y < e.getMaxY(); y+=this.res) {
				Coordinate c = new Coordinate(x,y);
				queryCoords.add(c);
			}
		}
		ret.setQueryCoordinates(queryCoords);
		return ret;
	}


	private NNGaussianKernelEstimator createBasicEstimator() {
		NNGaussianKernelEstimator ret = new NNGaussianKernelEstimator();
		ret.addGroupId("r");
		ret.addGroupId("g");
		ret.setResolution(this.res);


		ret.setLambda(1);
		ret.setMinDist(.300);
		ret.setEventsManager(this.events);

		StaticEnvironmentDistancesField sedf = this.sc.getScenarioElement(StaticEnvironmentDistancesField.class);
		ret.setStaticEnvironmentDistancesField(sedf);


		return ret;
	}

	private Envelope getEnvelope(){
		double maxX = Double.NEGATIVE_INFINITY;
		double minX = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		for (Node node : this.sc.getNetwork().getNodes().values()) {
			double x = node.getCoord().getX();
			double y = node.getCoord().getY();
			if (x > maxX) {
				maxX = x;
			}
			if (x < minX) {
				minX = x;
			}

			if (y > maxY) {
				maxY = y;
			}

			if (y < minY) {
				minY = y;
			}


		}

		Coordinate c1 = new Coordinate(minX,minY);
		Coordinate c2 = new Coordinate(maxX,maxY);
		Envelope e = new Envelope(c1,c2);
		return e;
	}
}
