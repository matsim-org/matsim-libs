package playground.wdoering.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.collections.Tuple;

import playground.wdoering.oldstufffromgregor.DoubleValueStringKeyAtCoordinateEvent;
import playground.wdoering.oldstufffromgregor.DoubleValueStringKeyAtCoordinateEventHandler;
import playground.wdoering.oldstufffromgregor.ScenarioLoader2DImpl;
import playground.wdoering.oldstufffromgregor.Sim2DConfigGroup;
import playground.wdoering.oldstufffromgregor.XYVxVyEvent;
import playground.wdoering.oldstufffromgregor.XYVxVyEventsFileReader;
import playground.wdoering.oldstufffromgregor.XYVxVyEventsHandler;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class BottleneckTimeDiagram implements XYVxVyEventsHandler, DoubleValueStringKeyAtCoordinateEventHandler, PersonArrivalEventHandler, LinkEnterEventHandler{

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
	
	private boolean measurementInterval = false;
	private double measurementStartTime = 0d;
	
	private static LinkedList<PersonsPerTime> personsPerTimeList;
	private int arrivalCount = 0;
	private double measurementEndTime;
	private static int personCount;
	private static Id<Link> bottleneckEnterLink; 


	public BottleneckTimeDiagram(Envelope e) {
		this.e = e;
		this.timeRes = new Sim2DConfigGroup().getTimeStepSize();
		this.fl0 = new Coordinate(e.getMinX(),e.getMaxY());
		this.fl1 = new Coordinate(e.getMaxX(),e.getMaxY());
//		this.fl0 = new Coordinate(e.getMinX(),(e.getMinY()+e.getMaxY())/2);
//		this.fl1 = new Coordinate(e.getMaxX(),(e.getMinY()+e.getMaxY())/2);
	}


	

	@Override
	public void handleEvent(XYVxVyEvent event) {
		

		
//		if ((c0.y<=fl0.y) && (!measurementInterval))
//		{
//			measurementInterval = true;
//			measurementStartTime  = event.getTime();
//			
//		}
			
			
			if (event.getTime() > this.eventTime) {
				this.timeFlow.put(this.eventTime, this.flow*25);
				this.flow = 0;
				this.eventTime = event.getTime();
			}
	
			if (this.e.contains(event.getCoordinate()))
			{
				Coordinate c0 = event.getCoordinate();
				Coordinate c1 = new Coordinate();
				c1.x = c0.x - event.getVX()*this.timeRes;
				c1.y = c0.y - event.getVY()*this.timeRes;
				
				//System.out.println(event.getPersonId() + ": (c0x: " + c0.x + ", c1x:" + c1.x + ") | (c0y:" + c0.y + ", c1y:" + c1.y +")");
				
				if ((c1.y > this.fl0.y) && (c0.y <= this.fl0.y))
				{
					//System.out.println("!");
					this.flow ++;
				}
				else
				{
					System.out.println("c0y: " + c0.y + "|fl0y: " + this.fl0.y + "_c1y: " + c1.y);
				}
				
//				if ((!Algorithms.isAbove(c0, this.fl0, this.fl1))  && (!Algorithms.isBelow(c1, this.fl0, this.fl1))) {
//					this.flow ++;
//				}
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


    /*
	public static void saveAsPng(Map<Double,Double> timeFlow, TreeMap<Double, Double> timeDens, String dir)
	{
		List<Tuple<Double, Double>> l = smoothData(timeFlow, timeDens);
		XYLineChart chart = new XYLineChart("Fundamental diagram", "density p/m^2", "flow in (ms)^-1");
		//XYLineChart chart = new XYLineChart("Fundamental diagram", "density p/m^2", "flow in (ms)^-1");
		double [] xs = new double[l.size()];
		double [] ys = new double[l.size()];
		int pos = 0;
		int i = 0;
		for (Tuple<Double, Double> t : l) {
			i++;

			xs[pos] = t.getFirst();
			xs[pos] = //i; //t.getFirst();
			ys[pos++] = t.getSecond();
		}

		chart.addSeries("model", xs, ys);
		chart.saveAsPng(dir + "/flowFnd.png", 800, 400);
	}
	*/
	
	public static void saveAsPng(String title, String xAxisString, String yAxisString, LinkedList<Series> seriesList, String dir)
	{
		XYLineChart chart = new XYLineChart(title, xAxisString, yAxisString);
//		XYLineChart chart = new XYLineChart(title, xAxisString, yAxisString);
		
		
	
		for (Series series : seriesList)
			chart.addSeries(series.getName(), series.getXs(), series.getYs());
		
		chart.saveAsPng(dir + "/flowFnd2.png", 800, 800);

	}
		
	
	public Series getSeries(String measurementTitle, Map<Double,Double> timeFlow, TreeMap<Double, Double> timeDens)
	{
		List<Tuple<Double, Double>> l = smoothData(timeFlow, timeDens);
		XYLineChart chart = new XYLineChart("Fundamental diagram", "density p/m^2", "flow in (ms)^-1");
		//XYLineChart chart = new XYLineChart("Fundamental diagram", "density p/m^2", "flow in (ms)^-1");
		double [] xs = new double[l.size()];
		double [] ys = new double[l.size()];
		int pos = 0;
		int i = 0;
		for (Tuple<Double, Double> t : l) {
			i++;

			xs[pos] = t.getFirst();
			xs[pos] = //i; //t.getFirst();
			ys[pos++] = t.getSecond();
		}
		
		Series series = new Series(measurementTitle, xs, ys);

		return series;
		
	}	

	private static List<Tuple<Double, Double>> smoothData(Map<Double, Double> timeFlow, TreeMap<Double, Double> timeDens)
	{
		double nextK = .25;

		List<Double> flow = new ArrayList<Double>();
		List<Double> rho = new ArrayList<Double>();
		List<Tuple<Double,Double>> ret = new ArrayList<Tuple<Double,Double>>();
		
		for (Entry<Double, Double> dens : timeDens.entrySet())
		{
			Double j = timeFlow.get(dens.getValue());
			double d = dens.getKey();
			if (d < nextK && j != null)
			{
				rho.add(d);
				flow.add(j);
			}
			else if (d >= nextK)
			{
				Collections.sort(rho);
				double jj = 0;
				for (double t : flow)
				{
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
		
		LinkedList<Series> seriesList = new LinkedList<Series>();
		bottleneckEnterLink = Id.create(4, Link.class);
		
		int model = 9;
		
		//sf
		if (model==0)
		{
			//w=2.5
			String eventsFile = "C:/temp/bottleneck/sf/p175-w2.5/output/ITERS/it.0/0.events.xml.gz";
			String configFile = "C:/temp/bottleneck/sf/p175-w2.5/input/config.xml";
			Series series5 = runMeasurement("w=2.5",eventsFile, configFile);
			seriesList.add(series5);
			
			//w=2.0
			eventsFile = "C:/temp/bottleneck/sf/p175-w2.0/output/ITERS/it.0/0.events.xml.gz";
			configFile = "C:/temp/bottleneck/sf/p175-w2.0/input/config.xml";
			Series series4 = runMeasurement("w=2.0",eventsFile, configFile);
			seriesList.add(series4);
	
			//w=1.4
			eventsFile = "C:/temp/bottleneck/sf/p175-w1.4/output/ITERS/it.0/0.events.xml.gz";
			configFile = "C:/temp/bottleneck/sf/p175-w1.4/input/config.xml";
			Series series3 = runMeasurement("w=1.4",eventsFile, configFile);
			seriesList.add(series3);
			
			//w=1.1
			eventsFile = "C:/temp/bottleneck/sf/p175-w1.1/output/ITERS/it.0/0.events.xml.gz";
			configFile = "C:/temp/bottleneck/sf/p175-w1.1/input/config.xml";
			Series series2 = runMeasurement("w=1.1",eventsFile, configFile);
			seriesList.add(series2);
	
			//w=0.9
			eventsFile = "C:/temp/bottleneck/sf/p175-w0.9/output/ITERS/it.0/0.events.xml.gz";
			configFile = "C:/temp/bottleneck/sf/p175-w0.9/input/config.xml";
			Series series1 = runMeasurement("w=0.9", eventsFile, configFile);
			seriesList.add(series1);
		}
		else if (model==1)
		{
			//w=2.5
			String eventsFile = "C:/temp/bottleneck/cp/p175-w2.5/output/ITERS/it.0/0.events.xml.gz";
			String configFile = "C:/temp/bottleneck/cp/p175-w2.5/input/config.xml";
			Series series5 = runMeasurement("w=2.5",eventsFile, configFile);
			seriesList.add(series5);
			
			//w=2.0
			eventsFile = "C:/temp/bottleneck/cp/p175-w2.0/output/ITERS/it.0/0.events.xml.gz";
			configFile = "C:/temp/bottleneck/cp/p175-w2.0/input/config.xml";
			Series series4 = runMeasurement("w=2.0",eventsFile, configFile);
			seriesList.add(series4);
	
			//w=1.4
			eventsFile = "C:/temp/bottleneck/cp/p175-w1.4/output/ITERS/it.0/0.events.xml.gz";
			configFile = "C:/temp/bottleneck/cp/p175-w1.4/input/config.xml";
			Series series3 = runMeasurement("w=1.4",eventsFile, configFile);
			seriesList.add(series3);
			
			//w=1.1
			eventsFile = "C:/temp/bottleneck/cp/p175-w1.1/output/ITERS/it.0/0.events.xml.gz";
			configFile = "C:/temp/bottleneck/cp/p175-w1.1/input/config.xml";
			Series series2 = runMeasurement("w=1.1",eventsFile, configFile);
			seriesList.add(series2);
	
			//w=0.9
			eventsFile = "C:/temp/bottleneck/cp/p175-w0.9/output/ITERS/it.0/0.events.xml.gz";
			configFile = "C:/temp/bottleneck/cp/p175-w0.9/input/config.xml";
			Series series1 = runMeasurement("w=0.9", eventsFile, configFile);
			seriesList.add(series1);			
		}
		else if (model==2)
		{
			//w=2.5
			String eventsFile = "C:/temp/bottleneck/vo/p175-w2.5/output/ITERS/it.0/0.events.xml.gz";
			String configFile = "C:/temp/bottleneck/vo/p175-w2.5/input/config.xml";
			Series series5 = runMeasurement("w=2.5",eventsFile, configFile);
			seriesList.add(series5);
			
			//w=2.0
			eventsFile = "C:/temp/bottleneck/vo/p175-w2.0/output/ITERS/it.0/0.events.xml.gz";
			configFile = "C:/temp/bottleneck/vo/p175-w2.0/input/config.xml";
			Series series4 = runMeasurement("w=2.0",eventsFile, configFile);
			seriesList.add(series4);
	
			//w=1.4
			eventsFile = "C:/temp/bottleneck/vo/p175-w1.4/output/ITERS/it.0/0.events.xml.gz";
			configFile = "C:/temp/bottleneck/vo/p175-w1.4/input/config.xml";
			Series series3 = runMeasurement("w=1.4",eventsFile, configFile);
			seriesList.add(series3);
			
			//w=1.1
			eventsFile = "C:/temp/bottleneck/vo/p175-w1.1/output/ITERS/it.0/0.events.xml.gz";
			configFile = "C:/temp/bottleneck/vo/p175-w1.1/input/config.xml";
			Series series2 = runMeasurement("w=1.1",eventsFile, configFile);
			seriesList.add(series2);
	
			//w=0.9
			eventsFile = "C:/temp/bottleneck/vo/p175-w0.9/output/ITERS/it.0/0.events.xml.gz";
			configFile = "C:/temp/bottleneck/vo/p175-w0.9/input/config.xml";
			Series series1 = runMeasurement("w=0.9", eventsFile, configFile);
			seriesList.add(series1);			
		}
		else if (model>=4)
		{
			
			double firstPersonArrivalTime = Double.MAX_VALUE;
			
			if (model==4)
			{
				
				System.err.println("adding w=1.4 model series");
				
				//sf model w=1.4
				String eventsFile = "C:/temp/bottleneck/sf/p175-w1.4/output/ITERS/it.0/0.events.xml.gz";
				String configFile = "C:/temp/bottleneck/sf/p175-w1.4/input/config.xml";
				Series series3 = runMeasurement("SF",eventsFile, configFile);
				
				//cp model w=1.4
				eventsFile = "C:/temp/bottleneck/cp/p175-w1.4/output/ITERS/it.0/0.events.xml.gz";
				configFile = "C:/temp/bottleneck/cp/p175-w1.4/input/config.xml";
				Series series2 = runMeasurement("CP",eventsFile, configFile);
		
				//vo model w=1.4
				eventsFile = "C:/temp/bottleneck/vo/p175-w1.4/output/ITERS/it.0/0.events.xml.gz";
				configFile = "C:/temp/bottleneck/vo/p175-w1.4/input/config.xml";
				Series series1 = runMeasurement("VO", eventsFile, configFile);
				
				seriesList.add(series1);
				seriesList.add(series3);
				seriesList.add(series2);
				
				for (Series series : seriesList)
					firstPersonArrivalTime = Math.min(series.getXs()[0], firstPersonArrivalTime);

			}
			
			if (model == 6)
			{
				System.err.println("adding w=0.9 model series");
				
				//sf model w=0.9
				String eventsFile = "C:/temp/bottleneck/sf/p175-w0.9/output/ITERS/it.0/0.events.xml.gz";
				String configFile = "C:/temp/bottleneck/sf/p175-w0.9/input/config.xml";
				Series series31 = runMeasurement("SF",eventsFile, configFile);

				//sf model w=0.9
				eventsFile = "C:/temp/bottleneck/cp/p175-w0.9/output/ITERS/it.0/0.events.xml.gz";
				configFile = "C:/temp/bottleneck/cp/p175-w0.9/input/config.xml";
				Series series32 = runMeasurement("CP",eventsFile, configFile);

				//sf model w=0.9
				eventsFile = "C:/temp/bottleneck/vo/p175-w0.9/output/ITERS/it.0/0.events.xml.gz";
				configFile = "C:/temp/bottleneck/vo/p175-w0.9/input/config.xml";
				Series series33 = runMeasurement("VO",eventsFile, configFile);

				
				
				seriesList.add(series31);
				seriesList.add(series32);
				seriesList.add(series33);
				
				for (Series series : seriesList)
					firstPersonArrivalTime = Math.min(series.getXs()[0], firstPersonArrivalTime);

			}

			if (model == 7)
			{
				System.err.println("adding w=1.1 model series");
				
				//sf model w=1.1
				String eventsFile = "C:/temp/bottleneck/sf/p175-w1.1/output/ITERS/it.0/0.events.xml.gz";
				String configFile = "C:/temp/bottleneck/sf/p175-w1.1/input/config.xml";
				Series series31 = runMeasurement("SF",eventsFile, configFile);
				
				//sf model w=1.1
				eventsFile = "C:/temp/bottleneck/cp/p175-w1.1/output/ITERS/it.0/0.events.xml.gz";
				configFile = "C:/temp/bottleneck/cp/p175-w1.1/input/config.xml";
				Series series32 = runMeasurement("CP",eventsFile, configFile);

				//sf model w=1.1
				eventsFile = "C:/temp/bottleneck/vo/p175-w1.1/output/ITERS/it.0/0.events.xml.gz";
				configFile = "C:/temp/bottleneck/vo/p175-w1.1/input/config.xml";
				Series series33 = runMeasurement("VO",eventsFile, configFile);

				
				seriesList.add(series31);
				seriesList.add(series32);
				seriesList.add(series33);
				
				for (Series series : seriesList)
					firstPersonArrivalTime = Math.min(series.getXs()[0], firstPersonArrivalTime);


			}

			if (model == 8)
			{
				System.err.println("adding w=2.0 model series");
				
				
				//sf model w=2.0
				String eventsFile = "C:/temp/bottleneck/sf/p175-w2.0/output/ITERS/it.0/0.events.xml.gz";
				String configFile = "C:/temp/bottleneck/sf/p175-w2.0/input/config.xml";
				Series series31 = runMeasurement("SF",eventsFile, configFile);
				
				//sf model w=2.0
				eventsFile = "C:/temp/bottleneck/cp/p175-w2.0/output/ITERS/it.0/0.events.xml.gz";
				configFile = "C:/temp/bottleneck/cp/p175-w2.0/input/config.xml";
				Series series32 = runMeasurement("CP",eventsFile, configFile);

				//sf model w=2.0
				eventsFile = "C:/temp/bottleneck/vo/p175-w2.0/output/ITERS/it.0/0.events.xml.gz";
				configFile = "C:/temp/bottleneck/vo/p175-w2.0/input/config.xml";
				Series series33 = runMeasurement("VO",eventsFile, configFile);

				
				seriesList.add(series31);
				seriesList.add(series32);
				seriesList.add(series33);
				
				for (Series series : seriesList)
					firstPersonArrivalTime = Math.min(series.getXs()[0], firstPersonArrivalTime);


			}

			if (model == 9)
			{
				System.err.println("adding w=2.5 model series");
				
				
				//sf model w=2.5
				String eventsFile = "C:/temp/bottleneck/sf/p175-w2.5/output/ITERS/it.0/0.events.xml.gz";
				String configFile = "C:/temp/bottleneck/sf/p175-w2.5/input/config.xml";
				Series series31 = runMeasurement("SF",eventsFile, configFile);
				
				//sf model w=2.5
				eventsFile = "C:/temp/bottleneck/cp/p175-w2.5/output/ITERS/it.0/0.events.xml.gz";
				configFile = "C:/temp/bottleneck/cp/p175-w2.5/input/config.xml";
				Series series32 = runMeasurement("CP",eventsFile, configFile);

				//sf model w=2.5
				eventsFile = "C:/temp/bottleneck/vo/p175-w2.5/output/ITERS/it.0/0.events.xml.gz";
				configFile = "C:/temp/bottleneck/vo/p175-w2.5/input/config.xml";
				Series series33 = runMeasurement("VO",eventsFile, configFile);

				
				seriesList.add(series31);
				seriesList.add(series32);
				seriesList.add(series33);
				
				for (Series series : seriesList)
					firstPersonArrivalTime = Math.min(series.getXs()[0], firstPersonArrivalTime);

			}
			
			
			

			//lab result variables
			String rName = "";
			double[] rXs;
			double[] rYs;
			
			////////////////////////////////////////////////
			// lab results (w=2.5)                        //
			////////////////////////////////////////////////
			rXs = new double[]{ 0, 2.50, 4.17, 9.44, 13.33, 20.00, 20.56, 25.00, 25.56, 28.89, 29.44, 32.38 };
			rYs = new double[]{ 0, 12,   29,   64,   91,    124,   129,   146,   150,   161,   170,   175   };
			//shift to first measure
			if (model!=5) for (int i = 0; i<rXs.length; i++) rXs[i] += firstPersonArrivalTime;
			rName = "lab result (w=2.5)";
			Series labResult25 = new Series(rName, rXs, rYs);
		
			////////////////////////////////////////////////
			// lab results (w=2.0)                        //
			////////////////////////////////////////////////
			rXs = new double[]{ 0, 2.78, 8.06, 8.89, 11.94, 17.50, 24.44, 26.11, 30.56, 33.06, 35.28, 44.72 };
			rYs = new double[]{ 0, 17,   44,   49,   63,    91,    117,   120,   136,   143,   146,  174    };
			//shift to first measure
			if (model!=5) for (int i = 0; i<rXs.length; i++) rXs[i] += firstPersonArrivalTime;
			rName = new String("lab result (w=2.0)");
			Series labResult20 = new Series(rName, rXs, rYs);
			
			////////////////////////////////////////////////
			// lab results (w=1.4)                        //
			////////////////////////////////////////////////
			rXs = new double[]{ 0, 10.83, 13.61, 17.78, 20.83, 23.89, 34.72,  36.67,  44.72,  50.00,  53.06,  56.39,  56.94,  58.89,  59.19  };
			rYs = new double[]{ 0, 42,    52,    65,    73,    81,    113,    114,    138,    152,    160,    167,    170,    173,    175    };
			//shift to first measure
			if (model!=5) for (int i = 0; i<rXs.length; i++) rXs[i] += firstPersonArrivalTime;
			rName = new String("lab result (w=1.4)");
			Series labResult14 = new Series(rName, rXs, rYs);
			
			////////////////////////////////////////////////
			// lab results (w=1.1)                        //
			////////////////////////////////////////////////

			rXs = new double[]{ 0, 9.44, 12.50, 24.72, 26.39, 30.00, 38.06, 50.28, 55.00, 70.00, 78.33, 80.00 };
			rYs = new double[]{ 0, 30,   39,    64,    71,    78,    93,    122,   131,   161,   173,   175   };
			//shift to first measure
			if (model!=5) for (int i = 0; i<rXs.length; i++) rXs[i] += firstPersonArrivalTime;
			rName = new String("lab result (w=1.1)");
			Series labResult11 = new Series(rName, rXs, rYs);
			
			////////////////////////////////////////////////
			// lab results (w=0.9)                        //
			////////////////////////////////////////////////
			rXs = new double[]{ 0, 5.00, 12.22, 21.39, 22.50, 23.61, 31.39, 39.17, 51.67, 56.39, 69.44, 71.67, 76.94, 93.89, 106.11 };
			rYs = new double[]{ 0, 14,   32,    47,    51,    52,    68,    82,    99,    109,   131,   132,   142,   163,   175    };
			//shift to first measure
			if (model!=5) for (int i = 0; i<rXs.length; i++) rXs[i] += firstPersonArrivalTime;
			rName = new String("lab result (w=0.9)");
			Series labResult09 = new Series(rName, rXs, rYs);
			

			//ADD ALL LAB RESULTS
			if (model == 5)
			{
				seriesList.add(labResult09);
				seriesList.add(labResult11);
				seriesList.add(labResult14);
				seriesList.add(labResult20);
				seriesList.add(labResult25);
				
				saveAsPng("bottleneck n-t diagram lab results (p=175;wc=7;l=4)", "time", "persons", seriesList, "C:/temp/");


			}
			
			//ADD SPECIFIC LAB RESULT
			if (model == 6)
			{
				
				seriesList.add(labResult09);
				saveAsPng("bottleneck n-t diagram (w=0.9,p=175;wc=7;l=4)", "time", "persons", seriesList, "C:/temp/");
				System.err.println("created w=0.9 model - lab result data comparison diagram");
				
			}
			if (model == 7)
			{
				seriesList.add(labResult11);
				saveAsPng("bottleneck n-t diagram (w=1.1,p=175;wc=7;l=4)", "time", "persons", seriesList, "C:/temp/");
				System.err.println("created w=1.1 model - lab result data comparison diagram");
			}
			if (model == 8)
			{
				seriesList.add(labResult20);
				saveAsPng("bottleneck n-t diagram (w=2.0,p=175;wc=7;l=4)", "time", "persons", seriesList, "C:/temp/");
				System.err.println("created w=2.0 model - lab result data comparison diagram");
			}
			if (model == 9)
			{
				seriesList.add(labResult25);
				saveAsPng("bottleneck n-t diagram (w=2.5,p=175;wc=7;l=4)", "time", "persons", seriesList, "C:/temp/");
				System.err.println("created w=2.5 model - lab result data comparison diagram");
			}

			
			
			
		}
		
		
//		saveAsPng("bottleneck n-t diagram using velocity obstacle model (p=175;w=7;l=4)", "time", "persons", seriesList, "C:/temp/");
//		saveAsPng("bottleneck n-t diagram (w=1.4,p=175;wc=7;l=4)", "time", "persons", seriesList, "C:/temp/");
//		saveAsPng("bottleneck n-t diagram using social force model (p=175;w=7;l=4)", "time", "persons", seriesList, "C:/temp/");
		
		//		System.out.println(fundi);
	}



	public static Series runMeasurement(String measurementTitle, String eventsFile, String configFile)
	{
		
		Config c = ConfigUtils.loadConfig(configFile);
		
		Scenario sc = ScenarioUtils.loadScenario(c);
		
		new ScenarioLoader2DImpl(sc).load2DScenario();

		EventsManager em = EventsUtils.createEventsManager();
		XYVxVyEventsFileReader r = new XYVxVyEventsFileReader(em);

		//standard bottleneck experiment
		Envelope e = new Envelope(-0.5,0.5,-3.25,-7);
		
		personCount = sc.getPopulation().getPersons().size();


		List<Coordinate> l = new ArrayList<Coordinate>();
		for (double x = e.getMinX(); x <= e.getMaxX(); x += .125 )
		{
			for (double y = e.getMinY(); y <= e.getMaxY(); y += .125)
			{
				l.add(new Coordinate(x,y));
			}
		}

//		NNGaussianKernelEstimator est = new DensityEstimatorFactory(em, sc).createDensityEstimator(l);
//		em.addHandler(est);

		BottleneckTimeDiagram fundi = new BottleneckTimeDiagram(e);
		em.addHandler(fundi);

		r.parse(eventsFile);
		
		System.err.println(eventsFile + " parsed."); System.out.println("");
		
		return fundi.getSeries(measurementTitle, personsPerTimeList);
		//return fundi.getSeries(fundi.timeFlow, fundi.timeDens);

		//fundi.saveAsPng(fundi.timeFlow, fundi.timeDens, c.controler().getOutputDirectory());
		
	}
	

	private Series getSeries(String measurementTitle, LinkedList<PersonsPerTime> personsPerTimeList)
	{
		double [] xs = new double[personsPerTimeList.size()];
		double [] ys = new double[personsPerTimeList.size()];	        
		int pos = 0;
		
		for (PersonsPerTime ppt : personsPerTimeList)
		{
	        
	        xs[pos] = ppt.getTime();
	        ys[pos] = new Double(ppt.getPersons());
	        
	        pos++;
	    }
	    
	    Series series = new Series(measurementTitle,xs,ys);

		return series;
	}




	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub




	}


	@Override
	public void handleEvent(PersonArrivalEvent event)
	{

			
		if (personCount > 0)
		{
			if (this.arrivalCount==0)
			{
				System.err.println("t-"+ event.getTime() + ": starting measurement");
				personsPerTimeList = new LinkedList<PersonsPerTime>();
			}
			
			this.arrivalCount++;
			
			personsPerTimeList.add(new PersonsPerTime(event.getTime(), this.arrivalCount));
			
			if (this.arrivalCount==personCount)
			{
				System.err.println("t-"+ event.getTime() + ": all agents arrived");
				this.measurementEndTime = event.getTime();
				this.arrivalCount = 0;
				personCount = 0;
			}
		}
		
	}




	@Override
	public void handleEvent(LinkEnterEvent event)
	{
		
		
		if ((!this.measurementInterval) && (event.getLinkId().equals(bottleneckEnterLink)))
		{
			this.measurementInterval = true;
			this.measurementStartTime = event.getTime();
		}
		
	}









}

class ChartData
{
	private String xAxisString;
	private String yAxisString;
	private String title;
	private String dataString;
	
	public String getxAxisString() {
		return this.xAxisString;
	}
	public void setxAxisString(String xAxisString) {
		this.xAxisString = xAxisString;
	}
	public String getyAxisString() {
		return this.yAxisString;
	}
	public void setyAxisString(String yAxisString) {
		this.yAxisString = yAxisString;
	}
	public String getTitle() {
		return this.title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDataString() {
		return this.dataString;
	}
	public void setDataString(String dataString) {
		this.dataString = dataString;
	}
	
	public ChartData(String xAxisString, String yAxisString, String title, String dataString)
	{
		this.xAxisString = xAxisString;
		this.yAxisString = yAxisString;
		this.title = title;
		this.dataString = dataString;
	}
	
	
	
}

class PersonsPerTime
{
	private double time;
	private int persons;
	
	public PersonsPerTime(double time, int persons)
	{
		this.time = time;
		this.persons = persons;
	}

	public double getTime() {
		return this.time;
	}

	public void setTime(double time) {
		this.time = time;
	}

	public int getPersons() {
		return this.persons;
	}

	public void setPersons(int persons) {
		this.persons = persons;
	}
	
	
	
}

class Series
{
	private double[] xs;
	private double[] ys;
	private String name;

	public String getName() {
		return this.name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public double[] getXs() {
		return this.xs;
	}
	public void setXs(double[] xs) {
		this.xs = xs;
	}
	public double[] getYs() {
		return this.ys;
	}
	public void setYs(double[] ys) {
		this.ys = ys;
	}
	
	public Series(String name, double[] xs, double[] ys)
	{
		this.name = name;
		this.xs = xs;
		this.ys = ys;
	}
	
	public Series(Series series)
	{
		this.xs = series.xs.clone();
		this.ys = series.ys.clone();
		this.name = new String(series.getName());
	}
	
	public Series()
	{
		
	}

	
}

