package playgroundMeng.plansAnalysis;

import java.awt.geom.Line2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class PlansInfoReader {
	
	public static void main(String[] args) throws IOException {
		String outputTextString = "C:/Users/VW3RCOM/Desktop/outputWriter.xml";
		String zeroOutputTextString = "C:/Users/VW3RCOM/Desktop/0.OutputWriter.xml";
		String InputString = "C:/Users/VW3RCOM/Desktop/InputWriter.xml";
		
		
		String scoreChangeAgent = "C:/Users/VW3RCOM/Desktop/ScoreChangeAgentInputAnd0.xml";
		
		File file1 = new File(InputString);
		File file2 = new File(zeroOutputTextString);
		File file3 = new File(scoreChangeAgent);
		
		Reader fileReader1 = new FileReader(file1);
		Reader fileReader2 = new FileReader(file2);
		Writer filewWriter = new FileWriter(file3);
		
		BufferedReader bufferedReader1 = new BufferedReader(fileReader1);
		BufferedReader bufferedReader2 = new BufferedReader(fileReader2);
		BufferedWriter bufferedWriter = new BufferedWriter(filewWriter);
		
		String line1 = bufferedReader1.readLine();
		String line2 = bufferedReader2.readLine();
		
		System.out.println("Beginn");
		while(line1 != null) {
			String[] array1 = line1.split("\\+");
			String[] array2 = line2.split("\\+");
			if(!array1[0].equals(array2[0])) {
				System.out.println(array1[0]);
				System.out.println(array2[0]);
				throw new RuntimeException();
			} else if (!array1[2].equals(array2[2])) {
				bufferedWriter.write(line1);
				bufferedWriter.newLine();
				bufferedWriter.write(line2);
				bufferedWriter.newLine();
				bufferedWriter.write(" ");
				bufferedWriter.newLine();	
			} else if (!array1[1].equals(array2[1])) {
				bufferedWriter.write(array1[0] + " " + array1[1]);
				bufferedWriter.newLine();
				bufferedWriter.write(array2[0] + " " + array2[1]);
				bufferedWriter.newLine();
				bufferedWriter.write(" ");
				bufferedWriter.newLine();	
			}
//			if(!line1.toString().equals(line2.toString())) {
//				bufferedWriter.write(line1);
//				bufferedWriter.newLine();
//				bufferedWriter.write(line2);
//				bufferedWriter.newLine();
//				bufferedWriter.write(" ");
//				bufferedWriter.newLine();	
//			}
			 line1 = bufferedReader1.readLine();
			 line2 = bufferedReader2.readLine();
		}
		System.out.println("finish");
			
	}
	

}
