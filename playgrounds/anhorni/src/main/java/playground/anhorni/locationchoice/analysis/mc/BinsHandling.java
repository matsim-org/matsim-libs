package playground.anhorni.locationchoice.analysis.mc;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.utils.charts.BarChart;
import org.matsim.core.utils.io.IOUtils;

import playground.anhorni.locationchoice.preprocess.helper.BinsOld;
import playground.anhorni.locationchoice.preprocess.helper.Utils;

public class BinsHandling {
	
	private final static Logger log = Logger.getLogger(BinsHandling.class);
	
	private int durationBinUnit = 3600;
	private int carDistanceUnit = 5000;
	private int ptDistanceUnit = 5000;
	private int bikeDistanceUnit = 500;
	private int walkDistanceUnit = 200;
	
	private int carMaxDistance = 100 * 1000;
	private int ptMaxDistance = 100 * 1000;
	private int bikeMaxDistance = 50 * 1000;
	private int walkMaxDistance = 20 * 1000;
	
	
	private TreeMap<String, BinsOld> createDurationBins() {
		TreeMap<String, BinsOld> durationBins = new TreeMap<String, BinsOld>();
		durationBins.put("car", new BinsOld(durationBinUnit, durationBinUnit * 24 ));			
		durationBins.put("pt", new BinsOld(durationBinUnit, durationBinUnit * 24));
		durationBins.put("walk", new BinsOld(durationBinUnit, durationBinUnit * 24));
		durationBins.put("bike", new BinsOld(durationBinUnit, durationBinUnit * 24));
		return durationBins;
	}
	
	private TreeMap<String, BinsOld> createDistanceBins(double binUnitFactor) {
		TreeMap<String, BinsOld> distanceBins = new TreeMap<String, BinsOld>();
		distanceBins.put("car", new BinsOld((int)(binUnitFactor * carDistanceUnit), carMaxDistance));			
		distanceBins.put("pt", new BinsOld((int)(binUnitFactor * ptDistanceUnit), ptMaxDistance));
		distanceBins.put("bike", new BinsOld((int)(binUnitFactor * bikeDistanceUnit), bikeMaxDistance));
		distanceBins.put("walk", new BinsOld((int)(binUnitFactor * walkDistanceUnit), walkMaxDistance));
		return distanceBins;
	}
		
	public void createBinOutput(MZ mz) {	
		log.info("Creating bin output ...");		
		this.createOutOfHomeBinOutput(mz);
		this.createDurationOutput(mz);
		this.createDistanceOutput(mz, "ch", 1.0);
		this.createDistanceOutput(mz, "zh", 1.0);
		this.createDistanceOutput(mz, "zh", 0.2);
	}
	

