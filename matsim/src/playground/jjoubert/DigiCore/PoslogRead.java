package playground.jjoubert.DigiCore;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Formatter;
import java.util.Scanner;

public class PoslogRead {
	// fields
	private Formatter output;
	private Scanner input;
	private int numberOfLines;

	// constructor
	public PoslogRead(String inputFile, String outputFile, int numberOfLines) throws FileNotFoundException{
		this.numberOfLines = numberOfLines;
		input = new Scanner(new File(inputFile));
		output = new Formatter(outputFile);
	}
	
	// main method
	public static void main(String[] args) throws FileNotFoundException {
		String inFile = "/Volumes/Data/DigiCore/Poslog_Research_Data.csv";
		String outFile = "/Volumes/Data/DigiCore/Poslog10000Lines.txt";
		new PoslogRead(inFile,outFile,10000).readAndWrite();
	}

	private void readAndWrite() {
		for (int i=0; i<this.numberOfLines;i++){
			output.format("%s\n", input.nextLine());
		}
		closeFiles();
	}

	private void closeFiles() {
		if(input != null)
			input.close();
		if (output != null)
			output.close();
	}
}
