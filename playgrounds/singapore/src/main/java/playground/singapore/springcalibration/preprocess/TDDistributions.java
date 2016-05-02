package playground.singapore.springcalibration.preprocess;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.charts.BarChart;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.rank.Percentile;

/**
 * 
 * @author anhorni
 */
public class TDDistributions {

	private final static Logger log = Logger.getLogger(TDDistributions.class);
	
	private final Population population;
	private final List<DistributionClass> classes;
	private final String outdir;
	private Counter counter = new Counter();
	String [] allmodes = {"car","pt","other","pass","walk","taxi","schoolbus"};
	private DecimalFormat df = new DecimalFormat("0.00");
	
	public TDDistributions(final Population population, String outdir) {
		this.population = population;
		this.outdir = outdir;
		this.classes = new ArrayList<DistributionClass>();
	}
	
	public void run() {
		this.init();
		this.analyzePlans();
		log.info("Analyses finished ###################################");
	}
		
	private void init() {
		String [] hitsActivities = {"home", "work", "leisure", "pudo", "personal", "primaryschool", "secondaryschool", "tertiaryschool", "foreignschool"};
		
		
		for (String mode : allmodes) {
			counter.counts.put(mode, 0);
			counter.totalDistance.put(mode, 0.0);
			counter.totalTime.put(mode, 0.0);
		}
				
		for (String activity: hitsActivities) {
			for (String mode : allmodes) {
				DistributionClass distributionClass = this.createAndAddDistributionClass(mode + "-" + activity, "distance");
				this.addToActivityType(distributionClass, activity);
				this.addMainMode(distributionClass, mode);
				this.defineDistanceBinsForDistributionClass(distributionClass);
			}
		}
		
		for (String activity: hitsActivities) {
			for (String mode : allmodes) {
				DistributionClass distributionClass = this.createAndAddDistributionClass(mode + "-" + activity, "time");
				this.addToActivityType(distributionClass, activity);
				this.addMainMode(distributionClass, mode);
				this.defineTimeBinsForDistributionClass(distributionClass);
			}
		}
		
		for (String activity: hitsActivities) {
			DistributionClass distributionClass = this.createAndAddDistributionClass("all modes" + "-" + activity, "distance");
			this.defineDistanceBinsForDistributionClass(distributionClass);
			this.addToActivityType(distributionClass, activity);
			for (String mode : allmodes) {				
				this.addMainMode(distributionClass, mode);			
			}
		}
		
		for (String activity: hitsActivities) {
			DistributionClass distributionClass = this.createAndAddDistributionClass("all modes" + "-" + activity, "time");
			this.defineTimeBinsForDistributionClass(distributionClass);
			this.addToActivityType(distributionClass, activity);
			for (String mode : allmodes) {				
				this.addMainMode(distributionClass, mode);			
			}
		}
	}
	
	private void defineDistanceBinsForDistributionClass(DistributionClass distributionClass) {		
		this.createAndAddBin(distributionClass, 0.0, 0.5);
		this.createAndAddBin(distributionClass, 0.5, 1.0);
		this.createAndAddBin(distributionClass, 1.0, 2.0);
		this.createAndAddBin(distributionClass, 2.0, 5.0);
		this.createAndAddBin(distributionClass, 5.0, 10.0);
		this.createAndAddBin(distributionClass, 10.0, 20.0);
		this.createAndAddBin(distributionClass, 20.0, 50.0);
		this.createAndAddBin(distributionClass, 50.0, 100.0);
		this.createAndAddBin(distributionClass, 100.0, Double.MAX_VALUE);
	}
	
	private void defineTimeBinsForDistributionClass(DistributionClass distributionClass) {		
		this.createAndAddBin(distributionClass, 0.0, 1.0);
		this.createAndAddBin(distributionClass, 1.0, 5.0);
		this.createAndAddBin(distributionClass, 5.0, 10.0);
		this.createAndAddBin(distributionClass, 10.0, 20.0);
		this.createAndAddBin(distributionClass, 20.0, 30.0);
		this.createAndAddBin(distributionClass, 30.0, 45.0);
		this.createAndAddBin(distributionClass, 45.0, 60.0);
		this.createAndAddBin(distributionClass, 60.0, 90.0);
		this.createAndAddBin(distributionClass, 90.0, 120.0);
		this.createAndAddBin(distributionClass, 120.0, Double.MAX_VALUE);
	}
	
