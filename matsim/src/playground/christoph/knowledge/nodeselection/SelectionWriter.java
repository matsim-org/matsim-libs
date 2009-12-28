/* *********************************************************************** *
 * project: org.matsim.*
 * SelectionWriter.java
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

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.internal.MatsimFileWriter;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.MatsimXmlWriter;

import playground.christoph.knowledge.container.NodeKnowledge;
import playground.christoph.router.util.KnowledgeTools;

public class SelectionWriter extends MatsimXmlWriter implements MatsimFileWriter {

	private boolean fileOpened = false;
	private String description = "";
	private String dtdFile;
	private String outfile;
	private String dtd;
	//private int numDigits = 3;
	
	private SelectionWriterHandler handler = null;
	private final Population population;
	private KnowledgeTools knowledgeTools;
	
	private final static Logger log = Logger.getLogger(SelectionWriter.class);


	/*
	 * Creates a new SelectionWriter to write out the specified plans to the specified file and with
	 * the specified version.
	 *
	 * @param population the population to write to file
	 * @param filename the filename where to write the data
	 * @param version specifies the file-format
	 */
	public SelectionWriter(final Population population, final String dtdFile, final String version, final String description)
	{
		super();
		
		this.population = population;
		this.dtdFile = dtdFile;
		this.description = description;
		createHandler(this.dtdFile);
		
		this.knowledgeTools = new KnowledgeTools();
	}

	/*
	 * Just a helper method to instantiate the correct handler
	 */
	private void createHandler(String dtd)
	{
		//this.dtd = "http://www.matsim.org/files/dtd/plans_v4.dtd";
		this.dtd = dtd;
		this.handler = new SelectionWriterHandlerImpl();
	}

	public final void setWriterHandler(final SelectionWriterHandler handler) 
	{
		this.handler = handler;
	}

	//////////////////////////////////////////////////////////////////////
	// write methods
	//////////////////////////////////////////////////////////////////////

	public final void writeStartSelection(String description) 
	{
		try {
			openFile(this.outfile);
			this.fileOpened = true;
			this.writeXmlHead();
			this.writeDoctype("selection", this.dtd);
			this.handler.startSelection(description, this.writer);
			this.handler.writeSeparator(this.writer);
		}
		catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	public final void writePerson(final Person p) 
	{
		try 
		{
			this.handler.startPerson(p,this.writer);

			// NodeKnowledge
			if (p.getCustomAttributes().containsKey("NodeKnowledge")) 
			{
				NodeKnowledge nodeKnowledge = (NodeKnowledge)p.getCustomAttributes().get("NodeKnowledge");
				
				this.handler.startKnowledge(nodeKnowledge, this.writer);
				
					// activity space
					this.handler.startActivitySpace(this.writer);
					
						// Nodes
						this.handler.startNodes(this.writer);
						
						Map<Id, Node> nodesMap = knowledgeTools.getKnownNodes(p);
						this.handler.nodes(nodesMap, this.writer);
						
						this.handler.endNodes(this.writer);
				
						// Links - not used yet and not implemented yet in the KnowledgeTools
		/*				this.handler.startLinks(out);
						
						Map<Id, Link> linksMap = KnowledgeTools.getKnownLinks(p);
						this.handler.nodes(linksMap, out);
						
						this.handler.endLinks(out);
		*/							
					this.handler.endActivitySpace(this.writer);
				
				this.handler.endKnowledge(this.writer);	
			}
			
			this.handler.endPerson(this.writer);
			this.handler.writeSeparator(this.writer);
			this.writer.flush();
		}
		catch (IOException e) 
		{
			Gbl.errorMsg(e);
		}
	}

	public final void writePersons()
	{
		for (Person p : this.population.getPersons().values()) {
			writePerson(p);
		}
	}

	public final void writeEndSelection() 
	{
		if (this.fileOpened) 
		{
			try 
			{
				this.handler.endSelection(this.writer);
				this.writer.flush();
				close();
			}
			catch (IOException e) 
			{
				Gbl.errorMsg(e);
			}
		}
	}

	/*
	 * Write a single file containing all data.
	 */
	public void writeFile(final String filename) 
	{
		this.outfile = filename;
		this.writeStartSelection(description);
		this.writePersons();
		this.writeEndSelection();
	}

	/*
	 * Splits up the population in some files with n person in each of them (except the last one...).
	 */
	public void write(int n, final String filename)
	{
		FileNameCreator fileNameCreator = new FileNameCreator(filename);
		// don't allow zero persons per file
		if(n == 0) n = this.population.getPersons().size();

		// counter variable
		int i = 0;
/*		
		// save default fileName
		String defaultFileName = new String(this.outfile);

		// create header and ending of the creates filesnames
		String header;
		String ending;
		
		if(this.outfile.toLowerCase().endsWith(".xml.gz"))
		{
			header = this.outfile.toLowerCase().substring(0, this.outfile.length() - 7);
			ending = ".xml.gz";
		}
		else if (this.outfile.toLowerCase().endsWith(".xml"))
		{
			header = this.outfile.toLowerCase().substring(0, this.outfile.length() - 4);
			ending = ".xml";
		}
		else
		{
			log.error("Didn't recognize the ending of the output file!");
			header = new String(this.outfile + ".");
			ending = new String();
		}
			
		// NumberFormat to format the counter in the filenames
		NumberFormat nf = NumberFormat.getInstance();

		// set how many places you want to the left of the decimal.
		nf.setMinimumIntegerDigits(this.numDigits);
*/				
		Iterator<? extends Person> p_it = this.population.getPersons().values().iterator();
		
		while (p_it.hasNext()) 
		{
			if (i % n == 0)
			{
				//this.outfile = new String(header + "_" + nf.format(i / n) + ending);
				this.outfile = fileNameCreator.getNextFileName();
				log.info(this.outfile);
				this.writeStartSelection(description);
			}
			Person p = p_it.next();
			writePerson(p);
			
			if ( ((i + 1) % n == 0) || !p_it.hasNext())
			{	
				this.writeEndSelection();
				//this.outfile = new String(defaultFileName);
			}
			i++;
		}
		
		this.outfile = fileNameCreator.getBaseFileName();
		
		log.info("Splitted the created knowledge selection into " + fileNameCreator.getFileCounter() + " files.");
	}
	
//	public void setNumDigits(int numDigits)
//	{
//		//this.numDigits = numDigits;
//		this.fileNameCreator.setNumDigits(numDigits);
//	}
//	
//	public int getNumDigits()
//	{
//		//return this.numDigits;
//		return this.fileNameCreator.getNumDigits();
//	}
//
//	public SelectionWriterHandler getHandler() {
//		return this.handler;
//	}
//	
}