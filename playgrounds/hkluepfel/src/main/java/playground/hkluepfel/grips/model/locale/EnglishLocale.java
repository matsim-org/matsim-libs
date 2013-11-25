/* *********************************************************************** *
 * project: org.matsim.*
 * MyMapViewer.java
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

package org.matsim.contrib.grips.model.locale;


public class EnglishLocale implements Locale
{
	
	private String btOK = "ok"; 
	private String btOpen = "open"; 
	private String btSave = "save"; 
	private String btCancel = "cancel"; 
	private String btRun = "run";
	private String btRemove = "remove";
	
	private String infoGripsFile = "Grips settings";
	private String infoMatsimFile = "MATSim settings";
	private String infoMatsimTime = "The calculation may take a long time!";
	
	private String msgOpenGripsConfigFailed      = "The Grips file could not be opened."; 
	private String msgOpenMatsimConfigFailed     = "The MATSim file could not be opened."; 
	private String msgOpenEvacShapeFailed 		 = "The evacuation area shape file could not be opened.";

	private String moduleEvacAreaSelector 		 = "evacuation area";
	private String modulePopAreaSelector 		 = "population areas";
	private String moduleScenearioGenerator 	 = "Grips scenario";
	private String moduleRoadClosureEditor 		 = "road closures";
	private String modulePTLEditor 				 = "bus stops";
	private String moduleMatsimScenarioGenerator = "MATSim scenario";
	private String moduleEvacuationAnalysis 	 = "analysis";
	
	private String titlePopAreas = "population areas";
	private String titleAreaID = "ID";
	private String titlePopulation = "population";
	
	private String usage = "usage 1: " + "currentmodule.java" +"\n" +
						   "         starts the editor and uses openstreetmap as backround layer\n" +
						   "         requires a working internet connection \n\n" + 
						   "usage 2: " + "currentmodule.java"  + " -wms <url> -layer <layer name>\n" +
				           "         starts the editor and uses the given wms server to load a backgorund layer\n\n";
	
	
	
	@Override
	public String btOK()
	{
		return btOK;
	}

	@Override
	public String btCancel()
	{
		return btCancel;
	}

	@Override
	public String btOpen()
	{
		return btOpen;
	}

	@Override
	public String btSave()
	{
		return btSave;
	}
	
	@Override
	public String btRun()
	{
		return btRun;
	}
	
	@Override
	public String infoGripsFile()
	{
		return infoGripsFile;
	}
	
	@Override
	public String msgOpenGripsConfigFailed()
	{
		return msgOpenGripsConfigFailed;
	}
	
	@Override
	public String msgOpenEvacShapeFailed()
	{
		return msgOpenEvacShapeFailed;
	}
	
	@Override
	public String infoMatsimFile()
	{
		return infoMatsimFile;
	}
	
	@Override
	public String msgOpenMatsimConfigFailed()
	{
		return msgOpenMatsimConfigFailed;
	}
	
	@Override
	public String btRemove()
	{
		return btRemove;
	}

	@Override
	public String moduleEvacAreaSelector()
	{
		return moduleEvacAreaSelector;
	}

	@Override
	public String modulePopAreaSelector()
	{
		return modulePopAreaSelector;
	}

	@Override
	public String moduleScenarioGenerator()
	{
		return moduleScenearioGenerator;
	}

	@Override
	public String moduleRoadClosureEditor()
	{
		return moduleRoadClosureEditor;
	}

	@Override
	public String modulePTLEditor()
	{
		return modulePTLEditor;
	}

	@Override
	public String moduleMatsimScenarioGenerator()
	{
		return moduleMatsimScenarioGenerator;
	}

	@Override
	public String moduleEvacuationAnalysis()
	{
		return moduleEvacuationAnalysis;
	}
	
	@Override
	public String infoMatsimTime()
	{
		return infoMatsimTime;
	}
	
	@Override
	public String titlePopAreas()
	{
		return titlePopAreas;
	}

	@Override
	public String titlePopulation()
	{
		return titlePopulation;
	};
	
	@Override
	public String titleAreaID()
	{
		return titleAreaID;
	}

	@Override
	public String getUsage() {
		return usage;
	}
	
}
