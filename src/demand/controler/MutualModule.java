package demand.controler;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.contrib.freight.CarrierConfig;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provides;

import demand.decoratedLSP.LSPDecorators;
import demand.demandObject.DemandObjects;
import demand.mutualReplanning.MutualReplanningModule;
import demand.scoring.MutualScoringModule;
import lsp.events.EventCreator;
import lsp.mobsim.CarrierResourceTracker;
import lsp.mobsim.FreightQSimFactory;
import lsp.scoring.LSPScoringModule;

public class MutualModule extends AbstractModule{

	private LSPDecorators lsps;
	private DemandObjects demandObjects;
	private MutualScoringModule mutualScoringModule;
	private MutualReplanningModule replanningModule;	
	private CarrierConfig carrierConfig = new CarrierConfig();
	private Collection<EventCreator> creators;
	
	public static class Builder{
		
		private LSPDecorators lsps;
		private DemandObjects demandObjects;
		private MutualScoringModule mutualScoringModule;
		private MutualReplanningModule replanningModule;
		private Collection<EventCreator> creators;
		
		public static Builder newInstance() {
			return new Builder();
		}
		
		public Builder setLsps(LSPDecorators lsps) {
			this.lsps = lsps;
			return this;
		}
	
		public Builder setMutualScoringModule(MutualScoringModule demandScoringModule) {
			this.mutualScoringModule = demandScoringModule;
			return this;
		}
				
		public Builder setMutualReplanningModule(MutualReplanningModule replanningModule) {
			this.replanningModule = replanningModule;
			return this;
		}
		
		public Builder setDemandObjects(DemandObjects demandObjects) {
			this.demandObjects = demandObjects;
			return this;
		}
		
		public Builder setEventCreators(Collection<EventCreator> creators) {
			this.creators = creators;
			return this;
		}
		
		public MutualModule build() {
			return new MutualModule(this);
		}
	}
	
	private MutualModule(Builder builder) {
		this.lsps = builder.lsps;
		this.demandObjects = builder.demandObjects;
		this.mutualScoringModule = builder.mutualScoringModule;
		this.replanningModule = builder.replanningModule;
		this.creators = builder.creators; 
	}
	
	
	@Override
	public void install() {
			bind(CarrierConfig.class).toInstance(carrierConfig);
			bind(LSPDecorators.class).toInstance(lsps);
			bind(DemandObjects.class).toInstance(demandObjects);
			
			if(replanningModule != null) {
	        	bind(MutualReplanningModule.class).toInstance(replanningModule);
	        }
			if(mutualScoringModule != null) {
				 bind(MutualScoringModule.class).toInstance(mutualScoringModule);
			}
			bind(MutualControlerListener.class).asEagerSingleton();
	        addControlerListenerBinding().to(MutualControlerListener.class);
	        bindMobsim().toProvider(FreightQSimFactory.class);
		
	}

	@Provides
	Collection<EventCreator> provideEventCreators(){
		return this.creators;
	}
	
	@Provides
    CarrierResourceTracker provideCarrierResourceTracker(MutualControlerListener mutualControlerListener) {
        return mutualControlerListener.getCarrierResourceTracker();
    }

    public void setPhysicallyEnforceTimeWindowBeginnings(boolean physicallyEnforceTimeWindowBeginnings) {
        this.carrierConfig.setPhysicallyEnforceTimeWindowBeginnings(physicallyEnforceTimeWindowBeginnings);
    }
	
}
