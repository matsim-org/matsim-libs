package playground.dziemke.potsdam.analysis.aggregated;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class ModalSplitWriter {

    public static void writeToFile(String filename, Map <Integer, PairPtCar> modalSplit  ) {
        
		try {
				FileWriter fw = new FileWriter(filename);
				fw.append("Iteration" + ";" + "arrivals_car" + ";" + "arrivals_pt" + ";\n");
	
				for (int i : modalSplit.keySet()){
						fw.append(i + ";" + modalSplit.get(i).getArrivals_car() + ";"
									+ modalSplit.get(i).getArrivals_pt() +";\n");
					}
								
		fw.flush();
		fw.close();
		} catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
    }
}