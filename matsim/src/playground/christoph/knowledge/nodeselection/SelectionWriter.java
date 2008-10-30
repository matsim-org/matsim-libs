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
import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.network.Node;
import org.matsim.population.Knowledge;
import org.matsim.population.Person;
import org.matsim.population.Population;
import org.matsim.utils.io.IOUtils;
import org.matsim.writer.Writer;

import playground.christoph.router.util.KnowledgeTools;

public class SelectionWriter extends Writer {

	private boolean fileOpened = false;
	private String description = "";
	private String dtdFile;
	
	private SelectionWriterHandler handler = null;
	private final Population population;

	private final static Logger log = Logger.getLogger(SelectionWriter.class);


	/*
	 * Creates a new SelectionWriter to write out the specified plans to the specified file and with
	 * the specified version.
	 *
	 * @param population the population to write to file
	 * @param filename the filename where to write the data
	 * @param version specifies the file-format
	 */
	public SelectionWriter(final Population population, final String filename, final String dtdFile, final String version, final String description)
	{
		super();
		this.population = population;
		this.outfile = filename;
		this.dtdFile = dtdFile;
		this.description = description;
		createHandler(this.dtdFile);
	}

	/**
	 * Just a helper method to instantiate the correct handler
	 * @param version
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
			this.out = IOUtils.getBufferedWriter(this.outfile);
			this.fileOpened = true;
			this.writeHeader("selection");
			this.handler.startSelection(description, out);
			this.handler.writeSeparator(this.out);
		}
		catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	public final void writePerson(final Person p) 
	{
		try 
		{
			this.handler.startPerson(p,this.out);

			// knowledge
			if (p.getKnowledge() != null) 
			{
				Knowledge k = p.getKnowledge();
				this.handler.startKnowledge(k, this.out);
				
					// activity space
					this.handler.startActivitySpace(out);
					
						// Nodes
						this.handler.startNodes(out);
						
						Map<Id, Node> nodesMap = KnowledgeTools.getKnownNodes(p);
						this.handler.nodes(nodesMap, out);
						
						this.handler.endNodes(out);
				
						// Links - not used yet and not implemented yet in the KnowledgeTools
		/*				this.handler.startLinks(out);
						
						Map<Id, Link> linksMap = KnowledgeTools.getKnownLinks(p);
						this.handler.nodes(linksMap, out);
						
						this.handler.endLinks(out);
		*/							
					this.handler.endActivitySpace(out);
				
				this.handler.endKnowledge(out);	
			}
			
			this.handler.endPerson(out);
			this.handler.writeSeparator(out);
			this.out.flush();
		}
		catch (IOException e) 
		{
			Gbl.errorMsg(e);
		}
	}

	public final void writePersons()
	{
		Iterator<Person> p_it = this.population.getPersons().values().iterator();
		
		while (p_it.hasNext()) 
		{
			Person p = p_it.next();
			writePerson(p);
		}
	}

	public final void writeEndSelection() 
	{
		if (this.fileOpened) 
		{
			try 
			{
				this.handler.endSelection(this.out);
				this.out.flush();
				this.out.close();
			}
			catch (IOException e) 
			{
				Gbl.errorMsg(e);
			}
		}
	}

	@Override
	public void write() 
	{
		this.writeStartSelection(description);
		this.writePersons();
		this.writeEndSelection();
	}

	public SelectionWriterHandler getHandler() {
		return this.handler;
	}
	
}