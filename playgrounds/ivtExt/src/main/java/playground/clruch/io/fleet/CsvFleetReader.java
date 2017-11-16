// code by jph
package playground.clruch.io.fleet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;

import com.google.inject.matcher.Matcher;

import ch.ethz.idsc.queuey.datalys.csv.CSVUtils;
import ch.ethz.idsc.queuey.util.FileDelete;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.dispatcher.core.RequestStatus;

public class CsvFleetReader {

    private final DayTaxiRecord dayTaxiRecord;

    public CsvFleetReader(DayTaxiRecord dayTaxiRecord) {
        this.dayTaxiRecord = dayTaxiRecord;
    }

    public DayTaxiRecord populateFrom(List<File> files, ArrayList<String> people, int idcounter)
            throws Exception {
        files.stream().forEach(f -> GlobalAssert.that(f.isFile()));
        int dataline = 0, x = 0;
        dayTaxiRecord.lastTimeStamp = null;
//      int idcounter = 0; // TODO do more elegant with the database!

        for (File file : files) {
            
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                {
                    Scanner id = new Scanner(file.getName());
                    ArrayList<String> nameFile = new ArrayList<String>();   
                    while (id.hasNext()) {
                        id.useDelimiter("_");
                        id.next();
                        nameFile.add(id.next());
                    }
                    id.close(); 
                    String ha= nameFile.toString().replace(".txt]", "").replace("[", "");       
//                  while (!people.get(x).equals(ha))
//                      x++;                    
//                  int realId=Integer.parseInt(number.get(x));
                    System.out.println("INFO name: "+ha);
                    System.out.println("INFO Id: "+idcounter);

                    String line = br.readLine();
                    List<String> list = CSVUtils.csvLineToList(line, " ");
                    int count = 0;
                    System.out.println("CSV HEADER");
                    for (String token : list) {
                        System.out.println(" col " + count + " = " + token);
                        ++count;
                    }
                }
                while (true) {              
                    String line = br.readLine();                    
                    if (Objects.isNull(line))
                        break;
                    List<String> list = CSVUtils.csvLineToList(line, " ");
                    dayTaxiRecord.insert(list, idcounter);
                    
                    String timeStamp = list.get(3);     
                    long unixSeconds = Long.valueOf(timeStamp).longValue();
                    Date date = new Date(unixSeconds*1000L); // *1000 is to convert seconds to milliseconds
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss"); // the format of your date
                    String formattedDate = sdf.format(date);
                    timeStamp = formattedDate;
                    
                    dayTaxiRecord.lastTimeStamp = timeStamp;
                    ++dataline;
                    
                }
            } finally {
                // ...
            }
//          idcounter++;
}

        // Going through all timestamps and check for offservice vehicles && parse requests

        // TODO put both time we did this in one function, no double code
        System.out.println("MAXTIME: " + dayTaxiRecord.getNow(dayTaxiRecord.lastTimeStamp));

        final int MAXTIME = dayTaxiRecord.getNow(dayTaxiRecord.lastTimeStamp);
        final int TIMESTEP = 5;

        System.out.println("INFO Checking for OFFSERVICE & RequestStatus for " + dayTaxiRecord.size() + " vehicles...");
        for (int now = 0; now < MAXTIME; now += TIMESTEP) {
            if (now % 10000 == 0)
                System.out.println("now=" + now);
            for (int vehicleIndex = 0; vehicleIndex < dayTaxiRecord.size(); ++vehicleIndex) {
                TaxiTrail taxiTrail = dayTaxiRecord.get(vehicleIndex);

                // Check and propagate offservice status
                taxiTrail.checkOffService(now);
//                taxiTrail.setRequestStatus(now, RequestStatusParser.parseRequestStatus(now, taxiTrail));

            }
        }

        System.out.println("INFO lines      " + dataline);
        System.out.println("INFO vehicles   " + dayTaxiRecord.size());
        // System.out.println("timestamps " + dayTaxiRecord.keySet().size());
//        System.out.println(dayTaxiRecord.status);
        return dayTaxiRecord;
    }
}

