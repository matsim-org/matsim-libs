package org.matsim.codeexamples;

import org.matsim.api.core.v01.Scenario;
import org.matsim.codeexamples.mobsim.ownMobsimAgentUsingRouter.RunOwnMobsimAgentUsingRouterExample;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import java.net.URL;

class RunAbcExample{

    private Config config = null ;
    private Scenario scenario = null ;
    private Controler controler = null ;

    public static void main ( String [] args ) {
        new RunAbcExample().run() ;
    }

    Config prepareConfig() {
        URL url = ExamplesUtils.getTestScenarioURL( "equil" ) ;
        URL configUrl = IOUtils.newUrl( url, "config.xml" );;
        config = ConfigUtils.loadConfig( configUrl ) ;
        return config ;
    }

    Scenario prepareScenario() {
        if ( config==null ) {
            prepareConfig() ;
        }
        scenario = ScenarioUtils.loadScenario( config ) ;
        return scenario ;
    }

    Controler prepareControler() {
        if ( scenario==null ) {
            prepareScenario() ;
        }
        controler = new Controler( scenario ) ;
        return controler ;
    }

    void run() {
        if ( controler==null ) {
            prepareControler() ;
        }
        controler.run();
    }

}
