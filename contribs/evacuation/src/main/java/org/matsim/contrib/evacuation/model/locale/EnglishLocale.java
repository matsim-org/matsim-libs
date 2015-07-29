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

package org.matsim.contrib.evacuation.model.locale;

public class EnglishLocale implements Locale {

	private static final String leaveEmptyToCreateNewLabel = "Leave empty to create a new file.";
	private String btOK = "ok";
	private String btOpen = "open";
	private String btSave = "save";
	private String btCancel = "cancel";
	private String btRun = "run";
	private String btRemove = "remove";
	private String btClear = "clear";

	private String popArea = "Area";

	private String btCircular = "circular";
	private String btPolygonal = "polygonal";

	private String infoEvacuationFile = "evacuation settings";
	private String infoMatsimFile = "MATSim settings";
	private String infoMatsimTime = "The calculation may take a long time!";

	private String msgOpenEvacuationConfigFailed = "The evacuation file could not be opened.";
	private String msgOpenMatsimConfigFailed = "The MATSim file could not be opened.";
	private String msgOpenEvacShapeFailed = "The evacuation area shape file could not be opened.";

	private String moduleEvacAreaSelector = "evacuation area";
	private String modulePopAreaSelector = "population areas";
	private String moduleScenearioGenerator = "evacuation scenario";
	private String moduleRoadClosureEditor = "road closures";
	private String modulePTLEditor = "bus stops";
	private String moduleMatsimScenarioGenerator = "MATSim scenario";
	private String moduleEvacuationAnalysis = "analysis";
	private String moduleScenarioXml = "scenario";

	private String titlePopAreas = "population areas";
	private String titleAreaID = "ID";
	private String titlePopulation = "population";

	private String usage = "usage 1: "
			+ "currentmodule.java"
			+ "\n"
			+ "         starts the editor and uses openstreetmap as backround layer\n"
			+ "         requires a working internet connection \n\n"
			+ "usage 2: "
			+ "currentmodule.java"
			+ " -wms <url> -layer <layer name>\n"
			+ "         starts the editor and uses the given wms server to load a backgorund layer\n\n";

	private String labelSelection = "Selection mode";

	public String trafficTypeVeh = "vehicular";
	public String trafficTypePed = "pedestrian";
	public String trafficTypeMixed = "mixed";

	public String labelNetworkFile = "network file";
	public String labelTrafficType = "main traffic type";
	public String labelEvacFile = "evacuation file";
	public String labelPopFile = "population file";
	public String labelOutDir = "output directory";
	public String labelSampleSize = "sample size";
	public String labelDepTime = "departure time distribution";
	public String labelSigma = "sigma";
	public String labelMu = "mu";
	public String labelEarliest = "earliest";
	public String labelLatest = "latest";
	public String btNew = "new";
	public String btSet = "set";
	public String labelCurrentFile = "current file";
	public String msgSameFiles = "The selected file is equal to another selected file!";
	public String msgUnsavedChanges = "Do you want to save the current changes?";
	private Object infoMatsimOverwrite = "Output directory exists and will be renamed";
	public String labelExistingShapeFile = "existing shape file";
	public String labelWMS = "WMS";
	public String labelLayer = "layer";

	@Override
	public String btOK() {
		return btOK;
	}

	@Override
	public String btCancel() {
		return btCancel;
	}

	@Override
	public String btOpen() {
		return btOpen;
	}

	@Override
	public String btSave() {
		return btSave;
	}

	@Override
	public String btRun() {
		return btRun;
	}

	@Override
	public String infoEvacuationFile() {
		return infoEvacuationFile;
	}

	@Override
	public String msgOpenEvacuationConfigFailed() {
		return msgOpenEvacuationConfigFailed;
	}

	@Override
	public String msgOpenEvacShapeFailed() {
		return msgOpenEvacShapeFailed;
	}

	@Override
	public String infoMatsimFile() {
		return infoMatsimFile;
	}

