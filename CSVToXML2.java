package org.matsim.urbanEV;


import com.google.common.collect.Iterables;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargerWriter;
import org.matsim.contrib.ev.infrastructure.ImmutableChargerSpecification;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.run.drt.BerlinShpUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import static java.lang.Double.parseDouble;

public class CSVToXML2 {

    private static final String COMMA_DELIMITER = ";";
    public String fileName;
    List<ChargerSpecification> chargers = new ArrayList<>();





    public CSVToXML2(String fileName, Network network) throws IOException, CsvException {

        List<List<String>> records = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(COMMA_DELIMITER);
                records.add(Arrays.asList(values));
            }


            }
            for (List<String> record : Iterables.skip(records, 2)) {

//                URL url = new URL("https://download.geofabrik.de/europe/germany/berlin-latest-free.shp.zip");
//                List<Geometry> berlinSHP = ShpGeometryUtils.loadGeometries(url);

                double x = Double.parseDouble(record.get(0));
                double y = Double.parseDouble(record.get(1));
                Coord coord1 = new Coord(x,y);






               Link chargerLink = NetworkUtils.getNearestLink(network,coord1);




                ImmutableChargerSpecification.Builder builder = ImmutableChargerSpecification.newBuilder();
                chargers.add(builder
                        .linkId(Id.createLinkId(chargerLink.getId()))
                        .id(Id.create("charger" + chargerLink.getId(), Charger.class))
                        .chargerType(record.get(4))
                        .plugCount((int) Math.round(parseDouble(record.get(2))))
                        .plugPower((int) Math.round(parseDouble(record.get(4))*1000))
                        .build());
        }






//








        }

        }











