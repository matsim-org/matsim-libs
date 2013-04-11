package playground.wdoering.grips.scenariomanager.model.locale;

import javax.swing.Icon;

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
