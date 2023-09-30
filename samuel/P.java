class P{
	public static void main(String []args){
		B t1;
		C t2;
		Buf b;
		Buf b2;
		try {
			t1 = new B();
			t2 = new C();
			b = new Buf();
			b2 = new Buf();
			t1.b = b;
			t2.b = b;
			t2.b2 = b2;

			t1.start();
			t2.start();

			t1.goo();

			t1.join();
			t2.join();
		} catch (Exception e){}
	}
}
class Buf{
}
class B extends Thread {
	Buf b;
	int a;
	public void run() {
		try {
			synchronized (b){
				a = this.bar();;
				b.wait();
				this.gar();
			}
			this.foo();

		} catch (Exception e){}
	}
	public void foo(){ }
	public int bar(){ int t; t=10; return t;}
	public void goo(){}
	public void gar(){}
}

class C extends Thread {
	Buf b;
	Buf b2;
	public void run() {
		try {
			synchronized (b) {
				this.f4();
				b.notifyAll();
				this.f2();
			}
			this.f3();

		} catch (Exception e){}
	}
	public void f2(){}
	public void f3(){}
	public void f4(){}
}