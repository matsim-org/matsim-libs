package playground.matsim_decoupling;

import org.matsim.core.controler.AbstractModule;

import ch.ethz.matsim.av.framework.AVUtils;
import playground.clruch.dispatcher.DriveByDispatcher;
import playground.clruch.dispatcher.EdgyDispatcher;
import playground.clruch.dispatcher.GlobalBipartiteMatchingDispatcher;
import playground.clruch.dispatcher.KMedianDispatcher;
import playground.clruch.dispatcher.LPFBDispatcher;
import playground.clruch.dispatcher.LPFFDispatcher;
import playground.clruch.dispatcher.NewSingleHeuristicDispatcher;
import playground.clruch.dispatcher.TestBedDispatcher;
import playground.clruch.dispatcher.UncoordinatedDispatcher;
import playground.clruch.dispatcher.selfishdispatcher.SelfishDispatcher;
import playground.fseccamo.dispatcher.MPCDispatcher;

public class IDSCDispatcherModule extends AbstractModule {
	@Override
	public void install() {
        /** dispatchers for UniversalDispatcher */
        bind(DriveByDispatcher.Factory.class);
        AVUtils.bindDispatcherFactory(binder(), DriveByDispatcher.class.getSimpleName()).to(DriveByDispatcher.Factory.class);

        bind(EdgyDispatcher.Factory.class);
        AVUtils.bindDispatcherFactory(binder(), EdgyDispatcher.class.getSimpleName()).to(EdgyDispatcher.Factory.class);

        bind(UncoordinatedDispatcher.Factory.class);
        AVUtils.bindDispatcherFactory(binder(), UncoordinatedDispatcher.class.getSimpleName()).to(UncoordinatedDispatcher.Factory.class);

        bind(GlobalBipartiteMatchingDispatcher.Factory.class);
        AVUtils.bindDispatcherFactory(binder(), GlobalBipartiteMatchingDispatcher.class.getSimpleName()).to(GlobalBipartiteMatchingDispatcher.Factory.class);

        bind(SelfishDispatcher.Factory.class);
        AVUtils.bindDispatcherFactory(binder(), SelfishDispatcher.class.getSimpleName()).to(SelfishDispatcher.Factory.class);

        bind(NewSingleHeuristicDispatcher.Factory.class);
        AVUtils.bindDispatcherFactory(binder(), NewSingleHeuristicDispatcher.class.getSimpleName()).to(NewSingleHeuristicDispatcher.Factory.class);

        bind(TestBedDispatcher.Factory.class);
        AVUtils.bindDispatcherFactory(binder(), TestBedDispatcher.class.getSimpleName()).to(TestBedDispatcher.Factory.class);

        // bind(SPLICEDispatcher.Factory.class);
        //        AVUtils.bindDispatcherFactory(binder(), SPLICEDispatcher.class.getSimpleName()).to(SPLICEDispatcher.Factory.class);

        bind(KMedianDispatcher.Factory.class);
        AVUtils.bindDispatcherFactory(binder(), KMedianDispatcher.class.getSimpleName()).to(KMedianDispatcher.Factory.class);

        
        
        
        

        
        
        
        // bind(PolyMultiGBMDispatcher.Factory.class);
        // AVUtils.bindDispatcherFactory(binder(), PolyMultiGBMDispatcher.class.getSimpleName()).to(PolyMultiGBMDispatcher.Factory.class);

        /** dispatchers for PartitionedDispatcher */
        // //bind(ConsensusDispatcherDFR.Factory.class);
        // //AVUtils.bindDispatcherFactory(binder(), ConsensusDispatcherDFR.class.getSimpleName()).to(ConsensusDispatcherDFR.Factory.class);

        bind(LPFBDispatcher.Factory.class);
        AVUtils.bindDispatcherFactory(binder(), LPFBDispatcher.class.getSimpleName()).to(LPFBDispatcher.Factory.class);

        bind(LPFFDispatcher.Factory.class);
        AVUtils.bindDispatcherFactory(binder(), LPFFDispatcher.class.getSimpleName()).to(LPFFDispatcher.Factory.class);

        // bind(DFRDispatcher.Factory.class);
        // AVUtils.bindDispatcherFactory(binder(), DFRDispatcher.class.getSimpleName()).to(DFRDispatcher.Factory.class);

        bind(MPCDispatcher.Factory.class);
        AVUtils.bindDispatcherFactory(binder(), MPCDispatcher.class.getSimpleName()).to(MPCDispatcher.Factory.class);

	}
}
