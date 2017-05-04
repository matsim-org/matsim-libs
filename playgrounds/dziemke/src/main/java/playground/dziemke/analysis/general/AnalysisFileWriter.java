package playground.dziemke.analysis.general;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class AnalysisFileWriter {
	private static final Logger log = Logger.getLogger(AnalysisFileWriter.class);
	
	public static void writeToFileIntegerKey(Map<Integer, Double> map, String outputFile, int binWidth, double aggregateWeight, double average) {
		BufferedWriter bufferedWriter = null;
		
		try {
            File output = new File(outputFile);
    		FileWriter fileWriter = new FileWriter(output);
    		bufferedWriter = new BufferedWriter(fileWriter);
    		
    		double writeCounter = 0.;
    		
    		for (int key : map.keySet()) {
    			int binCaption = key * binWidth;
    			double value = map.get(key);
    			bufferedWriter.write(binCaption + "\t" + value + "\t" + value/aggregateWeight);
    			writeCounter = writeCounter + value;
    			bufferedWriter.newLine();
    		}
    		bufferedWriter.write("Average = " + average);
			bufferedWriter.newLine();
			bufferedWriter.write("Sum = " + writeCounter);
    		
			double countDifference = Math.abs(writeCounter - aggregateWeight);
    		if (countDifference >1.) {
    			log.error("Weighted number of trips in " + outputFile + " is not equal to aggregate weight!");
    			log.error("writeCounter: " + writeCounter + "; aggregateWeight: " + aggregateWeight);
    		}
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.flush();
                    bufferedWriter.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
		log.info("Analysis file " + outputFile + " written.");
	}
	
	
	public static void writeToFileIntegerKeyCumulative(Map<Integer, Double> map, String outputFile, int binWidth, double aggregateWeight, double average) {
		BufferedWriter bufferedWriter = null;
		
		try {
            File output = new File(outputFile);
    		FileWriter fileWriter = new FileWriter(output);
    		bufferedWriter = new BufferedWriter(fileWriter);
    		
    		double writeCounter = 0.;
    		double cumulativeValue = 0.;
    		
    		for (int key : map.keySet()) {
    			int binCaption = key * binWidth;
    			double value = map.get(key);
    			cumulativeValue = cumulativeValue + value;
    			bufferedWriter.write(binCaption + "\t" + cumulativeValue + "\t" + cumulativeValue/aggregateWeight);
    			writeCounter = writeCounter + value;
    			bufferedWriter.newLine();
    		}
    		bufferedWriter.write("Average = " + average);
			bufferedWriter.newLine();
			bufferedWriter.write("Sum = " + writeCounter);
    		
			double countDifference = Math.abs(writeCounter - aggregateWeight);
    		if (countDifference >1.) {
    			log.error("Weighted number of trips in " + outputFile + " is not equal to aggregate weight!");
    			log.error("writeCounter: " + writeCounter + "; aggregateWeight: " + aggregateWeight);
    		}
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.flush();
                    bufferedWriter.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
		log.info("Analysis file " + outputFile + " written.");
	}
	
	
	public static void writeToFileOther(Map<String, Double> map, String outputFile) {
		BufferedWriter bufferedWriter = null;

		try {
			File output = new File(outputFile);
			FileWriter fileWriter = new FileWriter(output);
			bufferedWriter = new BufferedWriter(fileWriter);

			for (String key : map.keySet()) {
				double value = map.get(key);
				bufferedWriter.write(key + "\t" + value);
				bufferedWriter.newLine();
			}

            bufferedWriter.write(createVersionControlIdentifier());
			bufferedWriter.newLine();

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			try {
				if (bufferedWriter != null) {
					bufferedWriter.flush();
					bufferedWriter.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		log.info("Analysis file " + outputFile + " written.");
	}
	
	private static String createVersionControlIdentifier() {
	    String identifier = "Created with: ";
	    identifier += AnalysisFileWriter.class.getName();
	    identifier += " on ";
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        identifier += dateFormat.format(new Date());
        return identifier;
    }

	// file writer for data that has a string (e.g. an activity name) as key
	public static void writeToFileStringKey(Map<String, Double> map, String outputFile, double aggregateWeight) {
		BufferedWriter bufferedWriter = null;
		
		try {
	        File output = new File(outputFile);
			FileWriter fileWriter = new FileWriter(output);
			bufferedWriter = new BufferedWriter(fileWriter);
		
			//int writeCounter = 0;
			double writeCounter = 0;
    		
    		for (String key : map.keySet()) {
    			double value = map.get(key);
    			bufferedWriter.write(key + "\t" + value + "\t" + value/aggregateWeight);
    			// writeCounter++;
    			writeCounter = writeCounter + value;
    			bufferedWriter.newLine();
    		}
    		//bufferedWriter.write("Sum = " + writeCounter);
    		
    		double countDifference = Math.abs(writeCounter - aggregateWeight);
    		if (countDifference > 1.) {
    			log.error("Weighted number of trips in " + outputFile + " is not equal to aggregate weight!");
    			log.error("writeCounter: " + writeCounter + "; aggregateWeight: " + aggregateWeight);
    		}
	    } catch (IOException ex) {
	        ex.printStackTrace();
	    } finally {
	        try {
	            if (bufferedWriter != null) {
	                bufferedWriter.flush();
	                bufferedWriter.close();
	            }
	        } catch (IOException ex) {
	            ex.printStackTrace();
	        }
	    }
		log.info("Analysis file " + outputFile + " written.");
	}
	
	
	// file writer for comparison file routed distance vs. beeline distance
    public static void writeRoutedBeelineDistanceComparisonFile(Map<Id<Trip>, Double> mapRouted, Map<Id<Trip>, Double> mapBeeline, String outputFile, double tripCounter) {
		BufferedWriter bufferedWriter = null;
		
		try {
	        File output = new File(outputFile);
			FileWriter fileWriter = new FileWriter(output);
			bufferedWriter = new BufferedWriter(fileWriter);
		
			int mapEntryCounter = 0;
			int counter = 0;
			double minDistanceRouted = Double.POSITIVE_INFINITY;
			double maxDistanceRouted = Double.NEGATIVE_INFINITY;
			double minDistanceBeeline = Double.POSITIVE_INFINITY;
			double maxDistanceBeeline = Double.NEGATIVE_INFINITY;
			double minRatioBeeline = Double.POSITIVE_INFINITY;
			double maxRatioBeeline = Double.NEGATIVE_INFINITY;
			double aggregateRatioRoutedBeeline = 0.;
    		
    		for (Id<Trip> tripId : mapRouted.keySet()) {
    			if (mapBeeline.containsKey(tripId)) {
	    			double distanceRouted = mapRouted.get(tripId);
	    			double distanceBeeline = mapBeeline.get(tripId);
	    			double ratioRoutedBeeline = distanceRouted / distanceBeeline;
	    			
	    			// not that calcuations which do not make sense are written here to the lines in the files
	    			// but not included into the below calculation of the average
	    			bufferedWriter.write(tripId + "\t" + distanceRouted + "\t" + distanceBeeline + "\t" + ratioRoutedBeeline);
	    			mapEntryCounter++;
	    			bufferedWriter.newLine();
	    			
	    			// adjust minimum and maximum distances if new minumum or maximum is found
	    			if (distanceRouted < minDistanceRouted) { minDistanceRouted = distanceRouted; }
	    			if (distanceRouted > maxDistanceRouted) { maxDistanceRouted = distanceRouted; }
	    			if (distanceBeeline < minDistanceBeeline) { minDistanceBeeline = distanceBeeline; }
	    			if (distanceBeeline > maxDistanceBeeline) { maxDistanceBeeline = distanceBeeline; }
	    			
	    			// only consider trips of a distance of at least 1km for calculation of average ratio
	    			// this also precludes erroneous calculations based on non-existent distances
	    			if (distanceBeeline > 1. && distanceRouted > 1.) {
	    				aggregateRatioRoutedBeeline = aggregateRatioRoutedBeeline + ratioRoutedBeeline;
	    				counter++;
	    				if (ratioRoutedBeeline < minRatioBeeline) { minRatioBeeline = ratioRoutedBeeline; }
	    				if (ratioRoutedBeeline > maxRatioBeeline) { maxRatioBeeline = ratioRoutedBeeline; }
	    			}
    			}
     		}
    		bufferedWriter.write("Number of map entries = " + mapEntryCounter);
    		bufferedWriter.newLine();
    		    		
    		if (mapEntryCounter != tripCounter) {
    			Log.error("Number of map entries in " + outputFile + " is not equal to number of trips!");
    		}
    		
    		//
    		double averageRatioRoutedBeeline = aggregateRatioRoutedBeeline / counter;
    		
    		bufferedWriter.write("Minimum routed distance is = " + minDistanceRouted);
    		bufferedWriter.newLine();
    		bufferedWriter.write("Maximum routed distance is = " + maxDistanceRouted);
    		bufferedWriter.newLine();
    		bufferedWriter.write("Minimum beeline distance is = " + minDistanceBeeline);
    		bufferedWriter.newLine();
    		bufferedWriter.write("Maximum beeline distance is = " + maxDistanceBeeline);
    		bufferedWriter.newLine();
    		bufferedWriter.write("Minimum ratio routed/beeline distance is = " + minRatioBeeline);
    		bufferedWriter.newLine();
    		bufferedWriter.write("Average ratio routed/beeline distance is = " + averageRatioRoutedBeeline);
    		bufferedWriter.newLine();
    		bufferedWriter.write("Maximum ratio routed/beeline distance is = " + maxRatioBeeline);
    		
	    } catch (IOException ex) {
	        ex.printStackTrace();
	    } finally {
	        try {
	            if (bufferedWriter != null) {
	                bufferedWriter.flush();
	                bufferedWriter.close();
	            }
	        } catch (IOException ex) {
	            ex.printStackTrace();
	        }
	    }
		log.info("Analysis file " + outputFile + " written.");
	}
}