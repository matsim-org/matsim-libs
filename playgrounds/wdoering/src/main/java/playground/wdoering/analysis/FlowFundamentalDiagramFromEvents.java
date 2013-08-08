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

import playground.wdoering.oldstufffromgregor.Algorithms;
import playground.wdoering.oldstufffromgregor.DensityEstimatorFactory;
import playground.wdoering.oldstufffromgregor.DoubleValueStringKeyAtCoordinateEvent;
import playground.wdoering.oldstufffromgregor.DoubleValueStringKeyAtCoordinateEventHandler;
import playground.wdoering.oldstufffromgregor.NNGaussianKernelEstimator;
import playground.wdoering.oldstufffromgregor.ScenarioLoader2DImpl;
import playground.wdoering.oldstufffromgregor.Sim2DConfigGroup;
import playground.wdoering.oldstufffromgregor.XYVxVyEvent;
import playground.wdoering.oldstufffromgregor.XYVxVyEventsFileReader;
import playground.wdoering.oldstufffromgregor.XYVxVyEventsHandler;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class FlowFundamentalDiagramFromEvents implements XYVxVyEventsHandler, DoubleValueStringKeyAtCoordinateEventHandler{

	private double eventTime = 0;
	private double densEventTime = 0;
	private final Envelope e;

	private double flow = 0;
	private final List<Double> dens = new ArrayList<Double>();

	private final Map<Double,Double> timeFlow = new TreeMap<Double,Double>();
	private final TreeMap<Double,Double> timeDens = new TreeMap<Double,Double>();
	private final double timeRes;
	private final Coordinate fl0;
	private final Coordinate fl1;


	public FlowFundamentalDiagramFromEvents(Envelope e) {
		this.e = e;
		this.timeRes = new Sim2DConfigGroup().getTimeStepSize();
		this.fl0 = new Coordinate(e.getMinX(),(e.getMinY()+e.getMaxY())/2);
		this.fl1 = new Coordinate(e.getMaxX(),(e.getMinY()+e.getMaxY())/2);
	}



	@Override
	public void handleEvent(XYVxVyEvent event) {
		if (event.getTime() > this.eventTime) {
			this.timeFlow.put(this.eventTime, this.flow*25);
			this.flow = 0;
			this.eventTime = event.getTime();
		}

		if (this.e.contains(event.getCoordinate())){
			Coordinate c0 = event.getCoordinate();
			Coordinate c1 = new Coordinate();
			c1.x = c0.x - event.getVX()*this.timeRes;
			c1.y = c0.y - event.getVY()*this.timeRes;
			
			//this might be wrong.
			if ((Algorithms.isBelow(c1, this.fl0, this.fl1))  && (Algorithms.isAbove(c1, this.fl0, this.fl1))) {
				this.flow ++;
			}
		}

	}


	@Override
	public void handleEvent(DoubleValueStringKeyAtCoordinateEvent e) {
		if (e.getTime() > this.densEventTime) {
			Collections.sort(this.dens);
			this.timeDens.put(this.dens.get(this.dens.size()/2),this.densEventTime);
			this.dens.clear();
			this.densEventTime = e.getTime();
		}
		if (this.e.contains(e.getCoordinate())){
			this.dens.add(e.getValue());
		}
	}


	public void saveAsPng(String dir) {
		List<Tuple<Double, Double>> l = smoothData();
		XYLineChart chart = new XYLineChart("Fundamental diagram", "density p/m^2", "flow in (ms)^-1");
		double [] xs = new double[l.size()];
		double [] ys = new double[l.size()];
		int pos = 0;
		for (Tuple<Double, Double> t : l) {

			xs[pos] = t.getFirst();
			ys[pos++] = t.getSecond();
		}

		chart.addSeries("model", xs, ys);
		chart.saveAsPng(dir + "/flowFnd.png", 800, 400);
	}

	private List<Tuple<Double, Double>> smoothData() {
		double nextK = .25;

		List<Double> flow = new ArrayList<Double>();
		List<Double> rho = new ArrayList<Double>();
		List<Tuple<Double,Double>> ret = new ArrayList<Tuple<Double,Double>>();
		for (Entry<Double, Double> dens : this.timeDens.entrySet()) {
			Double j = this.timeFlow.get(dens.getValue());
			double d = dens.getKey();
			if (d < nextK && j != null) {
				rho.add(d);
				flow.add(j);
			} else if (d >= nextK){
				Collections.sort(rho);
				double jj = 0;
				for (double t : flow) {
					jj += t;
				}
				jj /= flow.size();
				Tuple<Double, Double> t = new Tuple<Double,Double>(rho.get(rho.size()/2),jj);
				ret.add(t);
				nextK += .25;
				rho.clear();
				flow.clear();
			}
		}
		return ret;
	}

	@Override
	public String toString() {

		StringBuffer sb = new StringBuffer();

		for (Entry<Double, Double> dens : this.timeDens.entrySet()) {
			double velo = this.timeFlow.get(dens.getValue());
			sb.append(dens.getKey());
			sb.append(' ');
			sb.append(velo);
			sb.append('\n');
		}

		return sb.toString();
	}

	public static void main(String [] args) {
//		String eventsFile = "/Users/laemmel/devel/oval/output/ITERS/it.0/0.events.xml.gz";
//		String config = "/Users/laemmel/devel/oval/input/config.xml";
		String eventsFile = "C:/temp/bottleneck/output/ITERS/it.0/0.events.xml.gz";
		String config = "C:/temp/bottleneck/input/config.xml";

		
		Config c = ConfigUtils.loadConfig(config);
		Scenario sc = ScenarioUtils.loadScenario(c);
		new ScenarioLoader2DImpl(sc).load2DScenario();

		EventsManager em = EventsUtils.createEventsManager();
		XYVxVyEventsFileReader r = new XYVxVyEventsFileReader(em);

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

		FlowFundamentalDiagramFromEvents fundi = new FlowFundamentalDiagramFromEvents(e);
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
