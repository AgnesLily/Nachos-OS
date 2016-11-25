package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;    
    static int childOnO;
    static int adultOnO;
    static Semaphore mainSemaphore;
    static boolean boatOnO;
    static int boatSeated;
    static Lock lock;
    static Condition2 adultOnOCon;
    static Condition2 childOnOCon;
    static Condition2 childOnMCon;
    
    
    public static void selfTest()
    {
    	BoatGrader b = new BoatGrader();
		
//		System.out.println("Testing Boats with only 2 children");
//		begin(0, 2, b);
	
	    System.out.println("Testing Boats with 2 children, 1 adult");
        begin(1, 2, b);

//      System.out.println("Testing Boats with 3 children, 3 adults");
//      begin(3, 3, b);
	
//	System.out.println("***************Task6 Boat乘船测试结束 ***************");
		mainSemaphore.P();	
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
		bg = b;
	
		// Instantiate global variables here
		childOnO=children;
		adultOnO=adults;
		mainSemaphore=new Semaphore(0);
		boatOnO=true;
		boatSeated=0;
		lock=new Lock();
		adultOnOCon=new Condition2(lock);
		childOnMCon=new Condition2(lock);
		childOnOCon=new Condition2(lock);
	
		// Create threads here. See section 3.4 of the Nachos for Java
		for(int i=0;i<children;i++){
			Runnable r = new Runnable() {
			    public void run() {
	                ChildItinerary();
	            }
		    };
		    KThread t = new KThread(r).setName("Child Thread "+i);
		    t.fork();
		}
		
		for(int i=0;i<adults;i++){
			Runnable r = new Runnable() {
			    public void run() {
	                AdultItinerary();
	            }
		    };
		    KThread t = new KThread(r).setName("Adult Thread "+i);
		    t.fork();
		}
		mainSemaphore.P();   
		System.out.println("******Boat Test ends!******"); 
	// Walkthrough linked from the projects page.

	/*Runnable r = new Runnable() {
	    public void run() {
                SampleItinerary();
            }
        };
        KThread t = new KThread(r);
        t.setName("Sample Boat Thread");
        t.fork();*/
	
	//waitMainThread.P();

	 
    }

    
    /**
     * 成年人乘船条件：船在O岛，当前船是空的，在O岛的孩子小于等于1个
     */
    static void AdultItinerary()
    {
	bg.initializeAdult(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE. 

	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/
		lock.acquire();
		while (!(boatOnO && boatSeated == 0 && childOnO <= 1)){
		adultOnOCon.sleep();
		}
	boatOnO=false;
	adultOnO--;
	bg.AdultRowToMolokai();
	if(childOnO==0&&adultOnO==0){
		mainSemaphore.V();//结束
	}else{
		childOnMCon.wake();//唤醒M岛的孩子
	}
	lock.release();
    
    }

    static void ChildItinerary()
    {
	bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE. 
	
	boolean isOnO=true;
	lock.acquire();
	while(true){
		if(isOnO){
			if(boatOnO&&boatSeated==0){//船为空
				isOnO=false;
				boatSeated++;
				childOnO--;
				bg.ChildRowToMolokai();//第一个上船的孩子划船
				
				if(childOnO>0){//如果O岛还有孩子
					childOnOCon.wake();
				}else{//O岛上没有孩子了
					boatOnO=false; 
					boatSeated=0;
					if(childOnO==0&&adultOnO==0){//O岛上既没有孩子也没有大人了，结束
						mainSemaphore.V();
						break;
					}else{//O岛上还有大人，唤醒M上的孩子
						childOnMCon.wake();
					}
				}
			}else if(boatOnO&&boatSeated==1){//船为半满
				isOnO=false;
				childOnO--;
				bg.ChildRideToMolokai();//第二个上船的孩子坐船
				boatOnO=false;
				boatSeated=0;
				if(childOnO==0&&adultOnO==0){//O岛上既没有孩子也没有大人了，结束
					mainSemaphore.V();
					break;
				}else{//O岛上还有大人
					childOnMCon.wake();//唤醒M岛上的孩子
				}
			}
			
			if(isOnO){//船满了
				childOnOCon.sleep();
			}else{//孩子不在O岛
				childOnMCon.sleep();
			}
		}else{//孩子在M岛
			if(!boatOnO){
				isOnO=true;
				childOnO++;
				bg.ChildRowToOahu();
				boatOnO=true;
				boatSeated=0;
				adultOnOCon.wake();
				childOnOCon.wake();
			}
			
			if(isOnO){//这个条件是什么意思
				childOnOCon.sleep();
			}else{
				childOnMCon.sleep();
			}
		}
	}
	lock.release();	
    }

    static void SampleItinerary()
    {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai();
	bg.ChildRideToMolokai();
    }
    
}

