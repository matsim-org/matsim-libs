package playground.pbouman.transitfares;

import java.util.LinkedList;
import java.util.Map;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.scenario.MutableScenario;

public class FarePolicies
{
	/** name of the element for adding it as scenario element*/
	public static final String ELEMENT_NAME = "farePolicies";

	private LinkedList<DiscountInterval> discountIntervals;
	private LinkedList<PricingPolicy> pricingPolicies;
	private MutableScenario scenario;
	private double transferGracePeriod;

	public static final double DEFAULT_FIXED_PRICE = 0.001;
	public static final double DEFAULT_UNIT_PRICE = 0.001;

	
	public FarePolicies(MutableScenario s)
	{
		scenario = s;
		parsePolicies();
	}
	
	
	private void parsePolicies()
	{
		discountIntervals = new LinkedList<DiscountInterval>();
		pricingPolicies = new LinkedList<PricingPolicy>();
		
		ConfigGroup mod = scenario.getConfig().getModule("transitPricing");
		if (mod != null)
		{
			Map<String, String> map = mod.getParams();
			
			for (String key : map.keySet())
			{
				if (key.startsWith("pricingPolicy_"))
				{
					String [] split = map.get(key).split(" ");
					
					String ppId = "pricingPolicyLines_"+key.replace("pricingPolicy_", "");
					String lines = "";
					if (map.containsKey(ppId))
						lines = map.get(ppId);

					ppId = "pricingPolicyRoutes_"+key.replace("pricingPolicy_", "");
					String routes = "";
					if (map.containsKey(ppId))
						routes = map.get(ppId);

					
					double f = Double.parseDouble(split[0]);
					double d = Double.parseDouble(split[1]);
					

					pricingPolicies.add(new PricingPolicy(f,d,lines,routes));
				}
				else if (key.startsWith("discountInterval_"))
				{
					String [] split = map.get(key).split(" ");
					
					String ppId = "discountIntervalLines_"+key.replace("discountInterval_", "");
					String lines = "";
					if (map.containsKey(ppId))
					{
						lines = map.get(ppId);
					}
					
					ppId = "discountIntervalRoutes_"+key.replace("discountInterval_", "");
					String routes = "";
					if (map.containsKey(ppId))
					{
						routes = map.get(ppId);
					}
							
					String from = split[0];
					String to = split[1];
					double d = Double.parseDouble(split[2]);
					
					discountIntervals.add(new DiscountInterval(from,to,d,lines,routes));
				}
				else if (key.equals("transferGracePeriod"))
				{
					transferGracePeriod = DiscountInterval.parseTime(map.get(key));
				}
			}
			
		}
	}
	
	static class DiscountInterval
	{
		public final double from;
		public final double to;
		public final double discount;
		//public final LinkedList<String> types;
		public final LinkedList<String> lines;
		public final LinkedList<String> routes;
		
		/**
		 * Constructor for a discount interval using double notation for the times
		 * @param f the earliest time the discount is valid
		 * @param t the earliest time the discount is not valid any more
		 * @param d the factor of the discount (0.4 is a 40% discount, -0.2 is a 20% markup)
		 * @param type A string containing vehicle types this discount applies to.
		 *             In case of "", it applies to all vehicles. "Small Train" matches to one type.
		 *             It can be comma separated, so "Small Train,High Speed" matches to two types.
		 */
		public DiscountInterval(double f, double t, double d, String line, String route)
		{
			from = f;
			to = t;
			discount = d;
			
			//LinkedList<String> tlist = new LinkedList<String>();
			//String [] split = type.split(",");
			//for (int i = 0; i < split.length; i++)
			//	tlist.add(split[i].trim());
			//types = tlist;
			
			LinkedList<String> rlist = new LinkedList<String>();
			String [] split = route.split(",");
			if (route.trim().length() > 0)
			{
				for (int i = 0; i < split.length; i++)
					rlist.add(split[i].trim());
			}
			routes = rlist;
			
			LinkedList<String> llist = new LinkedList<String>();
			split = line.split(",");
			if (line.trim().length() > 0)
			{
				for (int i = 0; i < split.length; i++)
					llist.add(split[i].trim());
			}
			lines = llist;
		}
		
		/**
		 * Constructor for a  discount interval use textual notation for the times
		 * @param f the earliest time the discount is valid (in hh:mm:ss format)
		 * @param t the earliest time the discount is not valid any more (in hh:mm:ss format)
		 * @param d the factor of the discount (0.4 is a 40% discount, -0.2 is a 20% markup)
		 * @param type A string containing vehicle types this discount applies to.
		 *             In case of "", it applies to all vehicles. "Small Train" matches to one type.
		 *             It can be comma separated, so "Small Train,High Speed" matches to two types.
		 */
		
		public DiscountInterval(String f, String t, double d, String line, String route)
		{
			this(parseTime(f),parseTime(t),d,line,route);
		}
		
		/**
		 * Constructor for a  discount interval using double notation for the times.
		 * Matches to all types of vehicles
		 * @param f the earliest time the discount is valid
		 * @param t the earliest time the discount is not valid any more
		 * @param d the factor of the discount (0.4 is a 40% discount, -0.2 is a 20% markup)
		 */
		
		public DiscountInterval(double f, double t, double d)
		{
			this(f,t,d,"","");
		}
		
