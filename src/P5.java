
class P5 {

	public static void main(String[] args) {
		Ponting p1;
		p1 = new Ponting();
		Perrera p2;
		p2 = new Perrera();
		MyLock5 l;
		l = new MyLock5();
		p1.setLock(l);
		p2.setLock(l);
		p1.setNext(p2);
		try {
			p1.start();
			p2.start();
			p1.join();
			p2.join();
		} catch (Exception e) {
		}
	}

}

class MyLock5 {
	public void bar1() {

	}

	public void bar2() {

	}

	public void bar3() {

	}

	public void bar4() {

	}
}

class Ponting extends Thread {
	MyLock5 l;
	boolean t1;
	Perrera nextThread;
	
	public void run() {
		try {
			synchronized (l) {
				l.bar1();
				t1 = true;
				nextThread.setT(t1);
				l.notifyAll();
				l.bar2();
			}
		} catch (Exception e) {
		}
	}

	public void setLock(MyLock5 l) {
		this.l = l;
	}
	
	public void setNext(Perrera next) {
		this.nextThread = next;
	}

}

class Perrera extends Thread {
	MyLock5 l;
	boolean t;
	boolean t1;
	
	public void run() {
		this.other();
	}

	public void setT(boolean t) {
		this.t = t;
	}
	
	public void other() {
		try {
			synchronized (l) {
				t1 = this.t;
				t1 = !t1;
				while (t1) {
					l.bar3();
					l.wait();
					t1 = this.t;
					t1 = !t1;
				}
				l.notifyAll();
				l.bar4();
			}
		} catch (Exception e) {
		}
	}

	public void setLock(MyLock5 l) {
		this.l = l;
	}

}