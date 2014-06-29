package playground.jbischoff.taxi.berlin.demand;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;


public class BerlinTaxiDemandEvaluator
{

    Map<Integer, Integer> hourlyFromDemand = new TreeMap<Integer, Integer>();

    String fileprefix = "C:\\local_jb\\data\\OD\\kw9\\";


    /**
     * @param args
     */
    public static void main(String[] args)
    {
        BerlinTaxiDemandEvaluator bte = new BerlinTaxiDemandEvaluator();
        bte.read();
        bte.write("C:\\local_jb\\data\\OD\\kw9\\demandWeekly.csv");

    }


    private void write(String outputFileName)
    {
        try {
            BufferedWriter bw = IOUtils.getBufferedWriter(outputFileName);
            for (Entry<Integer, Integer> e : hourlyFromDemand.entrySet()) {
                bw.append(e.getKey() + "\t" + e.getValue() + "\n");
            }
            bw.flush();
            bw.close();
        }

        catch (IOException e1) {
            e1.printStackTrace();
        }
    }


    private void read()
    {
        int z = 0;
        for (long i = 20130225; i < 20130228; i++) {
            for (int t = 0; t < 24; t++) {
                String hrstring = String.format("%02d", t);
                String filename = fileprefix + i + "/OD_" + i + hrstring + "0000.dat";
                DemandCounter dc = new DemandCounter();
                read(filename, dc);
                String key = i + hrstring;
                //				hourlyFromDemand.put(Integer.parseInt(key), dc.getFrom());
                hourlyFromDemand.put(z * 3600, dc.getFrom());

                z++;
            }
        }

        for (long i = 20130301; i < 20130304; i++) {
            for (int t = 0; t < 24; t++) {
                String hrstring = String.format("%02d", t);
                String filename = fileprefix + i + "/OD_" + i + hrstring + "0000.dat";
                DemandCounter dc = new DemandCounter();
                read(filename, dc);
                String key = i + hrstring;
                //					hourlyFromDemand.put(Integer.parseInt(key), dc.getFrom());
                hourlyFromDemand.put(z * 3600, dc.getFrom());

                z++;
            }
        }
    }


    private void read(String file, TabularFileHandler handler)
    {
        TabularFileParserConfig config = new TabularFileParserConfig();
        config.setDelimiterTags(new String[] { "\t", " " });
        config.setFileName(file);
        new TabularFileParser().parse(config, handler);
    }
}


class DemandCounter
    implements TabularFileHandler
{
    private Integer from = 0;


    @Override
    public void startRow(String[] row)
    {
        try {
            from = from + Integer.parseInt(row[2]);
        }
        catch (IndexOutOfBoundsException e) {}
    }


    public Integer getFrom()
    {
        return from;
    }

}