package demand.controler;

import java.util.Collection;

import org.matsim.contrib.freight.CarrierConfig;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;

import demand.decoratedLSP.LSPsWithOffers;
import demand.demandObject.DemandObject;
import demand.demandObject.DemandObjects;
import demand.mutualReplanning.MutualReplanningModule;
import demand.scoring.DemandScoringModule;
import lsp.mobsim.CarrierResourceTracker;
import lsp.mobsim.FreightQSimFactory;
import lsp.scoring.LSPScoringModule;

public class MutualModule extends AbstractModule{

	private LSPsWithOffers lsps;
	private DemandObjects demandObjects;
	private DemandScoringModule demandScoringModule;
	private LSPScoringModule lspScoringModule;
	private MutualReplanningModule replanningModule;
	
	private CarrierConfig carrierConfig = new CarrierConfig();
	
	
	
	public static class Builder{
		
		private LSPsWithOffers lsps;
		private DemandObjects demandObjects;
		private DemandScoringModule demandScoringModule;
		private LSPScoringModule lspScoringModule;
		private MutualReplanningModule replanningModule;
		
		public Builder newInstance() {
			return new Builder();
		}
		
		public Builder setLsps(LSPsWithOffers lsps) {
			this.lsps = lsps;
			return this;
		}
	
		public Builder setDemandScoringModule(DemandScoringModule demandScoringModule) {
			this.demandScoringModule = demandScoringModule;
			return this;
		}
		
		public Builder setLSPScoringModule(LSPScoringModule lspScoringModule) {
			this.lspScoringModule = lspScoringModule;
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
		
		public MutualModule build() {
			return new MutualModule(this);
		}
	}
	
	private MutualModule(Builder builder) {
		this.lsps = builder.lsps;
		this.demandObjects = builder.demandObjects;
		this.demandScoringModule = builder.demandScoringModule;
		this.lspScoringModule = builder.lspScoringModule;
		this.replanningModule = builder.replanningModule;
	}
	
	
	@Override
	public void install() {
			bind(CarrierConfig.class).toInstance(carrierConfig);
			bind(LSPsWithOffers.class).toInstance(lsps);
			bind(DemandObjects.class).toInstance(demandObjects);
			
			if(replanningModule != null) {
	        	bind(MutualReplanningModule.class).toInstance(replanningModule);
	        }
			if(demandScoringModule != null) {
				 bind(DemandScoringModule.class).toInstance(demandScoringModule);
			}
			if(lspScoringModule != null) {
				 bind(LSPScoringModule.class).toInstance(lspScoringModule);
			}
	        bind(MutualControlerListener.class).asEagerSingleton();
	        addControlerListenerBinding().to(MutualControlerListener.class);
	        bindMobsim().toProvider(FreightQSimFactory.class);
		
	}

	
	@Provides
    CarrierResourceTracker provideCarrierResourceTracker(MutualControlerListener mutualControlerListener) {
        return mutualControlerListener.getCarrierResourceTracker();
    }

    public void setPhysicallyEnforceTimeWindowBeginnings(boolean physicallyEnforceTimeWindowBeginnings) {
        this.carrierConfig.setPhysicallyEnforceTimeWindowBeginnings(physicallyEnforceTimeWindowBeginnings);
    }
	
}
