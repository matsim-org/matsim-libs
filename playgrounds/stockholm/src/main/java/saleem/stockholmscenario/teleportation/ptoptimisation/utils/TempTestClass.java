package saleem.stockholmscenario.teleportation.ptoptimisation.utils;

public class TempTestClass {
	class A{
		public int i;
		public void setI(int i){
			this.i = i;
		}
		public int getI(){
			return i;
		}
	}
	class B{
		public A a;
		public void setA(A a){
			this.a = a;
		}
		public A getA(){
			return a;
		}
	}
	public  static void main(String[] arsg){
		TempTestClass t = new TempTestClass();
		
		A a = t.new A();
		a.setI(5);
		
		B b = t.new B();
		b.setA(a);
		
		B c = t.new B();
		c.setA(a);
		
		a.setI(10);
		b.setA(a);
		
		System.out.println(b.getA().getI());
		System.out.println(c.getA().getI());
	}
}
