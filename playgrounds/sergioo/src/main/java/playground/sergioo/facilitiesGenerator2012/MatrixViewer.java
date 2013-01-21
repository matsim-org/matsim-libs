package playground.sergioo.facilitiesGenerator2012;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;

import others.sergioo.util.algebra.Matrix2DImpl;
import others.sergioo.util.algebra.Matrix3DImpl;

public class MatrixViewer {

	
	//Attributes

	//Methods
	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {
		/*ObjectInputStream ois = new ObjectInputStream(new FileInputStream(args[0]));
		Matrix2DImpl capacities = (Matrix2DImpl) ois.readObject();
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		boolean cont = true;
		String res = "";
		do {
			System.out.println("Stop area position:");
			res = br.readLine();
			try {
				int pos = Integer.parseInt(res);
				if(pos<0 || pos>=capacities.getDimension(1))
					throw new Exception("Invalid position");
				double sum = 0;
				for(int f=0; f<capacities.getDimension(0); f++) {
					if(capacities.getElement(f, pos)>4.9E-324)
						System.out.print(f+": "+capacities.getElement(f, pos)+", ");
					sum+=capacities.getElement(f, pos);
				}
				System.out.println("Total: "+sum);
			} catch (Exception e) {
				if(e.getMessage().startsWith("F")) {
					System.out.println("Do you want to exit?");
					res = br.readLine();
					if(res.startsWith("Y") || res.startsWith("y"))
						cont = false;
				}
				System.out.println(e.getMessage());
			}
		} while(cont);*/
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(args[0]));
		Matrix3DImpl capacities = (Matrix3DImpl) ois.readObject();
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		boolean cont = true;
		String res = "";
		do {
			System.out.println("Master Plan area position:");
			res = br.readLine();
			try {
				int pos = Integer.parseInt(res);
				if(pos<0 || pos>=capacities.getDimension(0))
					throw new Exception("Invalid position");
				for(int o=0; o<capacities.getDimension(1); o++) {
					double sum = 0;
					for(int s=0; s<capacities.getDimension(2); s++)
						sum += capacities.getElement(pos, o, s);
					System.out.println(o+": "+sum);
				}
				
			} catch (Exception e) {
				if(e.getMessage().startsWith("F")) {
					System.out.println("Do you want to exit?");
					res = br.readLine();
					if(res.startsWith("Y") || res.startsWith("y"))
						cont = false;
				}
				System.out.println(e.getMessage());
			}
		} while(cont);
	}

}
