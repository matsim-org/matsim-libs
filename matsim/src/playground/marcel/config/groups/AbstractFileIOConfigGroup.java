/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractFileIOConfigGroup.java
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

package playground.marcel.config.groups;

import java.util.LinkedHashSet;
import java.util.Set;

import playground.marcel.config.ConfigGroupI;
import playground.marcel.config.ConfigListI;

abstract public class AbstractFileIOConfigGroup implements ConfigGroupI {

	public static final String INPUT_FILE = "inputFile";
	public static final String INPUT_FORMAT = "inputVersion";
	public static final String OUTPUT_FILE = "outputFile";
	public static final String OUTPUT_FORMAT = "outputVersion";
	
	private String inputFile = "";
	private String inputFormat = "";
	private String outputFile = "";
	private String outputFormat = "";
	
	protected Set<String> keyset = null;
	
	/*
	 * these should be deprecated soon in my opinion -marcel, 26mar07
	 */
	public static final String INPUT_LOCALDTD = "localInputDTD";
	public static final String OUTPUT_DTD = "ouputDTD";
	private String inputDTD = "";
	private String outputDTD = "";
	
	public AbstractFileIOConfigGroup() {
		this.keyset = new LinkedHashSet<String>();
		this.keyset.add(INPUT_FILE);
		this.keyset.add(INPUT_FORMAT);
		this.keyset.add(OUTPUT_FILE);
		this.keyset.add(OUTPUT_FORMAT);
	}
	
	/* interface implementatiton */

	abstract public String getName();
	
	public String getValue(String key) {
		if (key.equals(INPUT_FILE)) {
			return getInputFile();
		} else if (key.equals(INPUT_FORMAT)) {
			return getInputFormat();
		} else if (key.equals(OUTPUT_FILE)) {
			return getOutputFile();
		} else if (key.equals(OUTPUT_FORMAT)) {
			return getOutputFormat();
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	public void setValue(String key, String value) {
		if (key.equals(INPUT_FILE)) {
			setInputFile(value);
		} else if (key.equals(INPUT_FORMAT)) {
			setInputFormat(value);
		} else if (key.equals(OUTPUT_FILE)) {
			setOutputFile(value);
		} else if (key.equals(OUTPUT_FORMAT)) {
			setOutputFormat(value);
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	public Set<String> paramKeySet() {
		return keyset;
	}
	
	public Set<String> listKeySet() {
		return EMPTY_LIST_SET;
	}
	
	public ConfigListI getList(String key) {
		throw new UnsupportedOperationException();
	}

	/* direct access to the config members */

	public String getInputFile() {
		return this.inputFile;
	}

	public void setInputFile(String filename) {
		this.inputFile = filename;
	}
	
	public String getOutputFile() {
		return this.outputFile;
	}
	
	public void setOutputFile(String filename) {
		this.outputFile = filename;
	}
	
	public String getInputFormat() {
		return this.inputFormat;
	}
	
	public void setInputFormat(String format) {
		this.inputFormat = format;
	}
	
	public String getOutputFormat() {
		return this.outputFormat;
	}
	
	public void setOutputFormat(String format) {
		this.outputFormat = format;
	}

}
