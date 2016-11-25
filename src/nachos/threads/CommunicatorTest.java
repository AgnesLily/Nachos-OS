package nachos.threads;

import nachos.machine.*;
public class CommunicatorTest {
	public CommunicatorTest() {
		Message = 1;
		numOfSpeakers = 5;
		numOfListeners = 5;
		communicator = new Communicator();
	}
	
	public void commTest(int num){
		for(int i = 1;i<=5;i++){
			createListeners(i);
			createSpeakers(i);			
			sleep(1);
		}	
			
	}
	public void sleep(int numThreadsCreated){   
		ThreadedKernel.alarm.waitUntil(numThreadsCreated*100);    
	} 
	public class Listener implements Runnable{        
		public void run(){             
			int messageToRecieve = communicator.listen();        
			System.out.println("   " + KThread.currentThread().getName() + " 收到信息 " + messageToRecieve); 
		}    
	}
	public class Speaker implements Runnable{        
		public void run(){             
			communicator.speak(Message++);  			  			
		}     
	}
	public void createSpeakers(int speakers){         
		int j;          
		//for(j=1; j<=speakers; j++){  
			KThread speakerThread = new KThread(new Speaker());    
			speakerThread.setName("Speaker" + speakers);                     
			speakerThread.fork();           
		//};    
	}
	public void createListeners(int listeners){  
		int k;       
		//for(k=1; k<=listeners; k++){  
			KThread listenerThread = new KThread(new Listener());                
			listenerThread.setName("Listener" + listeners);  
			listenerThread.fork();  
		//}  
	} 

	private static int Message;      
	private Communicator communicator;     
	private int numOfSpeakers;    
	private int numOfListeners;
}
