/**
 * 
 */
package playground.tschlenther.analysis.modules.taxiTrips;

import java.util.List;

import org.matsim.core.events.handler.EventHandler;

import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * @author Tilmann Schlenther
 *
 */
public class TaxiTripsAnalyzer extends AbstractAnalysisModule {

	/**
	 * @param name
	 */
	public TaxiTripsAnalyzer() {
		super(TaxiTripsAnalyzer.class.getSimpleName());
	}

	/* (non-Javadoc)
	 * @see playground.vsp.analysis.modules.AbstractAnalysisModule#getEventHandler()
	 */
	@Override
	public List<EventHandler> getEventHandler() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see playground.vsp.analysis.modules.AbstractAnalysisModule#preProcessData()
	 */
	@Override
	public void preProcessData() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see playground.vsp.analysis.modules.AbstractAnalysisModule#postProcessData()
	 */
	@Override
	public void postProcessData() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see playground.vsp.analysis.modules.AbstractAnalysisModule#writeResults(java.lang.String)
	 */
	@Override
	public void writeResults(String outputFolder) {
		// TODO Auto-generated method stub

	}

}