	private void analyzePlans() {
		List<Plan> plans = new ArrayList<Plan>();
		for (Person person : this.population.getPersons().values()) plans.add(person.getSelectedPlan());
		
		for (Plan plan : plans) {
			analyzePlan(plan);
		}
		for (DistributionClass distributionClass : this.classes) {
			writeDistributionClass(distributionClass);
		}
		this.writeCounter();
	}
	
	private void analyzePlan(Plan plan) {		
		List<PlanElement> pes = plan.getPlanElements();
		int index = -1;
		Coord prevcoords = null;
		double prevendTime = 0.0;
		for (PlanElement pe : pes){	
			if(pe instanceof Activity){
				index++;
				String actType = ((Activity) pe).getType();
				Coord coords = ((Activity) pe).getCoord();
				double startTime = ((Activity) pe).getStartTime();
				double endTime = ((Activity) pe).getEndTime();
				
				if (index > 0) {
					Leg previousLeg = ((PlanImpl)plan).getPreviousLeg((Activity)pe);
					String mode = previousLeg.getMode();			
					double travelTime = (startTime - prevendTime) / 60.0; // in minutes				
					double travelDistance = CoordUtils.calcEuclideanDistance(coords, prevcoords) / 1000.0; // in km
					this.add2Distribution(actType, travelDistance, mode, "distance");
					this.add2Distribution(actType, travelTime, mode, "time");	
					
					this.counter.incCounts(mode);
					this.counter.addDistance(mode, travelDistance);
					this.counter.addTime(mode, travelTime);
				}
				prevcoords = coords;
				prevendTime = endTime;
			}
		}
	}
		
	private void add2Distribution(String actToType, double value, String mode, String measure) {
		for (DistributionClass distributionClass : classes) {
			boolean containsMainMode = distributionClass.mainModes.contains(mode);
			
			if (actToType.startsWith("w")) actToType = "work";
			
			boolean containsActivityToType = distributionClass.activityTypesTo.contains(actToType); 
			boolean isMeasure = distributionClass.measure.equals(measure); 
			
			if (containsMainMode && containsActivityToType && isMeasure) {	
				distributionClass.mean.increment(value);
				distributionClass.values.add(value);
				for (Bin bin : distributionClass.distributionBins) {
					if (value >= bin.low && value < bin.high) {
						bin.count++;
					}
				}
			}
		}
	}
	
