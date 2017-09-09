// code by jph
package playground.clruch.traveldata;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.DataFormatException;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.tensor.io.ObjectFormat;

public class TravelDataIO {

    /** Saves travelData as a bitmap file
     * 
     * @param file
     * @param travelData
     * @throws IOException */
    public static void toByte(File file, TravelData travelData) throws IOException {
        travelData.checkConsistency();
        Files.write(file.toPath(), ObjectFormat.of(travelData));
    }

    /** loads travelData from a bitmap file, if possible use {@link TravelDataGet.readDefault()}
     * 
     * @param virtualNetwork
     * @param file
     * @return
     * @throws ClassNotFoundException
     * @throws DataFormatException
     * @throws IOException */
    /* package */ static TravelData fromByte(VirtualNetwork virtualNetwork, File file)//
            throws ClassNotFoundException, DataFormatException, IOException {
        TravelData travelData = ObjectFormat.parse(Files.readAllBytes(file.toPath()));
        travelData.fillSerializationInfo(virtualNetwork);
        travelData.checkConsistency();
        return travelData;
    }
}
