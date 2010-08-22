package playground.wrashid.parkingSearch.planLevel.strc2010;

import java.io.File;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.CollectLogMessagesAppender;
import org.matsim.core.utils.io.IOUtils;

import playground.wrashid.lib.GlobalRegistry;
import playground.wrashid.lib.Reflection;
import playground.wrashid.lib.RunLib;
import playground.wrashid.parkingSearch.planLevel.scenario.BaseControlerScenarioOneLiner;

public class RunSeries {
	public static String inputBasePath="H:/data/experiments/STRC2010/input/";
	public static String outputBasePath="H:/data/experiments/STRC2010/output/";
	public static String configBaseName="config";
	
	
	public static String getConfigName(int runNumber){
		return inputBasePath+configBaseName+runNumber+".xml";
	}
	
	public static String getOutputFolderFullPath(int runNumber){
		return outputBasePath+"run"+runNumber+"/";
	}
	
	public static Controler getControler(int runNumber){
		GlobalRegistry.runNumber=runNumber;
		GlobalRegistry.doPrintGraficDataToConsole=true;
		Controler controler= BaseControlerScenarioOneLiner.getControler(getConfigName(runNumber));
		addOutputFolderPathToControler(controler);
		return controler;
	}
	
	private static void addOutputFolderPathToControler(Controler controler){
		String outputFolder=getOutputFolderFullPath(GlobalRegistry.runNumber);
		new File(outputFolder).mkdir();
		String iterationFoler=getOutputFolderFullPath(GlobalRegistry.runNumber)+"ITERS/";
		new File(iterationFoler).mkdir();
		
		controler.addControlerListener(new StartupListener() {
			
			@Override
			public void notifyStartup(StartupEvent event) {
				String outputFolder=getOutputFolderFullPath(GlobalRegistry.runNumber);
				Controler controler= event.getControler();
				ControlerIO controlerIO=new ControlerIO(outputFolder);
				
				//TODO: write tests for this, which fail when the variable or method names change (and as such can be easily adapted).
				
				Reflection.setField(controler, "controlerIO", controlerIO);
				
				Reflection.setField(controler.getConfig().controler(), "outputDirectory", outputFolder);
				
				Reflection.setField(controler, "collectLogMessagesAppender", new CollectLogMessagesAppender());
				
				Reflection.callMethod(new IOUtils(), "closeOutputDirLogging", null);
				
				Reflection.callMethod(controler, "initLogging", null);
			}
		});
	}
	
}
