package playground.vsp.analysis.utils;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author dziemke, tthunig, gthunig
 */
public class GnuplotUtils {
	public static final Logger LOG = Logger.getLogger(GnuplotUtils.class);

	public static void runGnuplotScript(String pathToSpecificAnalysisDir, String relativePathToGnuplotScript) {
		runGnuplotScript(pathToSpecificAnalysisDir, relativePathToGnuplotScript, "");
	}

	public static void runGnuplotScript(String pathToSpecificAnalysisDir, String relativePathToGnuplotScript, String... gnuplotArguments){
		/*
			arguments will now be passed dynamically on windows
			may work on mac as well, not tested

			a fine testscript for gnuplot would be:
				#!/usr/local/bin/gnuplot --persist
				print "script name        : ", ARG0
				print "first argument     : ", ARG1
				print "second argument    : ", ARG2
				print "third argument     : ", ARG3
				print "number of arguments: ", ARGC

			[gthunig]
		*/
		LOG.info("Analysis directory is = " + pathToSpecificAnalysisDir);
		LOG.info("Relative path from analysis directory to directory with gnuplot script is = " + relativePathToGnuplotScript);
		
		String osName = System.getProperty("os.name");
		LOG.info("OS name is = " + osName);
		
		/*String allArgumentsAsString = "";
		for (int i=0; i < gnuplotArguments.length ; i++) {
			allArgumentsAsString = allArgumentsAsString.concat(gnuplotArguments[i] + " ");
		}*/

		try {
			ProcessBuilder processBuilder = null;
			
			// If OS is Windows --- example (daniel r) // os.arch=amd64 // os.name=Windows 7 // os.version=6.1
			if (osName.contains("Win") || osName.contains("win")) {
				List<String> commands = new ArrayList<>();
				commands.add("cmd");
				commands.add("/c");
				commands.add("cd");
				commands.add(pathToSpecificAnalysisDir);
				commands.add("&");
				commands.add("gnuplot");
				commands.add("-c");
				commands.add(relativePathToGnuplotScript);
				Collections.addAll(commands, gnuplotArguments);
				processBuilder = new ProcessBuilder(commands);

			// If OS is Macintosh --- example (dominik) // os.arch=x86_64 // os.name=Mac OS X // os.version=10.10.2
			} else if ( osName.contains("Mac") || osName.contains("mac") ) {
				String argumentsAsString = "";
				for (String argument : gnuplotArguments) {
					argumentsAsString += argument;
					argumentsAsString += " ";
				}
				processBuilder = new ProcessBuilder("bash", "-c", "(cd " + pathToSpecificAnalysisDir + " && /usr/local/bin/gnuplot -c " + relativePathToGnuplotScript + " " + argumentsAsString + ")");
			
			// If OS is Linux --- example (benjamin) // os.arch=amd64 // os.name=Linux	// os.version=3.13.0-45-generic
			} else if ( osName.contains("Lin") || osName.contains("lin") ) {
				LOG.warn("This implemenation has not yet been tested for Linux. Please report if you use this.");
				processBuilder = new ProcessBuilder("bash", "-c", "(cd " + pathToSpecificAnalysisDir + " && /usr/local/bin/gnuplot " + relativePathToGnuplotScript + ")");
			
			// If OS is neither Win nor Mac nor Linux
			} else {
				LOG.error("Not implemented for os.arch=" + System.getProperty("os.arch") );
			}

			assert processBuilder != null;
			Process process = processBuilder.start();
			
			/* Print command line infos and errors on console */
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			
			while ((line = reader.readLine()) != null) {
				LOG.info("input stream: " + line);
			}			

			reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			while ((line = reader.readLine()) != null) {
				LOG.warn(": " + line);
			}
		} catch (IOException e) {
			LOG.error("ERROR while executing gnuplot command.");
			e.printStackTrace();
		}
	}
}