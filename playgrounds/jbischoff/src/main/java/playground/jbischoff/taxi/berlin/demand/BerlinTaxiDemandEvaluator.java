package playground.jbischoff.taxi.berlin.demand;

import java.io.*;
import java.text.*;
import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.matrices.*;
import org.matsim.matrices.Entry;

import playground.jbischoff.taxi.berlin.data.BeelineDistanceExractor;
import playground.michalm.util.matrices.MatrixUtils;


public class BerlinTaxiDemandEvaluator
{

    Map<Date, Integer> hourlyFromDemand = new TreeMap<Date, Integer>();
    Map<Date, Double> hourlyAverageTripLength = new TreeMap<Date, Double>();
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat filenameformat = new SimpleDateFormat("yyyyMMddHHmmss");

   static String fileprefix = "/Users/jb/sustainability-w-michal-and-dlr/data/taxi_berlin/2014_10_bahnstreik/OD_BRB_2014-10/oct/";
//   static String fileprefix = "C:\\local_jb\\data\\taxi_berlin\\2013\\OD\\";
//   static String fileprefix = "C:\\local_jb\\data\\taxi_berlin\\2014\\OD\\";


    /**
     * @param args
     * @throws ParseException 
     */
    public static void main(String[] args) throws ParseException
    {
        BerlinTaxiDemandEvaluator bte = new BerlinTaxiDemandEvaluator();
//        Date start = filenameformat.parse("20130415000000");
//        Date start = filenameformat.parse("20140407000000");
//        Date end = filenameformat.parse("20130421230000");
//        Date end = filenameformat.parse("20140413230000");
        
        
        Date start = filenameformat.parse("20141013000000");
        Date end =	 filenameformat.parse("20141020230000");
      
        bte.read(fileprefix+"demandMatrices.xml",start, end);
        
//        bte.dumpSym(fileprefix+"demandMatrices.xml",start, end);
//        Id alex = Id.create("01011303");
//        Id friedrichshain = Id.create("02040701");
//        Id zoo = Id.create("04030931");
//        Id txl = Id.create("12214125");
        
//        bte.analyse(fileprefix+"demandMatrices.xml", start, end, bte.initZones());
        
//        bte.read(fileprefix+"demandMatrices.xml",start, end, alex,friedrichshain);
//        bte.read(fileprefix+"demandMatrices.xml",start, end, friedrichshain, alex);
//        bte.read(fileprefix+"demandMatrices.xml",start, end, zoo, alex);
//        bte.read(fileprefix+"demandMatrices.xml",start, end,  alex, zoo);
//        bte.read(fileprefix+"demandMatrices.xml",start, end,  txl);
//        
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
    
    private List<String> initZones(){
        List<String> zones = new ArrayList<String>();
        zones.add("0101");
        zones.add("0102");
        zones.add("0103");
        zones.add("0201");
        zones.add("0202");
        zones.add("0203");
        zones.add("0204");
        zones.add("0205");
        zones.add("0801");
        zones.add("0704");
        zones.add("0701");
        zones.add("0702");
        zones.add("0405");
        zones.add("0403");
        zones.add("0306");
        zones.add("0307");
        return zones;
        
    }

    private void write(String outputFileName,Map<Date,Integer> values)
    {

        try {
            BufferedWriter bw = IOUtils.getBufferedWriter(outputFileName);
            for (java.util.Map.Entry<Date, Integer> e : values.entrySet()) {
                bw.append(SDF.format(e.getKey()) + "\t"+e.getValue()+"\n");
            }
            bw.flush();
            bw.close();
        }

        catch (IOException e1) {
            e1.printStackTrace();
        }
    }
    
    
    private void dumpSym(String filename, Date start, Date end)
    {
        Matrices matrices = MatrixUtils.readMatrices(filename);
        BeelineDistanceExractor bde = new BeelineDistanceExractor();
        Map<Id<Zone>,Double> fromDemand = new TreeMap<>();
        Map<Id<Zone>,Double> toDemand = new TreeMap<>();
        Date currentHr = start;
        
        do {
            Matrix currentMatrix = matrices.getMatrix(filenameformat.format(currentHr));
            for (ArrayList<Entry> l :currentMatrix.getFromLocations().values()){
                for (Entry e: l){
                    addToMap(fromDemand,Id.create(e.getFromLocation(), Zone.class),e.getValue());
                    addToMap(toDemand,Id.create(e.getToLocation(), Zone.class),e.getValue());
                }
            }
            currentHr = getNextTime(currentHr);

        }
        while (currentHr.before(end));
        
        Set<Id<Zone>> allZones = new TreeSet<>();
        allZones.addAll(fromDemand.keySet());
        allZones.addAll(toDemand.keySet());
        
        try {
            BufferedWriter bw = IOUtils.getBufferedWriter(fileprefix+"symdemand_tuesday.txt");
            for (Id<Zone> zid : allZones){
                double from = 0;
                double to = 0;
                if (fromDemand.containsKey(zid)) from = fromDemand.get(zid);
                if (toDemand.containsKey(zid)) to= toDemand.get(zid);
                bw.append(zid.toString()+"\t"+Math.round(from)+"\t"+Math.round(to)+"\t"+Math.round(from+to)+"\n");
            }
            bw.flush();
            bw.close();
        }

        catch (IOException e1) {
            e1.printStackTrace();
        }
        
    }
    
    private void addToMap(Map<Id<Zone>,Double> map, Id<Zone> zone, double value){
        double newValue = 0;
        if (map.containsKey(zone)) newValue = map.get(zone);
        newValue+=value;
        map.put(zone, newValue);
        
    }
    
    
    private void read(String filename, Date start, Date end)
    {
        Matrices matrices = MatrixUtils.readMatrices(filename);
        BeelineDistanceExractor bde = new BeelineDistanceExractor();
        Date currentHr = start;
        int[] distances = new int[100]; 
        for (int i = 0;i<100;i++){
            distances[i] = 0;
        }
        do {
            int currentDemand = 0;
            double currentPkm = 0.;
            Matrix currentMatrix = matrices.getMatrix(filenameformat.format(currentHr));
            for (ArrayList<Entry> l :currentMatrix.getFromLocations().values()){
                for (Entry e: l){
                    currentDemand += e.getValue();
                    double distance = bde.calcDistance(Id.create(e.getFromLocation(), Zone.class), Id.create(e.getToLocation(), Zone.class));
                    int distClass = (int)Math.ceil(distance);
                    distances[distClass]++;
                    currentPkm += distance * e.getValue();
                }
            }
            this.hourlyFromDemand.put(currentHr, currentDemand);
            this.hourlyAverageTripLength.put(currentHr,currentPkm/currentDemand);
            currentHr = getNextTime(currentHr);
        }
        while (currentHr.before(end));
        
        try {
            BufferedWriter bw = IOUtils.getBufferedWriter(fileprefix+"distances.txt");
            for (int i = 0;i<100;i++){
                bw.append(i+"\t"+distances[i]+"\n");
            }
            bw.flush();
            bw.close();
        }

        catch (IOException e1) {
            e1.printStackTrace();
        }
        
    }
    
    private void read(String filename, Date start, Date end, Id<Zone> fromZone, Id<Zone> toZone)
    {
        Map<Date,Integer> amount = new TreeMap<Date, Integer>();
        Matrices matrices = MatrixUtils.readMatrices(filename);
        Date currentHr = start;
        do {
            Matrix currentMatrix = matrices.getMatrix(filenameformat.format(currentHr));
            double value = 0.;
            try{
            value = currentMatrix.getEntry(fromZone.toString(), toZone.toString()).getValue();
            }
            catch (NullPointerException e) {
                
            }
            currentHr = getNextTime(currentHr);
            amount.put(currentHr, (int) value);
        }
        while (currentHr.before(end));
        String outfile = fileprefix+fromZone.toString()+"_"+toZone.toString()+".txt";
        write(outfile,amount);
    }
    
    private void analyse(String filename, Date start, Date end, List<String> zonesPrefixes)
    {
        Matrices matrices = MatrixUtils.readMatrices(filename);
        BeelineDistanceExractor bde = new BeelineDistanceExractor();
        Date currentHr = start;
        double from = 0.;
        double to = 0.;
        double inner = 0.;
        double other = 0.;
        do {
            Matrix currentMatrix = matrices.getMatrix(filenameformat.format(currentHr));
            for (ArrayList<Entry> l :currentMatrix.getFromLocations().values()){
                for (Entry e: l){
                    String fromZ = e.getFromLocation().toString().substring(0, 4);
                    
                    String toZ = e.getToLocation().toString().substring(0,4);
                    if (zonesPrefixes.contains(fromZ) && zonesPrefixes.contains(toZ)){
                        inner+=e.getValue();
                        continue;
                    }
                    else if (zonesPrefixes.contains(fromZ)){
                        from += e.getValue();
                        continue;
                    }
                    else if (zonesPrefixes.contains(toZ)){
                        to += e.getValue();
                        continue;
                    }
                    else {
                        other += e.getValue();
                    }
                }
            }
            currentHr = getNextTime(currentHr);
        }
        while (currentHr.before(end));
        
        System.out.println("Trips within marked zones: \t"+ inner);
        System.out.println("Trips from marked zones: \t"+ from);
        System.out.println("Trips to marked zones: \t"+ to);
        System.out.println("Trips out of marked zones: \t"+ other);
        
    }
    

    private void read(String filename, Date start, Date end, Id zone)
    {
        Map<Date,Integer> fromAmount = new TreeMap<Date, Integer>();
        Map<Date,Integer> toAmount = new TreeMap<Date, Integer>();
        Matrices matrices = MatrixUtils.readMatrices(filename);
        Date currentHr = start;
        do {
            Matrix currentMatrix = matrices.getMatrix(filenameformat.format(currentHr));
            double fromValue=0.;
            double toValue=0.;
            try{
            for(Entry e: currentMatrix.getFromLocEntries(zone.toString())){
                fromValue+=e.getValue();
            }
            for(Entry e: currentMatrix.getToLocEntries(zone.toString())){
                toValue+=e.getValue();
            }
            }
            catch (NullPointerException e)
            {
                System.err.println(currentHr + " no Demand?");
            }
            fromAmount.put(currentHr, (int) fromValue);
            toAmount.put(currentHr, (int) toValue);
            currentHr = getNextTime(currentHr);
        }
        while (currentHr.before(end));
        String outfileFrom = fileprefix+zone.toString()+"_from.txt";
        String outfileTo = fileprefix+zone.toString()+"_to.txt";
        write(outfileFrom,fromAmount);
        write(outfileTo,toAmount);
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