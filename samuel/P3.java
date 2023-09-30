class P3 {
    public static void main(String []args){
        T t1;
        T t2;
        int b;
        int c;
        Buf l;

        try{
        l = new Buf();
        t1 = new T();
        t1.set1(l);
        t1.start();
        synchronized(l){
            b= 10;
            c =  20;
            c = l.add(b, c);;
            l.notify();
            l.l1();
        }
        l.l2();

        t1.join();

        l.l7();
    }catch (Exception e){}
        
    }
}


class Buf{
    public int add(int a, int b){
        int c;
        c = a + b;
        return c;
    }
    public void l1(){}
    public void l2(){}
    public void l3(){}
    public void l4(){}
    public void l5(){}
    public void l6(){}
    public void l7(){}


}

class C{


}

class T extends Thread{
    Buf b;

    public void set1(Buf k){
        b = k;
    }


    public void f1(C a){
        a = new C();
    }


    public void run(){
        C a;
        try{

        a = new C();
        b.l3();
        synchronized(b){
            b.l4();
            b.wait();
            this.f1(a);
            b.l5();
        }
        b.l6();
    }catch (Exception e){}

    }
}