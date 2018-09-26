package org.matsim.contrib.emissions.roadTypeMapping;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.utils.EmissionUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by molloyj on 01.12.2017.
 * class to mimic the old org.matsim.contrib.emissions.roadTypeMapping that berlin uses with VISUM
 */
public class VisumHbefaRoadTypeMapping extends HbefaRoadTypeMapping {
    private static Logger logger = Logger.getLogger(VisumHbefaRoadTypeMapping.class);

    Map<String, String> mapping = new HashMap<>();

    private VisumHbefaRoadTypeMapping(){}

    @Override
    public String determineHebfaType(Link link) {
        String roadType = NetworkUtils.getType(link);
        return mapping.get(roadType);
    }

    public void put(String visumRtNr, String hbefaRtName) {
        mapping.put(visumRtNr, hbefaRtName);
    }

    public static VisumHbefaRoadTypeMapping emptyMapping() {
        return new VisumHbefaRoadTypeMapping();
    }

    public static HbefaRoadTypeMapping createVisumRoadTypeMapping(String filename){
        logger.info("entering createRoadTypeMapping ...") ;

        VisumHbefaRoadTypeMapping mapping = new VisumHbefaRoadTypeMapping();
        try{
            BufferedReader br = IOUtils.getBufferedReader(filename);
            String strLine = br.readLine();
            Map<String, Integer> indexFromKey = EmissionUtils.createIndexFromKey(strLine);

            while ((strLine = br.readLine()) != null){
                if ( strLine.contains("\"")) throw new RuntimeException("cannot handle this character in parsing") ;

                String[] inputArray = strLine.split(";");
                String visumRtNr = inputArray[indexFromKey.get("VISUM_RT_NR")];
                String hbefaRtName = (inputArray[indexFromKey.get("HBEFA_RT_NAME")]);

                mapping.put(visumRtNr, hbefaRtName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("leaving createRoadTypeMapping ...") ;
        return mapping;
    }
}
