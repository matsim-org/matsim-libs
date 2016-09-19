package besttimeresponseintegration;

import org.matsim.core.controler.AbstractModule;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ExperiencedScoreAnalyzerModule extends AbstractModule {
	@Override
	public void install() {
		addControlerListenerBinding().to(ExperiencedScoreAnalyzer.class);
		// for direct access to ExperiencedScoreAnalyzer:
		bind(ExperiencedScoreAnalyzer.class);
	}
}
