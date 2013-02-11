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
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.core.utils.io.IOUtils;
import playground.anhorni.csestimation.EstimationPerson;
import playground.anhorni.csestimation.ShopLocation;
import playground.anhorni.csestimation.ShoppingTrip;
import playground.anhorni.csestimation.ShopsEnricher;

public class ChoiceSetWriter {

	private final static Logger log = Logger.getLogger(ChoiceSetWriter.class);
	private TreeMap<Id, ShopLocation> universalCS;
	private Population population;
	private WGS84toCH1903LV03 trafo = new WGS84toCH1903LV03();
	private DecimalFormat formatter = new DecimalFormat("0.000");
	
	public ChoiceSetWriter(TreeMap<Id, ShopLocation> universalCS, Population population) {
		this.universalCS = universalCS;
		this.population = population;
	}
		
	public void write(String outdir, String bzFile)  {	 		
		ShopsEnricher enricher = new ShopsEnricher();
		enricher.enrich(this.universalCS, bzFile);
		this.write(outdir);		
	}
	
	private int getIndex(Id id) {
		int cnt = 0;
		for (Id idcs : this.universalCS.keySet()) {
			cnt++;
			if (idcs.compareTo(id) == 0) {
				return cnt;
			}
		}
		return -99;
	}
		
	private void write(String outdir) {
				
		String outfile = outdir + "sample.dat";			
		String header = this.getHeader();
	
		try {								
			final BufferedWriter out = IOUtils.getBufferedWriter(outfile);
			out.write(header);
			out.newLine();	
						
			for (Person p:this.population.getPersons().values()) {
				EstimationPerson person = (EstimationPerson)p;
				
				for (ShoppingTrip st:person.getShoppingTrips()) {	
					double id_WP = person.getWeight();
					int choice = this.getIndex(st.getShop().getId());	
					int sex = 0;
					if (person.getSex().equals("f")) sex = 1;
					
					String attributes = person.getAge() + "\t" + sex + "\t" + person.getHhIncome() + "\t" + person.getHhSize();
					String alternatives = "";								
					for (ShopLocation shop:this.universalCS.values()) {
						Coord start = this.trafo.transform(st.getStartCoord());
						Coord end = this.trafo.transform(st.getEndCoord());
						Coord s = this.trafo.transform(shop.getCoord());
											
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
						alternatives += shop.getId() + "\t1\t" + formatter.format(additionalDistance / 1000.0) + "\t" + shop.getSize() + "\t" + shop.getPrice() + "\t";
					}			
					out.write(person.getId() + "\t" + id_WP + "\t" + choice + "\t" + attributes + "\t" + alternatives);
					out.newLine();
				}
				out.flush();
			}
			out.flush();			
			out.flush();
			out.close();
			log.info("cs file writen to: " + outfile);
						
		} catch (final IOException e) {
				Gbl.errorMsg(e);
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
