
class P4 {

	public static void main(String[] args) {
		Penny p1;
		p1 = new Penny();
		Penny p2;
		p2 = new Penny();
		MyLock l;
		MyLock l2;
		l = new MyLock();
		l2 = new MyLock();
		p1.setLock(l, l2);
		p2.setLock(l, l2);
		p1.setTID(0);
		p2.setTID(1);
		try {
			p1.start();
			p2.start();
			p1.join();
			p2.join();
		} catch (Exception e) {
		}
	}

}

class MyLock {
	public void bar1() {
		
	}
	public void bar2() {
		
	}
	public void bar3() {
		
	}
	public void bar4() {
		
	}
}

class Penny extends Thread {
	MyLock l;
	int tid;
	MyLock l2;
	boolean t;
	boolean t1;

	public void run() {
		try {
			synchronized (l) {
				l.bar1();
				synchronized (l2) {
					l.bar3();
					if (tid > 0) {
						l.notifyAll();
					} else {
						t = true;
						l.notifyAll();
						
					}
					l.bar4();
				}
			}
		} catch (Exception e) {
		}
	}
	public void setLock(MyLock l, MyLock l2) {
		this.l = l;
		this.l2 = l2;
	}
	public void setTID (int tid) {
		this.tid = tid;
	}
}
