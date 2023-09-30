class P1spec{
    public static void main(String []args){
    try {
        T t1;
        T t2;
        Buf l;
        l = new Buf();
        t1 = new T();
        t2 = new T();
        t1.set(l);
        t2.set(l);

        t1.run();
        t2.run();

        t1.join();
        t2.join();
        }catch (Exception e){}

    }
}

class Buf{

}

class A{
    public Buf ret(){
        Buf c;
        c = new Buf();
        return c;
    }
}

class T extends Thread{
    Buf b;

    public void set(Buf k){
        b = k;
    }

    public void f1(int a){
        a = 10; 
    }


    public void f2(int a){

        a = 20; 
    }

    public void run(){
        int a;

        a =10;

        Buf c;
        c = new Buf();

        try{
            synchronized(b){
                f2(a);
            }
        }catch (Exception e){}

    }
}