package playground.andreas.intersection.zuerich;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;


/**
 * Reads a simple initial demand from a tab seperated ascii file
 * that has to contain the following columns:
 * PersonId	HomeLocation	Age	Gender	Income	PrimaryActivityType	PrimaryActivityLocation
 * 
 * A simple example of such a table can be seen in: 
 * test/input/org/matsim/demandmodeling/PopulationAsciiFileReader/asciipopulation.txt
 * @author dgrether
 *
 */
public class LSAFileParser implements TabularFileHandler {

	private static final Logger log = Logger.getLogger(LSAFileParser.class);
	
//	private static final String[] HEADER = {"PersonId", "HomeLocation", "Age", "IsFemale", "Income", "PrimaryActivityType", "PrimaryActivityLocation"};
	
	private TabularFileParserConfig tabFileParserConfig;
		
//	private boolean isFirstLine = true;
	
	private HashMap<Integer, LinkedList<String[]>> lsaList = new HashMap<Integer, LinkedList <String[]>>();

	public void startRow(String[] row) throws IllegalArgumentException {
				
		if(row[0].contains("DATA")){
//			log.info("Ignoring: " + row.toString());
		} else {
//			log.info("Added: " + row.toString());
			
			LinkedList<String[]> tempList = this.lsaList.get(Integer.valueOf(row[2]));
			
			if(tempList == null){
				tempList = new LinkedList<String[]>();
				this.lsaList.put(Integer.valueOf(row[2]), tempList);
			}
			
			tempList.add(row);			
		}
		
	}
	
	
	public HashMap<Integer, LinkedList<String[]>> readFile(String filename) throws IOException {
		this.tabFileParserConfig = new TabularFileParserConfig();
		this.tabFileParserConfig.setFileName(filename);
		this.tabFileParserConfig.setDelimiterTags(new String[] {" "}); // \t
//		this.tabFileParserConfig.setDelimiterTags(new String[] {"D"});
		new TabularFileParser().parse(this.tabFileParserConfig, this);
		return this.lsaList;
	}
	
	
	public static void main(final String[] args) {
		
		//Statistics

		
		LSAFileParser myLSAFileParser = new LSAFileParser();
		try {
			
			log.info("Start reading file...");
			HashMap<Integer, LinkedList<String[]>>  lsaData = myLSAFileParser.readFile("E:/ampel/input/all.txt");
			log.info("...finished.");
			
			HashMap<Integer, Set<String>> allSGIds = countTLAndSG(lsaData);			
			
			filterInvalidData(lsaData);
			filterZeroDataEntries(lsaData);
			filterLastGreenIsZeroDataEntries(lsaData);

			HashMap<Integer, Set<String>> passedFilterSGIds = countTLAndSG(lsaData);
			
			filterIncompleteTL(lsaData, allSGIds, passedFilterSGIds);
			
			countTLAndSG(lsaData);	
			
			HashMap<Integer, HashMap<String, DataCollector>> dataSet = generateDataSets(lsaData);
//			removeTLWithInsufficientNumberOfRecord(dataSet);
			removeTLWithAdaptiveControl(dataSet);
			writeDataSet(dataSet);
			computeOutputData(dataSet);
			
						
			log.info("Finished writing data");
						
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void computeOutputData(HashMap<Integer, HashMap<String, DataCollector>> dataSet) throws IOException{
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File("E:/ampel/output/computedData.txt")));
		writer.write("LSA; LSAID; Umlaufzeit");
		int numberOFTL = 0;
		int numberOFSG = 0;
		HashMap<Integer, Integer> umlaufzeit = new HashMap<Integer, Integer>();
		for (Integer tlId : dataSet.keySet()) {			
			int ii = 0;
			double averageUmlaufzeit = 0;
			for (String sgId : dataSet.get(tlId).keySet()) {
				averageUmlaufzeit += dataSet.get(tlId).get(sgId).getAverageU();
				ii++;				
			}
			umlaufzeit.put(tlId, (Integer.valueOf((int) (averageUmlaufzeit / ii))));
			writer.newLine();
			writer.write("LSA; " + tlId + "; " + umlaufzeit.get(tlId));
			numberOFTL++;
		}
		
		writer.newLine();
		writer.write("SG; SGID; LSAID; StartRed");
		for (Integer tlId : dataSet.keySet()) {			
			for (String sgId : dataSet.get(tlId).keySet()) {
				if(((int)dataSet.get(tlId).get(sgId).getAverageRed()) > umlaufzeit.get(tlId).intValue()){
					log.warn(" Got a 'RedStartsTime' of " + ((int)dataSet.get(tlId).get(sgId).getAverageRed()) + ", which is bigger than its umlaufzeit " + umlaufzeit.get((tlId)));
				}
				writer.newLine();
				writer.write("SG; " + sgId + "; " + tlId + "; " + ((int)dataSet.get(tlId).get(sgId).getAverageRed()));
				numberOFSG++;
			}
		
		}
		
		writer.flush(); writer.close();
		log.info(" ComputeOutputData computed data for " + numberOFTL + " TLs and " + numberOFSG + " SGs.");
		
		
	}
	
