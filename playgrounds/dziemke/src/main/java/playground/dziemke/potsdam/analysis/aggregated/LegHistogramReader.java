package playground.dziemke.potsdam.analysis.aggregated;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LegHistogramReader {

	public static List <PairPtCar>  read(String filename){

		List <PairPtCar> modalSplitList = new ArrayList <PairPtCar>();
		
		FileReader fileReader;
		BufferedReader bufferedReader = null;
		
		try {
			fileReader = new FileReader(new File (filename));
			bufferedReader = new BufferedReader(fileReader);
			String line = null;
			String firstLine = bufferedReader.readLine();
			
			String[] firstLineSplit = firstLine.split("\t");
			
			int car = 0;
			int pt = 0;
			
			for (int i=0; i<firstLineSplit.length; i++ ){
				if (firstLineSplit[i].equals("arrivals_car")){
					car = i;
				}
				if (firstLineSplit[i].equals("arrivals_pt")){
					pt = i;
				}
			}
			
			while ((line = bufferedReader.readLine()) != null) {
				String[] currentLineSplit = line.split("\t");
				PairPtCar currentPtCar = new PairPtCar(Integer.parseInt(currentLineSplit[pt]),
												Integer.parseInt(currentLineSplit[car]));
				modalSplitList.add(currentPtCar);
			}
		
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
			bufferedReader.close();
		} catch (IOException ex) {
            ex.printStackTrace();
		}
	}
	
	return modalSplitList;
	}
}