 package playground.dziemke.potsdam.analysis.aggregated;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PotsdamModalSplitAnalyzer {
		
	
	public static void main(String[] args) {
		int numberOfIterations = 150;
		// String numberOfRun = "run_x11b";
		String numberOfRun = "run_x7b";

		Map <Integer, PairPtCar> sum = new HashMap <Integer, PairPtCar>();
		
		for (int i=0; i<=numberOfIterations; i++ ){
			
			List <PairPtCar> modalSplitList = LegHistogramReader.read("D:/Workspace/container/potsdam-pg/output/" + numberOfRun + "/ITERS/it." + i + "/" + i
					+ ".legHistogram.txt");
			
			PairPtCar initial = new PairPtCar(0, 0);
			sum.put(i, initial);
			int pt = 0;	
			int car = 0;
			
			for (int j=0; j<modalSplitList.size(); j++){
				int currentPt = modalSplitList.get(j).getArrivals_pt();
				int currentCar = modalSplitList.get(j).getArrivals_car();
				pt = pt + currentPt;
				car = car + currentCar;
				PairPtCar currentMS = new PairPtCar(pt, car);
				
				sum.put(i, currentMS)	;

			}
		}

			String filename = "D:/Workspace/container/potsdam-pg/output/" + numberOfRun + "/modalsplit_" + numberOfRun + ".csv";
			ModalSplitWriter.writeToFile(filename, sum);
			System.out.println("Der Modal Split wurde nach " + filename + " geschrieben.");	
	}
}