	private static void writeDataSet(HashMap<Integer, HashMap<String, DataCollector>> dataSet) throws IOException{
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File("E:/ampel/output/output.txt")));
		writer.write("lsaID; SGID; avGreen; avRed; avU; avMinGreen; avMaxGreen; avMinRed; avMaxRed; avMinU; avMaxU; avLastGreen; n; G + R -U");
		int entriesWritten = 0;
		for (Integer tlId : dataSet.keySet()) {
			for (String sgId : dataSet.get(tlId).keySet()) {
				writer.newLine();
				writer.write(dataSet.get(tlId).get(sgId).getAverage());
				entriesWritten++;
			}
		}
		writer.flush(); writer.close();
		log.info(" WriteDataSet wrote " + entriesWritten + " entries.");
	}	
	
	private static HashMap<Integer, HashMap<String, DataCollector>> removeTLWithAdaptiveControl(HashMap<Integer, HashMap<String, DataCollector>> dataSet) throws IOException{
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File("E:/ampel/output/adaptiveControl.txt")));
		writer.write("lsaID; SGID; avGreen; avRed; avU; avMinGreen; avMaxGreen; avMinRed; avMaxRed; avMinU; avMaxU; avLastGreen; n; G + R -U");
		int sgRemoved = 0;
		int sgRemained = 0;
		int tlRemoved = 0;
		TreeSet<Integer> toBeRemovedTL = new TreeSet<Integer>();
		for (Integer tlId : dataSet.keySet()) {
			HashMap<String, DataCollector> currentDataSet = dataSet.get(tlId);		
			
			for (String sgId : currentDataSet.keySet()) {
				if (Math.abs(currentDataSet.get(sgId).getDivergation()) > 1){
					toBeRemovedTL.add(tlId);
					sgRemoved++;
					writer.newLine();
					writer.write(currentDataSet.get(sgId).getAverage());
				} else{
					sgRemained++;
				}
			}			
		}
		
		for (Integer tlId : toBeRemovedTL) {
			dataSet.remove(tlId);
			tlRemoved++;
		}
		writer.flush(); writer.close();
		
		int numberOfSGsActuallyRemained = 0;
		for (Integer tlId : dataSet.keySet()) {
			numberOfSGsActuallyRemained += dataSet.get(tlId).keySet().size();
		}
		
		log.info(" Guess adaptive marked " + sgRemoved + " SGs to be removed and " + sgRemained + " SGs as could remain.\n"
				 + " However, " + tlRemoved + " TLs were marked as to be removed." 
				 + " Thus only " + dataSet.keySet().size() + " TLs with " + numberOfSGsActuallyRemained +  " SGs actually remained.");
		return dataSet;
	}
	
	private static HashMap<Integer, HashMap<String, DataCollector>> removeTLWithInsufficientNumberOfRecord(HashMap<Integer, HashMap<String, DataCollector>> dataSet) throws IOException{
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File("E:/ampel/output/insufficient.txt")));
		writer.write("lsaID; SGID; avGreen; avRed; avU; avMinGreen; avMaxGreen; avMinRed; avMaxRed; avMinU; avMaxU; avLastGreen; n; G + R -U");
		int sgRemoved = 0;
		int sgRemained = 0;
		int tlRemoved = 0;
		TreeSet<Integer> toBeRemovedTL = new TreeSet<Integer>();
		for (Integer tlId : dataSet.keySet()) {
			HashMap<String, DataCollector> currentDataSet = dataSet.get(tlId);		
			
			for (String sgId : currentDataSet.keySet()) {
				if (currentDataSet.get(sgId).getNumberOfEntries() < 50){
					toBeRemovedTL.add(tlId);
					sgRemoved++;
					writer.newLine();
					writer.write(currentDataSet.get(sgId).getAverage());
				} else{
					sgRemained++;
				}
			}			
		}
		
		for (Integer tlId : toBeRemovedTL) {
			dataSet.remove(tlId);
			tlRemoved++;
		}
		writer.flush(); writer.close();
		
		int numberOfSGsActuallyRemained = 0;
		for (Integer tlId : dataSet.keySet()) {
			numberOfSGsActuallyRemained += dataSet.get(tlId).keySet().size();
		}
		
		log.info(" InsufficientFilter marked " + sgRemoved + " SGs to be removed and " + sgRemained + " SGs as could remain.\n"
				 + " However, " + tlRemoved + " TLs were marked as to be removed." 
				 + " Thus only " + dataSet.keySet().size() + " TLs with " + numberOfSGsActuallyRemained +  " SGs actually remained.");
		return dataSet;
	}
	
	private static HashMap<Integer, HashMap<String, DataCollector>> generateDataSets(HashMap<Integer,LinkedList<String[]>> lsaData) throws IOException{
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File("E:/ampel/output/fusedData.txt")));
		writer.write("lsaID; SGID; avGreen; avRed; avU; avMinGreen; avMaxGreen; avMinRed; avMaxRed; avMinU; avMaxU; avLastGreen; n; G + R -U");
		int numberOfFusedEntries = 0;
		int numberOfDataSets = 0;
		HashMap<Integer, HashMap<String, DataCollector>> dataSet = new HashMap<Integer, HashMap<String,DataCollector>>();
		
		for (Integer nodeID : lsaData.keySet()) {

			HashMap<String, DataCollector> currentNodeSG = new HashMap<String, DataCollector>();
			
			for (String[] stringData : lsaData.get(nodeID)) {
				DataCollector sgData = currentNodeSG.get(stringData[1]);
				if (sgData == null){
					sgData = new DataCollector(stringData);
					currentNodeSG.put(stringData[1], sgData);
				}
				sgData.add(stringData);
				numberOfFusedEntries++;
			}			
			dataSet.put(nodeID, currentNodeSG);
			
			for (DataCollector data : currentNodeSG.values()) {
				writer.newLine();
				writer.write(data.getAverage());
				numberOfDataSets++;
			}			
		}
		
		writer.flush(); writer.close();
		log.info(" GenerateDataSets generated " + numberOfDataSets + " data sets from " + numberOfFusedEntries + " entries.");
		return dataSet;
	}
	
	private static void filterIncompleteTL(HashMap<Integer,LinkedList<String[]>> lsaData, HashMap<Integer, Set<String>> allSGIds, HashMap<Integer, Set<String>> passedFilterSGIds){
		int numberOfCompleteSG = 0;
		int numberOfIncompleteSG = 0;
		LinkedList<Integer> incompleteTL = new LinkedList<Integer>();
		for (Integer tlID : lsaData.keySet()) {
			if (passedFilterSGIds.get(tlID).size() == allSGIds.get(tlID).size()) {
				numberOfCompleteSG++;				
			} else {
				numberOfIncompleteSG++;
				incompleteTL.add(tlID);
			}
		}
		for (Integer tlID : incompleteTL) {
			lsaData.remove(tlID);
		}
		log.info(" IncompleteTLFilter\t removed " + numberOfIncompleteSG + " TLs.\t " + numberOfCompleteSG + " TLs remained.");
	}
	
	private static HashMap<Integer, Set<String>> countTLAndSG(HashMap<Integer,LinkedList<String[]>> lsaData){
		HashMap<Integer, Set<String>> completeSGSet = new HashMap<Integer, Set<String>>();
		int numberOfSG = 0;
		int numberOfLSA = 0;
		for (Integer tlID : lsaData.keySet()) {
			numberOfLSA++;
			Set<String> currentAllSGSet = completeSGSet.get(tlID);
			if (currentAllSGSet == null) {
				currentAllSGSet = new TreeSet<String>();
				completeSGSet.put(tlID, currentAllSGSet);
			}
			for (String[] stringData : lsaData.get(tlID)) {
				if(currentAllSGSet.add(stringData[1])){
					numberOfSG++;
				}
			}			
		}
		log.info(" Counted " + numberOfLSA + " TL with " + numberOfSG + " SGs");
		return completeSGSet;
	}
	
	private static void filterZeroDataEntries(HashMap<Integer,LinkedList<String[]>> lsaData) throws IOException{
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File("E:/ampel/output/zeroData.txt")));
		int zeroCounter = 0;
		int validCounter = 0;
		for (Integer tlID : lsaData.keySet()) {
			LinkedList<String[]> validEntries = new LinkedList<String[]>();			
			for (String[] stringData : lsaData.get(tlID)) {
				if(!stringData[11].equalsIgnoreCase("0") && !stringData[12].equalsIgnoreCase("0") &&
						!stringData[13].equalsIgnoreCase("0") && !stringData[14].equalsIgnoreCase("0") &&
						!stringData[15].equalsIgnoreCase("0") && !stringData[16].equalsIgnoreCase("0")){
					validEntries.add(stringData);
					validCounter++;
				} else {					
					writeDataToFile(writer, stringData);
					zeroCounter++;
				}
			}
			lsaData.put(tlID, validEntries);
		}
		writer.flush(); writer.close();
		log.info(" ZeroDataEntryFilter\t removed " + zeroCounter + " Entries.\t" + validCounter + " Entries remained.");
	}	
	
	private static void filterLastGreenIsZeroDataEntries(HashMap<Integer,LinkedList<String[]>> lsaData) throws IOException{
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File("E:/ampel/output/lastGreenZero.txt")));
		int zeroCounter = 0;
		int validCounter = 0;
		for (Integer tlID : lsaData.keySet()) {
			LinkedList<String[]> validEntries = new LinkedList<String[]>();			
			for (String[] stringData : lsaData.get(tlID)) {
				if(!stringData[19].equalsIgnoreCase("0")){
					validEntries.add(stringData);
					validCounter++;
				} else {					
					writeDataToFile(writer, stringData);
					zeroCounter++;
				}
			}
			lsaData.put(tlID, validEntries);
		}
		writer.flush(); writer.close();
		log.info(" LastGreenIsZeroFilter\t removed " + zeroCounter + " Entries.\t" + validCounter + " Entries remained.");
	}
	
	/**
	 * Removes entries marked as invalid.
	 * @param lsaData
	 * @throws IOException
	 */
	private static void filterInvalidData(HashMap<Integer,LinkedList<String[]>> lsaData) throws IOException{
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File("E:/ampel/output/invalid.txt")));
		int invalidCounter = 0;
		int validCounter = 0;
		for (Integer tlID : lsaData.keySet()) {
			LinkedList<String[]> validEntries = new LinkedList<String[]>();			
			for (String[] stringData : lsaData.get(tlID)) {
				if (stringData[4].equalsIgnoreCase("-1")){
					validEntries.add(stringData);
					validCounter++;
				} else {					
					writeDataToFile(writer, stringData);
					invalidCounter++;
				}
			}
			lsaData.put(tlID, validEntries);
		}
		writer.flush(); writer.close();
		log.info(" InvalidDataFilter\t removed " + invalidCounter + " Entries.\t" + validCounter + " Entries remained.");
	}
	
	private static void writeDataToFile(BufferedWriter writer, String[] data) throws IOException{
		for (int i = 0; i < data.length; i++) {
			String string = data[i];
			writer.write(string);
			writer.write(",\t");
		}					
		writer.newLine();
	}
}

