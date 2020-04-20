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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
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

	static final double BAHNPREISPROKM = 0.12; //Expertenmeinung 10 bis 12 Cent Einnahme je KM
	static final double WERKTAGEPROJAHR = 250;
	Map<String,Double> diffs = new HashMap<String,Double>();

	
	
	public IVVReaderV2(IVVReaderConfigGroup config) {
		this.config = config;
	}
	

    void runBVWP2015(ScenarioForEvalData nullfall, ScenarioForEvalData planfall, String outname){
		
		Values economicValues = EconomicValues.createEconomicValuesZielnetzRoad();
		UtilityChanges utilityChanges = new UtilityChangesBVWP2015();
		double diff = utilityChanges.computeAndPrintResults(economicValues, nullfall, planfall, outname) ; 
		this.diffs.put(outname, diff);
		
	}
	
	void read(){
		List<String> odRelations = new ArrayList<>();
		log.info("Creating Index using Production & Operations' file : "+config.getImpedanceMatrixFile() );
		read(config.getImpedanceMatrixFile(), new IndexFromImpendanceFileHandler(odRelations));
		log.info("Handling  "+odRelations.size() +" OD-Relations");
		

		
		ScenarioForEvalData nullfallData = new ScenarioForEvalData();
		log.info("Reading demand file (00-Matrix): "+config.getDemandMatrixFile() );
		read(config.getDemandMatrixFile(), new DemandHandler(nullfallData, odRelations));
		log.info("Filled with "+nullfallData.getAllRelations().size() + " Od-Relations");
		
		
		
		
		log.info("Reading Reseizeitmatrix Nullfall (06, 18): " +config.getTravelTimesBaseMatrixFile());
		TravelTimeHandler tth =  new TravelTimeHandler(nullfallData,odRelations,true);
		read(config.getTravelTimesBaseMatrixFile(),tth);
		log.info("Read "+tth.getCount()+" relations");
		
		log.info("Duplicating Nullfall");
		ScenarioForEvalData planfallData = nullfallData.createDeepCopy();

//	    log.info("Reading Verbleibend (01)) "+ config.getRemainingDemandMatrixFile());
//	    read(config.getRemainingDemandMatrixFile(), new DemandRemainingHandler(odRelations, planfallData));
//      Verbleibender Verkehr ist für die gegebenen Relationen mit Daten aus Nullfall identisch
		
		log.info("Reading Reseizeitmatrix Planfall (07): " +config.getTravelTimesStudyMatrixFile());
		TravelTimeHandler plantt = new TravelTimeHandler(planfallData, odRelations, false);
		read(config.getTravelTimesStudyMatrixFile(), plantt );
		log.info("Read "+plantt.getCount()+" relations");

		
		
		log.info("Reading Cost Of Production & Operations' file (10-11,12-13,14-15 Sammelfile Nullfall&Planfall ): "+config.getImpedanceMatrixFile() );
		read(config.getImpedanceMatrixFile(), new CostHandler(nullfallData, planfallData));

		log.info("Reading Neuentstehend,induziert (02/03) "+ config.getNewDemandMatrixFile());
		read(config.getNewDemandMatrixFile(), new DemandNewOrDroppedHandler(odRelations, planfallData));
		
		
		log.info("Reading Entfallend (04) "+ config.getDroppedDemandMatrixFile());
		read(config.getDroppedDemandMatrixFile(), new DemandNewOrDroppedHandler(odRelations, planfallData));
		
		log.info("Reading Verlagert (16-17): " +config.getImpedanceShiftedMatrixFile());
		read(config.getImpedanceShiftedMatrixFile(), new ImpedanceShiftedHandler(odRelations, planfallData , nullfallData));

		log.info("Dumping Nullfall Magdeburg-->Stendal");
		System.out.println(nullfallData.getByODRelation(getODId("1500301", "1509001")));
		
		log.info("Dumping Planfall Magdeburg-->Stendal");
		System.out.println(planfallData.getByODRelation(getODId("1500301", "1509001")));
		Map<String,ScenarioForEvalData> nullFallDataByFromId = splitDataByFromId(nullfallData);
		Map<String,ScenarioForEvalData> planFallDataByFromId = splitDataByFromId(planfallData);
		if ( nullFallDataByFromId.size() != planFallDataByFromId.size()){
		System.err.println("Null und Planfall haben verschiedene Anzahl an Von-Ids. Das kann nicht sein");
		throw new IllegalStateException();
		}
		for (String fromId : nullFallDataByFromId.keySet()){
			runBVWP2015(nullFallDataByFromId.get(fromId),planFallDataByFromId.get(fromId), fromId.toString());
		}
//		analyse(nullfallData, planfallData);
		runBVWP2015(nullfallData, planfallData, "all_out");
		log.info("fertig.");
//		writeDiffs();
	}
	private void analyse(ScenarioForEvalData nullfallData, ScenarioForEvalData planfallData)
    {
	    log.info("Analysiere Widerstandsunterschiede bei gleicher Nutzerzahl");
	    int x = 0;
	    for (String odId : planfallData.getAllRelations()){
	        double diffXX = 0.0;
	        for (DemandSegment ds : DemandSegment.values()){
	            double xxPlan = planfallData.getByODRelation(odId).getAttributes(Mode.Strasse, ds).getByEntry(Attribute.XX);
	            double xxNull= nullfallData.getByODRelation(odId).getAttributes(Mode.Strasse, ds).getByEntry(Attribute.XX);
	            if (xxPlan!=xxNull) diffXX += Math.abs(xxPlan-xxNull);
	        }
	        if (diffXX == 0.0){
	            double diffKM = 0.0;
	            double diffHr = 0.0;
	            double dPlan = planfallData.getByODRelation(odId).getAttributes(Mode.Strasse, DemandSegment.PV_ARBEIT).getByEntry(Attribute.Distanz_km);
	            double dNull = nullfallData.getByODRelation(odId).getAttributes(Mode.Strasse, DemandSegment.PV_ARBEIT).getByEntry(Attribute.Distanz_km);
	            double tPlan = planfallData.getByODRelation(odId).getAttributes(Mode.Strasse, DemandSegment.PV_ARBEIT).getByEntry(Attribute.Reisezeit_h);
	            double tNull = nullfallData.getByODRelation(odId).getAttributes(Mode.Strasse, DemandSegment.PV_ARBEIT).getByEntry(Attribute.Reisezeit_h);
	            diffKM = dPlan-dNull;
	            diffHr = tPlan-tNull;
	            if (diffHr!=0.0 || diffKM !=0.0){
	                log.info(odId.toString() +": Difference in distance: "+diffKM + " Difference in hrs: "+diffHr+", but XXDiff = 0 ");
	                x++;
	            }
	        }
	        
	    }
	    log.info("Widerstandsunterschiede bei gleicher Nutzerzahl treten in "+x+" Fällen auf.");

    }


    void writeDiffs(){
	    Writer writer = IOUtils.getBufferedWriter("difs.txt");
	    
	    try {
	    for (Entry<String,Double> e : diffs.entrySet()){
                writer.write(e.getKey()+"\t"+e.getValue()+"\n");
            }
        
	    writer.flush();
	    writer.close();
	    }
	    catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
	}
	
	Map<String,ScenarioForEvalData> splitDataByFromId(ScenarioForEvalData data){
	
		Map <String, ScenarioForEvalData> returnMap = new HashMap<>();
		for (String id : data.getAllRelations()){
			String fromId = getFromId(id);
			ScenarioForEvalData currentData;
			if (!returnMap.containsKey(fromId)) {
				 currentData = new ScenarioForEvalData();
					}
			else {
				currentData = returnMap.get(fromId);
			}
			
			currentData.setValuesForODRelation(id, data.getByODRelation(id));
			returnMap.put(fromId, currentData);
		}
		
		
		return returnMap;
	} 
	
	String getFromId (String idString){
		idString = idString.split("---")[0];
		return idString;
	}
	
	void checkCosts(ScenarioForEvalData data){
		
		for (DemandSegment seg : DemandSegment.values()){
			int usercostsTooLow = 0;
		for (String id: data.getAllRelations()){
			double pu = data.getByODRelation(id).getAttributes(Mode.Strasse, seg).getByEntry(Attribute.Nutzerkosten_Eu);
			double cop = data.getByODRelation(id).getAttributes(Mode.Strasse, seg).getByEntry(Attribute.Produktionskosten_Eu);
			if (pu<cop) usercostsTooLow++;
		}
		
		System.out.println("Out of "+data.getAllRelations().size()+ " Relations, user costs were lower than CoP in "+usercostsTooLow+ " cases for DemandSegment "+ seg);
		}	
	}
	
	

	


	/**
	 * @param type2ids
	 */
	@SuppressWarnings("unused")
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
		private final List<String> odRelations;
		/**
		 * @param data
		 * @param odRelations 
		 */
		public DemandHandler(ScenarioForEvalData data, List<String> odRelations) {
			this.data = data;
			this.odRelations=odRelations;
		}

		@Override
		public void startRow(String[] row) {
			if(comment(row)) return;

			String from = row[0].trim();
			String to = row[1].trim();
			String odId = getODId(from, to);
			if (this.odRelations.contains(odId)){
			
			Mode mode = Mode.Bahn;
			
			setValuesForODRelation(odId, 	Key.makeKey(mode, DemandSegment.PV_ARBEIT, Attribute.XX),Double.parseDouble(row[2].trim())	, data);
			setValuesForODRelation(odId, 	Key.makeKey(mode, DemandSegment.PV_AUSBILDUNG, Attribute.XX),Double.parseDouble(row[3].trim())	, data);
			setValuesForODRelation(odId, 	Key.makeKey(mode, DemandSegment.PV_EINKAUF, Attribute.XX),Double.parseDouble(row[4].trim())	, data);
			setValuesForODRelation(odId, 	Key.makeKey(mode, DemandSegment.PV_GESCHAEFT, Attribute.XX),Double.parseDouble(row[5].trim())	, data);
			setValuesForODRelation(odId, 	Key.makeKey(mode, DemandSegment.PV_URLAUB, Attribute.XX),Double.parseDouble(row[6].trim())	, data);
			setValuesForODRelation(odId, 	Key.makeKey(mode, DemandSegment.PV_SONST, Attribute.XX),Double.parseDouble(row[7].trim())	, data);
			
			mode = Mode.Strasse;
			setValuesForODRelation(odId, 	Key.makeKey(mode, DemandSegment.PV_ARBEIT, Attribute.XX),Double.parseDouble(row[8].trim())	, data);
			setValuesForODRelation(odId, 	Key.makeKey(mode, DemandSegment.PV_AUSBILDUNG, Attribute.XX),Double.parseDouble(row[9].trim())	, data);
			setValuesForODRelation(odId, 	Key.makeKey(mode, DemandSegment.PV_EINKAUF, Attribute.XX),Double.parseDouble(row[10].trim())	, data);
			setValuesForODRelation(odId, 	Key.makeKey(mode, DemandSegment.PV_GESCHAEFT, Attribute.XX),Double.parseDouble(row[11].trim())	, data);
			setValuesForODRelation(odId, 	Key.makeKey(mode, DemandSegment.PV_URLAUB, Attribute.XX),Double.parseDouble(row[12].trim())	, data);
			setValuesForODRelation(odId, 	Key.makeKey(mode, DemandSegment.PV_SONST, Attribute.XX),Double.parseDouble(row[13].trim())	, data);
			}
		}
		
	}
	

    private static class DemandNewOrDroppedHandler implements TabularFileHandler{

		private ScenarioForEvalData data;
		private final List<String> odRelations;

		/**
		 * @param data
		 */
		public DemandNewOrDroppedHandler(List<String> odRelations, ScenarioForEvalData data) {
			this.data = data;
			this.odRelations = odRelations;
		}

		@Override
		public void startRow(String[] row) {
			if(comment(row)) return;

			String from = row[0].trim();
			String to = row[1].trim();
			String odid = getODId(from, to);
			if (this.odRelations.contains(odid)){
			data.getByODRelation(odid).inc(Key.makeKey(Mode.Strasse, DemandSegment.PV_ARBEIT, Attribute.XX), Double.parseDouble(row[2].trim()));
			data.getByODRelation(odid).inc(Key.makeKey(Mode.Strasse, DemandSegment.PV_AUSBILDUNG, Attribute.XX), Double.parseDouble(row[3].trim()));
			data.getByODRelation(odid).inc(Key.makeKey(Mode.Strasse, DemandSegment.PV_EINKAUF, Attribute.XX), Double.parseDouble(row[4].trim()));
			data.getByODRelation(odid).inc(Key.makeKey(Mode.Strasse, DemandSegment.PV_URLAUB, Attribute.XX), Double.parseDouble(row[5].trim()));
			data.getByODRelation(odid).inc(Key.makeKey(Mode.Strasse, DemandSegment.PV_SONST, Attribute.XX), Double.parseDouble(row[6].trim()));
			}

		}
	}
	
	private static class DemandRemainingHandler implements TabularFileHandler{

		private ScenarioForEvalData data;
        private final List<String> odRelations;

		public DemandRemainingHandler(List<String> odRelations, ScenarioForEvalData data) {
			this.data = data;
	        this.odRelations = odRelations;

		}

		@Override
		public void startRow(String[] row) {
			if(comment(row)) return;

			String from = row[0].trim();
			String to = row[1].trim();
			String odId = getODId(from, to);
			if (this.odRelations.contains(odId)){
            setValuesForODRelation(odId, Key.makeKey(Mode.Strasse, DemandSegment.PV_ARBEIT, Attribute.XX),  Double.parseDouble(row[2]),	data);
			setValuesForODRelation(odId, Key.makeKey(Mode.Strasse, DemandSegment.PV_AUSBILDUNG, Attribute.XX),  Double.parseDouble(row[3]),	data);
			setValuesForODRelation(odId, Key.makeKey(Mode.Strasse, DemandSegment.PV_EINKAUF, Attribute.XX),  Double.parseDouble(row[4]),	data);
			setValuesForODRelation(odId, Key.makeKey(Mode.Strasse, DemandSegment.PV_URLAUB, Attribute.XX),  Double.parseDouble(row[5]),	data);
			setValuesForODRelation(odId, Key.makeKey(Mode.Strasse, DemandSegment.PV_SONST, Attribute.XX),  Double.parseDouble(row[6]),	data);
			 }

		}
	
	}
	
	private static class CostHandler implements TabularFileHandler{

		private ScenarioForEvalData nullfalldata;
		private ScenarioForEvalData planfalldata;
		
        Map<DemandSegment,Double> besetzungsgradeKurz;
        Map<DemandSegment,Integer> tableLookUp;

	    Map<DemandSegment,Double> besetzungsgradeLang;

		/**
		 * @param nullfalldata
		 */
		public CostHandler(ScenarioForEvalData nullfalldata, ScenarioForEvalData planfalldata ) {
			this.nullfalldata = nullfalldata;
			this.planfalldata = planfalldata;
			this.fillBesetzungsgrad();
			this.fillNullfallMap();
		}

		
		
		    
		private void fillNullfallMap(){
		    this.tableLookUp = new HashMap<MultiDimensionalArray.DemandSegment, Integer>();
		    this.tableLookUp.put(DemandSegment.PV_ARBEIT, 8);
		    this.tableLookUp.put(DemandSegment.PV_AUSBILDUNG, 9);
		    this.tableLookUp.put(DemandSegment.PV_EINKAUF, 10);
		    this.tableLookUp.put(DemandSegment.PV_GESCHAEFT, 11);
		    this.tableLookUp.put(DemandSegment.PV_URLAUB, 12);
		    this.tableLookUp.put(DemandSegment.PV_SONST, 13);
		    
		}
		  private void fillBesetzungsgrad()
		    {
		        this.besetzungsgradeKurz = new HashMap<DemandSegment, Double>();
		        this.besetzungsgradeLang = new HashMap<DemandSegment, Double>();
		        
		        this.besetzungsgradeKurz.put(DemandSegment.PV_ARBEIT, 1.1);
		        this.besetzungsgradeLang.put(DemandSegment.PV_ARBEIT, 1.1);

		        this.besetzungsgradeKurz.put(DemandSegment.PV_AUSBILDUNG, 1.7);
		        this.besetzungsgradeLang.put(DemandSegment.PV_AUSBILDUNG, 1.3);
		        
		        this.besetzungsgradeKurz.put(DemandSegment.PV_GESCHAEFT, 1.0);
		        this.besetzungsgradeLang.put(DemandSegment.PV_GESCHAEFT, 1.1);
		        
		        this.besetzungsgradeKurz.put(DemandSegment.PV_EINKAUF, 1.3);
		        this.besetzungsgradeLang.put(DemandSegment.PV_EINKAUF, 1.8);
		        
		        this.besetzungsgradeKurz.put(DemandSegment.PV_SONST, 1.6);
		        this.besetzungsgradeLang.put(DemandSegment.PV_SONST, 2.0);
		        
		        this.besetzungsgradeKurz.put(DemandSegment.PV_URLAUB, 1.6);
		        this.besetzungsgradeLang.put(DemandSegment.PV_URLAUB, 2.3);
		        
		        
		    }
		  
		  double getBesetzungsgrad(ScenarioForEvalData data, DemandSegment segment, String odId){
		        
		        double distance = data.getByODRelation(odId).getAttributes(Mode.Strasse,segment).getByEntry(Attribute.Distanz_km);
		        double besetzungsgrad;
		        if (distance<50) besetzungsgrad = this.besetzungsgradeKurz.get(segment);
		        else besetzungsgrad = this.besetzungsgradeLang.get(segment);
		        
		        return besetzungsgrad;
		    }

		@Override
		public void startRow(String[] row) {
			if(comment(row)) return;
			
			String from = row[0].trim();
			String to = row[1].trim();
			String odId = getODId(from, to);

			
			// VARIANTE EINS
	          for (DemandSegment dds : DemandSegment.values()){
	              if (dds.equals(DemandSegment.GV)) continue;
	              if (dds.equals(DemandSegment.PV_NON_COMMERCIAL)) continue;
	              double bsNullfall = getBesetzungsgrad(nullfalldata, dds, odId);
	              double bsPlanfall = getBesetzungsgrad(planfalldata, dds, odId);
	              setValuesForODRelation(odId,Key.makeKey(Mode.Strasse, dds, Attribute.Produktionskosten_Eu), Double.parseDouble(row[6])/bsNullfall, nullfalldata);
	              setValuesForODRelation(odId,Key.makeKey(Mode.Strasse, dds, Attribute.Nutzerkosten_Eu), Double.parseDouble(row[this.tableLookUp.get(dds)]), nullfalldata);

	              setValuesForODRelation(odId,Key.makeKey(Mode.Strasse, dds, Attribute.Produktionskosten_Eu), Double.parseDouble(row[7])/bsPlanfall, planfalldata);
	              setValuesForODRelation(odId,Key.makeKey(Mode.Strasse, dds, Attribute.Nutzerkosten_Eu), Double.parseDouble(row[this.tableLookUp.get(dds)+6]), planfalldata);
	              
	              

	          }
			
			// VARIANTE ZWEI
//          for (DemandSegment dds : DemandSegment.values()){
//              if (dds.equals(DemandSegment.GV)) continue;
//              if (dds.equals(DemandSegment.PV_NON_COMMERCIAL)) continue;
////              double bsNullfall = getBesetzungsgrad(nullfalldata, dds, odId);
////              double bsPlanfall = getBesetzungsgrad(planfalldata, dds, odId);
//              setValuesForODRelation(odId,Key.makeKey(Mode.Strasse, dds, Attribute.Produktionskosten_Eu), Double.parseDouble(row[this.tableLookUp.get(dds)]), nullfalldata);
//              setValuesForODRelation(odId,Key.makeKey(Mode.Strasse, dds, Attribute.Nutzerkosten_Eu), Double.parseDouble(row[this.tableLookUp.get(dds)]), nullfalldata);
//
//              setValuesForODRelation(odId,Key.makeKey(Mode.Strasse, dds, Attribute.Produktionskosten_Eu), Double.parseDouble(row[this.tableLookUp.get(dds)+6]), planfalldata);
//              setValuesForODRelation(odId,Key.makeKey(Mode.Strasse, dds, Attribute.Nutzerkosten_Eu), Double.parseDouble(row[this.tableLookUp.get(dds)+6]), planfalldata);
//              
//              
//
//          }


                if ((from.equals("1500301"))&&(to.equals("1509001")) ){
                    for (DemandSegment ds : DemandSegment.values()){
                        if (ds.equals(DemandSegment.GV)) continue;
                        if (ds.equals(DemandSegment.PV_NON_COMMERCIAL)) continue;
                        log.info("Nullfall MD-STD");
                        log.info(ds +" PK: "+ this.nullfalldata.getByODRelation(odId).get(Key.makeKey(Mode.Strasse, ds, Attribute.Produktionskosten_Eu)) +" NK: "+this.nullfalldata.getByODRelation(odId).get(Key.makeKey(Mode.Strasse, ds, Attribute.Nutzerkosten_Eu)));
                        log.info("Planfall MD-STD");
                        log.info(ds +" PK: "+ this.planfalldata.getByODRelation(odId).get(Key.makeKey(Mode.Strasse, ds, Attribute.Produktionskosten_Eu)) +" NK: "+this.planfalldata.getByODRelation(odId).get(Key.makeKey(Mode.Strasse, ds, Attribute.Nutzerkosten_Eu)));
                    }
//                    System.exit(0); //debug
                }
			}
		
	}
	

	
	private static class IndexFromImpendanceFileHandler implements TabularFileHandler{

        private List<String> allOdRelations;

        /**
         * @param nullfalldata
         */
        public IndexFromImpendanceFileHandler(List<String> allOdRelations) {
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
	
	private static class GetToIdsForFromIdsFromImpFileHandler implements TabularFileHandler{

        private List<String> allOdRelations;
        private String fromId;

        public GetToIdsForFromIdsFromImpFileHandler(String fromId, List<String> allOdRelations) {
            this.allOdRelations = allOdRelations;
            this.fromId = fromId;
        }

        @Override
        public void startRow(String[] row) {
            if(comment(row)) return;
            
            String from = row[0].trim();
            String to = row[1].trim();
            String currentFromId = from;

            if (currentFromId.equals(fromId)){
            this.allOdRelations.add(getODId(from, to));
            }
            }
        
        
    }
	
	private static class AllFromIdsFromImpendanceFileHandler implements TabularFileHandler{

        private Set<String> allFromIds;

        /**
         * @param nullfalldata
         */
        public AllFromIdsFromImpendanceFileHandler(Set<String> allFromIds) {
            this.allFromIds = allFromIds;
        }

        @Override
        public void startRow(String[] row) {
            if(comment(row)) return;
            String from = row[0].trim();
            this.allFromIds.add(from);
            }
        
        
    }
	
	private static class ImpedanceShiftedHandler implements TabularFileHandler{

		private ScenarioForEvalData planfalldata;
		private ScenarioForEvalData nullfalldata;
		private final List<String> odRelations;

		public ImpedanceShiftedHandler(List<String> odRelations, ScenarioForEvalData planfalldata, ScenarioForEvalData nullfalldata) {
			this.planfalldata = planfalldata;
			this.nullfalldata = nullfalldata;
			this.odRelations = odRelations;
		}

		@Override
		public void startRow(String[] row) {
			if(comment(row)) return;
			String from = row[0].trim();
			String to = row[2].trim();
			String odId = getODId(from, to);
			// Annahme: Wegezwecke fuer Verlagerungen sind identisch mit Wegezwecken im Bahn fuer OD-Relation im Nullfall
			
			if (this.odRelations.contains(odId)){
			Double totalBahn = 0.;
				
			Map<DemandSegment,Double> bahnZwecke = new HashMap<DemandSegment,Double>();
			for (DemandSegment segment : DemandSegment.values()){
				if (segment.equals(DemandSegment.GV)) continue;
				if (segment.equals(DemandSegment.PV_NON_COMMERCIAL)) continue;
				Double bahnhere = nullfalldata.getByODRelation(odId).getAttributes(Mode.Bahn, segment).getByEntry(Attribute.XX);	
				
				bahnZwecke.put(segment, bahnhere);
				totalBahn += bahnhere;
				if (odId.equals(getODId("1500301", "1509001"))) System.out.println(segment + " : "+bahnhere); 
				
			}
			if (odId.equals(getODId("1500301", "1509001"))) System.out.println("Total Bahn : "+totalBahn); 

			for (DemandSegment segment : DemandSegment.values()){
				if (segment.equals(DemandSegment.GV)) continue;
				if (segment.equals(DemandSegment.PV_NON_COMMERCIAL)) continue;
				Double verl = Double.parseDouble(row[11].trim()) * -1.0 *WERKTAGEPROJAHR * bahnZwecke.get(segment)/totalBahn ;
				planfalldata.getByODRelation(odId).inc(Key.makeKey(Mode.Bahn, segment, Attribute.XX), Math.round(verl));
				planfalldata.getByODRelation(odId).inc(Key.makeKey(Mode.Strasse, segment, Attribute.XX), -1*Math.round(verl));
				planfalldata.getByODRelation(odId).put(Key.makeKey(Mode.Bahn, segment, Attribute.Reisezeit_h), Double.parseDouble(row[5].trim())/60.);
				nullfalldata.getByODRelation(odId).put(Key.makeKey(Mode.Bahn, segment, Attribute.Reisezeit_h), Double.parseDouble(row[5].trim())/60.);

			}
		}}
		
	}
	
	private static class TravelTimeHandler implements TabularFileHandler{

		private ScenarioForEvalData data;
		boolean includeRail;
		private final List<String> odRelations;
		private long count;
		/**
		 * @param data
		 * @param odRelations 
		 */
		public TravelTimeHandler(ScenarioForEvalData data, List<String> odRelations, boolean includeRail) {
			this.data = data;
			this.includeRail = includeRail;
			this.odRelations=odRelations;
			this.count = 0;
		}

		@Override
		public void startRow(String[] row) {
			if(comment(row)) return;
			
			String[] myRow = myRowSplit(row[0]);
			String odId = getODId(getIdForTTfiles(myRow[2]), getIdForTTfiles(myRow[3]));
			String reverseOd = getODId(getIdForTTfiles(myRow[3]), getIdForTTfiles(myRow[2]));
			if ((this.odRelations.contains(odId)||this.odRelations.contains(reverseOd)) ){
			for (DemandSegment ds : DemandSegment.values()){
				if (ds.equals(DemandSegment.GV)) continue;
				if (ds.equals(DemandSegment.PV_NON_COMMERCIAL)) continue;
			setValuesForODRelation(odId, Key.makeKey(Mode.Strasse, ds, Attribute.Distanz_km), Double.parseDouble(myRow[4].trim()), data);
			setValuesForODRelation(odId, Key.makeKey(Mode.Strasse, ds, Attribute.Reisezeit_h), Double.parseDouble(myRow[5].trim())/60., data);
			setValuesForODRelation(reverseOd, Key.makeKey(Mode.Strasse, ds, Attribute.Distanz_km), Double.parseDouble(myRow[4].trim()), data);
			setValuesForODRelation(reverseOd, Key.makeKey(Mode.Strasse, ds, Attribute.Reisezeit_h), Double.parseDouble(myRow[5].trim())/60., data);
			
			if (includeRail){
			setValuesForODRelation(odId, Key.makeKey(Mode.Bahn, ds, Attribute.Distanz_km), Double.parseDouble(myRow[4].trim()), data);
			setValuesForODRelation(odId, Key.makeKey(Mode.Bahn, ds, Attribute.Nutzerkosten_Eu), Double.parseDouble(myRow[4].trim())*BAHNPREISPROKM, data);
	        setValuesForODRelation(odId, Key.makeKey(Mode.Bahn, ds, Attribute.Produktionskosten_Eu), 0., data);

			setValuesForODRelation(reverseOd, Key.makeKey(Mode.Bahn, ds, Attribute.Distanz_km), Double.parseDouble(myRow[4].trim()), data);
			setValuesForODRelation(reverseOd, Key.makeKey(Mode.Bahn, ds, Attribute.Nutzerkosten_Eu), Double.parseDouble(myRow[4].trim())*BAHNPREISPROKM, data);
			setValuesForODRelation(reverseOd, Key.makeKey(Mode.Bahn, ds, Attribute.Produktionskosten_Eu), 0., data);

			}
			}
			count++;

			}
		}

		public long getCount() {
			return count;
		}
		
		
	}
	
	
	
	
	
	
	//############## TabularFileHandler End###################################
	
	/**
	 * @param odId
	 * @param key
	 * @param double1
	 * @param data2 
	 */
	private static void setValuesForODRelation(String odId, Key key, Double value, ScenarioForEvalData data2) {
		
		Values v = data2.getByODRelation(odId);
		if(v == null){
			v = new Values();
			data2.setValuesForODRelation(odId, v);
		}
		v.put(key, value);
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
	
	private static String getODId(String from, String to){
		return from + "---" + to;
	}
	
	 static String getIdForTTfiles(String col){
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
//		String dir = "/Users/nagel/shared-svn/projects/bvwp/fe2-bewertung/testrechnungen_strasse/IVV_NeubauA14/MD_only/" ;
//		String dir = "/Users/jb/tucloud/bvwp/data/P2030_Daten_IVV_20131210/";
		String dir = "C:/local_jb/testrechnungen_strasse/IVV_NeubauA14/P2030_Daten_IVV_20131210/";
//		String dir = "C:/local_jb/testrechnungen_strasse/IVV_NeubauA14/MD_only/";
		config.setDemandMatrixFile(dir + "P2030_2010_BMVBS_ME2_131008.csv");
		config.setRemainingDemandMatrixFile(dir + "P2030_2010_verbleibend_ME2.csv");
		config.setNewDemandMatrixFile(dir + "P2030_2010_neuentstanden_ME2.csv");
		config.setDroppedDemandMatrixFile(dir + "P2030_2010_entfallend_ME2.csv");
		
		config.setTravelTimesBaseMatrixFile(dir + "P2030_Widerstaende_Ohnefall.wid");
		config.setTravelTimesStudyMatrixFile(dir + "P2030_Widerstaende_Mitfall.wid");
		config.setImpedanceMatrixFile(dir + "P2030_2010_A14_induz_ME2.wid");
//		config.setImpedanceMatrixFile(dir + "induz_test.wid");
//		config.setImpedanceMatrixFile(dir + "induz_mdst.wid");
		config.setImpedanceShiftedMatrixFile(dir + "P2030_2010_A14_verlagert_ME2.wid");
		
		IVVReaderV2 reader = new IVVReaderV2(config);
		reader.read();
	}
}

