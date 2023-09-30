class A2 {
	public static void main(String[] args) {
		B t1;
		C t2;
		E t3;
		Buf b;
		try {
			t1 = new B();
			t2 = new C();
			t3 = new E();
			b = new Buf();
			t1.b = b;
			t2.b = b;
			t3.b = b;

			t1.start();
			t2.start();
			t3.start();
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

	public void run() {
		try {
			this.amit1();
			synchronized (b) {
				this.bar();
				b.notify();
				this.amit4();
			}
			this.foo();

		} catch (Exception e) {
		}
	}

	public void foo() {
	}

	public void bar() {
	}

	public void amit1() {
	}

	public void amit4() {
	}
}

class C extends Thread {
	Buf b;

	public void run() {
		try {
			this.f4();
			synchronized (b) {
				this.f2();
				this.amit3();
				b.wait();
				this.amit2();
			}
			this.f3();
			synchronized (b) {
				b.wait();
			}

		} catch (Exception e) {
		}
	}

	public void f2() {
	}

	public void f3() {
	}

	public void f4() {
	}

	public void amit2() {
	}

	public void amit3() {
	}
}

class E extends Thread {
	Buf b;

	public void run() {
		try {
			this.f4();
			synchronized (b) {
				b.wait();
			}

		} catch (Exception e) {
		}
	}

	public void f4() {
	}
}