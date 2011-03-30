package analysis;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Stack;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.Attribute;

import org.matsim.core.utils.charts.BarChart;

/**
 * Can read a plan and output some data in a chart, e.g. departure time distribution.
 * For verification of plan files.
 * @author bvitins
 *
 */
public class ReadPlans {

	
	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		ReadPlans readPlans = new ReadPlans();
		readPlans.run();
	}

	private void run() 
	{	
		final String PERSON = "person";
		final String LEG = "leg";
		final String DEPTIME = "dep_time";
		
		int counter = 0;
		Stack<String> stack = new Stack<String>();
		
		try {
			// First create a new XMLInputFactory
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			
			
			// Setup a new eventReader
			InputStream in = new FileInputStream("//pingelap/matsim/data/switzerland/plans/xy/freight/ZH/10Pct/plans_LKW(tta)_10pc.xml");
			XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
			
			// Read the XML document
			
			while (eventReader.hasNext()) {
				XMLEvent event = eventReader.nextEvent();

				if (event.isStartElement()) {

					if (event.isStartElement()) {
						if (event.asStartElement().getName().getLocalPart().equals(PERSON)) 
						{
							event = eventReader.nextEvent();
							counter++;
							continue;
						}
					}
					if (event.asStartElement().getName().getLocalPart().equals(LEG)) 
					{
						Iterator iterator = ((StartElement) event).getAttributes();
						while (iterator.hasNext()) {
							Attribute attribute = (Attribute) iterator.next();
							if (attribute.getName().toString().contentEquals(DEPTIME)) 
							{
								stack.add(attribute.getValue());
							}
				        }
						continue;
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
		System.out.println("Number of persons: "+counter);
		
		generateChart(stack, 24);
	}
	
	/**
	 * Only works for 24 until now. Corrections needed for more flexible time segments. 
	 * @param stack
	 * @param numberOfSegments
	 */
	private void generateChart(Stack<String> stack, int numberOfSegments) 
	{
		String [] categories = new String [numberOfSegments];
		
		double von = 0.0;
		double incr = 24.0 / numberOfSegments;
		
		for (int i = 0; i < categories.length; i++) 
		{
			categories[i] = Double.toString(von).substring(0, 2);
			von = von + incr;
		}
		
		double [] values = new double[numberOfSegments];
		
		int counter2 = 0;
		int size = stack.size();
		for (int i = 0; i < size; i++) 
		{
			int time = Integer.parseInt(stack.pop().substring(0, 2));
			values[time]=values[time]+1.0;
			counter2++;
		}
		
		double totalValue = 0;
		for (int i = 0; i < values.length; i++) {totalValue = totalValue + values[i];}
		
		
		BarChart chart = new BarChart("Departure Time Distribution LKW 10%", "departure time [h]", "number of vehicles", categories);
		chart.addSeries("departure time", values);
		chart.saveAsPng("C:/Documents and Settings/bvitins/Desktop/Chart.png", 800, 600);
	}
}
