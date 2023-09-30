class P4Spec {
    public static void main(String []args){
        T t1;
        T t2;
        T t3;
        Buf l;
        Duf l2;

        l = new Buf();
        l2 = new Duf();
        t1 = new T();
        t2 = new T();
        t3 = new T();
        t1.set(l, l2);
        t2.set(l, l2);
        t3.set(l, l2);
        t1.start();
        t2.start();
        t3.start();
        l.l1();
        synchronized(l){
        l.notify();
        }
        l.l2();
        synchronized(l){
        l.notify();
        }        
        l.l3();
        synchronized(l){
            l.notify();
        }
        l.l13b();
        synchronized(l2){
            l2.notifyAll();
        }
        l.l4();
        try {
            t1.join();
            l.l5();
            t2.join();
            t3.join();
            l.l6();
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
    public void l13b(){}
    public void l14(){}
    public void l15(){}
    public void l16(){}
    public void l17(){}
    public void l18(){}
    public void l19(){}
    public void l20(){}
    public void l21(){}
    public void l22(){}
    
}

class Duf{
    public void kah(){}
}

class T extends Thread{
    Buf b;
    Duf b2;

    public void set(Buf k, Duf p){
        b = k;
        b2 =p;
    }

    public void run(){
        T nT;
        int a;

        try{
            synchronized(b2){
                b2.kah();
                synchronized(b){
                    b.l7();
                    b.wait();
                    b.l8();
                    a = 20; 
                    b2.wait();
                    b.l9();
                }
            }

        b.l20();
        synchronized(b){
            b.notify();
        }
        b.l21();
        synchronized(b2){
            b2.notifyAll();
        }
        b.l22();
    }catch (Exception e){}

    }
}