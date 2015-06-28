/* *********************************************************************** *
 * project: org.matsim.*
 * DgSatellicWithindayStarter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.satellic;

import java.util.Arrays;

import com.google.inject.Provider;

import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.Mobsim;

import playground.dgrether.DgOTFVisConfigWriter;
import playground.dgrether.DgPaths;



public class DgSatellicWithindayTestStarter {

	public static void main(String[] args) {
		final String configfile = DgPaths.EXAMPLEBASE + "equil/configPlans100.xml";
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader confReader = new MatsimConfigReader(config);
		confReader.readFile(configfile);

		config.controler().setLastIteration(0);
		config.controler().setSnapshotFormat(Arrays.asList("otfvis"));
		config.qsim().setSnapshotPeriod(10.0);
		config.qsim().setSnapshotStyle( SnapshotStyle.queue ) ;;
		
		
		final Controler controler = new Controler(config);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return new DgWithindayMobsimFactory().createMobsim(controler.getScenario(), controler.getEvents());
					}
				});
			}
		});
		
		
		controler.run();
		controler.addControlerListener(new DgOTFVisConfigWriter());
		String outdir = controler.getConfig().controler().getOutputDirectory();

		String file = controler.getControlerIO().getIterationFilename(0, "otfvis.mvi");
		OTFVis.playMVI(file);
		//		DgOTFVisReplayLastIteration.main(new String[]{outdir + "/" + Controler.FILENAME_CONFIG});
	}
	
}
