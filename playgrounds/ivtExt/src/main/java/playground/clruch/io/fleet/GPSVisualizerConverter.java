/**
 * 
 */
package playground.clruch.io.fleet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ch.ethz.idsc.queuey.datalys.csv.CSVUtils;

/** @author Claudio Ruch */
public class GPSVisualizerConverter {

    /** @param args
     * @throws Exception */
    public static void main(String[] args) throws Exception {


        List<SFTableEntry> entriesList = new ArrayList();
        
        /* GPS file conversion and sorting example */
        File inputFile = new File("/home/clruch/Downloads/SanFrancisco/new_abboip.txt");
        File ouputFile = new File("/home/clruch/Downloads/SanFrancisco/new_abboipGPSVisualizer.txt");
        BufferedReader br = new BufferedReader(new FileReader(inputFile));
        BufferedWriter bw = new BufferedWriter(new FileWriter(ouputFile));

        bw.write("latitude , longitude \n");

        try {
            while (true) {
                String oldline = br.readLine();
                if (Objects.isNull(oldline))
                    break;

                addentry(oldline, entriesList);
                bw.write(convertLine(oldline) + "\n");
            }

        } catch (Exception e) {

        }

        bw.write("hello does it work");
        bw.close();
        
        
        
        // sorting
        Collections.sort(entriesList, new SFTableEntryComparator());
        entriesList.stream().forEach(e-> System.out.println(e.c4));

        

    }

    public static String convertLine(String line) {
        List<String> entries = CSVUtils.csvLineToList(line, " ");
        String newLine = entries.get(0) + " , " + entries.get(1);
        return newLine;
    }

    
    public static void addentry(String oldline, List<SFTableEntry> entriesList){
        List<String> entries = CSVUtils.csvLineToList(oldline, " ");
        entriesList.add(new SFTableEntry(entries.get(0), entries.get(1), entries.get(2), entries.get(3)));
    }
}
