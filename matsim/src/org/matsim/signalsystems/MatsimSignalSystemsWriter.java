/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.signalsystems;

import org.matsim.core.basic.network.BasicLaneDefinitions;
import org.matsim.core.basic.signalsystems.BasicSignalSystems;
import org.matsim.utils.io.MatsimJaxbXmlWriter;

/**
 * Writes a light signal system definition to xml.
 * @author dgrether
 */
public class MatsimSignalSystemsWriter {
	
	private MatsimJaxbXmlWriter writerDelegate;
	
	public MatsimSignalSystemsWriter(BasicSignalSystems basiclss){
		this(new SignalSystemsWriter11(basiclss));
	}
	
	/**
	 * Use this constructor to write the default xml format.
	 * @param basiclss
	 * @deprecated lanedefinitions can be written separately 
	 */
	@Deprecated 
	public MatsimSignalSystemsWriter(BasicLaneDefinitions lanedefs, BasicSignalSystems basiclss) {
		this(new LightSignalSystemsWriter10(lanedefs, basiclss));
	}
	
	/**
	 * Customize the verion of the written xml by using this constructor with
	 * the SignalSystemsWriter of your choice (there is no specific type SignalSystemsWriter)
	 * @param basiclss
	 * @param writer
	 */
	public MatsimSignalSystemsWriter(MatsimJaxbXmlWriter writer){
		this.writerDelegate = writer;
	}
	
	
	public void writeFile(String filename){
		this.writerDelegate.writeFile(filename);
	}
}
