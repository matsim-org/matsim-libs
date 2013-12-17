/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.vsp.bvwp;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

import playground.vsp.bvwp.MultiDimensionalArray.Attribute;
import playground.vsp.bvwp.MultiDimensionalArray.DemandSegment;
import playground.vsp.bvwp.MultiDimensionalArray.Mode;

/**
 * @author droeder
 *
 */
public class IVVReader {

	private static final Logger log = Logger.getLogger(IVVReader.class);
	private IVVReaderConfigGroup config;
	private ScenarioForEvalData data;


	public IVVReader(IVVReaderConfigGroup config, ScenarioForEvalData data) {
		this.config = config;
		this.data = data;
	}
	
	
	void read(){
		// TODO currently implemented for debbuging-reasons. For later revisions
		// all data from the files should be added to this.data
		log.info("Start...");
		Map<String, Set<Id>> type2ids = new TreeMap<String, Set<Id>>();
		
		ScenarioForEvalData data = new ScenarioForEvalData();
		read(config.getDemandMatrixFile(), new DemandHandler(data));
		type2ids.put("00", data.getAllRelations());
		
//		data = new ScenarioForEvalData();		
//		read(config.getRemainingDemandMatrixFile(), new DemandRemainingHandler(data));
//		type2ids.put("01", data.getAllRelations());
//
//		data = new ScenarioForEvalData();
//		read(config.getNewDemandMatrixFile(), new DemandNewHandler(data));
//		type2ids.put("03", data.getAllRelations());
//		
//		data = new ScenarioForEvalData();
//		read(config.getDroppedDemandMatrixFile(), new DemandDroppedHandler(data));
//		type2ids.put("04", data.getAllRelations());
		
		data = new ScenarioForEvalData();		
		read(config.getTravelTimesBaseMatrixFile(), new TravelTimeBaseHandler(data));
		type2ids.put("06", data.getAllRelations());
		
//		data = new ScenarioForEvalData();
//		read(config.getTravelTimesStudyMatrixFile(), new TravelTimeStudyHandler(data));
//		type2ids.put("07", data.getAllRelations());
		
		data = new ScenarioForEvalData();
		read(config.getImpedanceMatrixFile(), new ImpedanceHandler(data));
		type2ids.put("06-15", data.getAllRelations());
		
		data = new ScenarioForEvalData();		
		read(config.getImpedanceShiftedMatrixFile(), new ImpedanceShiftedHandler(data));
		type2ids.put("16", data.getAllRelations());
		
		log.info("finished...");
		analyzeAndPrint(type2ids);
	}
	


	/**
	 * @param type2ids
	 */
	private void analyzeAndPrint(Map<String, Set<Id>> type2ids) {
		
		StringBuffer b = new StringBuffer();
		String del = ";";
		b.append("type" + del + "size\n");
		for(Entry<String, Set<Id>> row: type2ids.entrySet()){
			b.append(row.getKey() + del + row.getValue().size() + "\n");
		}
		b.append("\n\n" + del);
		for(String s: type2ids.keySet()){
			b.append(s + del);
		}
		b.append("\n");
		for(Entry<String, Set<Id>> row: type2ids.entrySet()){
			b.append(row.getKey() + del);
			for(Entry<String, Set<Id>> col: type2ids.entrySet()){
				if(row.getKey().equals("00") && col.getKey().equals("06")) prn = true;
				b.append(doSomething(row.getValue(), col.getValue()) + del);
				prn = false;
			}
			b.append("\n");
		}
		System.out.println(b.toString());
	}

	private boolean prn = false;
	/**
	 * @param value
	 * @param value2
	 */
	private String doSomething(Set<Id> row, Set<Id> col) {
		int i = 0;
		for(Id id: col){
			if(!row.contains(id)){
				if(prn)	System.out.println(id.toString());
				i--;
			}
		}
		return String.valueOf(i);
	}


	/**
	 * @param file
	 * @param handler
	 */
	private void read(String file, TabularFileHandler handler) {
		TabularFileParserConfig config = new TabularFileParserConfig();
		log.info("parsing " + file);
		config.setDelimiterTags(new String[]{";"});
		config.setCommentTags(new String[]{"#", " #"});
		config.setFileName(file);
		new TabularFileParser().parse(config, handler);
		log.info("done. (parsing " + file + ")");
	}

	
	//############## TabularFileHandler ###################################
	