	private void createDistanceOutput(MZ mz, String region, double binUnitFactor) {
		
		Persons persons = mz.getPersons();
		TreeMap<String, BinsOld> distanceBinsLeisure = this.createDistanceBins(binUnitFactor);
		TreeMap<String, BinsOld> distanceBinsShop = this.createDistanceBins(binUnitFactor);
		
		//filter zh region			
		Iterator<PersonTripActs> personActTrips_it = persons.getPersons().values().iterator();
		while (personActTrips_it.hasNext()) {
			PersonTripActs personTripActs = personActTrips_it.next();
			if (region.equals("zh")) {
				personTripActs.filterZHRegion();	
			}
			else {
				personTripActs.undoZHRegionFiltering();
			}
		}
		
		String [] modes = {"pt", "car", "bike", "walk"};		
		for (int i = 0; i < modes.length; i++) {
					
			personActTrips_it = persons.getPersons().values().iterator();
			while (personActTrips_it.hasNext()) {
				PersonTripActs personTripActs = personActTrips_it.next();				
				// shop distances
				if (personTripActs.isValid() && personTripActs.getActDurationPerTypeAggregated("shop").get(modes[i]) > 0.0) {	
					Iterator<Double> it_shop = personTripActs.getTripDistancePerType("shop").get(modes[i]).iterator();
					while (it_shop.hasNext()) {							
						double val = it_shop.next();
						distanceBinsShop.get(modes[i]).addVal(val, 1.0);
					}
				}
				// leisure distances
				if (personTripActs.isValid() && personTripActs.getActDurationPerTypeAggregated("leisure").get(modes[i]) > 0.0) {	
					Iterator<Double> it_shop = personTripActs.getTripDistancePerType("leisure").get(modes[i]).iterator();
					while (it_shop.hasNext()) {							
						double val = it_shop.next();
						distanceBinsLeisure.get(modes[i]).addVal(val, 1.0);
					}
				}
			}
		}
	
		DecimalFormat formatter = new DecimalFormat("0.00");
		
		int [] distanceUnits = {ptDistanceUnit, carDistanceUnit, bikeDistanceUnit, walkDistanceUnit};
		for (int i = 0; i < 4; i++) {	
			String [] categories  = new String[distanceBinsLeisure.get(modes[i]).getSizes().length];
			for (int j = 0; j < categories.length; j++) {
				categories[j] = Integer.toString(j);
			}
			BarChart chartDistancesLeisureSize = new BarChart(modes[i] + " : leisure trip distances", 
					"distance bin [" + distanceUnits[i]/1000.0 * binUnitFactor + "km]", "seize", categories);
			chartDistancesLeisureSize.addSeries(
					"Seize per bin", distanceBinsLeisure.get(modes[i]).getSizes());
			chartDistancesLeisureSize.saveAsPng("./output/analyzeMz/trips/" + 
					region + "/" + modes[i] + "_leisure" + " bin unit = " + 
					formatter.format(distanceUnits[i]/1000.0 * binUnitFactor) + "_TripDistances.png", 1600, 800);
			
			categories  = new String[distanceBinsShop.get(modes[i]).getSizes().length];
			for (int j = 0; j < categories.length; j++) {
				categories[j] = Integer.toString(j);
			}
			BarChart chartTripDistancesShopSize = new BarChart(modes[i] + " : shop trip distances", 
					"distance bin [" + distanceUnits[i]/1000.0 + "km]", "seize", categories);
			chartTripDistancesShopSize.addSeries(
					"Seize per bin", distanceBinsShop.get(modes[i]).getSizes());
			chartTripDistancesShopSize.saveAsPng("./output/analyzeMz/trips/" 
					+ region + "/" + modes[i] + "_shop" + " bin unit = " + 
					formatter.format(distanceUnits[i]/1000.0 * binUnitFactor) + "_TripDistances.png", 1600, 800);
		}			
		for (int i = 0; i < 4; i++) {
			
			try {			
				BufferedWriter outLeisure1 = IOUtils.getBufferedWriter("./output/analyzeMz/trips/" 
						+ region + "/" + modes[i] + "_leisure" + " bin unit = " + 
						formatter.format(distanceUnits[i]/1000.0 * binUnitFactor) + "_TripDistances.txt");
				outLeisure1.write("Distance bin [" + distanceUnits[i]/1000.0 * binUnitFactor + "km]" + "\tSize\n");
				for (int j = 0; j < distanceBinsLeisure.get(modes[i]).getMedian().size();  j++) {
					outLeisure1.write(j + "\t" + distanceBinsLeisure.get(modes[i]).getSizes()[j] + "\n");
				}					
				outLeisure1.flush();			
				outLeisure1.close();
				
				
				BufferedWriter outShop = IOUtils.getBufferedWriter("./output/analyzeMz/trips/" 
						+ region + "/" + modes[i] + "_shop" + " bin unit = " + 
						formatter.format(distanceUnits[i]/1000.0 * binUnitFactor) +"_TripDistances.txt");
				outShop.write("Distance bin [" + distanceUnits[i]/1000.0 * binUnitFactor + "km]" + "\tSize\n");
				for (int j = 0; j < distanceBinsShop.get(modes[i]).getMedian().size();  j++) {
					outShop.write(j + "\t" + distanceBinsShop.get(modes[i]).getSizes()[j] + "\n");
				}					
				outShop.flush();			
				outShop.close();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void createDurationOutput(MZ mz) {
		
		TreeMap<String, BinsOld> durationBinsLeisure = this.createDurationBins();
		
		Persons persons = mz.getPersons();
		
		String [] modes = {"pt", "car", "bike", "walk"};		
		for (int i = 0; i < modes.length; i++) {
		
			Iterator<PersonTripActs> personActTrips_it = persons.getPersons().values().iterator();
			while (personActTrips_it.hasNext()) {
				PersonTripActs personTripActs = personActTrips_it.next();
				
				if (personTripActs.isValid() && personTripActs.getActDurationPerTypeAggregated("leisure").get(modes[i]) > 0.0) {					
					int cnt = 0;
					Iterator<Double> it = personTripActs.getLeisureActDuration().get(modes[i]).iterator();
					while (it.hasNext()) {	
						double val = it.next();
						durationBinsLeisure.get(modes[i]).addVal(val,
								personTripActs.getTripDistancePerType("leisure").get(modes[i]).get(cnt)/1000.0);						
						cnt++;
					}
				}
			}
		}
		for (int i = 0; i < 4; i++) {

			String [] categories  = new String[durationBinsLeisure.get(modes[i]).getSizes().length];
			for (int j = 0; j < categories.length; j++) {
				categories[j] = Integer.toString(j);
			}
			double binUnit = 24.0 * 60.0 / durationBinsLeisure.get(modes[i]).getSizes().length;
			BarChart chartLeisureActDurations = new BarChart(modes[i] + " : leisure act durations", 
					"duration bin [" + binUnit + " min]", "leisure dist [km]", categories);
			chartLeisureActDurations.addSeries("Median leisure act duration", 
					Utils.convert(durationBinsLeisure.get(modes[i]).getMedian()));
			chartLeisureActDurations.saveAsPng("./output/analyzeMz/actstrips_durationsdistances/ch/" + modes[i] + "_LeisureActDurations.png", 1600, 800);
			
			BarChart chartLeisureActDurationBinSizes = new BarChart(modes[i] + " : leisure act duration bin sizes", 
					"duration bin [" + binUnit + " min]", "seize", categories);
			chartLeisureActDurationBinSizes.addSeries("Seizes", durationBinsLeisure.get(modes[i]).getSizes());
			chartLeisureActDurationBinSizes.saveAsPng(
					"./output/analyzeMz/actstrips_durationsdistances/ch/" + modes[i] + "_leisureActDurationBinSizes.png", 1600, 800);
		}	
		for (int i = 0; i < 4; i++) {
			
			try {
				DecimalFormat formatter = new DecimalFormat("0.00");
				
				BufferedWriter outLeisure = IOUtils.getBufferedWriter(
						"./output/analyzeMz/actstrips_durationsdistances/ch/" + modes[i] + "_durationBinsLeisure.txt");
				outLeisure.write("Duration bin\tMedian\tBin size\n");
				for (int j = 0; j < durationBinsLeisure.get(modes[i]).getMedian().size();  j++) {
					outLeisure.write(j + "\t" 
							+ formatter.format(durationBinsLeisure.get(modes[i]).getMedian().get(j)) + "\t" 
							+ durationBinsLeisure.get(modes[i]).getSizes()[j] + "\n");
				}					
				outLeisure.flush();			
				outLeisure.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void createOutOfHomeBinOutput(MZ mz) {
		TreeMap<String, BinsOld> durationBinsOutOfHome = this.createDurationBins();
		
		Persons persons = mz.getPersons();	
		
		String [] modes = {"pt", "car", "bike", "walk"};		
		for (int i = 0; i < modes.length; i++) {
			
				
			Iterator<PersonTripActs> personActTrips_it = persons.getPersons().values().iterator();
			while (personActTrips_it.hasNext()) {
				PersonTripActs personTripActs = personActTrips_it.next();
				
				// if person is invalid, remove whole person not only the invalid trips
				if (personTripActs.isValid() && personTripActs.getActDurationPerTypeAggregated("leisure").get(modes[i]) > 0.0) {
					
					double outOfHomeActDursExclLeisure = personTripActs.getDurationAllActsAllModesAggregated() - 
					personTripActs.getActDurationPerTypeAggregated("leisure").get(modes[i]);

					durationBinsOutOfHome.get(modes[i]).addVal(outOfHomeActDursExclLeisure, 
							personTripActs.getActDurationPerTypeAggregated("leisure").get(modes[i])/1000.0);
				}
			}
		}
		for (int i = 0; i < 4; i++) {
			String [] categories  = new String[durationBinsOutOfHome.get(modes[i]).getSizes().length];
			for (int j = 0; j < categories.length; j++) {
				categories[j] = Integer.toString(j);
			}
			double binUnit = 24.0 * 60.0 / durationBinsOutOfHome.get(modes[i]).getSizes().length;
			BarChart chartOutofHome = new BarChart(modes[i] + " : out of home acts (excl. leisure) durations", 
					"duration bin [" + binUnit + " min]", "leisure dist [km]", categories);
			chartOutofHome.addSeries("Median out of home act. durations (excl. leisure)", 
					Utils.convert(durationBinsOutOfHome.get(modes[i]).getMedian()));
			chartOutofHome.saveAsPng("./output/analyzeMz/actstrips_durationsdistances/ch/" + modes[i] + "_outOfHomeActDurations.png", 1600, 800);
						
			BarChart chartOutofHomeSize = new BarChart(modes[i] + " : out of home acts (excl. leisure) bin sizes", 
					"duration bin [" + binUnit + " min]", "seize", categories);
			chartOutofHomeSize.addSeries("Seizes", durationBinsOutOfHome.get(modes[i]).getSizes());
			chartOutofHomeSize.saveAsPng("./output/analyzeMz/actstrips_durationsdistances/ch/" + modes[i] + "_outOfHomeActDurationBinSizes.png", 1600, 800);
		}		
	}
	
		
/*	private void writeConvolutionFilesForMatlab(TreeMap<String, NewBins> durations, 
			TreeMap<String, NewBins> distances) {
		
		String [] modes = {"pt", "car", "bike", "walk"};
		for (int i = 0; i < modes.length; i++) {
			
			try {
				BufferedWriter outDistances = IOUtils.getBufferedWriter("./output/analyzeMZ/conv/" + modes[i] +"_distances.txt");
				BufferedWriter outDurations = IOUtils.getBufferedWriter("./output/analyzeMZ/conv/" + modes[i] +"_durations.txt");
				
				String out = "durations = [\t";
				for (int j = 0; j < durations.get(modes[i]).getSizes().length; j++) {
					out += durations.get(modes[i]).getSizes()[j] + "\t";	
				}
				outDurations.write(out + "]");
				
				out = "distances = [\t";
				for (int j = 0; j < distances.get(modes[i]).getSizes().length; j++) {
					out += distances.get(modes[i]).getSizes()[j] + "\t";
				}
				outDistances.write(out + "]");
				
				outDurations.flush();
				outDistances.flush();
				outDurations.close();
				outDistances.close();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}	
	}*/
}
