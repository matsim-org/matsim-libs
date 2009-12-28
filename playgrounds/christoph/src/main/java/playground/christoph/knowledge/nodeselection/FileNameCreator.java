/* *********************************************************************** *
 * project: org.matsim.*
 * SelectionReaderMatsim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.christoph.knowledge.nodeselection;

import java.text.NumberFormat;

import org.apache.log4j.Logger;

public class FileNameCreator {

	private static final Logger log = Logger.getLogger(FileNameCreator.class);
	
	protected String baseFileName;
	protected String fileHeader;
	protected String fileEnding;
	protected int numDigits = 3;	// use by default 3 digits
	protected int fileCounter;
	protected NumberFormat nf;

	public FileNameCreator()
	{
		// NumberFormat to format the counter in the filenames
		nf = NumberFormat.getInstance();
		
		// set how many places you want to the left of the decimal.
		nf.setMinimumIntegerDigits(this.numDigits);
	}
	
	public FileNameCreator(String baseFileName)
	{
		//this.baseFileName = baseFileName;
		setBaseFileName(baseFileName);
		
		// NumberFormat to format the counter in the filenames
		nf = NumberFormat.getInstance();
		
		// set how many places you want to the left of the decimal.
		nf.setMinimumIntegerDigits(this.numDigits);
	}

	public void setBaseFileName(String baseFileName)
	{
		this.baseFileName = new String(baseFileName);
		
		// create header and ending of the created filesnames
		if(this.baseFileName.toLowerCase().endsWith(".xml.gz"))
		{
			fileHeader = this.baseFileName.toLowerCase().substring(0, this.baseFileName.length() - 7);
			fileEnding = ".xml.gz";
		}
		else if (this.baseFileName.toLowerCase().endsWith(".xml"))
		{
			fileHeader = this.baseFileName.toLowerCase().substring(0, this.baseFileName.length() - 4);
			fileEnding = ".xml";
		}
		else
		{
			log.error("Didn't recognize the ending of the file!");
			fileHeader = new String(this.baseFileName + ".");
			fileEnding = new String();
		}
	}

	public String getBaseFileName()
	{
		return this.baseFileName;
	}
	
	public String getNextFileName()
	{
		String nextFileName = new String(fileHeader + "_" + nf.format(this.fileCounter) + fileEnding);

		this.fileCounter++;
		
		return nextFileName;
	}
	
	public int getFileCounter()
	{
		return this.fileCounter;
	}
	
	public void resetFileCounter()
	{
		this.fileCounter = 0;
	}
	
	public void setNumDigits(int numDigits)
	{
		this.numDigits = numDigits;
	}
	
	public int getNumDigits()
	{
		return this.numDigits;
	}
	
}