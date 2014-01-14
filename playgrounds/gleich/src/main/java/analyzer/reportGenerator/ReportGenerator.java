package analyzer.reportGenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;

public class ReportGenerator {
	
	String workingDirectory = "Z:/WinHome/ArbeitWorkspace/Analyzer/output/workingDirectory";
	String pathToRScriptFiles = "Z:/WinHome/ArbeitWorkspace/Analyzer/output/Rscripts";
	String pathToLatexFiles = "Z:/WinHome/ArbeitWorkspace/Analyzer/output/Latex";
	String pathToRScriptExe = "C:/Program Files/R/R-2.14.2/bin/Rscript.exe";
	String pathToPdfLatexExe = "pdflatex";

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		ReportGenerator reportGenerator = new ReportGenerator();
		reportGenerator.copyLatexAndRFiles();
		reportGenerator.runRScripts();
		reportGenerator.compileLatex();
	}
	
	private void copyLatexAndRFiles() throws IOException{
		System.out.println("Copying Latex and R scripts to working directory"
				+ " if missing:");
		copyDirectory(new File(pathToLatexFiles), new File(workingDirectory + "/Latex"));
		copyDirectory(new File(pathToRScriptFiles), new File(workingDirectory + "/R"));
	}
	
	private void compileLatex() {
		System.out.println("Compiling LATEX:");

		runSystemCall("\"" + pathToPdfLatexExe +
        			"\" -output-directory=" + workingDirectory + " \"" +
        			workingDirectory + "/Latex/LaTeX1.tex" + "\"");
//        			" \\include{" + pathToLatexFiles + "/LaTeX1.tex}");
         //Process p = Runtime.getRuntime().exec("pdflatex -output-directory=Z:/WinHome/ArbeitWorkspace/Analyzer/output/RSweave Z:/WinHome/ArbeitWorkspace/Analyzer/output/RSweave/LaTeX1.tex");
/* old:
 "\"" + pathToPdfLatexExe +
        			"\" -output-directory=" + workingDirectory +
        			" " + pathToLatexFiles + "/LaTeX1.tex"
 */
	}
	
	private void runRScripts() {
		System.out.println("Running R:");

		(new File(workingDirectory + "/ROutput")).mkdir(); 
		
		/*System.out.println("\"" + pathToRScriptExe + "\" " + "\"" 
    			+ pathToRScriptFiles + "\"" + "/analysis_main.R " +
    			"\"" + workingDirectory + "\" " +
    			"\"" + pathToRScriptFiles + "\"");*/
		// System Call to RScript.exe
		runSystemCall("\"" + pathToRScriptExe + "\" \""
				//Execute analysis_main.R
    			+ workingDirectory + "/R/analysis_main.R"
    			//1st Argument to be passed to R: Path to working directory
    			+ "\" \"" + workingDirectory + "\" " +
    			//2nd Argument to be passed to R: Path to other R scripts
    			"\"" + workingDirectory + "/R\"");
		
		}
	
	private void runSystemCall(String command){
		String s = null;
	       try {
	            // using the Runtime exec method:
	        	Runtime runTime = Runtime.getRuntime();
	        	Process p = runTime.exec(command);
	            
	            BufferedReader stdInput = new BufferedReader(new 
	                 InputStreamReader(p.getInputStream()));

	            BufferedReader stdError = new BufferedReader(new 
	                 InputStreamReader(p.getErrorStream()));

	            // read the output from the command
	            System.out.println("Standard output of the command:\n");
	            while ((s = stdInput.readLine()) != null) {
	                System.out.println(s);
	            }
	            
	            // read any errors from the attempted command
	            System.out.println("Standard error of the command (if any):\n");
	            while ((s = stdError.readLine()) != null) {
	                System.out.println(s);
	            }
	            
	        }
	        catch (IOException e) {
	            System.out.println("Java exceptions: ");
	            e.printStackTrace();
	            System.exit(-1);
	        }
	}
	
	private void copyDirectory(File source, File target) throws IOException{
		int counter = 0;
		target.mkdirs();
		File[] files = source.listFiles();
		
		for(File file: files){
			if(file.isDirectory()){
				copyDirectory(file, new File(target, file.getName()));
			} else {
				//No overwrite of user modifications to R and Latex scripts
				try{
					Files.copy(file.toPath(), target.toPath().resolve(file.getName()));
				} catch(FileAlreadyExistsException e) {
					counter++;
				}
			}
		}
		if(counter > 0){
			System.out.println((files.length - counter) + " of " 
					+ files.length + " files copied.");
		}
	}

}
