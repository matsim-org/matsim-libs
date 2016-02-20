/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertOsmToMatsim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.jjoubert.projects.capeTownFreight;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader;
import playground.southafrica.freight.digicore.utils.DigicoreUtils;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 * Class to estimate what the travel speed between two activities are. This is
 * done to determine speed as a function of trip length. Output plans are 
 * analysed.
 * 
 * @author jwjoubert
 */
public class TripSpeedEstimator{
	final private static Logger LOG = Logger.getLogger(TripSpeedEstimator.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		estimateFromChains(args);
	}
	
	public static void estimateFromPlans(String[] args){
		Header.printHeader(TripSpeedEstimator.class.toString(), args);
		String plans = args[0];
		String output = args[1];

		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(sc).parse(plans);

		LOG.info("Processing plans to extract trip times as a function of distance...");
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		Counter counter = new Counter(" persons # ");
		try{
			bw.write("personId,leg,distKm,timeS");
			bw.newLine();

			for(Person person : sc.getPopulation().getPersons().values()){
				int legId = 0;
				Plan plan = person.getSelectedPlan();
				Iterator<PlanElement> iterator = plan.getPlanElements().iterator();
				while(iterator.hasNext()){
					PlanElement pe = iterator.next();
					if(pe instanceof Leg){
						Leg leg = (Leg)pe;
						Route route = leg.getRoute();
						String s = String.format("%s,%d,%.3f,%.0f\n", 
								person.getId().toString(),
								legId++,
								route.getDistance()/1000.0,
								leg.getTravelTime());
						bw.write(s);
					}
				}
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + output);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + output);
			}
		}
		counter.printCounter();

		Header.printFooter();
	}

	public static void estimateFromChains(String[] args){
		Header.printHeader(TripSpeedEstimator.class.toString(), args);
		String xmlfolder = args[0];
		String output = args[1];
		String date = args[2];
		
		List<File> files = FileUtils.sampleFiles(new File(xmlfolder), Integer.MAX_VALUE, FileUtils.getFileFilter(".xml.gz"));
		LOG.info("Processing activity chains to extract trip times as a function of distance...");
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		Counter counter = new Counter(" vehicles # ");
		try{
			bw.write("id,leg,startHour,crowDistKm,timeS");
			bw.newLine();
			
			for(File file : files){
				DigicoreVehicleReader dvr = new DigicoreVehicleReader();
				dvr.parse(file.getAbsolutePath());
				DigicoreVehicle dv = dvr.getVehicle();
				
				int legId = 0;
				for(DigicoreChain chain : dv.getChains()){
					String chainDate = DigicoreUtils.getShortDate(chain.getFirstMajorActivity().getEndTimeGregorianCalendar());
					if(chainDate.equalsIgnoreCase(date)){
						for(int i = 1; i < chain.size(); i++){
							DigicoreActivity d1 = chain.get(i-1);
							DigicoreActivity d2 = chain.get(i);
							
							/* Get start hour */
							int hour = d1.getEndTimeGregorianCalendar().get(Calendar.HOUR_OF_DAY);
							
							double triptime = (((double)d2.getStartTimeGregorianCalendar().getTimeInMillis()) - ((double)d1.getEndTimeGregorianCalendar().getTimeInMillis()))/1000.0;
							double crowFlyDistance = ((double)CoordUtils.calcEuclideanDistance(d1.getCoord(), d2.getCoord()))/1000.0;
							
							String s = String.format("%s,%d,%d,%.3f,%.0f\n", 
									dv.getId().toString(),
									legId++,
									hour,
									crowFlyDistance,
									triptime);
							bw.write(s);
						}
					}
				}
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + output);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + output);
			}
		}
		counter.printCounter();
		
		Header.printFooter();
	}
	
}