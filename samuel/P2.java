class P2 {
    public static void main(String []args){
        try{
        T1 a;
        T2 b;
        Buf bu;
        bu = new Buf();
        a = new T1();
        a.l= bu;
        b = new T2();
        b.l = bu;
        a.start();
        a.l1();
        b.start();
        b.l2();
            a.join();
            a.l3();
            b.join();
            b.l4();
        }catch (Exception e){}
    }

}

class Buf {

}

class T1 extends Thread{
    Buf l;
    public void newExec(int a){
    try{
        T2 ot;
        ot = new T2();
        ot.set(a);

        ot.start();
        
        synchronized(l){
        this.tl3();
        }
        
        ot.join();
        
    }catch (Exception e){}
    }

    public void run(){
        int a;
        a = 7;
        newExec(a);
    }

    public void l1(){};
    public void l3(){};
    public void tl3(){};
}


class T2 extends Thread{
    Buf l;
    int b;
    public void set(int k){
        b = k;
    }

    public int f1(int a){
        return a + 2;
    }
    public void run(){

        synchronized(l){
            b = this.f1(b);;
        }
        this.f3();
    }

    public void f3(){};
    public void l2(){};
    public void l4(){};
}