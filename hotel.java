//Luca Donadello - lxd210013 - project 2 - Hotel threads

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.Random;

public class hotel{

    public static int NUMGUESTS = 25;   //constant for number of guests
    public static int NUMBELLHOPS = 2;  //constant for number of bellhops
    public static int NUMEMPLOYEE = 2;  //constant for number of employers
    public static int roomnum = 0;      //number of room
    public static int joinCounter = 0;  //number to count how many thread joined

    public static Queue<Guest> guest_q = new LinkedList<Guest>();           //guest queue
    public static Queue<Guest> bellhop_guest_q = new LinkedList<Guest>();   //bellhop queue interact with guest
    public static Queue<Bellhop> bellhop_q = new LinkedList<Bellhop>();     //bellhop queue
    public static int empID[];

    //list of semaphores
    public static Semaphore mutex = new Semaphore(1, true);             //mutex for mutual exclusion
    public static Semaphore mutex1 = new Semaphore(1, true);            //mutex for mutual exclusion
    public static Semaphore mutex2 = new Semaphore(1, true);            //mutex for mutual exclusion
    public static Semaphore employerReady = new Semaphore(2, true);     //how many employee ready at the time
    public static Semaphore guestReady = new Semaphore(0, true);        //how many guests ready to be served
    public static Semaphore bellhopRequest = new Semaphore(0, true);    //request for bellhop  
    public static Semaphore bellhopReady = new Semaphore(2, true);      //how many bellhop are ready
    public static Semaphore bellhopDone = new Semaphore(0, true);       //how may bellhops are done
    public static Semaphore guestReceivedRoom[];                        //list for how many guests recived the room

    public static void main(String[] args){

        System.out.println("Simulation starts");
        empID = new int [NUMGUESTS];
        
        //creating semaphores for each guest 
        guestReceivedRoom = new Semaphore[NUMGUESTS];

        for(int i = 0 ; i < NUMGUESTS ; i++)
            guestReceivedRoom[i] = new Semaphore(0,true);
        
        //creating threads
        for(int i = 0 ; i < NUMEMPLOYEE ; i++)
		    new Employer(i);
        
        for(int i = 0 ; i < NUMBELLHOPS ; i++)
		    new Bellhop(i);
        
        for(int i = 0 ; i < NUMGUESTS ; i++)
		    new Guest(i);

        while(true){
            if(joinCounter == NUMGUESTS){
                System.out.println("Simulation ends");
                System.exit(0);
            }
        }
        
	}

    //guest class + thread
    public static class Guest implements Runnable{

        //guest variables
        public int id;
        public int roomnum;
        public int numbags;
        public Thread guest;

        //assign guest 
        public Guest(int id){
            this.id = id;
            Random random = new Random();
            numbags = random.nextInt((5 - 0) + 1);
            System.out.println("Guest " + id + " created");
            guest = new Thread(this);
            guest.start();
        }

        //thread function
        public void run(){
            try{
                //wait for creation of all the guests
                System.out.print("Guest " + id + " entered with " + numbags + " bags\n");
                mutex.acquire();                    //mutal exclusion for queue add
                guest_q.add(this);  
                mutex.release();                    //mutal exclusion for queue add
                employerReady.acquire();            //wait for employer to be ready
                guestReady.release();               //guest is ready to be processed
                guestReceivedRoom[id].acquire();    //wait to receive the room 
                System.out.print("Guest " + id + " receives room key for room " + this.roomnum + " from front desk employee " + empID[id] + "\n");
                if(numbags > 2){
                    System.out.print("Guest " + id + " requests help with bags\n");
                    mutex.acquire();                        //mutal exclusion for queue add
                    bellhop_guest_q.add(this);
                    mutex.release();                        //mutal exclusion for queue add
                    bellhopReady.acquire();                 //bellhop request
                    bellhopRequest.release();               //bellhop processing
                    bellhopDone.acquire();                  //bellhop done
                    mutex.acquire();                        //mutual exclusion queue remove
                    Bellhop bellhop = bellhop_q.remove();   
                    mutex.release();                        //mutual exclusion queue remove
                    System.out.print("Guest " + id + " receives bags from bellhop " + bellhop.id + " and gives tip\n");
                    bellhopReady.release();                 //bellhop ready again
                }
                System.out.print("Guest " + id + " enters room " + roomnum + "\n");
                System.out.print("Guest " + id + " retires for the evening\n");
                System.out.print("Guest " + id + " joined\n");
                mutex.acquire();                            //increase counter mutual exclusion
                joinCounter = joinCounter + 1;
                mutex.release();                            //increase counter mutual exclusion
                guest.join();
            }
            catch(InterruptedException e){
                System.out.println("Error");
            }
        }
    }

    public static class Employer implements Runnable{

        //variables for employer
        public int id;
        public Thread employer;

        //assign to employer
        public Employer(int id){
            this.id = id;
            System.out.print("Front desk employee " + id + " created\n");
            employer = new Thread(this);
            employer.start();
        }

        //employer thread
        public void run(){
            try{
                while(true){
                    guestReady.acquire();                   //wait for guest
                    mutex2.acquire();                       //one at the time
                    Guest guest = guest_q.remove();
                    roomnum = roomnum+1;
                    guest.roomnum = roomnum;                //room assign
                    mutex2.release();
                    System.out.print("Front desk employer " + id + " registers guest " + guest.id + " and assigns room " + roomnum + "\n");
                    empID[guest.id] = id;
                    guestReceivedRoom[guest.id].release();  //recieved room
                    employerReady.release();                //ready again
                }
            }
            catch(InterruptedException e){
                System.out.println("Error");
            }
        }
    }

    public static class Bellhop implements Runnable{

        public int id;
        public int roomnum;
        public int numbags;
        public Thread bellhop;

        public Bellhop(int id){
            this.id = id;
            System.out.print("Bellhop " + id + " created\n");
            bellhop = new Thread(this);
            bellhop.start();
        }

        public void run(){
            try{
                while(true){
                    bellhopRequest.acquire();                   //wait for request
                    mutex.acquire();                            //mutual exclusion queue remove
                    Guest guest = bellhop_guest_q.remove();
                    mutex.release();                            //mutual exclusion queue remove
                    System.out.print("Bellhop " + id + " receives bags from guest " + guest.id + "\n");
                    mutex.acquire();                            //mutual exclusion queue add
                    bellhop_q.add(this);
                    mutex.release();                            //mutual exclusion queue add
                    System.out.print("Bellhop " + id + " delivers bags to guest " + guest.id + "\n");
                    bellhopDone.release();                      //bellhop ready again
                }
            }
            catch(InterruptedException e){
                System.out.println("Error");
            }
        }
    }
}