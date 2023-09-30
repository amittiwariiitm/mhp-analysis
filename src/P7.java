
class P7 {

	public static void main(String[] args) {
		Paul p1;
		Paul p2;
		MyLock7 l;
		p1 = new Paul();
		p2 = new Paul();
		l = new MyLock7();
		p1.setLock(l);
		try {
			p1.start();
			p2.start();
			p1.join();
			p2.join();
			l.bar3();
		} catch (Exception e) {
		}
	}

}

class MyLock7 {
	public void bar1() {

	}

	public void bar2() {

	}

	public void bar3() {

	}

}

class Paul extends Thread {
	MyLock7 l;

	public void run() {
		try {
			synchronized (l) {
				this.temp();
			}
		} catch (Exception e) {
		}
	}
	
	public void temp() {
		boolean t;
		t = false;
		l.bar1();
		try {
			while (t) {
				l.wait();
			}
		} catch (Exception e) {
		}
		l.bar2();
	}

	public void setLock(MyLock7 l) {
		this.l = l;
	}

}