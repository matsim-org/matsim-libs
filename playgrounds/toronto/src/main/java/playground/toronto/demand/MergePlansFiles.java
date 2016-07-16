package playground.toronto.demand;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;

public class MergePlansFiles {

	private static FileFilter matsimPlansFilefilter = new FileFilter() {
		@Override
		public String getDescription() {
			return "MATSim plans file in *.xml or *.gz format";
		}
		@Override
		public boolean accept(File f) {
			return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith( ".xml" ) || f.getName().toLowerCase(Locale.ROOT).endsWith( ".gz" );
		}
	};
	
	private static FileFilter matsimPlansFileFilterXML = new FileFilter() {
		@Override
		public String getDescription() {
			return "MATSim plans file in *.xml or *.gz format";
		}
		@Override
		public boolean accept(File f) {
			return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith( ".xml" );
		}
	};
	
	private static FileFilter matsimNetworkFileFilter = new FileFilter() {
		@Override
		public String getDescription() {
			return "MATSim network file in *.xml or *.gz format";
		}
		@Override
		public boolean accept(File f) {
			return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith( ".xml" );
		}
	};
	
	public static void main(String[] args){
		
		String networkFileName = openFile("Select a network file", matsimNetworkFileFilter);
		
		String currentPlansFile = null;
		int plansNumber = 1;
		String dialogTitle = "Select plan file #" + plansNumber++;
		currentPlansFile = openFile(dialogTitle, matsimPlansFilefilter);		
		ArrayList<String> files = new ArrayList<String>();
		
		while (currentPlansFile != null){
			files.add(currentPlansFile);
			dialogTitle = "Select plan file #" + plansNumber++;
			currentPlansFile = openFile(dialogTitle, matsimPlansFilefilter);
		}
		
		Population basePop;// = openPlansFile(files.get(0), networkFileName);
		Network net;
		{
			Tuple<Population, Network> t = openPlansFile(files.get(0), networkFileName);
			basePop = t.getFirst();
			net = t.getSecond();
		}
		for (int i = 1; i <  files.size(); i++){
			String file = files.get(i);
			
			Population nextPop = openPlansFile(file, networkFileName).getFirst();
			for (Person p : nextPop.getPersons().values()){
				try {
					basePop.addPerson(p);
				} catch (IllegalArgumentException e) {
					System.err.println(e.getMessage());
				}
			}
		}
		
		String outfile = saveFile("Save plans file", matsimPlansFileFilterXML);
		if (outfile == null) System.exit(0);
		
		new PopulationWriter(basePop, net).write(outfile);
	}
	
	private static Tuple<Population, Network> openPlansFile(String plansFileName, String networkFileName){
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFileName);
		config.plans().setInputFile(plansFileName);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Tuple<Population, Network> t = new Tuple<Population, Network>(scenario.getPopulation(), scenario.getNetwork());
		
		return t; 
	}
	
	private static String openFile(String dialogTitle, FileFilter ff){
		
		String fileName = "";
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle(dialogTitle);
		fc.setFileFilter(ff);
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		int state = fc.showOpenDialog(null);
		if (state == JFileChooser.APPROVE_OPTION){
			fileName =  fc.getSelectedFile().getAbsolutePath();
		}else if (state == JFileChooser.CANCEL_OPTION) return null;
		
		if (fileName.equals("")) return null;
		
		return fileName;		
	}
	
	private static String saveFile(String dialogTitle, FileFilter ff){
		String fileName = "";
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle(dialogTitle);
		fc.setFileFilter(ff);
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		int state = fc.showSaveDialog(null);
		if (state == JFileChooser.APPROVE_OPTION){
			fileName =  fc.getSelectedFile().getAbsolutePath();
		}else if (state == JFileChooser.CANCEL_OPTION) return null;
		
		if (fileName.equals("")) return null;
		
		return fileName;	
	}
}
