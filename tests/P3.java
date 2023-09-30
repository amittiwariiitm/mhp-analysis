class P3 {
	public static void main(String[] args) {
		Buf b;
		Buf b2;
		try {
			b = new Buf();
			b2 = new Buf();
			synchronized (b) {
				synchronized (b2) {
				}
				b.notifyAll();
			}
		} catch (Exception e) {
		}
	}
}

class Buf {
}

