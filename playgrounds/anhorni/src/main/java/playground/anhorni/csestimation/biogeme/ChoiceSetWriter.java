/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.csestimation.biogeme;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.anhorni.csestimation.EstimationPerson;
import playground.anhorni.csestimation.Location;
import playground.anhorni.csestimation.ShopLocation;
import playground.anhorni.csestimation.ShoppingTrip;

public class ChoiceSetWriter {

	private final static Logger log = Logger.getLogger(ChoiceSetWriter.class);
	private TreeMap<Id<Location>, ShopLocation> universalCS;
	private Population population;
	private DecimalFormat formatter = new DecimalFormat("0.000");
	Random random = new Random(37835409);
	private double sampleRate = 0.5;
	
	public ChoiceSetWriter(TreeMap<Id<Location>, ShopLocation> universalCS, Population population) {
		this.universalCS = universalCS;
		this.population = population;
	}
		
	public void write(String outdir, String bzFile)  {	 		
	//	ShopsEnricher enricher = new ShopsEnricher();
	//	enricher.enrich(this.universalCS, bzFile);
		this.write(outdir);		
	}
	
	private int getIndex(Id<Location> id) {
		int cnt = 0;
		for (Id<Location> idcs : this.universalCS.keySet()) {
			if (idcs.compareTo(id) == 0) {
				return cnt;
			}
			cnt++;
		}
		return -99;
	}
		
	private void write(String outdir) {
		String outfile = outdir + "sample.dat";			
		String header = this.getHeader();
		
		double avgCSDist = 0.0;
		double avgCSPrice = 0.0;
		double avgCSSize = 0.0;
		
		double avgCODist = 0.0;
		double avgCOSize = 0.0;
		double avgCOPrice = 0.0;
	
		try {								
			final BufferedWriter out = IOUtils.getBufferedWriter(outfile);
			out.write(header);
			out.newLine();	
			
			final BufferedWriter out0 = IOUtils.getBufferedWriter(outdir + "summary.dat");
			
			int cntCS = 0;
			int cntCO = 0;
			for (Person p:this.population.getPersons().values()) {
				EstimationPerson person = (EstimationPerson)p;
				
				for (ShoppingTrip st:person.getShoppingTrips()) {	
					double id_WP = person.getWeight();
					int choice = this.getIndex(st.getShop().getId());	
					int sex = 0;
					if (person.getSex().equals("f")) sex = 1;
					
					String attributes = person.getAge() + "\t" + sex + "\t" + person.getHhIncome() + "\t" + person.getHhSize();
					String alternatives = "";	
					int sizeCS = 0;
					for (ShopLocation shop:this.universalCS.values()) {
						Coord start = st.getStartCoord();
						Coord end = st.getEndCoord();
						Coord s = shop.getCoord();
											
						double additionalDistance = 0.0;					
						// shopping round trip
						if (CoordUtils.calcDistance(start, end) < 0.001) {
							additionalDistance = CoordUtils.calcDistance(start, s);
						} //intermediate shopping stop
						else {
							additionalDistance = CoordUtils.calcDistance(start, s) +
									CoordUtils.calcDistance(s, end) -
									CoordUtils.calcDistance(start, end);
						}						
						avgCSDist += additionalDistance;
						avgCSPrice += shop.getPrice();
						avgCSSize += shop.getSize();
						cntCS++;
						
						int avail = 1;
						if (st.getShop().getId().compareTo(shop.getId()) == 0) {
							out0.write(person.getId() + "\t" + shop.getId() + "\t" + additionalDistance + "\t" + shop.getSize() + "\t" + shop.getPrice());							
							avgCODist += additionalDistance;
							avgCOSize += shop.getSize();
							avgCOPrice += shop.getPrice();
							cntCO++;
							sizeCS++;
						}							
						alternatives += this.getIndex(shop.getId()) + "\t" + avail + "\t" + formatter.format(additionalDistance / 1000.0) + "\t" + shop.getSize() + "\t" + shop.getPrice() + "\t";
					}
					out.write(person.getId() + "\t" + id_WP + "\t" + choice + "\t" + attributes + "\t" + alternatives);
					out.newLine();
					out0.newLine();
				}
			}
			out.newLine();
			out0.write("--------------------------------------------\n");
			out0.write("Choice: \t\t" + formatter.format(avgCODist /cntCO) + "\t" + formatter.format(avgCOSize / cntCO) + "\t" + formatter.format(avgCOPrice /cntCO));
			out0.newLine();
			out0.write("Choice set: \t\t" + formatter.format(avgCSDist /cntCS) + "\t" + formatter.format(avgCSSize / cntCS) + "\t" + formatter.format(avgCSPrice /cntCS));
			out.flush();
			out.flush();			
			out.close();
			
			out0.flush();
			out0.close();
			
			log.info("cs file writen to: " + outfile);
						
		} catch (final IOException e) {
				throw new RuntimeException(e);
		}
	}
		
	private String getHeader() {
		String header="Id\t" +
		"WP\tChoice\tAge\tGender\thhIncome\thhSize\t" ;

		for (int i = 0; i < this.universalCS.size(); i++) {
			header += "SH" + i + "_Shop_id\t" +
					"SH" + i + "_AV\t" +
					"SH" + i + "_addDist\t" +
					"SH" + i + "_Size\t" +
					"SH" + i + "_Price\t";
		}	
		return header;
	}
}