	private void writeDistributionClass(DistributionClass distributionClass) {
		String measure = distributionClass.measure;
		String fileName = outdir + "/" + measure + "_" + distributionClass.name;	
		double[] share = new double[distributionClass.distributionBins.size()];
		
		// accumulate total count
		int count = 0;
		for (Bin bin : distributionClass.distributionBins) {
			count += bin.count;
		}
		
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(fileName + ".txt");
			StringBuffer stringBuffer = new StringBuffer();
			stringBuffer.append("low" + "\t");
			stringBuffer.append("high" + "\t");
			stringBuffer.append("share");
			
			writer.write(stringBuffer.toString());
			writer.newLine();
			
			int i = 0;
			log.info(measure + " distribution for class " + distributionClass.name);
			for (Bin bin : distributionClass.distributionBins) {
				share[i] = (double) bin.count / (double) count;
				
				stringBuffer = new StringBuffer();
				stringBuffer.append(bin.low + "\t");
				stringBuffer.append(bin.high + "\t");
				stringBuffer.append(String.valueOf(share[i]));				
				writer.write(stringBuffer.toString());
				writer.newLine();			
				i++;
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String title = measure + " distribution for " + distributionClass.name;
		
		String unit = " km ";
		if (measure.equals("time")) unit = " min ";
		
		Percentile percentile = new Percentile();
		Double[] valuesarray = distributionClass.values.toArray(new Double[distributionClass.values.size()]);
		percentile.setData(ArrayUtils.toPrimitive(valuesarray));
		String xAxisLabel = measure + " class:" + "\n" + " mean: " + df.format(distributionClass.mean.getResult()) + unit + " - 50% percentile: " + df.format(percentile.evaluate(50)) + unit;
		String yAxisLabel = "Share";
		String[] categories = new String[distributionClass.distributionBins.size()];
		int i = 0;
		for (Bin bin : distributionClass.distributionBins) {
			categories[i++] = bin.low + "\n" + " .. " + "\n" + bin.high;
		}
		BarChart chart = new BarChart(title, xAxisLabel, yAxisLabel, categories);
		
		CategoryPlot plot = chart.getChart().getCategoryPlot();
		CategoryAxis categoryAxis = plot.getDomainAxis();
		categoryAxis.setMaximumCategoryLabelLines(3);
		
		chart.addMatsimLogo();
		chart.addSeries("share", share);
		chart.saveAsPng(fileName + ".png", 1024, 768);
	}
	
	private void writeCounter() {
		String fileName = outdir + "/counter";		
		
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(fileName + ".txt");
			StringBuffer stringBuffer = new StringBuffer();
			stringBuffer.append("mode" + "\t" + "counts" + "\t" + "distance" + "\t" + "time");			
			writer.write(stringBuffer.toString());
			writer.newLine();
			
			
			for (String mode : allmodes) {
				stringBuffer = new StringBuffer();
				stringBuffer.append(mode + "\t");
				stringBuffer.append(df.format(this.counter.getShare(mode, "counts")) + "\t");
				stringBuffer.append(df.format(this.counter.getShare(mode, "distance")) + "\t");
				stringBuffer.append(df.format(this.counter.getShare(mode, "time")));
				writer.write(stringBuffer.toString());
				writer.newLine();
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public DistributionClass createAndAddDistributionClass(String className, String measure) {
		DistributionClass distributionClass = new DistributionClass();
		distributionClass.name = className;
		distributionClass.measure = measure;
		this.classes.add(distributionClass);
		return distributionClass;
	}
	
	public void createAndAddBin(DistributionClass distributionClass, double low, double high) {
		Bin distanceBin = new Bin(low, high);
		distributionClass.distributionBins.add(distanceBin);
	}
	
	public void addToActivityType(DistributionClass distributionClass, String toActivity) {
		distributionClass.activityTypesTo.add(toActivity);
	}
	
	public void addMainMode(DistributionClass distributionClass, String mainMode) {
		distributionClass.mainModes.add(mainMode);
	}
	
	public static class DistributionClass {
		String name;
		String measure;
		Set<String> activityTypesTo = new LinkedHashSet<String>();
		Set<String> mainModes = new LinkedHashSet<String>();
		Set<Bin> distributionBins = new LinkedHashSet<Bin>();
		Mean mean = new Mean();
		List<Double> values = new ArrayList<Double>();
	}
	
	public class Counter {
		TreeMap<String, Integer> counts = new TreeMap<String, Integer>();
		TreeMap<String, Double> totalDistance = new TreeMap<String, Double>();
		TreeMap<String, Double> totalTime = new TreeMap<String, Double>();
		
		public void incCounts(String mode) {
			int i = this.counts.get(mode);
			this.counts.put(mode, i + 1);
		}
		
		public void addDistance(String mode, double v) {
			double d = this.totalDistance.get(mode);
			this.totalDistance.put(mode, d + v);
		}
		
		public void addTime(String mode, double v) {
			double d = this.totalTime.get(mode);
			this.totalTime.put(mode, d + v);
		}
		
		public double getShare(String mode, String type) {
			double share = 0.0;
			double total = 0.0;
			if (type.equals("time")) {
				for (Double v : totalTime.values()) {
					total += v;
					share = totalTime.get(mode);
				}
			} else if (type.equals("distance")) {
				for (Double v : totalDistance.values()) {
					total += v;
					share = totalDistance.get(mode);
				}
			} else {
				for (Integer v : counts.values()) {
					total += v;
					share = counts.get(mode);
				}
			}			
			return share / total;			
		}
	}
	
	private static class Bin {
		final double low;
		final double high;
		int count = 0;
		
		public Bin(double low, double high) {
			this.low = low;
			this.high = high;
		}
	}

}
