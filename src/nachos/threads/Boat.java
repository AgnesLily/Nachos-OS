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
	
//	System.out.println("***************Task6 Boat�˴����Խ��� ***************");
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
     * �����˳˴�����������O������ǰ���ǿյģ���O���ĺ���С�ڵ���1��
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
		mainSemaphore.V();//����
	}else{
		childOnMCon.wake();//����M���ĺ���
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
			if(boatOnO&&boatSeated==0){//��Ϊ��
				isOnO=false;
				boatSeated++;
				childOnO--;
				bg.ChildRowToMolokai();//��һ���ϴ��ĺ��ӻ���
				
				if(childOnO>0){//���O�����к���
					childOnOCon.wake();
				}else{//O����û�к�����
					boatOnO=false; 
					boatSeated=0;
					if(childOnO==0&&adultOnO==0){//O���ϼ�û�к���Ҳû�д����ˣ�����
						mainSemaphore.V();
						break;
					}else{//O���ϻ��д��ˣ�����M�ϵĺ���
						childOnMCon.wake();
					}
				}
			}else if(boatOnO&&boatSeated==1){//��Ϊ����
				isOnO=false;
				childOnO--;
				bg.ChildRideToMolokai();//�ڶ����ϴ��ĺ�������
				boatOnO=false;
				boatSeated=0;
				if(childOnO==0&&adultOnO==0){//O���ϼ�û�к���Ҳû�д����ˣ�����
					mainSemaphore.V();
					break;
				}else{//O���ϻ��д���
					childOnMCon.wake();//����M���ϵĺ���
				}
			}
			
			if(isOnO){//������
				childOnOCon.sleep();
			}else{//���Ӳ���O��
				childOnMCon.sleep();
			}
		}else{//������M��
			if(!boatOnO){
				isOnO=true;
				childOnO++;
				bg.ChildRowToOahu();
				boatOnO=true;
				boatSeated=0;
				adultOnOCon.wake();
				childOnOCon.wake();
			}
			
			if(isOnO){//���������ʲô��˼
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

