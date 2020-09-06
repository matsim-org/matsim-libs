package lsp.controler;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import lsp.LSPs;
import org.matsim.contrib.freight.controler.CarrierAgentTracker;
import org.matsim.contrib.freight.events.eventsCreator.LSPEventCreator;
import lsp.replanning.LSPReplanningModule;
import lsp.scoring.LSPScoringModule;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.core.controler.AbstractModule;

import java.util.Collection;


public class LSPModule extends AbstractModule {

	
	private final LSPs lsps;
	private final LSPReplanningModule replanningModule;
	private final LSPScoringModule scoringModule;
	private final Collection<LSPEventCreator> creators;
	
	private final FreightConfigGroup carrierConfig = new FreightConfigGroup();
	
	public LSPModule(LSPs  lsps, LSPReplanningModule replanningModule, LSPScoringModule scoringModule, Collection<LSPEventCreator> creators) {
	   this.lsps = lsps;
	   this.replanningModule = replanningModule;
	   this.scoringModule = scoringModule;
	   this.creators = creators;
	}    
	   	  
		    
	@Override
	public void install() {
		bind(FreightConfigGroup.class).toInstance(carrierConfig);
		bind(LSPs.class).toInstance(lsps);
		if(replanningModule != null) {
			bind(LSPReplanningModule.class).toInstance(replanningModule);
		}
		if(scoringModule != null) {
			 bind(LSPScoringModule.class).toInstance(scoringModule);
		}
		
		bind( LSPControlerListenerImpl.class ).in( Singleton.class );
		addControlerListenerBinding().to( LSPControlerListenerImpl.class );
		bindMobsim().toProvider( LSPQSimFactory.class );
	}

	@Provides
	Collection<LSPEventCreator> provideEventCreators(){
		return this.creators;
	}
	
	@Provides
	CarrierAgentTracker provideCarrierResourceTracker( LSPControlerListenerImpl lSPControlerListener ) {
        return lSPControlerListener.getCarrierResourceTracker();
    }

}
