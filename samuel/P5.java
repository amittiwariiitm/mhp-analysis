class P5{

    public static void main(String []args){

    T t1;
    T t2;
    T t3;
    T t4;

    T a;
    T b;

    boolean p;
    boolean q;

    Buf l;
    Buf k;

    try {

    t1 = new T();
    t2 = new T();
    t3 = new T();
    t4 = new T();

    l = new Buf();
    k = new Buf();

    t1.set(l);
    t2.set(k);
    t3.set(l);
    t4.set(k);

    p= true;
    q = false;

    if(p){
        a = t1;
    }else{
        a = t2;
    }

    if(q){
        b = t3;
    }else{
        b = t4;
    }



    a.start();

    l.l1();

    b.start();

    l.l2();

        a.join();

        l.l5();

        b.join();

        l.l6();
    }catch (Exception e){}

    }

    
}

class Buf{
    public void l1(){};
    public void l2(){};
    public void l3(){};
    public void l4(){};
    public void l5(){};
    public void l6(){};
}


class T extends Thread{
    Buf b;

public void set(Buf k){
    b = k;
    }

public int add(int a, int b){
    int c;
    c =  a + b;
    return c;
}

public int some(){
    int c;
    c = 20;
    return c;
}

public void run(){
    A obj;
    int a;
    int d;
    int res;

    try{
        b.l3();
        synchronized(b){
            a = some(); 
        }
        b.l4();

        // a = 10;
        // d = 20;
        // res = add(a, d);
    }catch (Exception e){}

    }
}