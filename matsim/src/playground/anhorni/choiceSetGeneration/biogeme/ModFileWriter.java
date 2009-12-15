package playground.anhorni.choiceSetGeneration.biogeme;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;


public class ModFileWriter {

	/**
	 * @param args	0: input file containing the variables to be included in the modfile
	 * 				1: output file
	 */
	public static void main(String[] args) {
		
		if (args.length != 2) {
			System.out.println("Too few or too many arguments. Exit");
			System.exit(1);
		}
		String inputFile = args[0];
		String outputFile = args[1];
		Gbl.startMeasurement();

		ModFileWriter writer = new ModFileWriter();
		writer.writeModFile(inputFile, outputFile);
		Gbl.printElapsedTime();
	}
	
	
	public void writeModFile(String inputFile, String outputFile) {
		String [] variables = this.getVariables(inputFile);
		
		String openingBlock="[Choice]\n" +
			"Choice\n\n" +
			"//MODEL DESCRIPTION\n" +
			"//" + this.getModelDescription(inputFile) + "\n" + 
			"\n" +
			"[Beta]\n" +
			"//Name\tValue\tLower Bound\tUpperBound\tstatus (0=variable, 1=fixed)\n";
	
		for (int i = 0; i < variables.length; i++) {
			openingBlock += "B_" + variables[i] + "\t0\t-10\t10\t0\n";
		}	
		openingBlock += "\n";
		
		openingBlock += "[Mu]\n" + "// In general, the value of mu must be fixed to 1. For testing purposes, you\n" +
			"// may change its value or let it be estimated.\n" +
			"// Value\tLowerBound\tUpperBound\tStatus\n" +
			"1	0	1	1\n" +
			"\n" +
			"[Utilities]\n" +
			"//Id\tName\tAvail\tlinear-in-parameter expression (beta1*x1 + beta2*x2 + ...)\n";
				
		String closingBlock = "[Expressions]\n" +
			"one = 1\n" +
			"\n" +
			"//[Exclude]\n" +
			"//PURPOSE != 1\n" +
			"\n" +
			"[Model]\n" +
			"$MNL  // Multinomial Logit Model\n";
		
		try {					
			final BufferedWriter out = IOUtils.getBufferedWriter(outputFile);
			out.write(openingBlock);
			
			for (int j = 0; j < 1250; j++) {
				String line = j + "\t" + "SH" + j + "\t" + "SH" + j + "_AV\t";
				
				for (int i = 0; i < variables.length; i++) {
					if (i > 0) {
						line += " + ";
					}
					line += "B_" + variables[i] + " * SH" + j + "_" + variables[i];
				}
				out.write(line);
				out.newLine();
				out.flush();
			}
			
			out.newLine(); out.newLine();
			
			out.write(closingBlock);
			out.flush();				
			out.close();
						
		} catch (final IOException e) {
				Gbl.errorMsg(e);
		}		
	}
	
	private String getModelDescription(String inputFile) {	
		String[] entries = null;
		try {
			FileReader fileReader = new FileReader(inputFile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String curr_line = bufferedReader.readLine();	
			entries = curr_line.split("\t", -1);
			bufferedReader.close();
			fileReader.close();
		
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		return entries[0];		
	}
	
	private String [] getVariables(String inputFile) {	
		String[] entries = null;
		try {
			FileReader fileReader = new FileReader(inputFile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			//skip first line (model description)
			String curr_line = bufferedReader.readLine();
			
			curr_line = bufferedReader.readLine();
			entries = curr_line.split("\t", -1);
			bufferedReader.close();
			fileReader.close();
		
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		return entries;		
	}

}
