package playground.anhorni.locationchoice;

import org.matsim.controler.Controler;
import playground.anhorni.locationchoice.facilityLoad.FacilitiesLoadCalculator;
import playground.anhorni.locationchoice.scoring.LocationChoiceScoringFunctionFactory;

public class LCControler extends Controler {
	
	public LCControler(final String[] args) {
		super(args);
	}

    @Override
    protected void setup() {
      super.setup();
      this.scoringFunctionFactory = new LocationChoiceScoringFunctionFactory();
    }
 
    public static void main (final String[] args) {
      LCControler controler = new LCControler(args);
      controler.addControlerListener(new FacilitiesLoadCalculator());
      controler.run();
    }

}
