package playground.vsp.andreas.utils.var;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

public class FileComparison {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			System.err.println("Need two input files.");
			System.exit(0);
		}
		
		String fileOne = args[0];
		String fileTwo = args[1];
		
		BufferedReader readerOne = IOUtils.getBufferedReader(fileOne);
		BufferedReader readerTwo = IOUtils.getBufferedReader(fileTwo);
		
		String outOne = fileOne + ".out";
		String outTwo = fileTwo + ".out";
		
		BufferedWriter writerOne = IOUtils.getBufferedWriter(outOne);
		BufferedWriter writerTwo = IOUtils.getBufferedWriter(outTwo);
		
		String lineOne;
		String lineTwo;
		
		int lineNumber = 1;
		
		try {
			lineOne = readerOne.readLine();
			lineTwo = readerTwo.readLine();
			
			while (lineOne != null && lineTwo != null) {
				
				if (!lineOne.equals(lineTwo)) {
					writerOne.write(lineNumber + ": " + lineOne); writerOne.newLine();
					writerTwo.write(lineNumber + ": " + lineTwo); writerTwo.newLine();
				}
				
				lineOne = readerOne.readLine();
				lineTwo = readerTwo.readLine();
				lineNumber++;
			}
			
			writerOne.flush(); writerOne.close();
			writerTwo.flush(); writerTwo.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
