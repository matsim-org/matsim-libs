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

import java.util.HashMap;
import java.util.List;
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
public class IVVReaderV2 {

	private static final Logger log = Logger.getLogger(IVVReaderV2.class);
	private IVVReaderConfigGroup config;
	
	static final double BESETZUNGSGRAD_PV_PRIVAT = 1.74; // Wochenende 2.1, Woche 1.6 gemaess BVWP Methodik 2003
	static final double BESETZUNGSGRAD_PV_GESCHAEFT = 1.4;
	
	static final double BAHNPREISPROKM = 0.12; //Expertenmeinung 10 bis 12 Cent Einnahme je KM
	static final double WERKTAGEPROJAHR = 250;

	
	public IVVReaderV2(IVVReaderConfigGroup config) {
		this.config = config;
	}
	
	
	void read(){
		// TODO currently implemented for debbuging-reasons. For later revisions
		// all data from the files should be added to this.data
		
		
		
		log.info("Start...");

		
		ScenarioForEvalData nullfallData = new ScenarioForEvalData();
		
		
		nullfallData = new ScenarioForEvalData();		
//		
//		log.info("Reading demand file (00-Matrix): "+config.getDemandMatrixFile() );
//		read(config.getDemandMatrixFile(), new DemandHandler(nullfallData));
//		
		
	// read(config.getRemainingDemandMatrixFile(), new DemandRemainingHandler(data));
	// Dopplung: Entweder 00 - oder verbleibend-Matrix einlesen, 00 macht Aussagen zum Geschäftsreiseverkehr, die verbleibend Matrix nicht...
		
		log.info("Reading Reseizeitmatrix Nullfall (06, 18): " +config.getTravelTimesBaseMatrixFile());
		read(config.getTravelTimesBaseMatrixFile(), new TravelTimeHandler(nullfallData,true));

		
		log.info("Reading Cost Of Production & Operations' file (10,12,14 Sammelfile Nullfall): "+config.getImpedanceMatrixFile() );
		read(config.getImpedanceMatrixFile(), new ImpedanceNullfallHandler(nullfallData));

		
		log.info("Duplicating Nullfall");
		ScenarioForEvalData planfallData = nullfallData.createDeepCopy();
		
		
		log.info("Reading Neuentstehend,induziert (02/03) "+ config.getNewDemandMatrixFile());
		read(config.getNewDemandMatrixFile(), new DemandNewOrDroppedHandler(planfallData));
		
		log.info("Reading Entfallend (04) "+ config.getDroppedDemandMatrixFile());
		read(config.getDroppedDemandMatrixFile(), new DemandNewOrDroppedHandler(planfallData));
		
		log.info("Reading Reseizeitmatrix Planfall (06): " +config.getTravelTimesStudyMatrixFile());
		read(config.getTravelTimesStudyMatrixFile(), new TravelTimeHandler(planfallData, false));

		
		log.info("Reading Cost Of Production & Operations' file (11,13,15 Sammelfile Planfall): "+config.getImpedanceMatrixFile() );
		read(config.getImpedanceMatrixFile(), new ImpedancePlanfallHandler(planfallData));
		
//		log.info("Reading Verlagert (16-17): " +config.getImpedanceShiftedMatrixFile());
//		read(config.getImpedanceShiftedMatrixFile(), new ImpedanceShiftedHandler(nullfallData , nullfallData));
//		
		log.info("finished reading...");
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
			if(comment(row)) return;

			String from = row[0].trim();
			String to = row[1].trim();
			
			//verbleibender Verkehr wird im CT verlagert gerechnet, prinzipiell ist das egal.
			
			Mode mode = Mode.RAIL;
			
			setValuesForODRelation(getODId(from, to), 	Key.makeKey(mode, DemandSegment.PV_BERUF, Attribute.XX),Double.parseDouble(row[2].trim())	, data);
			setValuesForODRelation(getODId(from, to), 	Key.makeKey(mode, DemandSegment.PV_AUSBILDUNG, Attribute.XX),Double.parseDouble(row[3].trim())	, data);
			setValuesForODRelation(getODId(from, to), 	Key.makeKey(mode, DemandSegment.PV_EINKAUF, Attribute.XX),Double.parseDouble(row[4].trim())	, data);
			setValuesForODRelation(getODId(from, to), 	Key.makeKey(mode, DemandSegment.PV_COMMERCIAL, Attribute.XX),Double.parseDouble(row[5].trim())	, data);
			setValuesForODRelation(getODId(from, to), 	Key.makeKey(mode, DemandSegment.PV_URLAUB, Attribute.XX),Double.parseDouble(row[6].trim())	, data);
			setValuesForODRelation(getODId(from, to), 	Key.makeKey(mode, DemandSegment.PV_SONST, Attribute.XX),Double.parseDouble(row[7].trim())	, data);
			
			mode = Mode.ROAD;
			setValuesForODRelation(getODId(from, to), 	Key.makeKey(mode, DemandSegment.PV_BERUF, Attribute.XX),Double.parseDouble(row[8].trim())	, data);
			setValuesForODRelation(getODId(from, to), 	Key.makeKey(mode, DemandSegment.PV_AUSBILDUNG, Attribute.XX),Double.parseDouble(row[9].trim())	, data);
			setValuesForODRelation(getODId(from, to), 	Key.makeKey(mode, DemandSegment.PV_EINKAUF, Attribute.XX),Double.parseDouble(row[10].trim())	, data);
			setValuesForODRelation(getODId(from, to), 	Key.makeKey(mode, DemandSegment.PV_COMMERCIAL, Attribute.XX),Double.parseDouble(row[11].trim())	, data);
			setValuesForODRelation(getODId(from, to), 	Key.makeKey(mode, DemandSegment.PV_URLAUB, Attribute.XX),Double.parseDouble(row[12].trim())	, data);
			setValuesForODRelation(getODId(from, to), 	Key.makeKey(mode, DemandSegment.PV_SONST, Attribute.XX),Double.parseDouble(row[13].trim())	, data);
			}
		
		
	}
	
	private static class DemandIndexHandler implements TabularFileHandler{

        private List<Id> allOdRelations;

        /**
         * @param data
         */
        public DemandIndexHandler(List<Id> allOdRelations) {
            this.allOdRelations = allOdRelations;
        }

        @Override
        public void startRow(String[] row) {
            if(comment(row)) return;

            String from = row[0].trim();
            String to = row[1].trim();
            
            

            this.allOdRelations.add(getODId(from, to));
            }
        
        
    }
    private static class SetCertainODDemand implements TabularFileHandler{
        private ScenarioForEvalData data;
        private Id odid;

        /**
         * @param data
         */
        public SetCertainODDemand(Id odid, ScenarioForEvalData data) {
            this.data = data;
            this.odid = odid;
        }

        @Override
        public void startRow(String[] row) {
            if(comment(row)) return;

            String from = row[0].trim();
            String to = row[1].trim();
            Id currentOdId = getODId(from, to);
            if( currentOdId.equals(odid))
            {
            
         
            Mode mode = Mode.RAIL;
            
            setValuesForODRelation(currentOdId,     Key.makeKey(mode, DemandSegment.PV_BERUF, Attribute.XX),Double.parseDouble(row[2].trim())    , data);
            setValuesForODRelation(currentOdId,     Key.makeKey(mode, DemandSegment.PV_AUSBILDUNG, Attribute.XX),Double.parseDouble(row[3].trim())    , data);
            setValuesForODRelation(currentOdId,     Key.makeKey(mode, DemandSegment.PV_EINKAUF, Attribute.XX),Double.parseDouble(row[4].trim())    , data);
            setValuesForODRelation(currentOdId,     Key.makeKey(mode, DemandSegment.PV_COMMERCIAL, Attribute.XX),Double.parseDouble(row[5].trim())    , data);
            setValuesForODRelation(currentOdId,     Key.makeKey(mode, DemandSegment.PV_URLAUB, Attribute.XX),Double.parseDouble(row[6].trim())    , data);
            setValuesForODRelation(currentOdId,     Key.makeKey(mode, DemandSegment.PV_SONST, Attribute.XX),Double.parseDouble(row[7].trim())    , data);
            
            mode = Mode.ROAD;
            setValuesForODRelation(currentOdId,     Key.makeKey(mode, DemandSegment.PV_BERUF, Attribute.XX),Double.parseDouble(row[8].trim())    , data);
            setValuesForODRelation(currentOdId,     Key.makeKey(mode, DemandSegment.PV_AUSBILDUNG, Attribute.XX),Double.parseDouble(row[9].trim())    , data);
            setValuesForODRelation(currentOdId,     Key.makeKey(mode, DemandSegment.PV_EINKAUF, Attribute.XX),Double.parseDouble(row[10].trim())    , data);
            setValuesForODRelation(currentOdId,     Key.makeKey(mode, DemandSegment.PV_COMMERCIAL, Attribute.XX),Double.parseDouble(row[11].trim())    , data);
            setValuesForODRelation(currentOdId,     Key.makeKey(mode, DemandSegment.PV_URLAUB, Attribute.XX),Double.parseDouble(row[12].trim())    , data);
            setValuesForODRelation(currentOdId,     Key.makeKey(mode, DemandSegment.PV_SONST, Attribute.XX),Double.parseDouble(row[13].trim())    , data);
            }
            
        }
        
    }
	
	private static class DemandNewOrDroppedHandler implements TabularFileHandler{

		private ScenarioForEvalData data;

		/**
		 * @param data
		 */
		public DemandNewOrDroppedHandler(ScenarioForEvalData data) {
			this.data = data;
		}

		@Override
		public void startRow(String[] row) {
			if(comment(row)) return;

			String from = row[0].trim();
			String to = row[1].trim();
			Id odid = getODId(from, to);
			data.getByODRelation(odid).inc(Key.makeKey(Mode.ROAD, DemandSegment.PV_BERUF, Attribute.km), Double.parseDouble(row[2].trim()));
			data.getByODRelation(odid).inc(Key.makeKey(Mode.ROAD, DemandSegment.PV_AUSBILDUNG, Attribute.km), Double.parseDouble(row[3].trim()));
			data.getByODRelation(odid).inc(Key.makeKey(Mode.ROAD, DemandSegment.PV_EINKAUF, Attribute.km), Double.parseDouble(row[4].trim()));
			data.getByODRelation(odid).inc(Key.makeKey(Mode.ROAD, DemandSegment.PV_URLAUB, Attribute.km), Double.parseDouble(row[5].trim()));
			data.getByODRelation(odid).inc(Key.makeKey(Mode.ROAD, DemandSegment.PV_SONST, Attribute.km), Double.parseDouble(row[6].trim()));
					
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
			if(comment(row)) return;

			String from = row[0].trim();
			String to = row[1].trim();
			setValuesForODRelation(getODId(from, to), Key.makeKey(Mode.ROAD, DemandSegment.PV_BERUF, Attribute.XX),  Double.parseDouble(row[2]),	data);
			setValuesForODRelation(getODId(from, to), Key.makeKey(Mode.ROAD, DemandSegment.PV_AUSBILDUNG, Attribute.XX),  Double.parseDouble(row[3]),	data);
			setValuesForODRelation(getODId(from, to), Key.makeKey(Mode.ROAD, DemandSegment.PV_EINKAUF, Attribute.XX),  Double.parseDouble(row[4]),	data);
			setValuesForODRelation(getODId(from, to), Key.makeKey(Mode.ROAD, DemandSegment.PV_URLAUB, Attribute.XX),  Double.parseDouble(row[5]),	data);
			setValuesForODRelation(getODId(from, to), Key.makeKey(Mode.ROAD, DemandSegment.PV_SONST, Attribute.XX),  Double.parseDouble(row[6]),	data);


		}
	
	}
	
	private static class ImpedanceNullfallHandler implements TabularFileHandler{

		private ScenarioForEvalData data;

		/**
		 * @param data
		 */
		public ImpedanceNullfallHandler(ScenarioForEvalData data) {
			this.data = data;
		}

		@Override
		public void startRow(String[] row) {
			if(comment(row)) return;
			
			String from = row[0].trim();
			String to = row[1].trim();
			//TODO set correct values
				for (DemandSegment ds : DemandSegment.values()){
					setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.ROAD, ds, Attribute.costOfProduction), Double.parseDouble(row[6]), data);
				}
				setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.ROAD, DemandSegment.PV_BERUF, Attribute.costOfProduction), Double.parseDouble(row[8])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, data);
				setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.ROAD, DemandSegment.PV_AUSBILDUNG, Attribute.costOfProduction), Double.parseDouble(row[9])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, data);
				setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.ROAD, DemandSegment.PV_EINKAUF, Attribute.costOfProduction), Double.parseDouble(row[10])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, data);
				setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.ROAD, DemandSegment.PV_COMMERCIAL, Attribute.costOfProduction), Double.parseDouble(row[11])*IVVReaderV2.BESETZUNGSGRAD_PV_GESCHAEFT, data);
				setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.ROAD, DemandSegment.PV_URLAUB, Attribute.costOfProduction), Double.parseDouble(row[12])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, data);
				setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.ROAD, DemandSegment.PV_SONST, Attribute.costOfProduction), Double.parseDouble(row[13])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, data);
			
				
			
			}
		
	}
	
	private static class ImpedancePlanfallHandler implements TabularFileHandler{

		private ScenarioForEvalData data;

		/**
		 * @param data
		 */
		public ImpedancePlanfallHandler(ScenarioForEvalData data) {
			this.data = data;
		}

		@Override
		public void startRow(String[] row) {
			if(comment(row)) return;
			
			String from = row[0].trim();
			String to = row[1].trim();
			//TODO set correct values
				for (DemandSegment ds : DemandSegment.values()){
					setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.ROAD, ds, Attribute.costOfProduction), Double.parseDouble(row[7]), data);
				}
				setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.ROAD, DemandSegment.PV_BERUF, Attribute.costOfProduction), Double.parseDouble(row[14])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, data);
				setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.ROAD, DemandSegment.PV_AUSBILDUNG, Attribute.costOfProduction), Double.parseDouble(row[15])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, data);
				setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.ROAD, DemandSegment.PV_EINKAUF, Attribute.costOfProduction), Double.parseDouble(row[16])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, data);
				setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.ROAD, DemandSegment.PV_COMMERCIAL, Attribute.costOfProduction), Double.parseDouble(row[17])*IVVReaderV2.BESETZUNGSGRAD_PV_GESCHAEFT, data);
				setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.ROAD, DemandSegment.PV_URLAUB, Attribute.costOfProduction), Double.parseDouble(row[18])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, data);
				setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.ROAD, DemandSegment.PV_SONST, Attribute.costOfProduction), Double.parseDouble(row[19])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, data);
			
				
			
			}
		
	}
	
	private static class ImpedanceShiftedHandler implements TabularFileHandler{

		private ScenarioForEvalData planfalldata;
		private ScenarioForEvalData nullfalldata;
		/**
		 * @param data
		 */
		public ImpedanceShiftedHandler(ScenarioForEvalData planfalldata, ScenarioForEvalData nullfalldata) {
			this.planfalldata = planfalldata;
			this.nullfalldata = nullfalldata;
		}

		@Override
		public void startRow(String[] row) {
			if(comment(row)) return;
			String from = row[0].trim();
			String to = row[2].trim();
			Id odId = getODId(from, to);
			// Annahme: Wegezwecke fuer Verlagerungen sind identisch mit Wegezwecken im MIV fuer OD-Relation
			Double totalMIV = 0.;
			Map<DemandSegment,Double> mivZwecke = new HashMap<DemandSegment,Double>();
			for (DemandSegment segment : DemandSegment.values()){
				Double mivhere = nullfalldata.getByODRelation(odId).getAttributes(Mode.ROAD, segment).getByEntry(Attribute.XX);	
				totalMIV =+ mivhere;
				
			}
			
			for (DemandSegment segment : DemandSegment.values()){
			Double verl = Double.parseDouble(row[4].trim()) * WERKTAGEPROJAHR * mivZwecke.get(segment)/totalMIV ;
			setValuesForODRelation(odId, Key.makeKey(Mode.RAIL, segment, Attribute.XX), verl, nullfalldata);
			setValuesForODRelation(odId, Key.makeKey(Mode.RAIL, segment, Attribute.hrs), Double.parseDouble(row[5].trim()), nullfalldata);
			setValuesForODRelation(odId, Key.makeKey(Mode.RAIL, segment, Attribute.hrs), Double.parseDouble(row[5].trim()), planfalldata);
			setValuesForODRelation(odId, Key.makeKey(Mode.RAIL, segment, Attribute.XX), 0., planfalldata);

			}
		}
		
	}
	
	private static class TravelTimeHandler implements TabularFileHandler{

		private ScenarioForEvalData data;
		boolean includeRail;
		/**
		 * @param data
		 */
		public TravelTimeHandler(ScenarioForEvalData data, boolean includeRail) {
			this.data = data;
			this.includeRail = true;
		}

		@Override
		public void startRow(String[] row) {
			if(comment(row)) return;
			
			String[] myRow = myRowSplit(row[0]);
			//TODO set correct values
			for (DemandSegment ds : DemandSegment.values()){
				
			setValuesForODRelation(getODId(getIdForTTfiles(myRow[2]), getIdForTTfiles(myRow[3])), Key.makeKey(Mode.ROAD, ds, Attribute.km), Double.parseDouble(myRow[4].trim()), data);
			setValuesForODRelation(getODId(getIdForTTfiles(myRow[2]), getIdForTTfiles(myRow[3])), Key.makeKey(Mode.ROAD, ds, Attribute.hrs), Double.parseDouble(myRow[5].trim())/60., data);
			
			
			if (includeRail){
			setValuesForODRelation(getODId(getIdForTTfiles(myRow[2]), getIdForTTfiles(myRow[3])), Key.makeKey(Mode.RAIL, ds, Attribute.km), Double.parseDouble(myRow[4].trim()), data);
			setValuesForODRelation(getODId(getIdForTTfiles(myRow[2]), getIdForTTfiles(myRow[3])), Key.makeKey(Mode.RAIL, ds, Attribute.priceUser), Double.parseDouble(myRow[4].trim())*BAHNPREISPROKM, data);
			}
			
			}
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
		
		Values v = data2.getByODRelation(odId);
		if(v == null){
			v = new Values();
			data2.setValuesForODRelation(odId, v);
		}
		v.put(key, value);
		//TODO only for debugging
//		if(!data2.getAllRelations().contains(odId)) data2.setValuesForODRelation(odId, null);
		
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
		String dir = "/Users/jb/tucloud/bvwp/data/";
		config.setDemandMatrixFile(dir + "P2030_2010_BMVBS_ME2_131008.csv");
		config.setRemainingDemandMatrixFile(dir + "P2030_2010_verbleibend_ME2.csv");
		config.setNewDemandMatrixFile(dir + "P2030_2010_neuentstanden_ME2.csv");
		config.setDroppedDemandMatrixFile(dir + "P2030_2010_entfallend_ME2.csv");
		
		config.setTravelTimesBaseMatrixFile(dir + "P2030_Widerstaende_Ohnefall.wid");
		config.setTravelTimesStudyMatrixFile(dir + "P2030_Widerstaende_Mitfall.wid");
		config.setImpedanceMatrixFile(dir + "P2030_2010_A14_induz_ME2.wid");
		config.setImpedanceShiftedMatrixFile(dir + "P2030_2010_A14_verlagert_ME2.wid ");
		
		IVVReaderV2 reader = new IVVReaderV2(config);
		reader.read();
	}
}

