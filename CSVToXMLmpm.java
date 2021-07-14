package org.matsim.urbanEV;


import com.google.common.collect.Iterables;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargerWriter;
import org.matsim.contrib.ev.infrastructure.ImmutableChargerSpecification;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSVToXMLmpm {

    public String fileName;
    List<ChargerSpecification> chargers = new ArrayList<>();

    public CSVToXMLmpm(String fileName) throws IOException, CsvException {


        try (CSVReader reader = new CSVReader(new FileReader(fileName))) {
            List<String[]> rows = reader.readAll();





            for (String[] row : Iterables.skip(rows, 1)) {
                ImmutableChargerSpecification.Builder builder = ImmutableChargerSpecification.newBuilder();
                if (!(row.length == 1)) {
                    if(Double.parseDouble(row[1]) > 0){
                        chargers.add(builder
                                .linkId(Id.createLinkId(row[0]))
                                .id(Id.create("charger" +row[0], Charger.class))
                                .chargerType("AC")
//                                .plugCount((int) Math.round(Double.parseDouble(row[1])))
                                .plugCount(10)
                                .plugPower(3.7*1000)
                                .build());


                }
                    else if (Double.parseDouble(row[2]) > 0){
                        chargers.add(builder
                                .linkId(Id.createLinkId(row[0]))
                                .id(Id.create("charger" +row[0], Charger.class))
                                .chargerType("AC")
//                                .plugCount((int) Math.round(Double.parseDouble(row[2])))
                                .plugCount(10)
                                .plugPower(11*1000)
                                .build());
                    }
                    else if (Double.parseDouble(row[3]) > 0){
                        chargers.add(builder
                                .linkId(Id.createLinkId(row[0]))
                                .id(Id.create("charger" +row[0], Charger.class))
                                .chargerType("AC")
//                                .plugCount((int) Math.round(Double.parseDouble(row[3])))
                                .plugCount(10)
                                .plugPower(22*1000)
                                .build());
                    }

                    else if (Double.parseDouble(row[4]) > 0){
                        chargers.add(builder
                                .linkId(Id.createLinkId(row[0]))
                                .id(Id.create("charger" + row[0], Charger.class))
                                .chargerType("DC")
//                                .plugCount((int) Math.round(Double.parseDouble(row[4])))
                                .plugCount(10)
                                .plugPower(50*1000)
                                .build());
                    }

                    else if(Double.parseDouble(row[5]) > 0){
                        chargers.add(builder
                                .linkId(Id.createLinkId(row[0]))
                                .id(Id.create("charger" +row[0], Charger.class))
                                .chargerType("DC")
//                                .plugCount((int) Math.round(Double.parseDouble(row[5])))
                                .plugCount(10)
                                .plugPower(150*1000)
                                .build());
                    }


            }


            //rows.forEach(x -> System.out.println(Arrays.toString(x)));

        }
           // System.out.println(chargers);
        }

    }

    public static void main(String[] args) throws IOException, CsvException {
        String fileName = "C:\\Users\\admin\\IdeaProjects\\matsim-berlin\\src\\main\\java\\org\\matsim\\urbanEV\\ind_1004.csv";
        CSVToXMLmpm csvreader = new CSVToXMLmpm(fileName);
        new ChargerWriter(csvreader.chargers.stream()).write( "C:/Users/admin/IdeaProjects/matsim-berlin/scenarios/berlin-v5.5-1pct/input/ev/chargers_mpm_mittlere_Lösung.xml");


        //new CreateNewXML(csvreader.chargers);


    }

}



