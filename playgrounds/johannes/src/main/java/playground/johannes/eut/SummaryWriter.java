/* *********************************************************************** *
 * project: org.matsim.*
 * SummaryWriter.java
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
package playground.johannes.eut;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;

/**
 * @author illenberger
 *
 */
public class SummaryWriter implements ShutdownListener {

	private double tt_avr;
	
	private double tt_guided;
	
	private double tt_unguided;
	
	private double tt_replaned;
	
	private double tt_riskaverse;
	
	private double tt_benefitPerIter;
	
//	private double tt_avrCE;
	
	private double n_riskaverse;
	
	private double n_traversedRiskyLink;
	
	private double avrTTVariance;
	
	private String outFile;
	
	public void setAvrTTVariance(double avrTTVariance) {
		this.avrTTVariance = avrTTVariance;
	}

	public SummaryWriter(String outFile) {
		this.outFile = outFile;
	}
	
	public void setTt_avr(double tt_avr) {
		this.tt_avr = tt_avr;
	}

	public void setTt_guided(double tt_guided) {
		this.tt_guided = tt_guided;
	}

	public void setTt_unguided(double tt_unguided) {
		this.tt_unguided = tt_unguided;
	}

	public void setTt_replaned(double tt_replaned) {
		this.tt_replaned = tt_replaned;
	}

	public void setTt_riskaverse(double tt_riskaverse) {
		this.tt_riskaverse = tt_riskaverse;
	}

	public void setTt_benefitPerIter(double tt_benefitPerIter) {
		this.tt_benefitPerIter = tt_benefitPerIter;
	}

//	public void setTt_avrCE(double tt_avrCE) {
//		this.tt_avrCE = tt_avrCE;
//	}

	public void setN_riskaverse(double n_riskaverse) {
		this.n_riskaverse = n_riskaverse;
	}

	public void setN_traversedRiskyLink(double riskyLink) {
		n_traversedRiskyLink = riskyLink;
	}

	public void notifyShutdown(ShutdownEvent event) {
		try {
			boolean fileExists = new File(outFile).exists();
			BufferedWriter writer = new BufferedWriter(new FileWriter(outFile, true));
			if(!fileExists) {
				writer.write("tt_avr");
				writer.write("\ttt_guided");
				writer.write("\ttt_unguided");
				writer.write("\ttt_replaned");
				writer.write("\ttt_riskaverse");
				writer.write("\ttt_benefitPerIter");
//				writer.write("\ttt_avrCE");
				writer.write("\tn_riskaverse");
				writer.write("\tn_traversedRiskLink");
				writer.write("\tavrTTVariance");
				writer.newLine();
			}
			writer.write(String.valueOf((float)tt_avr));
			writer.write("\t");
			writer.write(String.valueOf((float)tt_guided));
			writer.write("\t");
			writer.write(String.valueOf((float)tt_unguided));
			writer.write("\t");
			writer.write(String.valueOf((float)tt_replaned));
			writer.write("\t");
			writer.write(String.valueOf((float)tt_riskaverse));
			writer.write("\t");
			writer.write(String.valueOf((float)tt_benefitPerIter));
//			writer.write("\t");
//			writer.write(String.valueOf(tt_avrCE));
			writer.write("\t");
			writer.write(String.valueOf((float)n_riskaverse));
			writer.write("\t");
			writer.write(String.valueOf((float)n_traversedRiskyLink));
			writer.write("\t");
			writer.write(String.valueOf((float)avrTTVariance));
			writer.newLine();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
