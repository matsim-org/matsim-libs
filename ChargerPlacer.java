package org.matsim.urbanEV;

import com.google.common.collect.Iterables;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargerWriter;
import org.matsim.contrib.ev.infrastructure.ImmutableChargerSpecification;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChargerPlacer {



            // System.out.println(chargers);




    public static void main(String[] args)  throws IOException, CsvException {


        List<ChargerSpecification> chargers = new ArrayList<>();


        Network network = NetworkUtils.createNetwork();

        new MatsimNetworkReader(network).readFile("C:/Users/admin/IdeaProjects/matsim-berlin/scenarios/berlin-v5.5-1pct/input/ev/berlin-v5.5-network.xml");

        // iterate through all links
        for (Link l : network.getLinks().values()){

            if (l.getAllowedModes().contains("car")) {
//            ShpGeometryUtils.i
                ImmutableChargerSpecification.Builder builder = ImmutableChargerSpecification.newBuilder();
                chargers.add(builder
                        .linkId(l.getId())
                        .id(Id.create("charger" + l.getId().toString(), Charger.class))
                        .chargerType("50kW")
                        .plugCount(10)
                        .plugPower(50000)
                        .build());
            }
       }

        new ChargerWriter(chargers.stream()).write( "C:/Users/admin/IdeaProjects/matsim-berlin/scenarios/berlin-v5.5-1pct/input/ev/AllChargersBerlin.xml");





    }
}
