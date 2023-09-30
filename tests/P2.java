class P2 {
	public static void main(String[] args) {
		B t1;
		B t3;
		C t2;
		Buf b;
		try {
			b = new Buf();
			int x;
			int y;
			x = 10;
			y = 15;
			if (x < y) {
				t3 = new B();
				t3.b = b;
			} else {
				t3 = new B();
				t3.b=b;
			}
			t1 = new B();
			t2 = new C();
			t1.b = b;
			t2.b = b;

			t3.start();
			t2.start();
			synchronized (t3) {
				x = y;
				b = new Buf();
				new B();
			}
			synchronized (t2) {
				synchronized (t1) {
					t1.wait();
					t1.start();
					t2.wait();
				}
			}
			t1.join();
			t2.join();
			t3.join();
		} catch (Exception e) {
		}
	}
}

class Buf {
}

class B extends Thread {
	Buf b;
	Buf c;

	public void run() {
		try {
			c = new Buf();
			synchronized (b) {
				this.bar();
				b.wait();
				synchronized(c) {
					b.wait();
					foo2(b);
				}
				synchronized(this) {
				}
				b.notify();
				b.notifyAll();
			}
			this.foo();

		} catch (Exception e) {
		}
	}

	public void foo2(Buf b) {
		try
		{
		b.wait();
		}
		catch(Exception e)
		{}
	}

	public void foo() {
	}

	public void bar() {
	}
}

class C extends Thread {
	Buf b;

	public void run() {
		try {
			synchronized (b) {
				this.f2();
			}
			this.f3();

		} catch (Exception e) {
		}
	}

	public void f2() {
	}

	public void f3() {
	}
}
