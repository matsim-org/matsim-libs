package playground.dziemke.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.matsim.api.core.v01.Id;

public class AnalysisFileWriter {
	
	
	public void writeToFileIntegerKey(Map<Integer, Double> map, String outputFile, int binWidth, double aggregateWeight, double average) {
		BufferedWriter bufferedWriter = null;
		
		try {
            File output = new File(outputFile);
    		FileWriter fileWriter = new FileWriter(output);
    		bufferedWriter = new BufferedWriter(fileWriter);
    		
    		double writeCounter = 0.;
    		
    		for (int key : map.keySet()) {
    			int binCaption = key * binWidth;
    			double value = map.get(key);
    			bufferedWriter.write(binCaption + "+" + "\t" + value + "\t" + value/aggregateWeight);
    			writeCounter = writeCounter + value;
    			bufferedWriter.newLine();
    		}
    		bufferedWriter.write("Average = " + average);
			bufferedWriter.newLine();
			bufferedWriter.write("Sum = " + writeCounter);
    		
			double countDifference = Math.abs(writeCounter - aggregateWeight);
    		if (countDifference >1.) {
    			System.err.println("Weighted number of trips in " + outputFile + " is not equal to aggregate weight!");
    			System.err.println("writeCounter: " + writeCounter + "; aggregateWeight: " + aggregateWeight);
    		}
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
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
		System.out.println("Analysis file " + outputFile + " written.");
	}
	
	
	// New: For cummulative Plots
	//-----------------------------------------------------------------------------------------------------------------------
	public void writeToFileIntegerKeyCumulative(Map<Integer, Double> map, String outputFile, int binWidth, double aggregateWeight, double average) {
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
    			System.err.println("Weighted number of trips in " + outputFile + " is not equal to aggregate weight!");
    			System.err.println("writeCounter: " + writeCounter + "; aggregateWeight: " + aggregateWeight);
    		}
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
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
		System.out.println("Analysis file " + outputFile + " written.");
	}
	//-----------------------------------------------------------------------------------------------------------------------
	
	
	// New: Other information
	//-----------------------------------------------------------------------------------------------------------------------
	public void writeToFileOther(Map<String, Integer> map, String outputFile) {
		BufferedWriter bufferedWriter = null;
			
		try {
			File output = new File(outputFile);
	    	FileWriter fileWriter = new FileWriter(output);
	    	bufferedWriter = new BufferedWriter(fileWriter);
	    		
	    	for (String key : map.keySet()) {
    			int value = map.get(key);
    			bufferedWriter.write(key + "\t" + value);
    			bufferedWriter.newLine();
	    	}
    		
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
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
			System.out.println("Analysis file " + outputFile + " written.");
		}
		//-----------------------------------------------------------------------------------------------------------------------
	
	
	// file writer for data that has a string (e.g. an activity name) as key
	public void writeToFileStringKey(Map<String, Double> map, String outputFile, double aggregateWeight) {
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
    		if (countDifference >1.) {
    			System.err.println("Weighted number of trips in " + outputFile + " is not equal to aggregate weight!");
    			System.err.println("writeCounter: " + writeCounter + "; aggregateWeight: " + aggregateWeight);
    		}
	    } catch (FileNotFoundException ex) {
	        ex.printStackTrace();
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
		System.out.println("Analysis file " + outputFile + " written.");
	}
	
	
	// file writer for comparison file routed distance vs. beeline distance
	public void writeComparisonFile(Map<Id<Trip>, Double> mapRouted, Map<Id<Trip>, Double> mapBeeline, String outputFile, int tripCounter) {
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
	    			    			
	    			bufferedWriter.write(tripId + "\t" + distanceRouted + "\t" + distanceBeeline + "\t" + ratioRoutedBeeline);
	    			mapEntryCounter++;
	    			bufferedWriter.newLine();
	    			
	    			if (distanceRouted < minDistanceRouted) { minDistanceRouted = distanceRouted; }
	    			if (distanceRouted > maxDistanceRouted) { maxDistanceRouted = distanceRouted; }
	    			if (distanceBeeline < minDistanceBeeline) { minDistanceBeeline = distanceBeeline; }
	    			if (distanceBeeline > maxDistanceBeeline) { maxDistanceBeeline = distanceBeeline; }
	    			if (distanceBeeline > 1 && distanceRouted > 1) {
	    				aggregateRatioRoutedBeeline = aggregateRatioRoutedBeeline + ratioRoutedBeeline;
	    				counter++;
	    				if (ratioRoutedBeeline < minRatioBeeline) { minRatioBeeline = ratioRoutedBeeline; }
	    				if (ratioRoutedBeeline > maxRatioBeeline) { maxRatioBeeline = ratioRoutedBeeline; }
	    			}
    			}
     		}
    		bufferedWriter.write("Number of map entries = " + "\t" + mapEntryCounter);
    		    		
    		if (mapEntryCounter != tripCounter) {
    			System.err.println("Number of map entries in " + outputFile + " is not equal to number of trips!");
    		}
    		double averageRatioRoutedBeeline = aggregateRatioRoutedBeeline / counter;
    		
    		System.out.println("Minimum routed distance is = " + minDistanceRouted);
    		System.out.println("Maximum routed distance is = " + maxDistanceRouted);
    		System.out.println("Minimum beeline distance is = " + minDistanceBeeline);
    		System.out.println("Maximum beeline distance is = " + maxDistanceBeeline);
    		System.out.println("Minimum ratio routed/beeline distance is = " + minRatioBeeline);
    		System.out.println("Average ratio routed/beeline distance is = " + averageRatioRoutedBeeline);
    		System.out.println("Maximum ratio routed/beeline distance is = " + maxRatioBeeline);
    		
	    } catch (FileNotFoundException ex) {
	        ex.printStackTrace();
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
		System.out.println("Analysis file " + outputFile + " written.");
	}
}
