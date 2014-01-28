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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
	
	void runBVWP2015(ScenarioForEvalData nullfall, ScenarioForEvalData planfall){
		
		Values economicValues = EconomicValues.createEconomicValuesZielnetzRoad();
		UtilityChanges utilityChanges = new UtilityChangesBVWP2015();
		utilityChanges.computeAndPrintResults(economicValues, nullfall, planfall) ; 
	}
	
	void read(){
		
		
//	// read(config.getRemainingDemandMatrixFile(), new DemandRemainingHandler(data));
//	// Dopplung: Entweder 00 - oder verbleibend-Matrix einlesen, 00 macht Aussagen zum Geschäftsreiseverkehr, die verbleibend Matrix nicht...

//		ScenarioForEvalData nullfallData = new ScenarioForEvalData();
//		ScenarioForEvalData planfallData = new ScenarioForEvalData();
//		log.info("Reading Cost Of Production & Operations' file (10-11,12-13,14-15 Sammelfile Nullfall&Planfall ): "+config.getImpedanceMatrixFile() );
//		read(config.getImpedanceMatrixFile(), new CostHandler(nullfallData, planfallData));
//		log.info("Validating Nullfall:");
//		checkCosts(nullfallData);
//		log.info("Validating Planfall:");
//		checkCosts(planfallData);
		
		
		List<Id> odRelations = new ArrayList<Id>();
		log.info("Creating Index using Production & Operations' file : "+config.getImpedanceMatrixFile() );
		read(config.getImpedanceMatrixFile(), new IndexFromImpendanceFileHandler(odRelations));
		log.info("Handling  "+odRelations.size() +" OD-Relations");
		
		ScenarioForEvalData nullfallData = new ScenarioForEvalData();
		log.info("Reading demand file (00-Matrix): "+config.getDemandMatrixFile() );
		read(config.getDemandMatrixFile(), new DemandHandler(nullfallData, odRelations));
		log.info("Filled with "+nullfallData.getAllRelations().size() + " Od-Relations");
		
		log.info("Reading Reseizeitmatrix Nullfall (06, 18): " +config.getTravelTimesBaseMatrixFile());
		read(config.getTravelTimesBaseMatrixFile(), new TravelTimeHandler(nullfallData,odRelations,true));
		
		log.info("Duplicating Nullfall");
		ScenarioForEvalData planfallData = nullfallData.createDeepCopy();

		log.info("Reading Reseizeitmatrix Planfall (07): " +config.getTravelTimesStudyMatrixFile());
		read(config.getTravelTimesStudyMatrixFile(), new TravelTimeHandler(planfallData, odRelations, false));
			
		log.info("Reading Cost Of Production & Operations' file (10-11,12-13,14-15 Sammelfile Nullfall&Planfall ): "+config.getImpedanceMatrixFile() );
		read(config.getImpedanceMatrixFile(), new CostHandler(nullfallData, planfallData));
		
		log.info("Reading Neuentstehend,induziert (02/03) "+ config.getNewDemandMatrixFile());
		read(config.getNewDemandMatrixFile(), new DemandNewOrDroppedHandler(odRelations, planfallData));
		
		log.info("Reading Entfallend (04) "+ config.getDroppedDemandMatrixFile());
		read(config.getDroppedDemandMatrixFile(), new DemandNewOrDroppedHandler(odRelations, planfallData));
		
		log.info("Reading Verlagert (16-17): " +config.getImpedanceShiftedMatrixFile());
		read(config.getImpedanceShiftedMatrixFile(), new ImpedanceShiftedHandler(odRelations, planfallData , nullfallData));

//		log.info("Dumping Nullfall Magdeburg-->Stendal");
//		System.out.println(nullfallData.getByODRelation(getODId("1500301", "1509001")));
//		
//		log.info("Dumping Planfall Magdeburg-->Stendal");
//		System.out.println(planfallData.getByODRelation(getODId("1500301", "1509001")));
		
		runBVWP2015(nullfallData, planfallData);
	}
	
	
	void checkCosts(ScenarioForEvalData data){
		
		for (DemandSegment seg : DemandSegment.values()){
			int usercostsTooLow = 0;
		for (Id id: data.getAllRelations()){
			double pu = data.getByODRelation(id).getAttributes(Mode.Strasse, seg).getByEntry(Attribute.Nutzerkosten_Eu);
			double cop = data.getByODRelation(id).getAttributes(Mode.Strasse, seg).getByEntry(Attribute.Produktionskosten_Eu);
//			System.out.println(pu  + ": vs " + cop);
			if (pu<cop) usercostsTooLow++;
		}
		
		System.out.println("Out of "+data.getAllRelations().size()+ " Relations, user costs were lower than CoP in "+usercostsTooLow+ " cases for DemandSegment "+ seg);
		}	
	}
	
	void createPlanUndNullfallForOdRelation(Id odRelation){
		ScenarioForEvalData	nullfallData = new ScenarioForEvalData();		
		log.info("Reading demand file (00-Matrix): "+config.getDemandMatrixFile() );
		read(config.getDemandMatrixFile(), new SetCertainODDemand(odRelation, nullfallData));
		log.info("Reading Reseizeitmatrix Nullfall (06, 18): " +config.getTravelTimesBaseMatrixFile());
		read(config.getTravelTimesBaseMatrixFile(), new SetTravelTimeForODRelationHandler(odRelation, nullfallData, true));

		log.info("Duplicating Nullfall");
		ScenarioForEvalData planfallData = nullfallData.createDeepCopy();
		
		log.info("Reading Cost Of Production & Operations' file (10,12,14 Sammelfile Nullfall): "+config.getImpedanceMatrixFile() );
		read(config.getImpedanceMatrixFile(), new SetImpedanceForOdRelationNullfallHandler(odRelation, nullfallData));

		log.info("Reading Cost Of Production & Operations' file (11,13,15 Sammelfile Planfall): "+config.getImpedanceMatrixFile() );
		read(config.getImpedanceMatrixFile(), new SetImpedanceForOdRelationPlanfallHandler(odRelation, planfallData));
		
		log.info("Reading Neuentstehend,induziert (02/03) "+ config.getNewDemandMatrixFile());
		read(config.getNewDemandMatrixFile(), new SetForCertainOdDemandNewOrDroppedHandler(odRelation, planfallData));
		log.info("Reading Entfallend (04) "+ config.getDroppedDemandMatrixFile());
		read(config.getDroppedDemandMatrixFile(), new SetForCertainOdDemandNewOrDroppedHandler(odRelation, planfallData));
		log.info("Reading Reseizeitmatrix Planfall (07): " +config.getTravelTimesStudyMatrixFile());
		read(config.getTravelTimesStudyMatrixFile(), new SetTravelTimeForODRelationHandler(odRelation, planfallData, false));
		log.info("Reading Verlagert (16-17): " +config.getImpedanceShiftedMatrixFile());
		read(config.getImpedanceShiftedMatrixFile(), new SetforOdImpedanceShiftedHandler(odRelation, planfallData, nullfallData));
		System.out.println("Nullfall:");
		System.out.println(nullfallData);

		System.out.println("Planfall:");
		System.out.println(planfallData);
		

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
		private final List<Id> odRelations;
		/**
		 * @param data
		 * @param odRelations 
		 */
		public DemandHandler(ScenarioForEvalData data, List<Id> odRelations) {
			this.data = data;
			this.odRelations=odRelations;
		}

		@Override
		public void startRow(String[] row) {
			if(comment(row)) return;

			String from = row[0].trim();
			String to = row[1].trim();
			Id odId = getODId(from, to);
			if (this.odRelations.contains(odId)){
			
			Mode mode = Mode.Bahn;
			
			setValuesForODRelation(odId, 	Key.makeKey(mode, DemandSegment.PV_BERUF, Attribute.XX),Double.parseDouble(row[2].trim())	, data);
			setValuesForODRelation(odId, 	Key.makeKey(mode, DemandSegment.PV_AUSBILDUNG, Attribute.XX),Double.parseDouble(row[3].trim())	, data);
			setValuesForODRelation(odId, 	Key.makeKey(mode, DemandSegment.PV_EINKAUF, Attribute.XX),Double.parseDouble(row[4].trim())	, data);
			setValuesForODRelation(odId, 	Key.makeKey(mode, DemandSegment.PV_COMMERCIAL, Attribute.XX),Double.parseDouble(row[5].trim())	, data);
			setValuesForODRelation(odId, 	Key.makeKey(mode, DemandSegment.PV_URLAUB, Attribute.XX),Double.parseDouble(row[6].trim())	, data);
			setValuesForODRelation(odId, 	Key.makeKey(mode, DemandSegment.PV_SONST, Attribute.XX),Double.parseDouble(row[7].trim())	, data);
			
			mode = Mode.Strasse;
			setValuesForODRelation(odId, 	Key.makeKey(mode, DemandSegment.PV_BERUF, Attribute.XX),Double.parseDouble(row[8].trim())	, data);
			setValuesForODRelation(odId, 	Key.makeKey(mode, DemandSegment.PV_AUSBILDUNG, Attribute.XX),Double.parseDouble(row[9].trim())	, data);
			setValuesForODRelation(odId, 	Key.makeKey(mode, DemandSegment.PV_EINKAUF, Attribute.XX),Double.parseDouble(row[10].trim())	, data);
			setValuesForODRelation(odId, 	Key.makeKey(mode, DemandSegment.PV_COMMERCIAL, Attribute.XX),Double.parseDouble(row[11].trim())	, data);
			setValuesForODRelation(odId, 	Key.makeKey(mode, DemandSegment.PV_URLAUB, Attribute.XX),Double.parseDouble(row[12].trim())	, data);
			setValuesForODRelation(odId, 	Key.makeKey(mode, DemandSegment.PV_SONST, Attribute.XX),Double.parseDouble(row[13].trim())	, data);
			}
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
            
         
            Mode mode = Mode.Bahn;
            
            setValuesForODRelation(currentOdId,     Key.makeKey(mode, DemandSegment.PV_BERUF, Attribute.XX),Double.parseDouble(row[2].trim())    , data);
            setValuesForODRelation(currentOdId,     Key.makeKey(mode, DemandSegment.PV_AUSBILDUNG, Attribute.XX),Double.parseDouble(row[3].trim())    , data);
            setValuesForODRelation(currentOdId,     Key.makeKey(mode, DemandSegment.PV_EINKAUF, Attribute.XX),Double.parseDouble(row[4].trim())    , data);
            setValuesForODRelation(currentOdId,     Key.makeKey(mode, DemandSegment.PV_COMMERCIAL, Attribute.XX),Double.parseDouble(row[5].trim())    , data);
            setValuesForODRelation(currentOdId,     Key.makeKey(mode, DemandSegment.PV_URLAUB, Attribute.XX),Double.parseDouble(row[6].trim())    , data);
            setValuesForODRelation(currentOdId,     Key.makeKey(mode, DemandSegment.PV_SONST, Attribute.XX),Double.parseDouble(row[7].trim())    , data);
            
            mode = Mode.Strasse;
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
		private final List<Id> odRelations;

		/**
		 * @param data
		 */
		public DemandNewOrDroppedHandler(List<Id> odRelations, ScenarioForEvalData data) {
			this.data = data;
			this.odRelations = odRelations;
		}

		@Override
		public void startRow(String[] row) {
			if(comment(row)) return;

			String from = row[0].trim();
			String to = row[1].trim();
			Id odid = getODId(from, to);
			if (this.odRelations.contains(odid)){
			data.getByODRelation(odid).inc(Key.makeKey(Mode.Strasse, DemandSegment.PV_BERUF, Attribute.Distanz_km), Double.parseDouble(row[2].trim()));
			data.getByODRelation(odid).inc(Key.makeKey(Mode.Strasse, DemandSegment.PV_AUSBILDUNG, Attribute.Distanz_km), Double.parseDouble(row[3].trim()));
			data.getByODRelation(odid).inc(Key.makeKey(Mode.Strasse, DemandSegment.PV_EINKAUF, Attribute.Distanz_km), Double.parseDouble(row[4].trim()));
			data.getByODRelation(odid).inc(Key.makeKey(Mode.Strasse, DemandSegment.PV_URLAUB, Attribute.Distanz_km), Double.parseDouble(row[5].trim()));
			data.getByODRelation(odid).inc(Key.makeKey(Mode.Strasse, DemandSegment.PV_SONST, Attribute.Distanz_km), Double.parseDouble(row[6].trim()));
			}

		}
	}
	
	private static class SetForCertainOdDemandNewOrDroppedHandler implements TabularFileHandler{

		private ScenarioForEvalData data;
		private Id odid;

		/**
		 * @param data
		 */
		public SetForCertainOdDemandNewOrDroppedHandler(Id odid, ScenarioForEvalData data) {
			this.data = data;
			this.odid = odid;
		}

		@Override
		
		public void startRow(String[] row) {
			if(comment(row)) return;

			String from = row[0].trim();
			String to = row[1].trim();
			Id currentOdid = getODId(from, to);
			if (currentOdid.equals(odid)){
<<<<<<< HEAD

			Values currentODRelation = data.getByODRelation(currentOdid);
			currentODRelation.inc(Key.makeKey(Mode.ROAD, DemandSegment.PV_BERUF, Attribute.km), Double.parseDouble(row[2].trim()));
			currentODRelation.inc(Key.makeKey(Mode.ROAD, DemandSegment.PV_AUSBILDUNG, Attribute.km), Double.parseDouble(row[3].trim()));
			currentODRelation.inc(Key.makeKey(Mode.ROAD, DemandSegment.PV_EINKAUF, Attribute.km), Double.parseDouble(row[4].trim()));
			currentODRelation.inc(Key.makeKey(Mode.ROAD, DemandSegment.PV_URLAUB, Attribute.km), Double.parseDouble(row[5].trim()));
			currentODRelation.inc(Key.makeKey(Mode.ROAD, DemandSegment.PV_SONST, Attribute.km), Double.parseDouble(row[6].trim()));
=======
			try {
			data.getByODRelation(currentOdid).inc(Key.makeKey(Mode.Strasse, DemandSegment.PV_BERUF, Attribute.Distanz_km), Double.parseDouble(row[2].trim()));
			data.getByODRelation(currentOdid).inc(Key.makeKey(Mode.Strasse, DemandSegment.PV_AUSBILDUNG, Attribute.Distanz_km), Double.parseDouble(row[3].trim()));
			data.getByODRelation(currentOdid).inc(Key.makeKey(Mode.Strasse, DemandSegment.PV_EINKAUF, Attribute.Distanz_km), Double.parseDouble(row[4].trim()));
			data.getByODRelation(currentOdid).inc(Key.makeKey(Mode.Strasse, DemandSegment.PV_URLAUB, Attribute.Distanz_km), Double.parseDouble(row[5].trim()));
			data.getByODRelation(currentOdid).inc(Key.makeKey(Mode.Strasse, DemandSegment.PV_SONST, Attribute.Distanz_km), Double.parseDouble(row[6].trim()));
			}
			catch (NullPointerException e){
				System.err.println("od ID : " + currentOdid + " has Changes in Demand, but initial Demand was not set.");
			}
>>>>>>> bvwp changes
				}
	
				
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
			setValuesForODRelation(getODId(from, to), Key.makeKey(Mode.Strasse, DemandSegment.PV_BERUF, Attribute.XX),  Double.parseDouble(row[2]),	data);
			setValuesForODRelation(getODId(from, to), Key.makeKey(Mode.Strasse, DemandSegment.PV_AUSBILDUNG, Attribute.XX),  Double.parseDouble(row[3]),	data);
			setValuesForODRelation(getODId(from, to), Key.makeKey(Mode.Strasse, DemandSegment.PV_EINKAUF, Attribute.XX),  Double.parseDouble(row[4]),	data);
			setValuesForODRelation(getODId(from, to), Key.makeKey(Mode.Strasse, DemandSegment.PV_URLAUB, Attribute.XX),  Double.parseDouble(row[5]),	data);
			setValuesForODRelation(getODId(from, to), Key.makeKey(Mode.Strasse, DemandSegment.PV_SONST, Attribute.XX),  Double.parseDouble(row[6]),	data);


		}
	
	}
	
	private static class CostHandler implements TabularFileHandler{

		private ScenarioForEvalData nullfalldata;
		private ScenarioForEvalData planfalldata;

		/**
		 * @param nullfalldata
		 */
		public CostHandler(ScenarioForEvalData nullfalldata, ScenarioForEvalData planfalldata ) {
			this.nullfalldata = nullfalldata;
			this.planfalldata = planfalldata;
		}

		@Override
		public void startRow(String[] row) {
			if(comment(row)) return;
			
			String from = row[0].trim();
			String to = row[1].trim();
			//TODO set correct values
				for (DemandSegment ds : DemandSegment.values()){
					if (ds.equals(DemandSegment.GV)) continue;
					if (ds.equals(DemandSegment.PV_NON_COMMERCIAL)) continue;
					
					setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.Strasse, ds, Attribute.Produktionskosten_Eu), Double.parseDouble(row[6]), nullfalldata);
					setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.Strasse, ds, Attribute.Produktionskosten_Eu), Double.parseDouble(row[7]), planfalldata);

				}
				setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.Strasse, DemandSegment.PV_BERUF, Attribute.Nutzerkosten_Eu), Double.parseDouble(row[8])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, nullfalldata);
				setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.Strasse, DemandSegment.PV_AUSBILDUNG, Attribute.Nutzerkosten_Eu), Double.parseDouble(row[9])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, nullfalldata);
				setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.Strasse, DemandSegment.PV_EINKAUF, Attribute.Nutzerkosten_Eu), Double.parseDouble(row[10])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, nullfalldata);
				setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.Strasse, DemandSegment.PV_COMMERCIAL, Attribute.Nutzerkosten_Eu), Double.parseDouble(row[11])*IVVReaderV2.BESETZUNGSGRAD_PV_GESCHAEFT, nullfalldata);
				setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.Strasse, DemandSegment.PV_URLAUB, Attribute.Nutzerkosten_Eu), Double.parseDouble(row[12])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, nullfalldata);
				setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.Strasse, DemandSegment.PV_SONST, Attribute.Nutzerkosten_Eu), Double.parseDouble(row[13])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, nullfalldata);
			
				setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.Strasse, DemandSegment.PV_BERUF, Attribute.Nutzerkosten_Eu), Double.parseDouble(row[14])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, planfalldata);
				setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.Strasse, DemandSegment.PV_AUSBILDUNG, Attribute.Nutzerkosten_Eu), Double.parseDouble(row[15])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, planfalldata);
				setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.Strasse, DemandSegment.PV_EINKAUF, Attribute.Nutzerkosten_Eu), Double.parseDouble(row[16])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, planfalldata);
				setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.Strasse, DemandSegment.PV_COMMERCIAL, Attribute.Nutzerkosten_Eu), Double.parseDouble(row[17])*IVVReaderV2.BESETZUNGSGRAD_PV_GESCHAEFT, planfalldata);
				setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.Strasse, DemandSegment.PV_URLAUB, Attribute.Nutzerkosten_Eu), Double.parseDouble(row[18])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, planfalldata);
				setValuesForODRelation(getODId(from, to),Key.makeKey(Mode.Strasse, DemandSegment.PV_SONST, Attribute.Nutzerkosten_Eu), Double.parseDouble(row[19])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, planfalldata);

			}
		
	}
	
	private static class IndexFromImpendanceFileHandler implements TabularFileHandler{

        private List<Id> allOdRelations;

        /**
         * @param nullfalldata
         */
        public IndexFromImpendanceFileHandler(List<Id> allOdRelations) {
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
	
	private static class SetImpedanceForOdRelationNullfallHandler implements TabularFileHandler{

		private ScenarioForEvalData data;
		private Id odid;

		/**
		 * @param data
		 */
		public SetImpedanceForOdRelationNullfallHandler(Id odid, ScenarioForEvalData data) {
			this.data = data;
			this.odid = odid;
		}

		@Override
		public void startRow(String[] row) {
			if(comment(row)) return;
			
			String from = row[0].trim();
			String to = row[1].trim();
			Id currentOdId = getODId(from, to);
			if(currentOdId.equals(odid)){
			
			for (DemandSegment ds : DemandSegment.values()){
					if (ds.equals(DemandSegment.GV)) continue;
					if (ds.equals(DemandSegment.PV_NON_COMMERCIAL)) continue;
					
					setValuesForODRelation(currentOdId,Key.makeKey(Mode.Strasse, ds, Attribute.Produktionskosten_Eu), Double.parseDouble(row[6]), data);
				}
				setValuesForODRelation(currentOdId,Key.makeKey(Mode.Strasse, DemandSegment.PV_BERUF, Attribute.Produktionskosten_Eu), Double.parseDouble(row[8])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, data);
				setValuesForODRelation(currentOdId,Key.makeKey(Mode.Strasse, DemandSegment.PV_AUSBILDUNG, Attribute.Produktionskosten_Eu), Double.parseDouble(row[9])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, data);
				setValuesForODRelation(currentOdId,Key.makeKey(Mode.Strasse, DemandSegment.PV_EINKAUF, Attribute.Produktionskosten_Eu), Double.parseDouble(row[10])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, data);
				setValuesForODRelation(currentOdId,Key.makeKey(Mode.Strasse, DemandSegment.PV_COMMERCIAL, Attribute.Produktionskosten_Eu), Double.parseDouble(row[11])*IVVReaderV2.BESETZUNGSGRAD_PV_GESCHAEFT, data);
				setValuesForODRelation(currentOdId,Key.makeKey(Mode.Strasse, DemandSegment.PV_URLAUB, Attribute.Produktionskosten_Eu), Double.parseDouble(row[12])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, data);
				setValuesForODRelation(currentOdId,Key.makeKey(Mode.Strasse, DemandSegment.PV_SONST, Attribute.Produktionskosten_Eu), Double.parseDouble(row[13])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, data);
			
				
			
			}}
		
	}
	
	

	
	private static class SetImpedanceForOdRelationPlanfallHandler implements TabularFileHandler{

		private ScenarioForEvalData data;
		private Id odid;

		/**
		 * @param data
		 */
		public SetImpedanceForOdRelationPlanfallHandler(Id odid, ScenarioForEvalData data) {
			this.data = data;
			this.odid = odid;
		}

		@Override
		public void startRow(String[] row) {
			if(comment(row)) return;
			
			String from = row[0].trim();
			String to = row[1].trim();
			Id currentOdId = getODId(from, to);
			if(currentOdId.equals(odid)){
			
			for (DemandSegment ds : DemandSegment.values()){
						if (ds.equals(DemandSegment.GV)) continue;
						if (ds.equals(DemandSegment.PV_NON_COMMERCIAL)) continue;
				
					setValuesForODRelation(currentOdId,Key.makeKey(Mode.Strasse, ds, Attribute.Produktionskosten_Eu), Double.parseDouble(row[7]), data);
				}
				setValuesForODRelation(currentOdId,Key.makeKey(Mode.Strasse, DemandSegment.PV_BERUF, Attribute.Produktionskosten_Eu), Double.parseDouble(row[14])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, data);
				setValuesForODRelation(currentOdId,Key.makeKey(Mode.Strasse, DemandSegment.PV_AUSBILDUNG, Attribute.Produktionskosten_Eu), Double.parseDouble(row[15])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, data);
				setValuesForODRelation(currentOdId,Key.makeKey(Mode.Strasse, DemandSegment.PV_EINKAUF, Attribute.Produktionskosten_Eu), Double.parseDouble(row[16])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, data);
				setValuesForODRelation(currentOdId,Key.makeKey(Mode.Strasse, DemandSegment.PV_COMMERCIAL, Attribute.Produktionskosten_Eu), Double.parseDouble(row[17])*IVVReaderV2.BESETZUNGSGRAD_PV_GESCHAEFT, data);
				setValuesForODRelation(currentOdId,Key.makeKey(Mode.Strasse, DemandSegment.PV_URLAUB, Attribute.Produktionskosten_Eu), Double.parseDouble(row[18])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, data);
				setValuesForODRelation(currentOdId,Key.makeKey(Mode.Strasse, DemandSegment.PV_SONST, Attribute.Produktionskosten_Eu), Double.parseDouble(row[19])*IVVReaderV2.BESETZUNGSGRAD_PV_PRIVAT, data);
			
				
			
			}}
		
	}
	
	
	
	private static class ImpedanceShiftedHandler implements TabularFileHandler{

		private ScenarioForEvalData planfalldata;
		private ScenarioForEvalData nullfalldata;
		private final List<Id> odRelations;

		/**
		 * @param nullfalldata
		 */
		public ImpedanceShiftedHandler(List<Id> odRelations, ScenarioForEvalData planfalldata, ScenarioForEvalData nullfalldata) {
			this.planfalldata = planfalldata;
			this.nullfalldata = nullfalldata;
			this.odRelations = odRelations;

			
		}

		@Override
		public void startRow(String[] row) {
			if(comment(row)) return;
			String from = row[0].trim();
			String to = row[2].trim();
			Id odId = getODId(from, to);
			// Annahme: Wegezwecke fuer Verlagerungen sind identisch mit Wegezwecken im Bahn fuer OD-Relation im Nullfall
			
			if (this.odRelations.contains(odId)){
			Double totalBahn = 0.;
				
			Map<DemandSegment,Double> bahnZwecke = new HashMap<DemandSegment,Double>();
			for (DemandSegment segment : DemandSegment.values()){
				if (segment.equals(DemandSegment.GV)) continue;
				if (segment.equals(DemandSegment.PV_NON_COMMERCIAL)) continue;
				
<<<<<<< HEAD
				Double bahnhere = nullfalldata.getByODRelation(odId).getAttributes(Mode.RAIL, segment).getByEntry(Attribute.XX);	
				bahnZwecke.put(segment, bahnhere);
				totalBahn += bahnhere;
				if (odId.equals(getODId("1500301", "1509001"))) System.out.println(segment + " : "+bahnhere); 
=======
				Double bahnhere = nullfalldata.getByODRelation(odId).getAttributes(Mode.Bahn, segment).getByEntry(Attribute.XX);	
				totalBahn =+ bahnhere;
>>>>>>> bvwp changes
				
			}
			if (odId.equals(getODId("1500301", "1509001"))) System.out.println("Total Bahn : "+totalBahn); 

			for (DemandSegment segment : DemandSegment.values()){
				if (segment.equals(DemandSegment.GV)) continue;
				if (segment.equals(DemandSegment.PV_NON_COMMERCIAL)) continue;
				
<<<<<<< HEAD
				Double verl = Double.parseDouble(row[11].trim()) * -1.0 * WERKTAGEPROJAHR * bahnZwecke.get(segment)/totalBahn ;
				if (odId.equals(getODId("1500301", "1509001"))) System.out.println(segment+": "+row[11].trim() + "* -1 * " +WERKTAGEPROJAHR+ " * "+bahnZwecke.get(segment) +" / "+ totalBahn +" = "+ verl ); 

				// taegl. Verlagerungen * -1 * werktage * rel zweckanteil
				planfalldata.getByODRelation(odId).inc(Key.makeKey(Mode.RAIL, segment, Attribute.XX), verl);
=======
				Double verl = Double.parseDouble(row[11].trim()) * WERKTAGEPROJAHR * bahnZwecke.get(segment)/totalBahn ;
				planfalldata.getByODRelation(odId).inc(Key.makeKey(Mode.Bahn, segment, Attribute.XX), verl);
>>>>>>> bvwp changes
			}
		}}
		
	}
	
	private static class SetforOdImpedanceShiftedHandler implements TabularFileHandler{

		private ScenarioForEvalData planfalldata;
		private ScenarioForEvalData nullfalldata;
		private Id odid;
		/**
		 * @param nullfalldata
		 */
		public SetforOdImpedanceShiftedHandler(Id odid, ScenarioForEvalData planfalldata, ScenarioForEvalData nullfalldata) {
			this.planfalldata = planfalldata;
			this.nullfalldata = nullfalldata;
			this.odid = odid;
		}

		@Override
		public void startRow(String[] row) {
			if(comment(row)) return;
			String from = row[0].trim();
			String to = row[2].trim();
			Id currentOdId = getODId(from, to);
			if (odid.equals(currentOdId)){
			
			// Annahme: Wegezwecke fuer Verlagerungen sind identisch mit Wegezwecken im MIV fuer OD-Relation
			Double totalMIV = 0.;
			Map<DemandSegment,Double> mivZwecke = new HashMap<DemandSegment,Double>();
			for (DemandSegment segment : DemandSegment.values()){
				if (segment.equals(DemandSegment.GV)) continue;
				if (segment.equals(DemandSegment.PV_NON_COMMERCIAL)) continue;
				
				Double mivhere = nullfalldata.getByODRelation(currentOdId).getAttributes(Mode.Strasse, segment).getByEntry(Attribute.XX);	
				totalMIV =+ mivhere;
				
			}
			
			for (DemandSegment segment : DemandSegment.values()){
				if (segment.equals(DemandSegment.GV)) continue;
				if (segment.equals(DemandSegment.PV_NON_COMMERCIAL)) continue;
			Double verl = Double.parseDouble(row[4].trim()) * WERKTAGEPROJAHR * mivZwecke.get(segment)/totalMIV ;
			setValuesForODRelation(currentOdId, Key.makeKey(Mode.Bahn, segment, Attribute.XX), verl, nullfalldata);
			setValuesForODRelation(currentOdId, Key.makeKey(Mode.Bahn, segment, Attribute.Reisezeit_h), Double.parseDouble(row[5].trim()), nullfalldata);
			setValuesForODRelation(currentOdId, Key.makeKey(Mode.Bahn, segment, Attribute.Reisezeit_h), Double.parseDouble(row[5].trim()), planfalldata);
			setValuesForODRelation(currentOdId, Key.makeKey(Mode.Bahn, segment, Attribute.XX), 0., planfalldata);

			}
		}}
		
	}
	
	private static class TravelTimeHandler implements TabularFileHandler{

		private ScenarioForEvalData data;
		boolean includeRail;
		private final List<Id> odRelations;
		/**
		 * @param data
		 * @param odRelations 
		 */
		public TravelTimeHandler(ScenarioForEvalData data, List<Id> odRelations, boolean includeRail) {
			this.data = data;
			this.includeRail = includeRail;
			this.odRelations=odRelations;
		}

		@Override
		public void startRow(String[] row) {
			if(comment(row)) return;
			
			String[] myRow = myRowSplit(row[0]);
			Id odId = getODId(getIdForTTfiles(myRow[2]), getIdForTTfiles(myRow[3]));
			Id reverseOd = getODId(getIdForTTfiles(myRow[3]), getIdForTTfiles(myRow[2]));
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
			setValuesForODRelation(reverseOd, Key.makeKey(Mode.Bahn, ds, Attribute.Distanz_km), Double.parseDouble(myRow[4].trim()), data);
			setValuesForODRelation(reverseOd, Key.makeKey(Mode.Bahn, ds, Attribute.Nutzerkosten_Eu), Double.parseDouble(myRow[4].trim())*BAHNPREISPROKM, data);

			}
			
			}
			}
		}
		
	}
	
	private static class SetTravelTimeForODRelationHandler implements TabularFileHandler{

		private ScenarioForEvalData data;
		boolean includeRail;
		private Id odid;
		/**
		 * @param data
		 */
		public SetTravelTimeForODRelationHandler(Id odid, ScenarioForEvalData data, boolean includeRail) {
			this.odid = odid;
			this.data = data;
			this.includeRail = includeRail;
		}

		@Override
		public void startRow(String[] row) {
			if(comment(row)) return;
			
			String[] myRow = myRowSplit(row[0]);
			Id currentOd = getODId(getIdForTTfiles(myRow[2]), getIdForTTfiles(myRow[3]));
			if (currentOd.equals(this.odid)){
			
			for (DemandSegment ds : DemandSegment.values()){
				if (ds.equals(DemandSegment.GV)) continue;
				if (ds.equals(DemandSegment.PV_NON_COMMERCIAL)) continue;
			setValuesForODRelation(currentOd, Key.makeKey(Mode.Strasse, ds, Attribute.Distanz_km), Double.parseDouble(myRow[4].trim()), data);
			setValuesForODRelation(currentOd, Key.makeKey(Mode.Strasse, ds, Attribute.Reisezeit_h), Double.parseDouble(myRow[5].trim())/60., data);
			
			
			if (includeRail){
			setValuesForODRelation(currentOd, Key.makeKey(Mode.Bahn, ds, Attribute.Distanz_km), Double.parseDouble(myRow[4].trim()), data);
			setValuesForODRelation(currentOd, Key.makeKey(Mode.Bahn, ds, Attribute.Nutzerkosten_Eu), Double.parseDouble(myRow[4].trim())*BAHNPREISPROKM, data);
			}
			
			}
			}}
			
		
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
		String dir = "/Users/jb/tucloud/bvwp/data/P2030_Daten_IVV_20131210/";
		config.setDemandMatrixFile(dir + "P2030_2010_BMVBS_ME2_131008.csv");
		config.setRemainingDemandMatrixFile(dir + "P2030_2010_verbleibend_ME2.csv");
		config.setNewDemandMatrixFile(dir + "P2030_2010_neuentstanden_ME2.csv");
		config.setDroppedDemandMatrixFile(dir + "P2030_2010_entfallend_ME2.csv");
		
		config.setTravelTimesBaseMatrixFile(dir + "P2030_Widerstaende_Ohnefall.wid");
		config.setTravelTimesStudyMatrixFile(dir + "P2030_Widerstaende_Mitfall.wid");
		config.setImpedanceMatrixFile(dir + "P2030_2010_A14_induz_ME2.wid");
//		config.setImpedanceMatrixFile(dir + "induz_test.wid");
		config.setImpedanceShiftedMatrixFile(dir + "P2030_2010_A14_verlagert_ME2.wid");
		
		IVVReaderV2 reader = new IVVReaderV2(config);
		reader.read();
	}
}

