package playground.dziemke.analysis;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author dziemke, tthunig
 */
public class GnuplotUtils {
	public static final Logger log = Logger.getLogger(GnuplotUtils.class);

	public static void main(String[] args) {
		String pathToSpecificAnalysisDir = "../../../../Desktop/gnuplottest/";
		String relativePathToGnuplotScript = "test.gnu";
		GnuplotUtils.runGnuplotScript(pathToSpecificAnalysisDir, relativePathToGnuplotScript, "one");
	}

	public static void runGnuplotScript(String pathToSpecificAnalysisDir, String relativePathToGnuplotScript, String gnuplotArgument){
		/*
			right now this only works with one argument
			if you need to pass more then one change "String gnuplotArgument" to "String... gnuplotArguments"
			also you have to think about a way to pass the multiple arguments dynamicly to the ProcessBuilder
			(you could also just implement the specific case for yourself)

			a fine testscript for gnuplot would be:
				#!/usr/local/bin/gnuplot --persist
				print "script name        : ", ARG0
				print "first argument     : ", ARG1
				print "second argument    : ", ARG2
				print "third argument     : ", ARG3
				print "number of arguments: ", ARGC
		*/
		log.info("Analysis directory is = " + pathToSpecificAnalysisDir);
		log.info("Relative path from analysis directory to directory with gnuplot script is = " + relativePathToGnuplotScript);
		
		String osName = System.getProperty("os.name");
		log.info("OS name is = " + osName);
		
		/*String allArgumentsAsString = "";
		for (int i=0; i < gnuplotArguments.length ; i++) {
			allArgumentsAsString = allArgumentsAsString.concat(gnuplotArguments[i] + " ");
		}*/

		try {
			ProcessBuilder processBuilder = null;
			
			// If OS is Windows --- example (daniel r) // os.arch=amd64 // os.name=Windows 7 // os.version=6.1
			if (osName.contains("Win") || osName.contains("win")) {
				String[] args = {"one", "two", "three", "four", "five"};
				processBuilder = new ProcessBuilder( "cmd", "/c", "cd", pathToSpecificAnalysisDir, "&", "gnuplot", "-c", relativePathToGnuplotScript, gnuplotArgument);
			
			// If OS is Macintosh --- example (dominik) // os.arch=x86_64 // os.name=Mac OS X // os.version=10.10.2
			} else if ( osName.contains("Mac") || osName.contains("mac") ) {
				processBuilder = new ProcessBuilder("bash", "-c", "(cd " + pathToSpecificAnalysisDir + " && /usr/local/bin/gnuplot -c " + relativePathToGnuplotScript + " " + gnuplotArgument + ")");
			
			// If OS is Linux --- example (benjamin) // os.arch=amd64 // os.name=Linux	// os.version=3.13.0-45-generic
			} else if ( osName.contains("Lin") || osName.contains("lin") ) {
				log.warn("This implemenation has not yet been tested for Linux. Please report if you use this.");
				processBuilder = new ProcessBuilder("bash", "-c", "(cd " + pathToSpecificAnalysisDir + " && /usr/local/bin/gnuplot " + relativePathToGnuplotScript + ")");
			
			// If OS is neither Win nor Mac nor Linux
			} else {
				log.error("Not implemented for os.arch=" + System.getProperty("os.arch") );
			}
			
			Process process = processBuilder.start();
			
			/* Print command line infos and errors on console */
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			
			while ((line = reader.readLine()) != null) {
				log.info("input stream: " + line);
			}			

			reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			while ((line = reader.readLine()) != null) {
				log.error("error: " + line);
			}
		} catch (IOException e) {
			log.error("ERROR while executing gnuplot command.");
			e.printStackTrace();
		}
	}
}