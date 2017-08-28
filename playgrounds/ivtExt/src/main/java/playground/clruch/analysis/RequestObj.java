/**
 * 
 */
package playground.clruch.analysis;

import org.matsim.api.core.v01.network.Link;

/**
 * @author Claudio Ruch
 *
 */
public class RequestObj {
    public double submissionTime;
    public Link fromLink;
    public Link toLink;

    public RequestObj(double submissionTime, Link fromLink, Link toLink) {
        this.submissionTime = submissionTime;
        this.fromLink = fromLink;
        this.toLink = toLink;
    }

}
