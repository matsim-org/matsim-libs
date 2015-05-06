package santiago;

public class RunCL_KT {
	
	//Input Dir KT: 
			final static String INPUT_DIR = "F:/Arbeit-VSP/Santiago/Kai_und_Daniel/Counts/" ;
			final static String OUTPUT_DIR = "F:/Arbeit-VSP/Santiago/Kai_und_Daniel/Counts/" ;
			
//			//Input Dir DH: 
//			private static final String INPUT_DIR = "" ;
//			private static final String OUTPUT_DIR = "" ;

			//Filename without extention
			
			final static String CSIdFILE_NAME = "CSId-LinkId";
			final static String CSDATAFILE_NAME = "DB_Counts_Test_KT";
			final static String COUNTFILE_NAME = "counts" ;
//			private static final String CONFIGFILE_NAME = "config";
			
			//File
			final static String CSIdFILE = INPUT_DIR + CSIdFILE_NAME + ".csv" ;
			final static String CSDATAFILE = INPUT_DIR + CSDATAFILE_NAME + ".csv" ;
			final static String COUNTFILE = OUTPUT_DIR + COUNTFILE_NAME + ".xml" ;
	
	public static void main(String args[]){

		//for creating CS
		CSVToCountingStations converter = new CSVToCountingStations(COUNTFILE, CSIdFILE, CSDATAFILE);
		converter.createIds();
		converter.readCountsFromCSV();
	}
	


}
