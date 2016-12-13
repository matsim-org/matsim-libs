package playground.gthunig.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;

public class BigFileReader {

	private final static Logger log = Logger.getLogger(BigFileReader.class);
	
	public static void main(String[] args) throws IOException {
//		String fileName = "C:/Users/gthunig/SVN/shared-svn/projects/cemdapMatsimCadyts/scenario/cemdap2matsim/100_1/plans.xml";
		String fileName = "C:/Users/gthunig/CEMDAP/cemdap_output/100/stops.out1";
		
		findEntriesWithSubstring(fileName, "1717337");//_171733701");
		
//		int numberOfLines = 300;
//		showFirstLinesOfFile(fileName, numberOfLines);
		
	}
	
	public static void showFirstLinesOfFile(String fileName, int numberOfLines) throws IOException {
		log.info("Start showing lines");
		
		log.info("Trying to read " + fileName);
		FileReader fileReader = new FileReader(fileName);
		BufferedReader reader = new BufferedReader(fileReader);
		String line = reader.readLine();
		for (int i=0; i<numberOfLines; i++) {
			System.out.println(line);
			line = reader.readLine();
		}
		reader.close();
	}
	
	public static void showLastLinesOfFile(String fileName, int numberOfLines) throws IOException {
		//TODO: Test this method
		log.info("Start showing lines");
		
		log.info("Trying to read " + fileName);
		FileReader fileReader = new FileReader(fileName);
		BufferedReader reader = new BufferedReader(fileReader);
		String line = reader.readLine();
		
		String[] lines = new String[numberOfLines];
		int i = 0;
		while((line=reader.readLine())!=null) {
			lines[i] = line;
			i++;
			if (i >= numberOfLines) i = 0;
		}
		
		for (int e = i; e<lines.length; e++) {
			System.out.println(lines[e]);
		}
		for (int e = 0; e<i; e++) {
			System.out.println(lines[e]);
		}
		
		reader.close();
	}

	public static void findEntriesWithSubstring(String fileName, String substring) throws IOException {
		log.info("Start finding entries");
		
		log.info("Trying to read " + fileName);
		FileReader fileReader = new FileReader(fileName);
		BufferedReader reader = new BufferedReader(fileReader);
		String line = reader.readLine();
		while((line=reader.readLine())!=null) {
			if (line.contains(substring)) {
				System.out.println(line);
			}
		}
		reader.close();
	}
	
}