	private static class DemandHandler implements TabularFileHandler{

		private ScenarioForEvalData data;

		/**
		 * @param data
		 */
		public DemandHandler(ScenarioForEvalData data) {
			this.data = data;
		}

		@Override
		public void startRow(String[] row) {
			String from = row[0].trim();
			String to = row[1].trim();
			//TODO set correct values
			setValuesForODRelation(getODId(from, to), 
					Key.makeKey(Mode.ROAD, DemandSegment.PV_NON_COMMERCIAL, Attribute.km), 
					new Double(0.), 
					data);
		}
		
	}
	
	private static class DemandNewHandler implements TabularFileHandler{

		private ScenarioForEvalData data;

		/**
		 * @param data
		 */
		public DemandNewHandler(ScenarioForEvalData data) {
			this.data = data;
		}

		@Override
		public void startRow(String[] row) {
			String from = row[0].trim();
			String to = row[1].trim();
			//TODO set correct values
			setValuesForODRelation(getODId(from, to), 
					Key.makeKey(Mode.ROAD, DemandSegment.PV_NON_COMMERCIAL, Attribute.km), 
					new Double(0.), 
					data);
		}
	}
	
	private static class DemandDroppedHandler implements TabularFileHandler{

		private ScenarioForEvalData data;

		/**
		 * @param data
		 */
		public DemandDroppedHandler(ScenarioForEvalData data) {
			this.data = data;
		}

		@Override
		public void startRow(String[] row) {
			String from = row[0].trim();
			String to = row[1].trim();
			//TODO set correct values
			setValuesForODRelation(getODId(from, to), 
					Key.makeKey(Mode.ROAD, DemandSegment.PV_NON_COMMERCIAL, Attribute.km), 
					new Double(0.), 
					data);
		}
		
	}
	
	private static class DemandRemainingHandler implements TabularFileHandler{

		private ScenarioForEvalData data;

		/**
		 * @param data
		 */
		public DemandRemainingHandler(ScenarioForEvalData data) {
			this.data = data;
		}

		@Override
		public void startRow(String[] row) {
			String from = row[0].trim();
			String to = row[1].trim();
			//TODO set correct values
			setValuesForODRelation(getODId(from, to), 
					Key.makeKey(Mode.ROAD, DemandSegment.PV_NON_COMMERCIAL, Attribute.km), 
					new Double(0.), 
					data);
		}
		
	}
	
	private static class ImpedanceHandler implements TabularFileHandler{

		private ScenarioForEvalData data;

		/**
		 * @param data
		 */
		public ImpedanceHandler(ScenarioForEvalData data) {
			this.data = data;
		}

		@Override
		public void startRow(String[] row) {
			if(comment(row)) return;
			
			String from = row[0].trim();
			String to = row[1].trim();
			//TODO set correct values
			setValuesForODRelation(getODId(from, to), 
					Key.makeKey(Mode.ROAD, DemandSegment.PV_NON_COMMERCIAL, Attribute.km), 
					new Double(0.), 
					data);
		}
		
	}
	
	private static class ImpedanceShiftedHandler implements TabularFileHandler{

		private ScenarioForEvalData data;

		/**
		 * @param data
		 */
		public ImpedanceShiftedHandler(ScenarioForEvalData data) {
			this.data = data;
		}

		@Override
		public void startRow(String[] row) {
			if(comment(row)) return;
			
			String from = row[0].trim();
			String to = row[2].trim();
			//TODO set correct values
			setValuesForODRelation(getODId(from, to), 
					Key.makeKey(Mode.ROAD, DemandSegment.PV_NON_COMMERCIAL, Attribute.km), 
					new Double(0.), 
					data);
		}
		
	}
	
	private static class TravelTimeBaseHandler implements TabularFileHandler{

		private ScenarioForEvalData data;

		/**
		 * @param data
		 */
		public TravelTimeBaseHandler(ScenarioForEvalData data) {
			this.data = data;
		}

