package playground.clruch.netdata;

import java.io.File;

import org.matsim.api.core.v01.network.Network;

public enum VirtualNetworkGet {
    ;

    public static VirtualNetwork readDefault(Network network) {
        final File virtualnetworkFile = new File("virtualNetwork/virtualNetwork");
        System.out.println("" + virtualnetworkFile.getAbsoluteFile());
        try {
            return VirtualNetworkIO.fromByte(network, virtualnetworkFile);
        } catch (Exception e) {
            // e.printStackTrace();
            System.out.println("cannot load default " + virtualnetworkFile);
        }
        return null;
    }

}
