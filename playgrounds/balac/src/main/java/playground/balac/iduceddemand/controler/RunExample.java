package playground.balac.iduceddemand.controler;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.socnetsim.utils.QuadTreeRebuilder;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacility;

import com.google.inject.name.Names;

import playground.balac.iduceddemand.strategies.InsertRandomActivityStrategy;
import playground.balac.iduceddemand.strategies.RandomActivitiesSwaperStrategy;

public class RunExample {

	public static void main(String[] args) {

		final Config config = ConfigUtils.loadConfig(args[0]);

		final Scenario sc = ScenarioUtils.loadScenario(config);

		final Controler controler = new Controler( sc );
	
		final QuadTreeRebuilder<ActivityFacility> shopFacilitiesQuadTree = new QuadTreeRebuilder<ActivityFacility>();
		
		for(ActivityFacility af : sc.getActivityFacilities().getFacilitiesForActivityType("shop").values()) {
			
			shopFacilitiesQuadTree.put(af.getCoord(), af);
		}
		
		final QuadTreeRebuilder<ActivityFacility> leisureFacilitiesQuadTree = new QuadTreeRebuilder<ActivityFacility>();
		
		for(ActivityFacility af : sc.getActivityFacilities().getFacilitiesForActivityType("leisure").values()) {
			
			leisureFacilitiesQuadTree.put(af.getCoord(), af);
		}
		
		QuadTree<ActivityFacility> shoping = shopFacilitiesQuadTree.getQuadTree();		
		
		QuadTree<ActivityFacility> leisure = leisureFacilitiesQuadTree.getQuadTree();		

		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				bind(QuadTree.class)
				.annotatedWith(Names.named("shopQuadTree"))
				.toInstance(shoping);
				
				bind(QuadTree.class)
				.annotatedWith(Names.named("leisureQuadTree"))
				.toInstance(leisure);
			}
			
		});		
		
		controler.addOverridingModule( new AbstractModule() {
			@Override
			public void install() {
				this.addPlanStrategyBinding("InsertRandomActivityStrategy").to( InsertRandomActivityStrategy.class ) ;

				this.addPlanStrategyBinding("RandomActivitiesSwaperStrategy").to( RandomActivitiesSwaperStrategy.class ) ;
			}
		});		
		controler.run();
		
		
	}

}
