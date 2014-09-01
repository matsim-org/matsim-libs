/**
 * 
 */
package playground.southafrica.gauteng.scoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.misc.Time;
import org.matsim.roadpricing.CalcAverageTolledTripLength;
import org.matsim.roadpricing.CalcPaidToll;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingWriterXMLv1;

/**
 * @author nagel
 *
 */
public class GenerationOfMoneyEvents implements StartupListener, AfterMobsimListener, IterationEndsListener, ShutdownListener
{
	private static final Logger log = Logger.getLogger(GenerationOfMoneyEvents.class);

	private final CalcPaidToll calcPaidToll ;
	private final CalcAverageTolledTripLength cattl ;
//	final GautengTollStatistics gautengTollStatistics ;

	private final RoadPricingScheme vehDepScheme;
	
	public GenerationOfMoneyEvents( Network network, Population population, RoadPricingScheme vehDepScheme ) {
		calcPaidToll = new CalcPaidToll(network, vehDepScheme) ;
		cattl = new CalcAverageTolledTripLength(network, vehDepScheme );
//		gautengTollStatistics = new GautengTollStatistics(tollFactor) ;
		this.vehDepScheme = vehDepScheme ;
	}

	@Override
	public void notifyStartup(final StartupEvent event) {
		Controler controler = event.getControler() ;

		// add the events handler to calculate the tolls paid by agents
		controler.getEvents().addHandler(calcPaidToll);

		// analysis:
		controler.getEvents().addHandler(cattl);
//		controler.getEvents().addHandler(gautengTollStatistics) ;

	}

	// send money event at end of iteration:
	@Override
	public void notifyAfterMobsim(final AfterMobsimEvent event) {
		// evaluate the final tolls paid by the agents and add them to their scores
		calcPaidToll.sendMoneyEvents(Time.MIDNIGHT, event.getControler().getEvents());
		// yyyyyy I would, in fact, prefer if agents did this at their arrival!!!!
	}


	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		log.info("The sum of all paid tolls          : " + calcPaidToll.getAllAgentsToll() + " monetary units.");
		log.info("The number of people who paid toll : " + calcPaidToll.getDraweesNr());
		log.info("The average paid trip length       : " + cattl.getAverageTripLength() + " m.");

//		int iteration = event.getIteration() ;
//		gautengTollStatistics.printTollInfo(event.getControler().getControlerIO().getIterationFilename(iteration, "")) ;
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		String filename = event.getControler().getControlerIO().getOutputFilename("output_toll.xml.gz") ;
		new RoadPricingWriterXMLv1(this.vehDepScheme).writeFile(filename);
	}

}
