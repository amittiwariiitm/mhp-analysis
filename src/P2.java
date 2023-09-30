class P2 {
	public static void main(String[] args) {
		int p;
		int q;
		int r;

		Buf b;
		Buf c;
		Buf d;
		Buf e;

		p = 10;
		q = 56;
		r = 33;

		b = new Buf1();
		B t1;
		c = new Buf2();
		C t2;
		d = new Buf3();
		D t3;
		E t4;
		t1 = null;
		t2 = null;
		t3 = null;

		if (p > q) {
			e = b;
			t1 = new B();
			t1.set(b);
			t1.start();

		} else {
			if (r < p) {
				e = c;
				t2 = new C();
				t2.set(c);
				t2.start();
			} else {
				e = d;
				t3 = new D();
				t3.set(d);
				t3.start();
			}
		}

		t4 = new E();
		t4.set(e);
		t4.start();
		try {
			if (p > q) {
				t1.join();
			} else {
				if (r < p) {
					t2.join();
				}

				else {
					t3.join();
				}

			}

			t4.join();
		} catch (Exception ex) {
		}
	}
}

class Buf {

	public void foo1() {

	}

	public void foo2() {

	}

	public void foo3() {

	}

	public void foobar() {
		this.bar();
	}

	public void bar() {
	}

	public void bar1() {
	}

	public void bar2() {
	}

	public void bar3() {
	}

}

class Buf1 extends Buf {
	public void foobar() {
		this.bar1();
	}
}

class Buf2 extends Buf {
	public void foobar() {
		this.bar2();
	}
}

class Buf3 extends Buf {
	public void foobar() {
		this.bar3();
	}
}

class B extends Thread {
	Buf b;

	public void run() {
		try {
			synchronized (b) {

				b.notify();
				b.foo1();
				b.wait();

			}

		}

		catch (Exception e) {

		}

	}

	public void set(Buf b) {
		this.b = b;
	}
}

class C extends Thread {
	Buf c;

	public void run() {
		try {
			synchronized (c) {

				c.notify();
				c.foo2();
				c.wait();
			}
		}

		catch (Exception e) {
		}

	}

	public void set(Buf c) {
		this.c = c;
	}

}

class D extends Thread {
	Buf d;

	public void run() {

		try {
			synchronized (d) {
				d.notify();
				d.foo3();
				d.wait();
			}

		} catch (Exception e) {
		}

	}

	public void set(Buf d) {
		this.d = d;
	}

}

class E extends Thread {
	Buf e;

	public void run() {

		try {
			synchronized (e) {

				e.wait();
				e.foobar();
				e.notify();

			}
		}

		catch (Exception e) {
		}

	}

	public void set(Buf e) {
		this.e = e;
	}

}