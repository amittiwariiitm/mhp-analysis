    class P1{
        public static void main(String []args){        

            T t1;
            T t2;
            Buf l;
            int a;
                
            try {

            l = new Buf();
            t1 = new T();
            t2 = new T();
            t1.set(l);
            t2.set(l);

            t1.start();
            
            t1.p5();

            t2.start();

            a = 10;
            a = l.f1(a) ;;

            t1.join();
            t2.join();

            } catch (Exception e){}

        }

        
    }

    class Buf{
        public int f1(int a){
            int t;
            t = 20;
            a = a +t;
            return a;
        }
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


        public void f2(int a){
            a = 20; 

        }

        public void run(){
            int a;
            try{

            a =10;
            this.p3();
            synchronized(b){
                this.f2(a);
            }
            this.p4();
        }catch (Exception e){}

        }

        public void p3(){}
        public void p4(){}
        public void p5(){}
    }