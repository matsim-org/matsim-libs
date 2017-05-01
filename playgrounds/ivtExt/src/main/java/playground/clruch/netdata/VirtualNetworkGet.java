package playground.clruch.netdata;

import java.io.File;
import java.io.IOException;
import java.util.zip.DataFormatException;

import org.matsim.api.core.v01.network.Network;

public enum VirtualNetworkGet {
    ;

    public static VirtualNetwork readDefault(Network network) {
        final File virtualnetworkFile = new File("virtualNetwork/virtualNetwork");
        System.out.println("" + virtualnetworkFile.getAbsoluteFile());
        try {
            return VirtualNetworkIO.fromByte(network, virtualnetworkFile);
        } catch (ClassNotFoundException | DataFormatException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

}
