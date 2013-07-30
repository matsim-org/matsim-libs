/**
 * 
 */
package org.matsim.contrib.cadyts.general;

import cadyts.calibrators.analytical.AnalyticalCalibrator;

/**
 * @author nagel
 *
 */
public interface CadytsContextI<T> {
	
	AnalyticalCalibrator<T> getCalibrator() ;

	PlansTranslator<T> getPlansTranslator();

}
