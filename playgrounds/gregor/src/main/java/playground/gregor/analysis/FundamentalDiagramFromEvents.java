package playground.gregor.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.XYLineChart;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

import playground.gregor.multidestpeds.densityestimation.DensityEstimatorFactory;
import playground.gregor.multidestpeds.densityestimation.NNGaussianKernelEstimator;
import playground.gregor.sim2d_v2.events.DoubleValueStringKeyAtCoordinateEvent;
import playground.gregor.sim2d_v2.events.DoubleValueStringKeyAtCoordinateEventHandler;
import playground.gregor.sim2d_v2.events.XYVxVyEvent;
import playground.gregor.sim2d_v2.events.XYVxVyEventsFileReader;
import playground.gregor.sim2d_v2.events.XYVxVyEventsHandler;
import playground.gregor.sim2d_v2.scenario.ScenarioLoader2DImpl;

public class FundamentalDiagramFromEvents implements XYVxVyEventsHandler, DoubleValueStringKeyAtCoordinateEventHandler{

	private double eventTime = 0;
	private double densEventTime = 0;
	private final Envelope e;

	private final List<Double> velo = new ArrayList<Double>();
	private final List<Double> dens = new ArrayList<Double>();

	private final Map<Double,Double> timeVelo = new TreeMap<Double,Double>();
	private final Map<Double,Double> timeDens = new TreeMap<Double,Double>();

	public FundamentalDiagramFromEvents(Envelope e) {
		this.e = e;
	}

	@Override
	public void handleEvent(XYVxVyEvent event) {
		if (event.getTime() > this.eventTime) {
			double denom = this.velo.size();
			double v = 0;
			for (double d : this.velo){
				v += d/denom;
			}
			this.timeVelo.put(this.eventTime, v);
			this.velo.clear();
			this.eventTime = event.getTime();
		}

		if (this.e.contains(event.getCoordinate())){
			double v = Math.sqrt(event.getVX()*event.getVX()+event.getVY()*event.getVY());
			this.velo.add(v);
		}
	}

	@Override
	public void handleEvent(DoubleValueStringKeyAtCoordinateEvent e) {
		if (e.getTime() > this.densEventTime) {

			double denom = this.dens.size();
			double v = 0;
			for (double d : this.dens){
				v += d/denom;
			}
			this.timeDens.put(v,this.densEventTime);
			this.dens.clear();
			this.densEventTime = e.getTime();
		}
		if (this.e.contains(e.getCoordinate())){
			this.dens.add(e.getValue());
		}
	}


	public void saveAsPng(String dir) {
		XYLineChart chart = new XYLineChart("Fundamental diagram", "density p/m^2", "velocity in m/s");
		double [] xs = new double[this.timeDens.size()];
		double [] ys = new double[this.timeDens.size()];
		int pos = 0;
		for (Entry<Double, Double> dens : this.timeDens.entrySet()) {
			double velo = this.timeVelo.get(dens.getValue());
			double d = dens.getKey();
			xs[pos] = d;
			ys[pos++] = velo;
		}

		chart.addSeries("model", xs, ys);
		chart.saveAsPng(dir + "/fnd.png", 800, 400);
	}

	@Override
	public String toString() {

		StringBuffer sb = new StringBuffer();

		for (Entry<Double, Double> dens : this.timeDens.entrySet()) {
			double velo = this.timeVelo.get(dens.getValue());
			sb.append(dens.getKey());
			sb.append(' ');
			sb.append(velo);
			sb.append('\n');
		}

		return sb.toString();
	}

	public static void main(String [] args) {
		String eventsFile = "/Users/laemmel/devel/counter/output/ITERS/it.0/0.events.xml.gz";
		String config = "/Users/laemmel/devel/counter/config.xml";

		Config c = ConfigUtils.loadConfig(config);
		Scenario sc = ScenarioUtils.loadScenario(c);
		new ScenarioLoader2DImpl(sc).load2DScenario();

		EventsManager em = EventsUtils.createEventsManager();
		XYVxVyEventsFileReader r = new XYVxVyEventsFileReader(em);

		Envelope e = new Envelope(6,8,1,3);

		List<Coordinate> l = new ArrayList<Coordinate>();
		for (double x = e.getMinX(); x <= e.getMaxX(); x += .125 ) {
			for (double y = e.getMinY(); y <= e.getMaxY(); y += .125) {
				l.add(new Coordinate(x,y));
			}
		}

		NNGaussianKernelEstimator est = new DensityEstimatorFactory(em, sc).createDensityEstimator(l);
		em.addHandler(est);

		FundamentalDiagramFromEvents fundi = new FundamentalDiagramFromEvents(e);
		em.addHandler(fundi);

		r.parse(eventsFile);

		fundi.saveAsPng(c.controler().getOutputDirectory());
		//		System.out.println(fundi);
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}




}
