package playground.dziemke.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class VolumeDifferenceAnalyzer implements Runnable{
  
//	private String runId = "57";
	private String runId = "60b";
//	private String fileBase = "D:/Workspace/container/examples/equil/output/" + runId + "/";
	private String fileBase = "/Users/dominik/Workspace/data/examples/equil/output/" + runId + "/";
	private int numberOfIterations = 200;
	
	private Map <Integer, Map<Integer, Double>> counts = new HashMap <Integer, Map<Integer, Double>>();
		

	public static void main(String[] args) {
		VolumeDifferenceAnalyzer analyzer = new VolumeDifferenceAnalyzer();
		analyzer.run();
	}

	
	@Override
	public void run() {
		readIn();
		writeFile();
	}
	
	
	private void readIn() {
		int lastIteration = this.numberOfIterations;
		int i = 10;
		
		while (i <= lastIteration) {
			// List <Counts> currentCounts = CountsReader.read(i, fileBase + "ITERS/it." + i + "/" + i + ".countscompareAWTV.txt");
			String fileName = fileBase + "ITERS/it." + i + "/" + runId + "." + i + ".countscompareAWTV.txt";
			Map <Integer, Double> count = new HashMap <Integer, Double>();
			
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(fileName));
				reader.readLine();
				String line = null;
				int j = 0;
				while ((line = reader.readLine()) != null) {
					String[] parts = line.split("\t");
					
					int linkId = Integer.parseInt(parts[0]);
					double matsimVolume = Double.parseDouble(parts[1]);
					double countVolume = Double.parseDouble(parts[2]);
					double difference = Math.round((matsimVolume - countVolume)*100)/100.;
										
					count.put(linkId, difference);
					j++;
				}
				this.counts.put(i, count);
				
				System.out.println("Es wurden in Iteration " + i + " insgesamt " + j + " Counts eingelesen.");
				i = i + 10;
						
			} catch (FileNotFoundException e) {
				System.err.println("File not found...");
					e.printStackTrace();
			} catch (NumberFormatException e) {
				System.err.println("Wrong No. format...");
				e.printStackTrace();
			} catch (IOException e) {
				System.err.println("I/O error...");
				e.printStackTrace();
			} finally {
				try {
		            reader.close();
				} catch (IOException ex) {
		            ex.printStackTrace();
		        }
			}
		}
	}
		
		
	public void writeFile() {
		BufferedWriter bufferedWriter = null;
		
		try {
            String outputFileName = fileBase + "analysis.txt";
			File outputFile = new File(outputFileName);
    		FileWriter fileWriter = new FileWriter(outputFile);
    		bufferedWriter = new BufferedWriter(fileWriter);
    		
    		int lastIteration = this.numberOfIterations;
    		int i = 10;
    		
    		while (i <= lastIteration) {
    		// for (Integer iteration : this.counts.keySet()) {
    			double difference14 = this.counts.get(i).get(14).doubleValue();
    			double difference15 = this.counts.get(i).get(15).doubleValue();
    			double difference16 = this.counts.get(i).get(16).doubleValue();
    			double difference21 = this.counts.get(i).get(21).doubleValue();
    			
    			bufferedWriter.write(i + "\t" + difference14 + "\t" + difference15 + "\t" + difference16 + "\t" + difference21);
	    		bufferedWriter.newLine();
	    		i = i + 10;
    		}		
		} catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            //Close the BufferedWriter
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.flush();
                    bufferedWriter.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
		System.out.println("Analyse geschrieben.");
    }
	
}