class DataCollector {

	private double sumOfMinGreen = 0;
	private double sumOfMaxGreen = 0;
	private double sumOfMinRed = 0;		
	private double sumOfMaxRed = 0;
	private double sumOfMinU = 0;
	private double sumOfMaxU = 0;
	private double sumLastGreen = 0;

	private int numberOfEntries = 0;
	private final String signalGroupID;
	private final String lsaID;
	
	public DataCollector(String[] entryData){
		this.signalGroupID = entryData[1];
		this.lsaID = entryData[2];
	}

	public void add(String[] newEntry){
		this.sumOfMinGreen += Integer.parseInt(newEntry[11]);
		this.sumOfMaxGreen += Integer.parseInt(newEntry[14]);
		this.sumOfMinRed += Integer.parseInt(newEntry[12]);
		this.sumOfMaxRed += Integer.parseInt(newEntry[15]);
		this.sumOfMinU += Integer.parseInt(newEntry[13]);
		this.sumOfMaxU += Integer.parseInt(newEntry[16]);
		this.sumLastGreen += Integer.parseInt(newEntry[19]);
		this.numberOfEntries ++;
	}

	public String getAverage(){
		double averageLastGreen = this.sumLastGreen / this.numberOfEntries / 10;

		return new String(this.lsaID + "; " + this.signalGroupID + "; "
				+ getAverageGreen() + "; " + getAverageRed() + "; " + getAverageU() + "; "
				+ getAverageMinGreen() + "; " + getAverageMaxGreen() + "; " 
				+ getAverageMinRed() + "; " + getAverageMaxRed() + "; " 
				+ getAverageMinU() + "; " + getAverageMaxU() + "; "
				+ averageLastGreen + "; "
				+ this.numberOfEntries + "; " + getDivergation());
	}
	
	public double getDivergation(){
		return getAverageGreen() + getAverageRed() - getAverageU();
	}
	public int getNumberOfEntries(){
		return this.numberOfEntries;
	}
	
	private double getAverageGreen(){
		return (getAverageMinGreen() + getAverageMaxGreen())/2;
	}
	public double getAverageRed(){
		return (getAverageMinRed() + getAverageMaxRed())/2;
	}
	public double getAverageU(){
		return (getAverageMinU() + getAverageMaxU())/2;
	}

	private double getAverageMinGreen(){
		return this.sumOfMinGreen / this.numberOfEntries / 10;
	}
	private double getAverageMaxGreen(){
		return this.sumOfMaxGreen / this.numberOfEntries / 10;
	}
	private double getAverageMinRed(){
		return this.sumOfMinRed / this.numberOfEntries / 10;
	}
	private double getAverageMaxRed(){
		return this.sumOfMaxRed / this.numberOfEntries / 10;
	}
	private double getAverageMinU(){
		return this.sumOfMinU / this.numberOfEntries / 10;
	}
	private double getAverageMaxU(){
		return this.sumOfMaxU / this.numberOfEntries / 10;
	}
}


