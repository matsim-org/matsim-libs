/**
 * 
 */
package playground.tschlenther.analysis.modules.taxiTrips;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.handler.EventHandler;

import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * @author Tilmann Schlenther
 *
 */
public class TaxiTripsAnalyzer extends AbstractAnalysisModule {

	private final static Logger log = Logger.getLogger(TaxiTripsAnalyzer.class);
	
	TaxiCustomerWaitHandler customerHandler;
	TaxiOperatorStatsHandler operatorHandler;
	
	/**
	 * @param name
	 */
	public TaxiTripsAnalyzer(Network network) {
		super(TaxiTripsAnalyzer.class.getSimpleName());
		customerHandler = new TaxiCustomerWaitHandler();
		operatorHandler = new TaxiOperatorStatsHandler(network);
	}

	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> l = new ArrayList<EventHandler>();
		l.add(customerHandler);
		l.add(operatorHandler);
		return l;
	}

	@Override
	public void preProcessData() {
		//nothing to do..
	}

	@Override
	public void postProcessData() {
		// nothing to do..
	}

	@Override
	public void writeResults(String outputFolder) {
		String actualOutput = outputFolder + "/customerStats";
		log.info("writing taxi customer stats to " + actualOutput);
		this.customerHandler.writeCustomerStats(actualOutput);
		
		actualOutput = outputFolder + "/V2operatorStats";
		log.info("writing V2operator stats to " + actualOutput);
		this.operatorHandler.writeTravelDistanceStatsToFiles(actualOutput);
		
		log.info("TaxiTripsAnalyzer finished writing output to " + outputFolder);

	}

}
