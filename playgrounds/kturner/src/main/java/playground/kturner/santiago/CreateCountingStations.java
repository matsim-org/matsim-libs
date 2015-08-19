package playground.kturner.santiago;

import java.io.File;
import java.text.DecimalFormat;

public class CreateCountingStations {

	private static final String svnWorkingDir = "../../shared-svn/"; 	//Path: KT (SVN-checkout)
	private static final String workingDirInputFiles = svnWorkingDir + "Kai_und_Daniel/inputFromElsewhere/counts/" ;
	private static final String outputDir = svnWorkingDir + "Kai_und_Daniel/inputForMATSim/creationResults/counts/" ; //outputDir of this class -> input for Matsim (KT)

	//Position (linkId) of Counting Stations
	private static final String CSIdFILE_NAME = "CSId-LinkId_merged";		
	private static final String CSIdFILE = workingDirInputFiles + CSIdFILE_NAME + ".csv" ;


	public static void main(String args[]){

		File file = new File(outputDir);
		System.out.println("Verzeichnis " + file + " erstellt: "+ file.mkdirs());	

		String csDataFile_Name;
		String countfile_Name;
		String csDataFile;

		//#Counts in persons
		csDataFile_Name = "T_VIAJESTEMP"; //Number of Persons = counts per Vehicle * Factor (#Persons / vehicle)
		csDataFile = workingDirInputFiles + csDataFile_Name + ".csv" ;
		countfile_Name = "counts_merged_PERS" ;	//Output-Countfile_name

		createCsFiles(countfile_Name, csDataFile);

		//#Counts in vehicles
		csDataFile_Name = "T_FLUJO_TASA"; //Number of Persons = counts per Vehicle * Factor (#Persons / vehicle)
		csDataFile = workingDirInputFiles + csDataFile_Name + ".csv" ;
		countfile_Name = "counts_merged_VEH" ;	//Output-Countfile_name

		createCsFiles(countfile_Name, csDataFile);

		System.out.println("#### Done.");
	}

	private static void createCsFiles(String countfile_Name, String csDataFile) {
		for (int i=1; i<=13; i++){
			String vehCat = "C"+ String.format("%2s", new DecimalFormat("00").format(i));
			System.out.println(vehCat);
			String countfile = outputDir + countfile_Name + "_" + vehCat + ".xml" ;	//Output-Countfile
			CSVToCountingStations converter = new CSVToCountingStations(countfile, CSIdFILE, csDataFile, vehCat);
			converter.createIds();
			converter.readCountsFromCSV();
			converter.writeCountsToFile();
		}
	}



}
