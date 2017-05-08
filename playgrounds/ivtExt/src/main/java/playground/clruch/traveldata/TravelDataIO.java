package playground.clruch.traveldata;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.DataFormatException;

import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.tensor.io.ObjectFormat;
import playground.clruch.netdata.VirtualNetwork;

public class TravelDataIO {

    /**
     * Saves virtualNetwork as a bitmap file
     * 
     * @param file
     * @param virtualNetwork
     * @throws IOException
     */
    public static void toByte(File file, TravelData travelData) throws IOException {
        travelData.checkConsistency();
        Files.write(file.toPath(), ObjectFormat.of(travelData));
    }

    /**
     * loads virtualNetwork from a bitmap file
     * 
     * @param network
     * @param file
     * @return
     * @throws ClassNotFoundException
     * @throws DataFormatException
     * @throws IOException
     */
    public static TravelData fromByte(Network network, VirtualNetwork virtualNetwork, File file) throws ClassNotFoundException, DataFormatException, IOException {
        TravelData travelData = ObjectFormat.parse(Files.readAllBytes(file.toPath()));
        travelData.fillSerializationInfo(virtualNetwork);
        travelData.checkConsistency();
        return travelData;
    }
}