		/**
		 * Constructor for a  discount interval use textual notation for the times.
		 * Matches to all types of vehicles
		 * @param f the earliest time the discount is valid (in hh:mm:ss format)
		 * @param t the earliest time the discount is not valid any more (in hh:mm:ss format)
		 * @param d the factor of the discount (0.4 is a 40% discount, -0.2 is a 20% markup)
		 */
		
		public DiscountInterval(String f, String t, double d)
		{
			this(f,t,d,"","");
		}
		
		
		/**
		 * Determines whether this discount interval applies for a given time and a given vehicle type
		 * @param time the time under consideration
		 * @param line the line of the 
		 * @return true if this discount interval applies, false otherwise
		 */
		
		public boolean matches(double time, String line, String route)
		{
			boolean within = from <= time && time < to;
			
			if (!within)
				return false;
			
			return matches(line,route);
		}
		
		/**
		 * Determines whether this discount interval applies for a given time and a given vehicle type
		 * @param line the 
		 * @param route
		 * @return
		 */
		public boolean matches(String line, String route)
		{
			if (!lines.isEmpty() && !lines.contains(line))
				return false;
			
			if (!routes.isEmpty() && !routes.contains(route))
				return false;
			
			return true;
		}
		
		private static double parseTime(String in)
		{
			String [] split = in.split(":");
			if (split.length != 3)
				throw new Error("Invalid time format, expecting hh:mm:ss");
			int h = Integer.parseInt(split[0]);
			int m = Integer.parseInt(split[1]);
			int s = Integer.parseInt(split[2]);
			
			if (h < 0 || h > 23)
				throw new Error("Hour must be in the range 0-23");
			if (m < 0 || m > 59)
				throw new Error("Minute must be in the range 0-59");
			if (s < 0 || s > 59)
				throw new Error("Second must be in the range 0-59");
			
			return (60*60*h)+(60*m)+s;
			
		}
		
	}

	static class PricingPolicy
	{
		public final double fixedPrice;
		public final double kmPrice;
		//public final LinkedList<String> types;
		public final LinkedList<String> lines;
		public final LinkedList<String> routes;
		
		/**
		 * A constructor for a basic pricing policy. Applies to all vehicle types.
		 * @param f The fixed price when boarding a vehicle
		 * @param d The price for each kilometer travelled
		 */
		public PricingPolicy(double f, double d)
		{
			this(f,d,"","");
		}
		
		/**
		 * A constructor for a basic pricing policy.
		 * @param f The fixed price when boarding a vehicle
		 * @param d The price for each kilometer travelled
		 * @param type A string containing vehicle types this policy applies to.
		 *             In case of "", it applies to all vehicles. "Small Train" matches to one type.
		 *             It can be comma separated, so "Small Train,High Speed" matches to two types.
		 * @param line A string containing the lines this policy applies to.
		 * @param route A string containing the routes this policy applies to.
		 */
		public PricingPolicy(double f, double d, String line, String route)
		{
			fixedPrice = f;
			kmPrice = d;
			
			//LinkedList<String> t = new LinkedList<String>();
			//String [] split = type.split(",");
			//for (int i=0; i < split.length; i++)
			//	t.add(split[i].trim());
			//types = t;
			
			LinkedList<String> l = new LinkedList<String>();
			String [] split = line.split(",");
			if (line.trim().length() > 0)
			{
				for (int i=0; i < split.length; i++)
					l.add(split[i].trim());
			}
			lines = l;
			
			LinkedList<String> r = new LinkedList<String>();
			split = route.split(",");
			if (route.trim().length() > 0)
			{
				for (int i=0; i < split.length; i++)
					r.add(split[i].trim());
			}
			routes = r;
		}
		
		/**
		 * Determines whether this pricing policy applies to a certain type of vehicle 
		 * @param type The type of vehicle under consideration
		 * @param line The name of the line under consideration
		 * @param route The name of the route under consideration
		 * @return true if this policy applies to the given vehicle type, false otherwise.
		 */
		public boolean appliesTo(String line, String route) {
//			if (!types.isEmpty() && !types.contains(type))
//				return false;
			if (!lines.isEmpty() && !lines.contains(line))
				return false;
			if (!routes.isEmpty() && !routes.contains(route))
				return false;
			return true;
		}		
	}

	public double getPolicyFixedPrice(String line, String route)
	{
		for (PricingPolicy pp : pricingPolicies)
		{
			if (pp.appliesTo(line,route))
			{
				return pp.fixedPrice;
			}
		}
		return DEFAULT_FIXED_PRICE;
	}


	public double getPolicyDistancePrice(String line, String route)
	{
		for (PricingPolicy pp : pricingPolicies)
		{
			if (pp.appliesTo(line,route))
			{
				return pp.kmPrice;
			}
		}
		return DEFAULT_UNIT_PRICE;
	}


	public double getTransferGracePeriod()
	{
		return transferGracePeriod;
	}


	public DiscountInterval getDiscountInterval(double time, String line, String route) {
		for (DiscountInterval di : discountIntervals)
			if (di.matches(time, line, route))
				return di;
		return null;
	}


	public DiscountInterval getMinimumDiscountInterval(String line, String route)
	{
		for (DiscountInterval di : discountIntervals)
			if (di.matches(line, route))
				return di;
		return null;
	}	
	
	
}
