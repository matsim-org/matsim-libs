package playground.dziemke.analysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

/**
 * @author dziemke, tthunig
 */
public class GnuplotUtils {
	public static final Logger log = Logger.getLogger(GnuplotUtils.class);
	
	public static void runGnuplotScript(String pathToSpecificAnalysisDir, String relativePathToGnuplotScript, String... gnuplotArguments){
		log.info("Analysis directory is = " + pathToSpecificAnalysisDir);
		log.info("Relative path from analysis directory to directory with gnuplot script is = " + relativePathToGnuplotScript);
		
		String osName = System.getProperty("os.name");
		log.info("OS name is = " + osName);
		
		String allArgumentsAsString = gnuplotArguments[0];
		for (int i=1; i < gnuplotArguments.length ; i++) {
			allArgumentsAsString.concat(" " + gnuplotArguments[i]);
		}
		
		try {
			ProcessBuilder processBuilder = null;
			
			// if OS is Windows --- example (daniel r) // os.arch=amd64 // os.name=Windows 7 // os.version=6.1
			if ( osName.contains("Win") || osName.contains("win")) {
				processBuilder = new ProcessBuilder( "cmd", "/c", "cd", pathToSpecificAnalysisDir, "&", "gnuplot", relativePathToGnuplotScript);
			
				// if OS is Macintosh --- example (dominik) // os.arch=x86_64 // os.name=Mac OS X // os.version=10.10.2
			} else if ( osName.contains("Mac") || osName.contains("mac") ) {
				processBuilder = new ProcessBuilder("bash", "-c", "(cd " + pathToSpecificAnalysisDir + " && /usr/local/bin/gnuplot -c " + relativePathToGnuplotScript + " " + allArgumentsAsString + ")");
			
				// if OS is Linux --- example (benjamin) // os.arch=amd64 // os.name=Linux	// os.version=3.13.0-45-generic
			} else if ( osName.contains("Lin") || osName.contains("lin") ) {
				log.warn("This implemenation has not yet been tested for Linux. Please report if you use this.");
				processBuilder = new ProcessBuilder("bash", "-c", "(cd " + pathToSpecificAnalysisDir + " && /usr/local/bin/gnuplot " + relativePathToGnuplotScript + ")");
			
				// if OS is neither Win nor Mac nor Linux
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