
class P3 {

	public static void main(String[] args) {
		Painter p1;
		p1 = new Painter();
		Painter p2;
		p2 = new Painter();
		Lock l;
		l = new Lock();
		p1.setLock(l);
		p2.setLock(l);
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

class Lock {
	public void bar1() {
		
	}
	public void bar2() {
		
	}
}

class Painter extends Thread {
	Lock l;
	int tid;
	boolean t;
	boolean t1;
	
	public void run() {
		try {
			synchronized (l) {
				l.bar1();
				if (tid > 0) {
					t1 = !t;
					while (t1) {
						l.wait();
						t1 = !t;
					}
					l.notifyAll();
				} else {
					while (t) {
						l.wait();
					}
					t = true;
					l.notifyAll();
				}
				l.bar2();
			}
		} catch (Exception e) {
		}
	}
	public void setLock(Lock l) {
		this.l = l;
	}
	public void setTID (int tid) {
		this.tid = tid;
	}
}