	@Override
	public String msgOpenMatsimConfigFailed() {
		return msgOpenMatsimConfigFailed;
	}

	@Override
	public String btRemove() {
		return btRemove;
	}

	@Override
	public String moduleScenarioXml() {
		return moduleScenarioXml;
	}

	@Override
	public String moduleEvacAreaSelector() {
		return moduleEvacAreaSelector;
	}

	@Override
	public String modulePopAreaSelector() {
		return modulePopAreaSelector;
	}

	@Override
	public String moduleScenarioGenerator() {
		return moduleScenearioGenerator;
	}

	@Override
	public String moduleRoadClosureEditor() {
		return moduleRoadClosureEditor;
	}

	@Override
	public String modulePTLEditor() {
		return modulePTLEditor;
	}

	@Override
	public String moduleMatsimScenarioGenerator() {
		return moduleMatsimScenarioGenerator;
	}

	@Override
	public String moduleEvacuationAnalysis() {
		return moduleEvacuationAnalysis;
	}

	@Override
	public String infoMatsimTime() {
		return infoMatsimTime;
	}

	@Override
	public String titlePopAreas() {
		return titlePopAreas;
	}

	@Override
	public String titlePopulation() {
		return titlePopulation;
	};

	@Override
	public String titleAreaID() {
		return titleAreaID;
	}

	@Override
	public String getUsage() {
		return usage;
	}

	@Override
	public String btCircular() {
		return btCircular;
	}

	@Override
	public String btPolygon() {
		return btPolygonal;
	}

	@Override
	public String labelSelectionMode() {
		return labelSelection;
	}

	@Override
	public String btClear() {
		return btClear;
	}

	@Override
	public String popArea() {
		return popArea;
	}

	@Override
	public String agents() {

		return "agents";
	}

	@Override
	public String trafficTypeVeh() {
		return trafficTypeVeh;
	}

	@Override
	public String trafficTypePed() {
		return trafficTypePed;
	}

	@Override
	public String trafficTypeMixed() {
		return trafficTypeMixed;
	}

	@Override
	public String labelNetworkFile() {
		return labelNetworkFile;
	}

	@Override
	public String labelTrafficType() {
		return labelTrafficType;
	}

	@Override
	public String labelEvacFile() {
		return labelEvacFile;
	}

	@Override
	public String labelPopFile() {
		return labelPopFile;
	}

	@Override
	public String labelSampleSize() {
		return labelSampleSize;
	}

	@Override
	public String labelDepTime() {
		return labelDepTime;
	}

	@Override
	public String labelSigma() {
		return labelSigma;
	}

	@Override
	public String labelMu() {
		return labelMu;
	}

	@Override
	public String labelEarliest() {
		return labelEarliest;
	}

	@Override
	public String labelLatest() {
		return labelLatest;
	}

	@Override
	public String labelOutDir() {
		return labelOutDir;
	}

	@Override
	public String[] getTrafficTypeStrings() {

		return new String[] { trafficTypeVeh, trafficTypePed, trafficTypeMixed };
	}

	@Override
	public String btNew() {
		return btNew;
	}

	@Override
	public String labelCurrentFile() {
		return labelCurrentFile;
	}

	@Override
	public String btSet() {
		return btSet;
	}

	@Override
	public String msgSameFiles() {
		return msgSameFiles;
	}

	@Override
	public String msgUnsavedChanges() {
		return msgUnsavedChanges;
	}

	@Override
	public Object infoMatsimOverwriteOutputDir() {
		return infoMatsimOverwrite;
	}

	@Override
	public String labelExistingShapeFile() {
		return labelExistingShapeFile;
	}

	@Override
	public String labelWMS() {
		return labelWMS;
	}

	@Override
	public String labelLayer() {
		return labelLayer;
	}

	@Override
	public String getLeaveEmptyToCreateNew() {
		return leaveEmptyToCreateNewLabel;
	}

}
