package playground.gleich.analyzer.analysisRunner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 
 * Creates a working copy of Latex and RScript Files for the interpretation of
 * vsp analyzer results and runs R and Latex on the files in order to create an
 * automatic report on the analysis results as .pdf.
 * <p>
 * While copying Latex and RScript Files, existing files are kept and not 
 * replaced, so user modifications are kept. However, this means that the Latex
 * and RScript Files are only updated, when another working directory is chosen,
 * where these files do not exist yet.
 * 
 * @author gleich
 * 
 * @param <b>workingDirectory</b>: should include the analysis results in the
 * directory structure used in the outputDirectory of the
 * RunAnalyzer Class
 * @param pathToRScriptFiles
 * @param pathToLatexFiles
 * @param pathToRScriptExe
 * @param pathToPdfLatexExe
 *
 */
public class ReportGenerator {
	
	String workingDirectory = "Z:/WinHome/MATSimAnalyzer";//"Z:/WinHome/MATSimAnalyzer" "Z:/WinHome/ArbeitWorkspace/Analyzer/output/workingDirectory"
	String pathToRScriptFiles = "Z:/WinHome/ArbeitWorkspace/Analyzer/output/Rscripts";
	String pathToLatexFiles = "Z:/WinHome/ArbeitWorkspace/Analyzer/output/Latex";
	String pathToRScriptExe = "C:/Program Files/R/R-2.14.2/bin/Rscript.exe";//linux Rscript 
	String pathToPdfLatexExe = "pdflatex";//Linux /usr/bin/pdflatex

	/**
	 * @param args
	 * @throws IOException 
	 */
	
	public static void main(String[] args) throws IOException {
		ReportGenerator reportGenerator = new ReportGenerator();
		reportGenerator.generateReport();
	}
	
	public ReportGenerator(){	}
	
	/**
	 * The working directory should include the analysis results in the
	 * directory structure used in the outputDirectory of the
	 * RunAnalyzer Class.
	 */
	public ReportGenerator(String pathToRScriptFiles, String pathToLatexFiles, 
			String pathToRScriptExe, String pathToPdfLatexExe, 
			String workingDirectory){
		this.pathToRScriptFiles = pathToRScriptFiles;
		this.pathToLatexFiles = pathToLatexFiles;
		this.pathToRScriptExe = pathToRScriptExe;
		this.pathToPdfLatexExe = pathToPdfLatexExe;
		this.workingDirectory = workingDirectory;
	}

	public void generateReport()
			throws IOException {
		copyLatexAndRFiles();
		runRScripts();
		compileLatex();
	}
	
	private void copyLatexAndRFiles() throws IOException{
		System.out.println("Copying Latex and R scripts to working directory"
				+ " if missing:");
		copyDirectory(new File(pathToLatexFiles), new File(workingDirectory + "/Latex"));
		copyDirectory(new File(pathToRScriptFiles), new File(workingDirectory + "/RScripts"));
	}
	
	private void compileLatex() {
		System.out.println("\nCompiling LATEX:");

		for(int i = 0; i < 3; i++){
		runSystemCall("\"" + pathToPdfLatexExe +
        			"\" -output-directory=" + workingDirectory 
        			+ " \"Latex/VspAnalyzerAutomaticReport.tex\"", 
        			new File(workingDirectory));
		}
	}

	/**
	 * R has to find all necessary packages in the libraries currently set with
	 * .libPaths. If this is not the case install packages and / or add all 
	 * necessary libraries by adding .libPaths("path to R library") to your 
	 * local copy of analysis_main.R at 
	 * <i>workingDirectory/RScripts/analysis_main.R</i> .
	 * <p>
	 * Necessary packages are listed in analysis_main.R .
	 * <p>
	 * R output and errors are shown on the console.
	 */
	private void runRScripts() {
		System.out.println("\nRunning R:");

		(new File(workingDirectory + "/ROutput")).mkdir(); 
		
		runSystemCall("\"" + pathToRScriptExe + "\" \""
				//Execute analysis_main.R
    			+ "RScripts/analysis_main.R\"",
    			new File(workingDirectory));
		
		}
	
	private void runSystemCall(String command, File workSpace){
		String s = null;
	       try {
	    	    ProcessBuilder pb = new ProcessBuilder(command);
	    	    
	    	    pb.directory(workSpace);
	    	    Process p = pb.start();
	            
	            BufferedReader stdInput = new BufferedReader(new 
	                 InputStreamReader(p.getInputStream()));

	            BufferedReader stdError = new BufferedReader(new 
	                 InputStreamReader(p.getErrorStream()));

	            // read the output from the command
	            System.out.println("\nStandard output of the command:\n");
	            while ((s = stdInput.readLine()) != null) {
	                System.out.println(s);
	            }
	            
	            // read any errors from the attempted command
	            System.out.println("\nStandard error of the command (if any):\n");
	            while ((s = stdError.readLine()) != null) {
	                System.err.println(s);
	            }
	            
	        }
	        catch (IOException e) {
	            System.out.println("Java exceptions: ");
	            e.printStackTrace();
	            System.exit(-1);
	        }
	}
	
	/**
	 * Copy without overwrite
	 */
	private void copyDirectory(File source, File target) throws IOException{
		int counter = 0;
		target.mkdirs();
		File[] files = source.listFiles();
		
		for(File file: files){
			if(file.isDirectory()){
				copyDirectory(file, new File(target, file.getName()));
			} else {
				//No overwrite of user modifications to R and Latex scripts
				if((new File(target, file.getName())).exists()){
					continue;
				} else {
					FileReader inReader = new FileReader(file);
					FileWriter outWriter = new FileWriter(new File(target, 
							file.getName()));
					BufferedReader in = new BufferedReader(inReader);
					BufferedWriter out = new BufferedWriter(outWriter);
					int c;
					while((c = in.read()) != -1){
						out.write(c);
					}
					in.close();
					out.close();
					counter++;
				}
			}
		}
		if(counter > 0){
			System.out.println(counter + " of " 
					+ files.length + " files copied.");
		}
	}

}
