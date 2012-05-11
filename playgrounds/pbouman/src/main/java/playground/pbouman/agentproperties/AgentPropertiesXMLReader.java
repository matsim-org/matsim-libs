package playground.pbouman.agentproperties;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.matsim.core.api.internal.MatsimWriter;

import playground.pbouman.agentproperties.xml.Agents;
import playground.pbouman.agentproperties.xml.Agents.Agent;
import playground.pbouman.agentproperties.xml.Agents.Agent.Activity;
import playground.pbouman.agentproperties.xml.Agents.Agent.DefaultPreferences;
import playground.pbouman.agentproperties.xml.Agents.Agent.Home;
import playground.pbouman.agentproperties.xml.Agents.Agent.LoadingTolerance;
import playground.pbouman.agentproperties.xml.Agents.Agent.Money;
import playground.pbouman.agentproperties.xml.Agents.Agent.Travel;
import playground.pbouman.agentproperties.xml.Location;
import playground.pbouman.agentproperties.xml.TemporalProperties;

public class AgentPropertiesXMLReader
{
	private String file;
	
	public AgentPropertiesXMLReader(String f)
	{
		file = f;
	}
	
	public Map<String, AgentProperties> read() throws Exception
	{
		JAXBContext jc = JAXBContext.newInstance("playground.pbouman.agentproperties.xml");
		Unmarshaller unmarshal = jc.createUnmarshaller();
		Agents as = (Agents) unmarshal.unmarshal(new File(file));

		HashMap<String,AgentProperties> map = new HashMap<String,AgentProperties>();
		
		for (Agent ag : as.getAgent())
		{
			String key = ag.getId();
			
			AgentProperties prop = new AgentProperties();
			
			for (Object o : ag.getMoneyOrTravelOrLoadingTolerance())
			{
				if (o instanceof Activity)
				{
					Activity act = (Activity) o;
					String actName = act.getName();
					ActivityProperties aprop = new ActivityProperties(actName);
					if (act.getAvailableFrom() != null && !act.getAvailableFrom().trim().equals(""))
						aprop.setAvailableFrom(parseTime(act.getAvailableFrom()));
					if (act.getAvailableTo() != null && !act.getAvailableTo().trim().equals(""))
						aprop.setAvailableTo(parseTime(act.getAvailableTo()));
					TimePreferences tp = aprop.getTimePreferences();
					for (JAXBElement<?> o2 : act.getLocationOrStartOrEnd())
					{
						Object o3 = o2.getValue();
						if (o3 instanceof Location)
						{
							Location loc = (Location) o3;
							if (loc.getLink() != null)
							{
								LocationDescription ld = new LocationDescription(loc.getLink());
								aprop.addLocation(ld);
							}
							else if (loc.getX() != null && loc.getY() != null)
							{
								double x = loc.getX().doubleValue();
								double y = loc.getY().doubleValue();
								LocationDescription ld = new LocationDescription(x,y);
								aprop.addLocation(ld);
							}
							
						}
						else if (o3 instanceof TemporalProperties)
						{
							TemporalProperties tprop = (TemporalProperties) o3;
							
							if (o2.getName().getLocalPart().equals("start"))
							{
								processStart(tprop,tp);
							}
							else if (o2.getName().getLocalPart().equals("end"))
							{
								processEnd(tprop,tp);
							}
							else if (o2.getName().getLocalPart().equals("duration"))
							{
								processDuration(tprop,tp);
							}
							
						}
						
						prop.addActivityProperties(aprop);
							
					}
				}
				else if (o instanceof DefaultPreferences)
				{
					TimePreferences pref = prop.getDefaultPreferences();
					DefaultPreferences dp = (DefaultPreferences) o;
					for (JAXBElement<TemporalProperties> o2 : dp.getStartOrEndOrDuration())
					{
						if (o2.getName().getLocalPart().equals("start"))
						{
							processStart(o2.getValue(),pref);
						}
						else if (o2.getName().getLocalPart().equals("end"))
						{
							processEnd(o2.getValue(),pref);
						}
						else if (o2.getName().getLocalPart().equals("duration"))
						{
							processDuration(o2.getValue(),pref);
						}
					}
				}
				else if (o instanceof Home)
				{
					Home h = (Home) o;
					for (Location loc : h.getLocation())
					{
						if (loc.getLink() != null)
						{
							LocationDescription ld = new LocationDescription(loc.getLink());
							prop.addHomeLocation(ld);
						}
						else if (loc.getX() != null && loc.getY() != null)
						{
							double x = loc.getX().doubleValue();
							double y = loc.getY().doubleValue();
							LocationDescription ld = new LocationDescription(x,y);
							prop.addHomeLocation(ld);
						}
					}
					if (h.getUtility() != null)
					{
						prop.setHomeUtility(h.getUtility().doubleValue());
					}
					if (h.getMorningEnd() != null && !h.getMorningEnd().trim().equals(""))
					{
						prop.setMorningEnd(parseTime(h.getMorningEnd()));
					}
					if (h.getEveningStart() != null && !h.getEveningStart().trim().equals(""))
					{
						prop.setEveningStart(parseTime(h.getEveningStart()));
					}
				}
				else if (o instanceof LoadingTolerance)
				{
					LoadingTolerance lt = (LoadingTolerance) o;
					prop.setMaximumLoading(lt.getMaximumAccepted().doubleValue());
					prop.setMaximumLoadingExceededUtility(lt.getUtility().doubleValue());
				}
				else if (o instanceof Money)
				{
					Money m = (Money) o;
					prop.setMoneyUtility(m.getUtility().doubleValue());
					if (m.getMaximum() != null)
						prop.setMoneyMaximum(m.getMaximum().doubleValue());
				}
				else if (o instanceof Travel)
				{
					Travel t = (Travel) o;
					if (t.getMode() != null && !t.getMode().trim().equals(""))
					{
						prop.setTravelUtility(t.getMode(), t.getUtility().doubleValue());
					}
					else
					{
						prop.setTravelUtility(t.getUtility().doubleValue());
					}
				}
			}
			
			map.put(key, prop);
		}
		return map;
	}
	
