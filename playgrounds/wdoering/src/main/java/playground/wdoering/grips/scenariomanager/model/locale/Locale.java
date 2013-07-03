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

package playground.wdoering.grips.scenariomanager.model.locale;


public interface Locale
{
	public String btOK();
	public String btCancel();
	public String btOpen();
	public String btSave();
	public String btRun();
	
	public String infoGripsFile();
	public String infoMatsimFile();
	public String msgOpenGripsConfigFailed();
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
	
}
