package playground.wdoering.grips.scenariomanager.model.locale;

public class GermanLocale implements Locale
{
	
	private String btOK = "ok"; 
	private String btOpen = "öffnen"; 
	private String btSave = "speichern"; 
	private String btCancel = "abbrechen"; 
	private String btRun = "ausführen";
	private String btRemove = "entfernen";
	
	private String infoGripsFile = "Grips Konfiguration";
	private String infoMatsimFile = "MATSim Konfiguration";
	private String infoMatsimTime = "Die Berechnung kann unter Umständen sehr viel Zeit beanspruchen!";
	
	private String msgOpenGripsConfigFailed      = "Die Grips-Datei konnte nicht geöffnet werden."; 
	private String msgOpenMatsimConfigFailed     = "Die MATSim-Datei konnte nicht geöffnet werden."; 
	private String msgOpenEvacShapeFailed 		 = "Die Shape-Datei des Evakuierungsgebietes konnte nicht geöffnet werden.";

//	private String moduleEvacAreaSelector 		 = "Evakuierungsgebiet markieren";
//	private String modulePopAreaSelector 		 = "Populationen markieren";
//	private String moduleScenearioGenerator 	 = "Grips Scenario generieren";
//	private String moduleRoadClosureEditor 		 = "Straßensperrungen markieren";
//	private String modulePTLEditor 				 = "Bushaltestellen markieren";
//	private String moduleMatsimScenarioGenerator = "MATSim Scenario erzeugen";
//	private String moduleEvacuationAnalysis 	 = "Evakuierungsanalyse";
//	
	private String moduleEvacAreaSelector 		 = "Evakuierungsgebiet";
	private String modulePopAreaSelector 		 = "Populationen";
	private String moduleScenearioGenerator 	 = "Grips Scenario";
	private String moduleRoadClosureEditor 		 = "Straßensperrungen";
	private String modulePTLEditor 				 = "Bushaltestellen";
	private String moduleMatsimScenarioGenerator = "MATSim Scenario";
	private String moduleEvacuationAnalysis 	 = "Analyse";
	
	private String titlePopAreas = "Populationsgebiete";
	private String titleAreaID = "ID";
	private String titlePopulation = "Population";
	
	
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
	
}
