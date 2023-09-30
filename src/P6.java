
class P6 {

	public static void main(String[] args) {
		Peterson p1;
		p1 = new Peterson();
		MyLock6 l;
		l = new MyLock6();
		p1.setLock(l);
		try {
			p1.start();
			l.bar3();
			p1.join();
		} catch (Exception e) {
		}
	}

}

class MyLock6 {
	public void bar1() {

	}

	public void bar2() {

	}

	public void bar3() {

	}

}

class Peterson extends Thread {
	MyLock6 l;

	public void run() {
		try {
			synchronized (l) {
				l.bar1();
				l.bar2();
			}
		} catch (Exception e) {
		}
	}

	public void setLock(MyLock6 l) {
		this.l = l;
	}

}