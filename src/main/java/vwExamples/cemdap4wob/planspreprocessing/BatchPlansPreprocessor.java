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
package vwExamples.cemdap4wob.planspreprocessing;

import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * @author  jbischoff
 *
 */
/**
 * Runs several Post-Cemdap-MATSim scripts at once. Note that this is rather for convenience and not very efficiently programmed (Population is read / written several times)
 */
public class BatchPlansPreprocessor {
	
	public static void main(String[] args) {
		final String baseFolder = "D:/cemdap-vw/";
		final String cemdapOutputFolder = baseFolder +"/cemdap_output/";
	
		//1. Schritt: Pläne mergen (Cemdap generiert i.d.R. fünf MATSim-Pläne pro Person)
		new PlansMerger().run(cemdapOutputFolder);
		//2. Schritt: Activity Dauern anpassen, um sinnvolle Aktivitätendauern aus dem Modell zu gewinnen und eine Sensitivität ggü. zeitlichen Änderungen zu erhalten. Dazu wird eine grundlegende Config-Datei erstellt

		new ActivityTypeTimeConverter().run(cemdapOutputFolder+"mergedPlans.xml.gz", cemdapOutputFolder+"mergedPlans_dur.xml.gz", cemdapOutputFolder+"activityConfig.xml");
		
		//Agenten herausfiltern, deren Aktivitäten nicht im Umkreis von 2000m einer Kante stattfinden. Verhindert Artefakte am Rande des Netzes,
		new FilterAgentsnotinNetwork().run(baseFolder+"/input/networkpt-av-nov17_cleaned.xml.gz",cemdapOutputFolder+"mergedPlans_dur.xml.gz" , cemdapOutputFolder+"mergedPlans_filtered.xml.gz",2000);
		
		// Schüler hinzufügen
		new MergeStudentsIntoPopulation().run(baseFolder+"/add_data/initial_plans1.0.xml.gz", cemdapOutputFolder+"mergedPlans_filtered.xml.gz", cemdapOutputFolder+"activityConfig.xml");
		
		// Attribute (alter, Geschlecht, Autobesitz) aus dem Vor-Cemdap-Populationsfile übernehmen
		new MergeInputAttributes().run(cemdapOutputFolder+"mergedPlans_filtered.xml.gz", baseFolder+"/cemdap_input/plans.xml.gz");
		new RenameAgents().run(baseFolder+"/add_data/shp/Statistische_Bezirke_mainzone.SHP", cemdapOutputFolder+"mergedPlans_filtered.xml.gz", "NO", TransformationFactory.getCoordinateTransformation("EPSG:25832", "EPSG:4647"));
		
		//Sample für 10 / bzw. 1 % erstellen
		new WobCemdapPlansSample().run(cemdapOutputFolder+"mergedPlans_filtered.xml.gz", 0.1);
		new WobCemdapPlansSample().run(cemdapOutputFolder+"mergedPlans_filtered.xml.gz", 0.01);
		
		// Configs erstellen
		new WobCemdapBasecaseConfigGenerator().run(baseFolder,1.0,1.0);
		new WobCemdapBasecaseConfigGenerator().run(baseFolder,0.1,0.3);
		new WobCemdapBasecaseConfigGenerator().run(baseFolder,0.01,0.03);

	}
}