	/*
	public static void main(String [] args)
	{
		try
		{
			AgentPropertiesXMLReader a = new AgentPropertiesXMLReader("example.xml");
			Map<String,AgentProperties> as = a.read();
			System.out.println(as);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	*/
	
	private static void processStart(TemporalProperties prop, TimePreferences pref)
	{
		pref.setStartDevUtility(prop.getUtility().doubleValue());
		if (prop.getTargetTime() != null && !prop.getTargetTime().trim().equals(""))
		{
			double timeVal = parseTime(prop.getTargetTime());
			pref.setStartMean(timeVal);
			pref.setStartStdDev(0);
		}
		if (prop.getAcceptedDeviation() != null && !prop.getAcceptedDeviation().trim().equals(""))
		{
			double timeVal = parseTime(prop.getAcceptedDeviation());
			pref.setStartStdDev(timeVal);
		}
	}
	
	private static void processEnd(TemporalProperties prop, TimePreferences pref)
	{
		pref.setEndDevUtility(prop.getUtility().doubleValue());
		if (prop.getTargetTime() != null && !prop.getTargetTime().trim().equals(""))
		{
			double timeVal = parseTime(prop.getTargetTime());
			pref.setEndMean(timeVal);
			pref.setEndStdDev(0);
		}
		if (prop.getAcceptedDeviation() != null && !prop.getAcceptedDeviation().trim().equals(""))
		{
			double timeVal = parseTime(prop.getAcceptedDeviation());
			pref.setEndStdDev(timeVal);
		}
	}
	
	private static void processDuration(TemporalProperties prop, TimePreferences pref)
	{
		pref.setDurationUtility(prop.getUtility().doubleValue());
		if (prop.getTargetTime() != null && !prop.getTargetTime().trim().equals(""))
		{
			double timeVal = parseTime(prop.getTargetTime());
			pref.setDurationMean(timeVal);
			pref.setDurationStdDev(0);
		}
		if (prop.getAcceptedDeviation() != null && !prop.getAcceptedDeviation().trim().equals(""))
		{
			double timeVal = parseTime(prop.getAcceptedDeviation());
			pref.setDurationStdDev(timeVal);
		}
	}
	
	private static double parseTime(String s)
	{
		String [] time = s.split(":");
		double timeVal = (Integer.parseInt(time[1]) * 60) + (Integer.parseInt(time[0]) * 3600);
		return timeVal;
	}

}
