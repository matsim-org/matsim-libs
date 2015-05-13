/**
 * 
 */
package playground.southafrica.projects.digicore.aggregation;

import org.apache.log4j.Logger;

/**
 * Basic interface to aggregate the complete data set, typically using 5Hz
 * acceleration and speed data, to a coarser time scale.
 * 
 * @author jwjoubert
 */
public interface DigiAggregator {
	final static Logger LOG = Logger.getLogger(DigiAggregator.class);
	
	public void aggregate(String input, String output);

}
