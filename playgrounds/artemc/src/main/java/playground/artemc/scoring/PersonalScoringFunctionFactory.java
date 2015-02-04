package playground.artemc.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.core.scoring.ScoringFunction;

import java.util.HashMap;

/**
 * Created by artemc on 3/2/15.
 */
public interface PersonalScoringFunctionFactory {

	public HashMap<Id, ScoringFunction> getPersonScoringFunctions();
}
