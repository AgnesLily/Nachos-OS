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
     * alarm.//Nachos将不会工作正常，在超过一个时钟时
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
    public void timerInterrupt() {//每500ticks自动调用一次，线程和TCB的切换
    			Waiter waiter = null;	    		
    			for(int i= 0;i<waitList.size();i++){
    				waiter=waitList.remove();
//        			System.out.println(waiter.thread.getName());
        			//System.out.println(waiter.wakeTime);
        			if(waiter.wakeTime<= Machine.timer().getTime()){
        				//System.out.println("jklkljlk");
    	    			System.out.println(""+waiter.thread.getName()+"  在时间："+Machine.timer().getTime()+"  醒来");          
    	    			waiter.thread.ready();//线程进入就绪状态     
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
    	boolean intStatus = Machine.interrupt().disable(); //系统关中断      
    	long wakeTime = Machine.timer().getTime() + x;     //确定唤醒的时间     
    	Waiter waiter = new Waiter(wakeTime,KThread.currentThread());       	
    	waitList.add(waiter);  //将线程加入到等待队列上  
    	System.out.println(KThread.currentThread().getName() +" 线程休眠，时间为："+Machine.timer().getTime()+",应在"+wakeTime+"醒来.");       
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
//		// 初始化waiting
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
//		 * 当等待队列为空的时候，返回
//		 */
//		if (waiting.isEmpty()) {
//			return;
//		}
//
//		/**
//		 * 当等待队列的第一个元素的重新开启时间大于当前时间，返回
//		 */
//		if (waiting.first().time > time) {
//			return;
//		}
//		/**
//		 * 当队列不为空，而且第一个元素的重新开启时间小于当前时间，从当前等待队列中拿开， 放到就绪队列中
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
//	 *      实现这个方法
//	 */
//
//	public void waitUntil(long x) {
//		System.out.println("daozhelile");
//		// 计算应该被唤醒awake 的时间
//		long wakeTime = Machine.timer().getTime() + x;
//
//		//关中断
//		boolean intStatus = Machine.interrupt().disable();
//         
//		//等待线程
//		WaitingThread toAlarm = new WaitingThread(wakeTime, KThread.currentThread());
//		
//		/*
//		 * task3使用，project2删除
//		 */
//		//System.out.println("Wait thread " + KThread.currentThread().getName()+" "+Machine.timer().getTime()+" begin sleep " + " until " + wakeTime);
//		
//		
//		//将当前应该沉睡的线程放到等待队列中，等待timeinterrupt的时候唤醒
//		waiting.add(toAlarm);
//		KThread.sleep();
//
//		Machine.interrupt().restore(intStatus);
//
//	}
//
//	/**
//	 * 等待队列，有自定义变量 等待时间
//	 * 
//	 * @author kingwen
//	 * 自定义一个数据结构WaitingThread，保存我们的线程对象和需要唤醒的时间
//	 */
//	private class WaitingThread implements Comparable {
//
//		public WaitingThread(long time, KThread thread) {
//			this.time = time;
//			this.thread = thread;
//		}
//		
//		/**
//		 * 重写了compareTo方法。
//		 * 在后期队列添加的时候可以实现从小到大排列
//		 * 方便于后期从队列中取出
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