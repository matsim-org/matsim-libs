package playground.wdoering.analysis;

import java.util.ArrayList;
import java.util.Collections;
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
import org.matsim.core.utils.collections.Tuple;

import playground.wdoering.oldstufffromgregor.DensityEstimatorFactory;
import playground.wdoering.oldstufffromgregor.DoubleValueStringKeyAtCoordinateEvent;
import playground.wdoering.oldstufffromgregor.DoubleValueStringKeyAtCoordinateEventHandler;
import playground.wdoering.oldstufffromgregor.NNGaussianKernelEstimator;
import playground.wdoering.oldstufffromgregor.ScenarioLoader2DImpl;
import playground.wdoering.oldstufffromgregor.XYVxVyEvent;
import playground.wdoering.oldstufffromgregor.XYVxVyEventsFileReader;
import playground.wdoering.oldstufffromgregor.XYVxVyEventsHandler;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class VelocityFundamentalDiagramFromEvents implements XYVxVyEventsHandler, DoubleValueStringKeyAtCoordinateEventHandler{

	private double eventTime = 0;
	private double densEventTime = 0;
	private final Envelope e;

	private final List<Double> velo = new ArrayList<Double>();
	private final List<Double> dens = new ArrayList<Double>();

	private final Map<Double,Double> timeVelo = new TreeMap<Double,Double>();
	private final TreeMap<Double,Double> timeDens = new TreeMap<Double,Double>();


	public VelocityFundamentalDiagramFromEvents(Envelope e) {
		this.e = e;
	}

	@Override
	public void handleEvent(XYVxVyEvent event) {
		if (event.getTime() > this.eventTime) {
			//			double denom = this.velo.size();
			//
			//			double v = 0;
			//			for (double d : this.velo){
			//				v += d;
			//			}
			if (this.velo.size() > 0) {
				Collections.sort(this.velo);
				this.timeVelo.put(this.eventTime, this.velo.get(this.velo.size()/2));
			} else {
				this.timeVelo.put(this.eventTime, 0.);
			}
			this.velo.clear();
			this.eventTime = event.getTime();
		}

		if (this.e.contains(event.getCoordinate())){
			double v = Math.sqrt(event.getVX()*event.getVX()+event.getVY()*event.getVY());
//			v *= event.getVX() < 0 ? -1 : 1; //pay attention to negative velocities
			this.velo.add(v);
		}
	}

	@Override
	public void handleEvent(DoubleValueStringKeyAtCoordinateEvent e) {
		if (e.getTime() > this.densEventTime) {

			//			double denom = this.dens.size();
			//			double v = 0;
			//			for (double d : this.dens){
			//				v += d;
			//			}
			Collections.sort(this.dens);
			this.timeDens.put(this.dens.get(this.dens.size()/2),this.densEventTime);
			//			if (v/denom > this.maxDens) {
			//				this.maxDens = v/denom;
			//			}
			this.dens.clear();
			this.densEventTime = e.getTime();
		}
		if (this.e.contains(e.getCoordinate())){
			this.dens.add(e.getValue());
		}
	}


	public void saveAsPng(String dir) {
		List<Tuple<Double, Double>> l = smoothData();
		XYLineChart chart = new XYLineChart("Fundamental diagram", "density p/m^2", "velocity in m/s");
		double [] xs = new double[l.size()];
		double [] ys = new double[l.size()];
		int pos = 0;
		for (Tuple<Double, Double> t : l) {

			xs[pos] = t.getFirst();
			ys[pos++] = t.getSecond();
		}

		chart.addSeries("Bottleneck Experiment (Social Force Model; w=1; p=49; waw=7)", xs, ys);
		chart.saveAsPng(dir + "/fnd.png", 800, 400);
	}

	private List<Tuple<Double, Double>> smoothData() {
		double nextK = 0.1;
		List<Double> v = new ArrayList<Double>();
		List<Double> rho = new ArrayList<Double>();
		List<Tuple<Double,Double>> ret = new ArrayList<Tuple<Double,Double>>();
		for (Entry<Double, Double> dens : this.timeDens.entrySet()) {
			double velo = this.timeVelo.get(dens.getValue());
			double d = dens.getKey();
			if (d < nextK) {
				rho.add(d);
				v.add(velo);
			} else{
				//				v.add(1.34);
				//				v.add(1.34);
				Collections.sort(rho);
				Collections.sort(v);
				Tuple<Double, Double> t = new Tuple<Double,Double>(rho.get(rho.size()/2),v.get(v.size()/2));
				ret.add(t);
				nextK += 0.1;
				rho.clear();
				v.clear();
				//				rho.add(d);
				//				v.add(velo);
			}
		}
		return ret;
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
//		String eventsFile = "C:\\temp\\bottleneck\\output\\ITERS\\it.0\\0.events.xml.gz";
//		String config = "C:\\temp\\bottleneck\\input\\config.xml";
		
		String eventsFile = "C:/temp/bottleneck/output/ITERS/it.0/0.events.xml.gz";
		String config = "C:/temp/bottleneck/input/config.xml";

		Config c = ConfigUtils.loadConfig(config);
		Scenario sc = ScenarioUtils.loadScenario(c);
		new ScenarioLoader2DImpl(sc).load2DScenario();

		EventsManager em = EventsUtils.createEventsManager();
		XYVxVyEventsFileReader r = new XYVxVyEventsFileReader(em);

		//		Envelope e = new Envelope(4,7,2.75,3.25);
		
		//standard bottleneck experiment
		Envelope e = new Envelope(-0.5,0.5,-3,-7);

		List<Coordinate> l = new ArrayList<Coordinate>();
		for (double x = e.getMinX(); x <= e.getMaxX(); x += .125 ) {
			for (double y = e.getMinY(); y <= e.getMaxY(); y += .125) {
				l.add(new Coordinate(x,y));
			}
		}

		NNGaussianKernelEstimator est = new DensityEstimatorFactory(em, sc).createDensityEstimator(l);
		em.addHandler(est);

		VelocityFundamentalDiagramFromEvents fundi = new VelocityFundamentalDiagramFromEvents(e);
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
