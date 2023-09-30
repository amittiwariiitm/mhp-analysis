class P4 {
    public static void main(String []args){
        T t1;
        T t2;
        T t3;
        Buf l;
        l = new Buf();
        t1 = new T();
        t2 = new T();
        t3 = new T();
        t1.set(l);
        t2.set(l);
        t3.set(l);
        t1.start();
        t2.start();
        t3.start();
        synchronized(l){
            l.l0();
            l.notify();
            l.l1();
            l.notifyAll();
            l.l2();
        }

        l.l14();

        synchronized(l){
            l.l3();
            l.notifyAll();
            l.l4();
            l.notify();
            l.l5();
        }

        try {
            t1.join();
            t2.join();
            t3.join();
        }catch (Exception e){}
    }
}

class Buf{
    public void l0(){}
    public void l1(){}
    public void l2(){}
    public void l3(){}
    public void l4(){}
    public void l5(){}
    public void l6(){}
    public void l7(){}
    public void l8(){}
    public void l9(){}
    public void l10(){}
    public void l11(){}
    public void l12(){}
    public void l13(){}
    public void l14(){}

}


class T extends Thread{
    Buf b;

    public void set(Buf k){
        b = k;
    }


    public int f1(int a){
        return a + 50;
    }


    public int f2(int a){
        return a + 20;
    }

    public void run(){
            int a;
            try{

            a =10;
            synchronized(b){
                b.l6();
                b.wait();
                b.l7();
                a = f1(a);
                b.l8();
            }
            b.l9();

            
            synchronized(b){
                b.l10();
                b.wait();
                b.l11();
                a = f2(a);
                b.l12();
            }
            b.l13();

        }catch(Exception e){}

    }
}