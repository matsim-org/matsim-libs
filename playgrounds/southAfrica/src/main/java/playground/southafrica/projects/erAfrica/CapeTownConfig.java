/* *********************************************************************** *
 * project: org.matsim.*
 * CapeTownConfig.java
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
package playground.southafrica.projects.erAfrica;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Generates the {@link Config} class necessary to run the accessibility 
 * calculations for the City of Cape Town.
 * 
 * @author jwjoubert
 */
public class CapeTownConfig {

	public static Config getConfig(String baseFolder){
		baseFolder += baseFolder.endsWith("/") ? "" : "/";
		Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup());
		
		/* Accessibility config group */
		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, true);
		acg.setOutputCrs(TransformationFactory.HARTEBEESTHOEK_LO19);
		acg.setMeasuringPointsFile(baseFolder + "measuringPoints.xml.gz");
		
		//FIXME Why do I need the next two lines?! 
		acg.setCellSizeCellBasedAccessibility(1000); 
		acg.setEnvelope(new Envelope(252000, 256000, 9854000, 9856000)) ;
		
		/* Facilities */
		config.facilities().setInputFile(baseFolder + "facilities_all.xml.gz");
		config.facilities().setInputCRS(TransformationFactory.HARTEBEESTHOEK_LO19);
	
		/* Network */
		config.network().setInputFile(baseFolder + "network_full.xml.gz");
		config.network().setInputCRS(TransformationFactory.WGS84_SA_Albers);
		
		/* Plans */
		config.plans().setInputFile(baseFolder + "persons.xml.gz");
		config.plans().setInputCRS(TransformationFactory.HARTEBEESTHOEK_LO19);
		
		/* Routed modes */
		{
			ModeRoutingParams pars = new ModeRoutingParams("walk");
			pars.setTeleportedModeSpeed(1.5);
			config.plansCalcRoute().addModeRoutingParams(pars);
		}
		{
			ModeRoutingParams pars = new ModeRoutingParams("taxi");
			pars.setTeleportedModeSpeed(30.0/3.6);
			config.plansCalcRoute().addModeRoutingParams(pars);
		}
		{
			ModeRoutingParams pars = new ModeRoutingParams("MyCiti");
			pars.setTeleportedModeSpeed(20.0/3.6);
			config.plansCalcRoute().addModeRoutingParams(pars);
		}
		{
			ModeRoutingParams pars = new ModeRoutingParams("bus");
			pars.setTeleportedModeSpeed(17.5/3.6);
			config.plansCalcRoute().addModeRoutingParams(pars);
		}
		{
			ModeRoutingParams pars = new ModeRoutingParams("rail");
			pars.setTeleportedModeSpeed(15.0/3.6);
			config.plansCalcRoute().addModeRoutingParams(pars);
		}
		{
			ModeRoutingParams pars = new ModeRoutingParams("ride");
			pars.setTeleportedModeSpeed(35.0/3.6);
			config.plansCalcRoute().addModeRoutingParams(pars);
		}
		{
			ModeRoutingParams pars = new ModeRoutingParams("other");
			pars.setTeleportedModeSpeed(20.0/3.6);
			config.plansCalcRoute().addModeRoutingParams(pars);
		}
		
		/* Controler */
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(baseFolder + "accessibility_output/");
		config.controler().setLastIteration(0);
		config.controler().setRunId("za_cpt_" + AccessibilityUtils.getDate());
		
		config.global().setCoordinateSystem(TransformationFactory.HARTEBEESTHOEK_LO19);
		ConfigUtils.setVspDefaults(config);
		
		return config;
	}
}
