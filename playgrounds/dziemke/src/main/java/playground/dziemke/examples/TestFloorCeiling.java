package playground.dziemke.examples;

public class TestFloorCeiling {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		{
			System.out.println("-----");
			double value = 1.62345234523;
			System.out.println("value = " + value);
			System.out.println("floor = " + Math.floor(value));
			System.out.println("ceiling = " + Math.ceil(value));
		}
		
		{
			System.out.println("-----");
			double value = 1.;
			System.out.println("value = " + value);
			System.out.println("floor = " + Math.floor(value));
			System.out.println("ceiling = " + Math.ceil(value));
		}
		
		{
			System.out.println("-----");
			double value = 0.;
			System.out.println("value = " + value);
			System.out.println("floor = " + Math.floor(value));
			System.out.println("ceiling = " + Math.ceil(value));
		}
		
		{
			System.out.println("-----");
			double value = -.5;
			System.out.println("value = " + value);
			System.out.println("floor = " + Math.floor(value));
			System.out.println("ceiling = " + Math.ceil(value));
		}		
	}
}
