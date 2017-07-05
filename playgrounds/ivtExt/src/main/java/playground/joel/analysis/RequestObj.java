/**
 * 
 */
package playground.joel.analysis;

import org.matsim.api.core.v01.network.Link;

/**
 * @author Claudio Ruch
 *
 */
public class RequestObj {
    public double submissionTime;
    public Link fromLink;
    public Link toLink;

    public RequestObj(double submissionTimIn, Link fromLinkIn, Link toLinkIn) {
        submissionTime = submissionTimIn;
        fromLink = fromLinkIn;
        toLink = toLinkIn;
    }

}
