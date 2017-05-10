package playground.fseccamo.dispatcher;

import playground.clruch.netdata.VirtualLink;
import playground.clruch.netdata.VirtualNode;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

public class MpcRequest {
    final AVRequest avRequest;
    final int vectorIndex; // up to m refers to virtual link, beyond m refers to virtual node (as self loop)

    public MpcRequest(AVRequest avRequest, VirtualLink virtualLink) {
        this.avRequest = avRequest;
        vectorIndex = virtualLink.index;
    }

    public MpcRequest(AVRequest avRequest, int m, VirtualNode virtualNode) {
        this.avRequest = avRequest;
        vectorIndex = m + virtualNode.index;
    }
}
