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

public class GermanLocale implements Locale {

	private static final String leaveEmptyToCreateNewLabel = "leer fuer neu";
	private String btOK = "ok";
	private String btOpen = "öffnen";
	private String btSave = "speichern";
	private String btCancel = "abbrechen";
	private String btRun = "ausführen";
	private String btRemove = "entfernen";
	private String btClear = "zurücksetzen";

	private String popArea = "Gebiet";

	private String btCircular = "Kreis";
	private String btPolygonal = "Polygon";

	private String infoEvacuationFile = "Evacuation Konfiguration";
	private String infoMatsimFile = "MATSim Konfiguration";
	private String infoMatsimTime = "Die Berechnung kann unter Umständen sehr viel Zeit beanspruchen!";

	private String msgOpenEvacuationConfigFailed = "Die Evacuation-Datei konnte nicht geöffnet werden.";
	private String msgOpenMatsimConfigFailed = "Die MATSim-Datei konnte nicht geöffnet werden.";
	private String msgOpenEvacShapeFailed = "Die Shape-Datei des Evakuierungsgebietes konnte nicht geöffnet werden.";

	private String moduleEvacAreaSelector = "Evakuierungsgebiet";
	private String modulePopAreaSelector = "Populationen";
	private String moduleScenearioGenerator = "Evacuation Scenario";
	private String moduleRoadClosureEditor = "Straßensperrungen";
	private String modulePTLEditor = "Bushaltestellen";
	private String moduleMatsimScenarioGenerator = "MATSim Scenario";
	private String moduleEvacuationAnalysis = "Analyse";
	private String moduleScenarioXml = "Evacuation Szenario";

	private String titlePopAreas = "Populationsgebiete";
	private String titleAreaID = "ID";
	private String titlePopulation = "Population";

	private String usage = "usage 1: "
			+ "currentmodule.java"
			+ "\n"
			+ "         startet das Modul und verwendet Openstreetmap als Karte\n"
			+ "         (benötigt eine Internet-Verbindung) \n\n"
			+ "usage 2: "
			+ "currentmodule.java"
			+ " -wms <url> -layer <layer name>\n"
			+ "         startet das module und verwendet den über den Parameter angegebenen WMS-Layer\n\n";

	private String labelSelection = "Auswahlmodus";

	public String trafficTypeVeh = "Fahrzeuge";
	public String trafficTypePed = "Fußgänger";
	public String trafficTypeMixed = "gemischt";

	public String labelNetworkFile = "Netzwerkdatei";
	public String labelTrafficType = "Primäre Verkehrsart";
	public String labelEvacFile = "Evakuierungsdatei";
	public String labelPopFile = "Populationsdatei";
	public String labelOutDir = "Ausgabeverzeichnis";
	public String labelSampleSize = "Samplegröße";
	public String labelDepTime = "Verteilung der Abfahrtzeiten";
	public String labelSigma = "Sigma";
	public String labelMu = "Mu";
	public String labelEarliest = "Frühste";
	public String labelLatest = "Späteste";
	public String btNew = "Neu";
	public String labelCurrentFile = "Aktuelle Datei";
	public String btSet = "Setzen";
	public String msgSameFiles = "Die angegebene Datei stimmt mit einer anderen ausgewählten Datei überein!";
	public String msgUnsavedChanges = "Möchten Sie die aktuellen Änderungen speichern?";
	public Object infoMatsimOverwrite = "Ausgabeverzeichnis existiert und wird umbenannt.";
	public String labelExistingShapeFile = "Existierende Shape-Datei";
	public String labelWMS = "WMS";
	public String labelLayer = "Ebene";

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
		return "Agenten";
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
	public String labelExistingShapeFile() {
		return labelExistingShapeFile;
	}

	@Override
	public Object infoMatsimOverwriteOutputDir() {
		return infoMatsimOverwrite;
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
