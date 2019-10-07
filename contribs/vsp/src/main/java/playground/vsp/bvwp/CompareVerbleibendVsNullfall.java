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

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;


public class CompareVerbleibendVsNullfall
{
    private static final Logger log = Logger.getLogger(CompareVerbleibendVsNullfall.class);
    Map<String,Double> verbleibend;
    Map<String,Double> nullnull;
    Set<String> diffIds;
    IVVReaderConfigGroup config;
    
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        IVVReaderConfigGroup config = new IVVReaderConfigGroup();
//      String dir = "/Users/jb/tucloud/bvwp/data/P2030_Daten_IVV_20131210/";
        String dir = "C:/local_jb/testrechnungen_strasse/IVV_NeubauA14/P2030_Daten_IVV_20131210/";
//      String dir = "C:/local_jb/testrechnungen_strasse/IVV_NeubauA14/MD_only/";
        config.setDemandMatrixFile(dir + "P2030_2010_BMVBS_ME2_131008.csv");
        config.setRemainingDemandMatrixFile(dir + "P2030_2010_verbleibend_ME2.csv");
        config.setNewDemandMatrixFile(dir + "P2030_2010_neuentstanden_ME2.csv");
        config.setDroppedDemandMatrixFile(dir + "P2030_2010_entfallend_ME2.csv");
        
        config.setTravelTimesBaseMatrixFile(dir + "P2030_Widerstaende_Ohnefall.wid");
        config.setTravelTimesStudyMatrixFile(dir + "P2030_Widerstaende_Mitfall.wid");
        config.setImpedanceMatrixFile(dir + "P2030_2010_A14_induz_ME2.wid");
//      config.setImpedanceMatrixFile(dir + "induz_test.wid");
//      config.setImpedanceMatrixFile(dir + "induz_mdst.wid");
        config.setImpedanceShiftedMatrixFile(dir + "P2030_2010_A14_verlagert_ME2.wid");
        CompareVerbleibendVsNullfall cvn = new CompareVerbleibendVsNullfall(config);
        cvn.read();
        cvn.compareAndPrint("comp.txt");
        
    }

       private void compareAndPrint(String fileName)
    {
        Set<String> allOdId = new TreeSet<>();
        
        allOdId.addAll(this.verbleibend.keySet());
        allOdId.addAll(this.nullnull.keySet());
        Writer writer = IOUtils.getBufferedWriter(fileName);
        try{
        for (String odId : allOdId){
            double verblV = 0.;
            double nullV = 0.;
            if (this.verbleibend.containsKey(odId)) verblV = this.verbleibend.get(odId);
            if (this.nullnull.containsKey(odId)) nullV = this.nullnull.get(odId);
            double diff = verblV - nullV;
            if ((diff>0) && (this.diffIds.contains(odId))){
            writer.write(odId.toString() + "\t"+verblV+ "\t"+nullV+ "\t"+diff+"\n");
            
            }
            }
        writer.flush();
        writer.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void read()
    {
           log.info("Reading Verbleibend (01)) "+ config.getRemainingDemandMatrixFile());
           read(config.getRemainingDemandMatrixFile(), new DemandRemainingHandler(this.verbleibend));    
           log.info("Reading Nullfall (0)) "+ config.getDemandMatrixFile());
           read(config.getDemandMatrixFile(), new DemandHandler(nullnull));   
           log.info("Creating index using Production & Operations' file : "+config.getImpedanceMatrixFile() );
           read(config.getImpedanceMatrixFile(), new IndexFromImpendanceFileHandler(diffIds));
    }

    public CompareVerbleibendVsNullfall(IVVReaderConfigGroup config)
    {
          this.verbleibend = new HashMap<>();
          this.nullnull = new HashMap<>();
          this.diffIds = new TreeSet<>();
          this.config = config;
    }
    
    private void read(String file, TabularFileHandler handler) {
        TabularFileParserConfig config = new TabularFileParserConfig();
        log.info("parsing " + file);
        config.setDelimiterTags(new String[]{";"});
        config.setCommentTags(new String[]{"#", " #"});
        config.setFileName(file);
        new TabularFileParser().parse(config, handler);
        log.info("done. (parsing " + file + ")");
    }
    
    private static String getODId(String from, String to){
        return from + "---" + to;
    }
    private static boolean comment(String[] row) {
        if(row[0].startsWith("#")){
            log.warn("TabularFileParser did not identify '#' as comment regex! Don't know why!?");
            return true;
        }
        return false;
    }

    private static class DemandRemainingHandler implements TabularFileHandler{
        
        
        Map<String ,Double> verbleibend;
        public DemandRemainingHandler(Map<String,Double> verbleibend) {
        this.verbleibend = verbleibend;
            
        }
        
        
        @Override
        public void startRow(String[] row) {
            if(comment(row)) return;
            
            String from = row[0].trim();
            String to = row[1].trim();
            String odId = getODId(from, to);
            double v = Double.parseDouble(row[6]);
            this.verbleibend.put(odId, v);
            //2 beruf
            //3 ausb
            //4 eink
            //5 urlaub
            //6 privat
            
        }
        
    }
    private static class DemandHandler implements TabularFileHandler{


        Map<String,Double> nullnull;
        public DemandHandler(Map<String,Double> nullnull) {
            this.nullnull = nullnull;
        }

        @Override
        public void startRow(String[] row) {
            if(comment(row)) return;

            String from = row[0].trim();
            String to = row[1].trim();
            String odId = getODId(from, to);
                     
            double v = Double.parseDouble(row[13].trim());
            //8 beruf
            //9 ausb
            //10 eink
            //12 urlaub
            //13 privat
            this.nullnull.put(odId, v);
          
            
        }
        
    }
    private static class IndexFromImpendanceFileHandler implements TabularFileHandler{

        private Set<String> allOdRelations;

        /**
         * @param nullfalldata
         */
        public IndexFromImpendanceFileHandler(Set<String> allOdRelations) {
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
}