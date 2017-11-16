package playground.clruch.io.fleet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

import ch.ethz.idsc.queuey.datalys.csv.CSVUtils;

public class NewData {
	
private static BufferedReader reader;

public static void change(File file) throws IOException {
	File inputFile = new File("/home/anape/Documents/BT/taxiTraces/new_abboip.txt");
	File outputFile = new File("/home/anape/Documents/BT/taxiTraces/2new_abboip.txt");
	BufferedReader in = new BufferedReader(new InputStreamReader(new ReverseLineInputStream(inputFile)));
	BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
	
	reader = new BufferedReader(new FileReader(file));
	String line = null;
	StringBuilder stringBuilder = new StringBuilder();
	while ((line = reader.readLine()) != null) {
		stringBuilder.append(line + " ");
	}
	String Header = stringBuilder.toString();	
	bw.write(Header+"\n");
	
	
	 try {
         while (true) {
             String oldline = in.readLine();
             if (Objects.isNull(oldline))
                 break;

             bw.write(oldline + "\n");
         }

     } catch (Exception e) {

     }
	 in.close();
	 bw.close();
	 }
	 
	 public static String convertLine(String line) {
	        List<String> entries = CSVUtils.csvLineToList(line, " ");
	        String newLine = entries.get(0) + " , " + entries.get(1);
	        return newLine;
	    }

}
