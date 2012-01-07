package city2000w;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class TestCarrierAgent {
	
	static class FooAgent extends Thread{
		private String name;

		public FooAgent(String name) {
			super();
			this.name = name;
		}
		
		public void uff(){
			run();
		}
		
		public String sayMyName(){
			return name;
		}
		
		public void run(){
			work();
		}
		
		public void work(){
			try {
				sleep((new Random()).nextInt(10000));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	public static void main(String[] args) {
		List<FooAgent> foos = new ArrayList<FooAgent>();
		for(int i=0;i<10;i++){
			foos.add(new FooAgent("spako"));
		}
		for(FooAgent f : foos){
			f.start();
		}
	}
}
