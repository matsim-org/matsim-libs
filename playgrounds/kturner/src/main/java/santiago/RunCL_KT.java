package santiago;

import java.text.DecimalFormat;

public class RunCL_KT {
	
	//Input Dir KT: 
			final static String INPUT_DIR = "F:/Arbeit-VSP/Santiago/Kai_und_Daniel/Counts/" ;
			final static String OUTPUT_DIR = "F:/Arbeit-VSP/Santiago/Kai_und_Daniel/Counts/" ;
			
//			//Input Dir DH: 
//			private static final String INPUT_DIR = "" ;
//			private static final String OUTPUT_DIR = "" ;

			//Filename without extention
			
			final static String CSIdFILE_NAME = "CSId-LinkId";
			final static String CSDATAFILE_NAME = "T_FLUJO_TASA";
			final static String COUNTFILE_NAME = "counts_VEH" ;	//Output-Countfilen_name
//			private static final String CONFIGFILE_NAME = "config";
			
			//File
			final static String CSIdFILE = INPUT_DIR + CSIdFILE_NAME + ".csv" ;
			final static String CSDATAFILE = INPUT_DIR + CSDATAFILE_NAME + ".csv" ;
			
	
	public static void main(String args[]){

		for (int i=1; i<=13; i++){
			String vehCat = "C"+ String.format("%2s", new DecimalFormat("00").format(i));
			System.out.println(vehCat);
			final String COUNTFILE = OUTPUT_DIR + COUNTFILE_NAME + "_" + vehCat + ".xml" ;	//Output-Countfile
			CSVToCountingStations converter = new CSVToCountingStations(COUNTFILE, CSIdFILE, CSDATAFILE, vehCat);
			converter.createIds();
			converter.readCountsFromCSV();
			converter.writeCountsToFile();
		}
		System.out.println("#### Done.");
	}
	


}
