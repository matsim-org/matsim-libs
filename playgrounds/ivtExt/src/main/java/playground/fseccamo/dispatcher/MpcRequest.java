package playground.fseccamo.dispatcher;

import playground.clruch.netdata.VirtualLink;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

public class MpcRequest {
    final AVRequest avRequest;
    final VirtualLink virtualLink;

    public MpcRequest(AVRequest avRequest, VirtualLink virtualLink) {
        this.avRequest = avRequest;
        this.virtualLink = virtualLink;
    }

}
