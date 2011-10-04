package playground.ikaddoura.analysis.beeline.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class TextFileWriter {

	public void writeFile(String input, List<Double> luftlinien, List<Double> distances, String modeName){
		File file = new File("output_planFileAnalysis/analyse_"+input+"_"+modeName+".txt");
		   
	    try {
	    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	    String zeile1 = "analyzed PlansFile: "+input;
	    String zeile2 = "LegNumber ; Luftlinie ; Distance; Verhältnis (Luftlinie/Distance)";

	    bw.write(zeile1);
	    bw.newLine();
	    bw.write(zeile2);
	    bw.newLine();

	    int n = 0;
	    for (Double luftlinie : luftlinien){
	    	String zeile = n+" ; "+luftlinien.get(n).toString()+" ; "+distances.get(n).toString()+" ; "+(distances.get(n)/luftlinien.get(n));
	    	bw.write(zeile);
	    	bw.newLine();
	    	n++;
	    }
	    bw.flush();
	    bw.close();
	    System.out.println("File "+file.toString()+" geschrieben");

	    } catch (IOException e) {}
	
	}
}
