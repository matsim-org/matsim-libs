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
package ft.cemdap4H.planspreprocessing;

import org.apache.commons.math.MathException;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * @author  jbischoff
 *
 */
/**
 * Runs several Post-Cemdap-MATSim scripts at once. Note that this is rather for convenience and not very efficiently programmed (Population is read / written several times)
 */
public class BatchPlansPreprocessor {
	
	public static void main(String[] args) throws MathException {
		final String baseFolder = "E:\\Thiel\\Programme\\MatSim\\01_HannoverModel_2.0\\Cemdap\\";
		final String cemdapOutputFolder = baseFolder +"\\output\\";
		final String cemdapInputFolder = baseFolder +"\\input\\";
		final String simulationFolder = "E:\\Thiel\\Programme\\MatSim\\01_HannoverModel_2.0\\Simulation\\";
	
//		//1. Schritt: Pläne mergen (Cemdap generiert i.d.R. fünf MATSim-Pläne pro Person)
//		new PlansMerger().run(cemdapOutputFolder);
//		
//		//2. Schritt: Activity Dauern anpassen, um sinnvolle Aktivitätendauern aus dem Modell zu gewinnen und eine Sensitivität ggü. zeitlichen Änderungen zu erhalten. Dazu wird eine grundlegende Config-Datei erstellt
//		new ActivityTypeTimeConverter().run(cemdapOutputFolder+"mergedPlans.xml.gz", cemdapOutputFolder+"mergedPlans_dur.xml.gz", cemdapOutputFolder+"activityConfig.xml");
//		
////		//Agenten herausfiltern, deren Aktivitäten nicht im Umkreis von 2000m einer Kante stattfinden. Verhindert Artefakte am Rande des Netzes,
//		FilterAgentsnotinNetwork.run(cemdapOutputFolder+"mergedPlans_dur.xml.gz", cemdapOutputFolder+"mergedPlans_dur_dropped.xml.gz", baseFolder+"add_data\\shp\\Statistische_Bezirke_Hannover_Region.shp", "NO", cemdapOutputFolder+"requiredNetworkBoundingBox.txt", "EPSG:25832");
////		
////		// Attribute (alter, Geschlecht, Autobesitz) aus dem Vor-Cemdap-Populationsfile übernehmen
		new MergeInputAttributes().run(cemdapOutputFolder+"mergedPlans_dur_dropped.xml.gz", cemdapInputFolder+"plans1.xml.gz",cemdapOutputFolder+"mergedPlans_dur_dropped_Stud.xml.gz");
		new RenameAgents().run(baseFolder+"\\add_data\\shp\\Statistische_Bezirke_Hannover_Region.shp", cemdapOutputFolder+"mergedPlans_dur_dropped_Stud.xml.gz",cemdapOutputFolder+"mergedPlans_dur_dropped_Stud_Ren.xml.gz", "NO", TransformationFactory.getCoordinateTransformation("EPSG:25832", "EPSG:25832"));
		
		//Assignment der "X"-Größten unternehmen der modellierten Stadt
		AssignAgentsToMajorWorkplaces.run(cemdapOutputFolder+"assignedWorkers.txt", baseFolder+"\\add_data\\shp\\Statistische_Bezirke_Hannover_Region.shp", "NO", cemdapOutputFolder+"mergedPlans_dur_dropped_Stud_Ren.xml.gz", cemdapOutputFolder+"mergedPlans_dur_dropped_Work.xml.gz", "E:\\Thiel\\Programme\\MatSim\\01_HannoverModel_2.0\\Network\\00_Final_Network\\network.xml.gz");
		
		//Refinement der Activity Locations
		new ReassignZonesByAttractiveness().run(baseFolder+"\\add_data\\shp\\Statistische_Bezirke_Hannover_Region.shp", baseFolder+"\\add_data\\Weights_All.txt", cemdapOutputFolder+"mergedPlans_dur_dropped_Work.xml.gz", simulationFolder+"\\input\\finishedPlans.xml.gz");
		
		//Sample für 10 / bzw. 1 % erstellen
		new WobCemdapPlansSample().run(simulationFolder+"\\input\\finishedPlans.xml.gz", 0.1);
		new WobCemdapPlansSample().run(simulationFolder+"\\input\\finishedPlans.xml.gz", 0.01);
		
//		// Configs erstellen
//		new WobCemdapBasecaseConfigGenerator().run(simulationFolder,1.0,1.0);
//		new WobCemdapBasecaseConfigGenerator().run(simulationFolder,0.1,0.3);
//		new WobCemdapBasecaseConfigGenerator().run(simulationFolder,0.01,0.03);

	}
}
