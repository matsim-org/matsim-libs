package playground.clruch.net;

import java.io.Serializable;

public class RequestContainer implements Serializable {
    public String requestId; // potentially unnecessary!?
    public int fromLinkId; // where the person is now
    public double submissionTime;
    public int toLinkId; // where the person wants to go
}
