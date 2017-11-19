package playground.lsieber.scenario.reducer;

import java.io.IOException;
import java.net.MalformedURLException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.facilities.ActivityFacilities;

public class ModeConverterImpl extends AbstractScenarioReducer{

    public ModeConverterImpl() throws IOException {
        // TODO Auto-generated constructor stub
        super();
    }
    
    public void ModePtToAv () {
        this.ConvertPtToAV();
    }

    @Override
    protected Network networkCutter() throws MalformedURLException, IOException {
        // TODO Auto-generated method stub
        return network;
    }

    @Override
    protected Population populationCutter() {
        // TODO Auto-generated method stub
        return population;
    }

    @Override
    protected ActivityFacilities facilitiesCutter() {
        // TODO Auto-generated method stub
        return facilities;
    }

}
