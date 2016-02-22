package playground.michalm.poznan.zone;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.zone.Zone;
import org.matsim.contrib.zone.io.ZoneXmlWriter;

import playground.michalm.util.visum.VisumMatrixReader;


public class PoznanPtapZoneCreator
{
    public static void main(String[] args)
    {
        String dir = "d:/GoogleDrive/Poznan/";
        String zonesXmlFile = dir + "Matsim_2015_02/zones.xml";
        String matrixFile = dir + "Visum_2014/demand/804 Suma KI poj.mtx";

        int[] ids = VisumMatrixReader.readIds(matrixFile);
        Map<Id<Zone>, Zone> zones = new LinkedHashMap<>();

        for (int i = 0; i < ids.length; i++) {
            Zone zone = new Zone(Id.create(ids[i], Zone.class), "");
            zones.put(zone.getId(), zone);
        }

        new ZoneXmlWriter(zones).write(zonesXmlFile);
    }
}
