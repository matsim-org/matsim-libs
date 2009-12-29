/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractRScript.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package playground.johannes.snowball2;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.matsim.utils.misc.ExeRunner;


/**
 * @author illenberger
 *
 */
public abstract class AbstractRScript {
	
	protected static final String NEW_LINE = "\n";
	
	protected final String exePath;
	
	protected final String tmpDir;
	
	protected final String stdoutFile;
	
	public AbstractRScript(String exePath, String tmpDir) {
		this.exePath = exePath;
		this.tmpDir = tmpDir;
		stdoutFile = tmpDir + "stdout.txt";
	}
	
	protected String loadGraphScript(String vertexFile, String edgeFile) {
		StringBuilder script = new StringBuilder(400);
		
		script.append("rm(list = ls())");
		script.append(NEW_LINE);
		script.append("require(\"statnet\")");
		script.append(NEW_LINE);
		script.append("vertexTable <- read.table(\"");
		script.append(vertexFile);
		script.append("\", header=TRUE, sep=\"\\t\")");
		script.append(NEW_LINE);
		script.append("vCount <- dim(vertexTable)[1]");
		script.append(NEW_LINE);
		script.append("net <- network.initialize(vCount, directed=FALSE)");
		script.append(NEW_LINE);
		script.append("edgeTable <- read.table(\"");
		script.append(edgeFile);
		script.append("\", header=TRUE, sep=\"\\t\")");
		script.append(NEW_LINE);
		script.append("eCount <- dim(edgeTable)[1]");
		script.append(NEW_LINE);
		script.append("add.edges(net, edgeTable$FROM, edgeTable$TO)");
		script.append(NEW_LINE);
		
		return script.toString();
	}
	
	protected int execute(String script) {
		String scriptFile = tmpDir + "script.r";
		PrintWriter writer;
		try {
			writer = new PrintWriter(scriptFile);
			writer.write(script);
			writer.close();
			
			StringBuilder cmd = new StringBuilder();
			cmd.append(exePath);
			cmd.append(" CMD BATCH ");
			cmd.append(scriptFile);
			
			return ExeRunner.run(cmd.toString(), stdoutFile, Integer.MAX_VALUE);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return -1;
		}
	}
}
