package playground.pbouman.transitfares;

import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Pattern;

import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.scenario.MutableScenario;

public class AgentSensitivities
{	
	/** name of the element for adding it as scenario element*/
	public static final String ELEMENT_NAME = "agentSensitivities";
	public static final double DEFAULT_SENSITIVITY = 1;

	public double minimum = 1;
	private LinkedList<SensitivityPattern> patterns;
	
	private MutableScenario scenario;

	public AgentSensitivities(MutableScenario si)
	{
		scenario = si;
		compilePatterns();
	}
	
	
	private void compilePatterns()
	{
		patterns = new LinkedList<SensitivityPattern>();
		ConfigGroup mod = scenario.getConfig().getModule("transitPricing");
		if (mod != null)
		{
			Map<String, String> map = mod.getParams();
			
			for (String key : map.keySet())
			{
				if (key.startsWith("sensitivityFactor_"))
				{
					String factor = map.get(key);
					
					String ppId = "sensitivityPattern_"+key.replace("sensitivityFactor_", "");
					String pattern = "";
					if (map.containsKey(ppId))
						pattern = map.get(ppId);
					
					ppId = "sensitivityPrefix_"+key.replace("sensitivityFactor_", "");
					String prefix = "";
					if (map.containsKey(ppId))
						pattern = map.get(ppId);
					
					SensitivityPattern pat = new SensitivityPattern(Double.parseDouble(factor),pattern,prefix);
					patterns.add(pat);
					minimum = Math.min(minimum, pat.utilityOfMoney);
				}
			}
			
		}

	}
	
	static class SensitivityPattern
	{
		public final double utilityOfMoney;
		public final Pattern regex;
		public final String prefix;
		
		public SensitivityPattern(double utility, String prefix, String regExPattern)
		{
			utilityOfMoney = utility;
			
			if (regExPattern != null && !regExPattern.equals(""))
				regex = Pattern.compile(regExPattern);
			else
				regex = null;
			this.prefix = prefix;
		}
		
		public boolean matches(Id agentId)
		{
			if (prefix != null && !prefix.equals(""))
				return agentId.toString().startsWith(prefix);
			
			if (regex != null)
				return regex.matcher(agentId.toString()).matches();
			
			return true;
		}
	}

	public double getSensitivity(Id p)
	{
		for (SensitivityPattern sp : patterns)
			if (sp.matches(p))
				return sp.utilityOfMoney;
		return DEFAULT_SENSITIVITY;
	}


	public double getMinimumSensitivity()
	{
		return minimum;
	}
}
