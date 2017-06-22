/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
package playground.jbischoff.sharedTaxiBerlin.run.taxidemand;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.drt.run.*;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class RunSharedBerlinBatch {

	public static void main(String[] args) {
		
		int capacity = Integer.parseInt(args[1]);
		DecimalFormat format = new DecimalFormat();
		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);
			for (double a = 1.1; a<=2.3; a = a+0.2){
				for (int b = 0; b<1200; b=b+120){
					String runId = "c_"+capacity+"_a_"+format.format(a)+"_b_"+b;
					try{
					
			String configFile = args[0];
			Config config = ConfigUtils.loadConfig(configFile, new DvrpConfigGroup(), new DrtConfigGroup(),
					new OTFVisConfigGroup(), new TaxiFareConfigGroup());
			DrtConfigGroup drt = (DrtConfigGroup) config.getModules().get(DrtConfigGroup.GROUP_NAME);
		
			drt.setEstimatedBeelineDistanceFactor(1.5);
			drt.setVehiclesFile("new_net.taxis4to4_cap"+capacity+".xml");
//			drt.setNumberOfThreads(8);
			drt.setMaxTravelTimeAlpha(a);
			drt.setMaxTravelTimeBeta(b);
			drt.setkNearestVehicles(56);
			
			config.controler().setRunId(runId);
			config.controler().setOutputDirectory("/net/ils4/jbischoff/sharedTaxi/parameterizedRuns/"+runId+"/");
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			DrtControlerCreator.createControler(config, false).run();
					}
					catch (Exception e){
						System.err.println("Run "+runId+ " failed." );
						e.printStackTrace();
					}
					}
				}
		}
		
		
	
}