		@Override
		public void startRow(String[] row) {
			if(comment(row)) return;
			
			String[] myRow = myRowSplit(row[0]);
			//TODO set correct values
			setValuesForODRelation(getODId(getIdForTTfiles(myRow[2]), getIdForTTfiles(myRow[3])), 
					Key.makeKey(Mode.ROAD, DemandSegment.PV_NON_COMMERCIAL, Attribute.km), 
					new Double(0.), 
					data);
		}
		
	}
	
	private static class TravelTimeStudyHandler implements TabularFileHandler{

		private ScenarioForEvalData data;

		/**
		 * @param data
		 */
		public TravelTimeStudyHandler(ScenarioForEvalData data) {
			this.data = data;
		}
		
		@Override
		public void startRow(String[] row) {
			if(comment(row)) return;
			
			String[] myRow = myRowSplit(row[0]);
			//TODO set correct values
			setValuesForODRelation(getODId(getIdForTTfiles(myRow[2]), getIdForTTfiles(myRow[3])), 
					Key.makeKey(Mode.ROAD, DemandSegment.PV_NON_COMMERCIAL, Attribute.km), 
					new Double(0.), 
					data);
//			setValuesForODRelation(getODId(getIdForTTfiles(myRow[3]), getIdForTTfiles(myRow[2])), 
//					Key.makeKey(Mode.ROAD, DemandSegment.PV_NON_COMMERCIAL, Attribute.km), 
//					new Double(0.), 
//					data);
		}
	}
	
	//############## TabularFileHandler End###################################
	
	/**
	 * @param odId
	 * @param key
	 * @param double1
	 * @param data2 
	 */
	private static void setValuesForODRelation(Id odId, Key key, Double value, ScenarioForEvalData data2) {
//		Values v = data2.getByODRelation(odId);
//		if(v == null){
//			v = new Values();
//			data2.setValuesForODRelation(odId, v);
//		}
//		v.put(key, value);
		//TODO only for debugging
		if(!data2.getAllRelations().contains(odId)) data2.setValuesForODRelation(odId, null);
	}


	/**
	 * @param string
	 * @return
	 */
	private static String[] myRowSplit(String string) {
		if(string.length() < 68){
			log.error(string);
			throw new RuntimeException("line is not long enough. Bailing out.");
		}
		String[] s = new String[6];
		s[0] = string.substring(0, 20);
		s[1] = string.substring(21, 41);
		s[2] = string.substring(42, 49);
		s[3] = string.substring(50, 57);
		s[4] = string.substring(58, 67);
		s[5] = string.substring(68);
		return s;
	}
	
	private static Id getODId(String from, String to){
		return new IdImpl(from + "---" + to);
	}
	
	private static String getIdForTTfiles(String col){
		String s = col.trim();
		if(s.length() == 5) s = s +"00";
		return s;
	}
	
	/**
	 * @param row
	 * @return
	 */
	private static boolean comment(String[] row) {
		if(row[0].startsWith("#")){
			log.warn("TabularFileParser did not identify '#' as comment regex! Don't know why!?");
			return true;
		}
		return false;
	}





	public static void main(String[] args) {
		IVVReaderConfigGroup config = new IVVReaderConfigGroup();
		String dir = "C:\\Users\\Daniel\\Desktop\\P2030_Daten_IVV_20131210\\";
		config.setDemandMatrixFile(dir + "P2030_2010_BMVBS_ME2_131008.csv");
		config.setRemainingDemandMatrixFile(dir + "P2030_2010_verbleibend_ME2.csv");
		config.setNewDemandMatrixFile(dir + "P2030_2010_neuentstanden_ME2.csv");
		config.setDroppedDemandMatrixFile(dir + "P2030_2010_entfallend_ME2.csv");
		
		config.setTravelTimesBaseMatrixFile(dir + "P2030_Widerstaende_Ohnefall.wid");
		config.setTravelTimesStudyMatrixFile(dir + "P2030_Widerstaende_Mitfall.wid");
		config.setImpedanceMatrixFile(dir + "P2030_2010_A14_induz_ME2.wid");
		config.setImpedanceShiftedMatrixFile(dir + "P2030_2010_A14_verlagert_ME2.wid ");
		
		IVVReader reader = new IVVReader(config, new ScenarioForEvalData());
		reader.read();
	}
}

