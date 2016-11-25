package nachos.threads;

import java.util.LinkedList;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.//Nachos�����Ṥ���������ڳ���һ��ʱ��ʱ
     */
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
    });
    }
    private LinkedList<Waiter> waitList =  new LinkedList<Waiter>();
    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {//ÿ500ticks�Զ�����һ�Σ��̺߳�TCB���л�
    			Waiter waiter = null;	    		
    			for(int i= 0;i<waitList.size();i++){
    				waiter=waitList.remove();
//        			System.out.println(waiter.thread.getName());
        			//System.out.println(waiter.wakeTime);
        			if(waiter.wakeTime<= Machine.timer().getTime()){
        				//System.out.println("jklkljlk");
    	    			System.out.println(""+waiter.thread.getName()+"  ��ʱ�䣺"+Machine.timer().getTime()+"  ����");          
    	    			waiter.thread.ready();//�߳̽������״̬     
        			}else{
        				waitList.add(waiter);
        			}
//        			  
//        			System.out.println(waitList.size());
    			}  		   		      		    	
//    	KThread.currentThread().yield();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
	// for now, cheat just to get something working (busy waiting is bad)
	//long wakeTime = Machine.timer().getTime() + x;
	//while (wakeTime > Machine.timer().getTime())
	  //  KThread.yield();
    	boolean intStatus = Machine.interrupt().disable(); //ϵͳ���ж�      
    	long wakeTime = Machine.timer().getTime() + x;     //ȷ�����ѵ�ʱ��     
    	Waiter waiter = new Waiter(wakeTime,KThread.currentThread());       	
    	waitList.add(waiter);  //���̼߳��뵽�ȴ�������  
    	System.out.println(KThread.currentThread().getName() +" �߳����ߣ�ʱ��Ϊ��"+Machine.timer().getTime()+",Ӧ��"+wakeTime+"����.");       
    	KThread.sleep();        
    	Machine.interrupt().restore(intStatus); 
    }
    
    class Waiter{
    	Waiter(long wakeTime,KThread thread){
    		this.wakeTime = wakeTime;
    		this.thread = thread;
    	}
    	private long wakeTime;
    	private KThread thread;
    }    
   
}

//package nachos.threads;
//
//import java.util.TreeSet;
//
//import com.sun.org.apache.regexp.internal.recompile;
//import com.sun.org.apache.xerces.internal.util.SynchronizedSymbolTable;
//
//import nachos.machine.*;
//
///**
// * Uses the hardware timer to provide preemption, and to allow threads to sleep
// * until a certain time.
// */
//public class Alarm {
//	private TreeSet<WaitingThread> waiting;
//
//	/**
//	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
//	 * alarm's callback.
//	 *
//	 * <p>
//	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
//	 */
//	public Alarm() {
//
//		// ��ʼ��waiting
//		waiting = new TreeSet<WaitingThread>();
//
//		Machine.timer().setInterruptHandler(new Runnable() {
//			public void run() {
//				timerInterrupt();
//			}
//		});
//	}
//
//	/**
//	 * The timer interrupt handler. This is called by the machine's timer
//	 * periodically (approximately every 500 clock ticks). Causes the current
//	 * thread to yield, forcing a context switch if there is another thread that
//	 * should be run.
//	 */
//	public void timerInterrupt() {
//		//System.out.println("timeinterupt" + Machine.timer().getTime());
//		// KThread.currentThread().yield();
//		long time = Machine.timer().getTime();
//
//		/**
//		 * ���ȴ�����Ϊ�յ�ʱ�򣬷���
//		 */
//		if (waiting.isEmpty()) {
//			return;
//		}
//
//		/**
//		 * ���ȴ����еĵ�һ��Ԫ�ص����¿���ʱ����ڵ�ǰʱ�䣬����
//		 */
//		if (waiting.first().time > time) {
//			return;
//		}
//		/**
//		 * �����в�Ϊ�գ����ҵ�һ��Ԫ�ص����¿���ʱ��С�ڵ�ǰʱ�䣬�ӵ�ǰ�ȴ��������ÿ��� �ŵ�����������
//		 */
//		while (!(waiting.isEmpty()) && (waiting.first().time <= time)) {
//			WaitingThread next = waiting.first();
//
//			next.thread.ready();
//			// System.out.println(Machine.timer().getTime()+"ready+1");
//			waiting.remove(next);
//			// System.out.println("next.time="+next.time+"currentime="+time);
//
//			Lib.assertTrue(next.time <= time);
//
//		}
//
//	}
//
//	/**
//	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
//	 * in the timer interrupt handler. The thread must be woken up (placed in
//	 * the scheduler ready set) during the first timer interrupt where
//	 *
//	 * <p>
//	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
//	 *
//	 * @param x
//	 *            the minimum number of clock ticks to wait.
//	 *
//	 * @see nachos.machine.Timer#getTime()
//	 * 
//	 *      ʵ���������
//	 */
//
//	public void waitUntil(long x) {
//		System.out.println("daozhelile");
//		// ����Ӧ�ñ�����awake ��ʱ��
//		long wakeTime = Machine.timer().getTime() + x;
//
//		//���ж�
//		boolean intStatus = Machine.interrupt().disable();
//         
//		//�ȴ��߳�
//		WaitingThread toAlarm = new WaitingThread(wakeTime, KThread.currentThread());
//		
//		/*
//		 * task3ʹ�ã�project2ɾ��
//		 */
//		//System.out.println("Wait thread " + KThread.currentThread().getName()+" "+Machine.timer().getTime()+" begin sleep " + " until " + wakeTime);
//		
//		
//		//����ǰӦ�ó�˯���̷߳ŵ��ȴ������У��ȴ�timeinterrupt��ʱ����
//		waiting.add(toAlarm);
//		KThread.sleep();
//
//		Machine.interrupt().restore(intStatus);
//
//	}
//
//	/**
//	 * �ȴ����У����Զ������ �ȴ�ʱ��
//	 * 
//	 * @author kingwen
//	 * �Զ���һ�����ݽṹWaitingThread���������ǵ��̶߳������Ҫ���ѵ�ʱ��
//	 */
//	private class WaitingThread implements Comparable {
//
//		public WaitingThread(long time, KThread thread) {
//			this.time = time;
//			this.thread = thread;
//		}
//		
//		/**
//		 * ��д��compareTo������
//		 * �ں��ڶ�����ӵ�ʱ�����ʵ�ִ�С��������
//		 * �����ں��ڴӶ�����ȡ��
//		 */	
//		
//		@Override
//		public int compareTo(Object arg0) {
//			WaitingThread toOccur = (WaitingThread) arg0;
//			if (time < toOccur.time)
//				return -1;
//			else if (time > toOccur.time)
//				return 1;
//			else
//				return thread.compareTo(toOccur.thread);
//		}
//		long time;
//		KThread thread;
//		
//	}
//
//}