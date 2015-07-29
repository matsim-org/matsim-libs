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

public interface Locale {
	public String btOK();

	public String btCancel();

	public String btOpen();

	public String btSave();

	public String btRun();

	public String btSet();

	public String infoEvacuationFile();

	public String infoMatsimFile();

	public String msgOpenEvacuationConfigFailed();

	public String msgOpenMatsimConfigFailed();

	public String msgOpenEvacShapeFailed();

	public String btRemove();

	public String moduleEvacAreaSelector();

	public String modulePopAreaSelector();

	public String moduleScenarioGenerator();

	public String moduleRoadClosureEditor();

	public String modulePTLEditor();

	public String moduleMatsimScenarioGenerator();

	public String moduleEvacuationAnalysis();

	public String infoMatsimTime();

	public String titlePopAreas();

	public String titlePopulation();

	public String titleAreaID();

	public String getUsage();

	public String btCircular();

	public String btPolygon();

	public String labelSelectionMode();

	public String btClear();

	public String popArea();

	public String moduleScenarioXml();

	public String agents();

	public String trafficTypeVeh();

	public String trafficTypePed();

	public String trafficTypeMixed();

	public String[] getTrafficTypeStrings();

	public String labelNetworkFile();

	public String labelTrafficType();

	public String labelEvacFile();

	public String labelPopFile();

	public String labelOutDir();

	public String labelSampleSize();

	public String labelDepTime();

	public String labelSigma();

	public String labelMu();

	public String labelEarliest();

	public String labelLatest();

	public String labelWMS();

	public String labelLayer();

	public String labelCurrentFile();

	public String btNew();

	public String msgSameFiles();

	public String msgUnsavedChanges();

	public Object infoMatsimOverwriteOutputDir();

	public String labelExistingShapeFile();

	public String getLeaveEmptyToCreateNew();

}
