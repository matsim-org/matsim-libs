package playground.andreas.intersection.tl;

import java.util.ArrayList;
import java.util.List;

import org.matsim.trafficlights.control.SignalSystemControler;
import org.matsim.trafficlights.data.PlanbasedSignalSystemControlInfoImpl;
import org.matsim.trafficlights.data.SignalGroupSettings;
import org.matsim.trafficlights.data.SignalSystemConfiguration;
import org.matsim.trafficlights.data.SignalSystemPlan;

public class SignalSystemControlerImpl extends SignalSystemControler {
	
	private SignalSystemConfiguration signalSystemConfiguration;

	public SignalSystemControlerImpl(SignalSystemConfiguration signalSystemConfiguration) {
		this.signalSystemConfiguration = signalSystemConfiguration;
	}

	@Override
	public SignalGroupSettings[] getGreenInLinks(double time) {
		
		if(this.signalSystemConfiguration == null){
			System.err.println(this.signalSystemConfiguration.getId() + " got no signalSystemConfiguation");
		}
		
		List<SignalSystemPlan> plan = ((PlanbasedSignalSystemControlInfoImpl) this.signalSystemConfiguration.getSignalSystemControler()).getSignalSystemPlans();
		
		
		// TODO [an] Es wird nur der erste Plan genommen, aber nicht ausgewählt
		SignalSystemPlan signalSystemPlan = plan.get(0);
		
		int currentSecondInPlan = (int) (time %  signalSystemPlan.getCirculationTime());		
		
		// Debug only
		System.out.println(currentSecondInPlan);
		
		ArrayList<SignalGroupSettings> greenLinksList = new ArrayList<SignalGroupSettings>();
		
		for (SignalGroupSettings signalGroupSetting : signalSystemPlan.getSignalGroupSettings().values()) {
			
			if ( signalGroupSetting.getRoughCast() <= currentSecondInPlan && currentSecondInPlan <= signalGroupSetting.getDropping()){
				// Debug only
				System.out.println("green " + signalGroupSetting.getSignalGroupDefinition().toString());
				
				greenLinksList.add(signalGroupSetting);
			} else System.out.println(" red " + signalGroupSetting.getSignalGroupDefinition().toString());
			
			
		}
		
		SignalGroupSettings[] greenLinks = new SignalGroupSettings[0];		
		greenLinks = (SignalGroupSettings[]) greenLinksList.toArray(greenLinks);
		
		// Debug only
		for (int i = 0; i < greenLinks.length; i++) {
			System.out.println(greenLinks[i].getSignalGroupDefinition().getId());
		}
	
		return greenLinks;
	}

}
