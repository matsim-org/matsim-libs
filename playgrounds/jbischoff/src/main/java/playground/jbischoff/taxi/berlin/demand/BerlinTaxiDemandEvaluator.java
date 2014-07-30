package playground.jbischoff.taxi.berlin.demand;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.Matrix;

import playground.jbischoff.taxi.berlin.data.BeelineDistanceExractor;
import playground.michalm.util.matrices.MatrixUtils;


public class BerlinTaxiDemandEvaluator
{

    Map<Date, Integer> hourlyFromDemand = new TreeMap<Date, Integer>();
    Map<Date, Double> hourlyAverageTripLength = new TreeMap<Date, Double>();
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat filenameformat = new SimpleDateFormat("yyyyMMddHHmmss");

   static String fileprefix = "C:\\local_jb\\data\\taxi_berlin\\2013\\OD\\";
//   static String fileprefix = "C:\\local_jb\\data\\taxi_berlin\\2014\\OD\\";


    /**
     * @param args
     * @throws ParseException 
     */
    public static void main(String[] args) throws ParseException
    {
        BerlinTaxiDemandEvaluator bte = new BerlinTaxiDemandEvaluator();
        Date start = filenameformat.parse("20130415000000");
//        Date start = filenameformat.parse("20140407000000");
        Date end = filenameformat.parse("20130421230000");
//        Date end = filenameformat.parse("20140413230000");
        bte.read(fileprefix+"demandMatrices.xml",start, end);
        bte.write(fileprefix+"demandWeekly.csv");

    }


    private void write(String outputFileName)
    {
        DecimalFormat f = new DecimalFormat("#0.00"); 

        try {
            BufferedWriter bw = IOUtils.getBufferedWriter(outputFileName);
            for (java.util.Map.Entry<Date, Integer> e : hourlyFromDemand.entrySet()) {
                bw.append(SDF.format(e.getKey()) + "\t" + e.getValue() + "\t"+f.format(this.hourlyAverageTripLength.get(e.getKey()))+"\n");
            }
            bw.flush();
            bw.close();
        }

        catch (IOException e1) {
            e1.printStackTrace();
        }
    }


    private void read(String filename, Date start, Date end)
    {
        Matrices matrices = MatrixUtils.readMatrices(filename);
        BeelineDistanceExractor bde = new BeelineDistanceExractor();
        Date currentHr = start;
        do {
            int currentDemand = 0;
            double currentPkm = 0.;
            Matrix currentMatrix = matrices.getMatrix(filenameformat.format(currentHr));
            for (ArrayList<Entry> l :currentMatrix.getFromLocations().values()){
                for (Entry e: l){
                    currentDemand += e.getValue();
                    currentPkm += bde.calcDistance(e.getFromLocation(), e.getToLocation()) * e.getValue();
                }
            }
            this.hourlyFromDemand.put(currentHr, currentDemand);
            this.hourlyAverageTripLength.put(currentHr,currentPkm/currentDemand);
            currentHr = getNextTime(currentHr);
        }
        while (currentHr.before(end));
        
    }
    
    private Date getNextTime(Date currentTime)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentTime);
        cal.add(Calendar.HOUR_OF_DAY, 1);
        return cal.getTime();
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