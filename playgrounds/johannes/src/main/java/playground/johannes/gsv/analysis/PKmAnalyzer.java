/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.johannes.gsv.analysis;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author johannes
 * 
 */
public class PKmAnalyzer implements IterationEndsListener, StartupListener {

	private PKmCalculator calculator;

	private final TransitLineAttributes attributes;
	
	public PKmAnalyzer(TransitLineAttributes attributes) {
		this.attributes = attributes;
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		Map<String, Double> stats = calculator.statistics();

		String file = event.getControler().getControlerIO()
				.getIterationFilename(event.getIteration(), "pkm.txt");

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));

			for (Entry<String, Double> entry : stats.entrySet()) {
				writer.write(entry.getKey());
				writer.write("\t");
				writer.write(String.valueOf(entry.getValue()));
				writer.newLine();
			}

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void notifyStartup(StartupEvent event) {
        calculator = new PKmCalculator(event.getControler().getScenario().getNetwork(), attributes);
		event.getControler().getEvents().addHandler(calculator);
	}

}
