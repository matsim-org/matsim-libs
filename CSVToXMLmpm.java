package org.matsim.urbanEV;


import com.google.common.collect.Iterables;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
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
    private static final Logger log = Logger.getLogger(CSVToXMLmpm.class);
    List<String> usedChargers = new ArrayList<>();

    public CSVToXMLmpm(String fileName, String fileName2) throws IOException, CsvException {
        try (CSVReader reader2 = new CSVReader(new FileReader(fileName2))) {
            List<String[]> rows2 = reader2.readAll();
            for (String[] row : Iterables.skip(rows2, 1)) {
                usedChargers.add(row[0]);
            }
        }


        try (CSVReader reader = new CSVReader(new FileReader(fileName))) {
            List<String[]> rows = reader.readAll();


            for (String[] row : Iterables.skip(rows, 1)) {
                Id<Link> linkId = Id.createLinkId(row[0]);
//                if (!linkId.toString().equals("")) {
//                    ImmutableChargerSpecification.Builder builder = ImmutableChargerSpecification.newBuilder();
//                    chargers.add(builder.linkId(linkId).plugPower(22 * 1000).plugCount(20).chargerType("AC").id(Id.create("charger" + linkId, Charger.class)).build());
//                }

                ImmutableChargerSpecification.Builder builder = ImmutableChargerSpecification.newBuilder();
                if (!(row.length == 1)) {
                    String chargerId = "charger" + row[0];
                    if (usedChargers.contains(chargerId)) {
                        chargers.add(builder.linkId(Id.createLinkId(row[0])).id(Id.create(chargerId, Charger.class)).chargerType("AC").plugCount(50).plugPower(22 * 1000).build());
                    }
//                        if (Double.parseDouble(row[1]) > 0) {
//
//                                chargers.add(builder
//                                        .linkId(Id.createLinkId(row[0]))
//                                        .id(Id.create("charger" + row[0], Charger.class))
//                                        .chargerType("AC")
//                                        //                                .plugCount((int) Math.round(Double.parseDouble(row[1])))
//                                        .plugCount(10)
//                                        .plugPower(22 * 1000)
//                                        .build());
//
//
//                        } else if (Double.parseDouble(row[2]) > 0) {
//
//                                chargers.add(builder
//                                        .linkId(Id.createLinkId(row[0]))
//                                        .id(Id.create("charger" + row[0], Charger.class))
//                                        .chargerType("AC")
//                                        //                                .plugCount((int) Math.round(Double.parseDouble(row[2])))
//                                        .plugCount(10)
//                                        .plugPower(22 * 1000)
//                                        .build());
//
//                        } else if (Double.parseDouble(row[3]) > 0) {
//                                    chargers.add(builder
//                                            .linkId(Id.createLinkId(row[0]))
//                                            .id(Id.create("charger" + row[0], Charger.class))
//                                            .chargerType("AC")
//                                            //                                .plugCount((int) Math.round(Double.parseDouble(row[3])))
//                                            .plugCount(10)
//                                            .plugPower(22 * 1000)
//                                            .build());
//
//                        } else if (Double.parseDouble(row[4]) > 0) {
//
//                                        chargers.add(builder
//                                                .linkId(Id.createLinkId(row[0]))
//                                                .id(Id.create("charger" + row[0], Charger.class))
//                                                .chargerType("DC")
//                                                //                                .plugCount((int) Math.round(Double.parseDouble(row[4])))
//                                                .plugCount(10)
//                                                .plugPower(50 * 1000)
//                                                .build());
//
//                        } else if (Double.parseDouble(row[5]) > 0) {
//
//                                            chargers.add(builder
//                                                    .linkId(Id.createLinkId(row[0]))
//                                                    .id(Id.create("charger" + row[0], Charger.class))
//                                                    .chargerType("DC")
//                                                    //                                .plugCount((int) Math.round(Double.parseDouble(row[5])))
//                                                    .plugCount(10)
//                                                    .plugPower(150 * 1000)
//                                                    .build());
//
//                                    }
//
//
//                    }


                    //rows.forEach(x -> System.out.println(Arrays.toString(x)));

                }
                // System.out.println(chargers);
            }

        }

//        public static void main (String[]args) throws IOException, CsvException {
//            String fileNameE = "C:\\Users\\admin\\IdeaProjects\\matsim-berlin\\src\\main\\java\\org\\matsim\\urbanEV\\ind_9619.csv";
//            String fileNameE2 = "C:/Users/admin/IdeaProjects/matsim-berlin/scenarios/berlin-v5.5-1pct/input/ev/Used_3.csv";
//            CSVToXMLmpm csvreader = new CSVToXMLmpm(fileNameE, fileNameE2);
//            new ChargerWriter(csvreader.chargers.stream()).write("C:/Users/admin/IdeaProjects/matsim-berlin/scenarios/berlin-v5.5-1pct/input/ev/Szenario3_refined_all.xml");
//
//
//            //new CreateNewXML(csvreader.chargers);
//
//
//        }

    }
}



