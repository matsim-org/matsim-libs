package playground.balac.sbbproject;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.core.utils.io.IOUtils;

public class ReadSBBData {
	
	private String inputFile1;
	private String inputFile2;

	private Map<String, ArrayList<TrainDataPerSegment>> trainDataMap;
	
	public ReadSBBData(String inputFile1, String inputFile2) {
		this.inputFile1 = inputFile1;
		this.inputFile2 = inputFile2;
		this.trainDataMap = new HashMap<String, ArrayList<TrainDataPerSegment>>();
		
	}
	
	public void read() throws IOException {
		
		BufferedReader readLink = IOUtils.getBufferedReader(inputFile1);
		String s = readLink.readLine();
		//s = readLink.readLine();
		
		while(s != null) {
			
			String[] arr = s.split(",");
			if (arr[13].equals("1")) {
				if (this.trainDataMap.containsKey(arr[1])) {
					
					TrainDataPerSegment newTrainData = new TrainDataPerSegment(Integer.parseInt(arr[1]), arr[3], arr[6], arr[7], arr[11], arr[12], arr[14], arr[15]);
					ArrayList<TrainDataPerSegment> previousSegment = this.trainDataMap.get(arr[1]);

					if (!previousSegment.get(previousSegment.size() - 1).getDidokFrom().equals(newTrainData.getDidokFrom()) ||
							!previousSegment.get(previousSegment.size() - 1).getDidokTo().equals(newTrainData.getDidokTo()))
					trainDataMap.get(arr[1]).add(newTrainData);
					
				}
				else {
					
					ArrayList<TrainDataPerSegment> newElement = new ArrayList<TrainDataPerSegment>();
					
					TrainDataPerSegment newTrainData = new TrainDataPerSegment(Integer.parseInt(arr[1]), arr[3], arr[6], arr[7], arr[11], arr[12], arr[14], arr[15]);

					newElement.add(newTrainData);
					
					trainDataMap.put(arr[1], newElement);
					
					
				}
			}
			
			s = readLink.readLine();
			
		}
		
		 readLink = IOUtils.getBufferedReader(inputFile2);
		 s = readLink.readLine();
		//s = readLink.readLine();
		
		while(s != null) {
			
			String[] arr = s.split(",");
			if (arr[13].equals("1")) {
				
				String[] number = (arr[1].split("\\."));
				
				if (this.trainDataMap.containsKey(number[0])) {
					
					TrainDataPerSegment newTrainData = new TrainDataPerSegment((int)Double.parseDouble(arr[1]), arr[3], (arr[6].split("\\."))[0], (arr[7].split("\\."))[0], arr[11], arr[12], arr[14], arr[15]);
					ArrayList<TrainDataPerSegment> previousSegment = this.trainDataMap.get(number[0]);
					if (!previousSegment.get(previousSegment.size() - 1).getDidokFrom().equals(newTrainData.getDidokFrom()) ||
							!previousSegment.get(previousSegment.size() - 1).getDidokTo().equals(newTrainData.getDidokTo()))
					
					trainDataMap.get(number[0]).add(newTrainData);
					
				}
				else {
					
					ArrayList<TrainDataPerSegment> newElement = new ArrayList<TrainDataPerSegment>();
					
					TrainDataPerSegment newTrainData = new TrainDataPerSegment((int)Double.parseDouble(arr[1]), arr[3], (arr[6].split("\\."))[0], (arr[7].split("\\."))[0], arr[11], arr[12], arr[14], arr[15]);

					newElement.add(newTrainData);
					
					trainDataMap.put(number[0], newElement);
					
					
				}
			}
			
			s = readLink.readLine();
			
		}
		
		
		
	}
	
	public ArrayList<TrainDataPerSegment> getTrainData (String trainNumber) {
		
		return this.trainDataMap.get(trainNumber);
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
