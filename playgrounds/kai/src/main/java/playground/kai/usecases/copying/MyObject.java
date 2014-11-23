package playground.kai.usecases.copying;

class MyObject extends AbstractObject implements Cloneable {
	private int age ;
	private String name ;
	MyObject( int age, String name ){
		this.age = age ;
		this.name = name ;
	}
	int getAge() {
		return age;
	}
	String getName() {
		return name;
	}
	@Override
	public String toString() {
		return " age=" + age + " name=" + name ;
	}
	@Override
	public MyObject createCopy() {
		return this.clone() ;
	}
	
	@Override
	protected MyObject clone() {
		// the following is strictly speaking not necessary at all.  But it might give other implementers the
		// correct hint.
		
		MyObject clone = (MyObject) super.clone();
		// nothing to do since no deep copies are necessary
		return clone ;
	}
}