package nachos.threads;

import nachos.machine.*;

/**
 * A KThread is a thread that can be used to execute Nachos kernel code. Nachos
 * allows multiple threads to run concurrently.
 *
 * To create a new thread of execution, first declare a class that implements
 * the <tt>Runnable</tt> interface. That class then implements the <tt>run</tt>
 * method. An instance of the class can then be allocated, passed as an
 * argument when creating <tt>KThread</tt>, and forked. For example, a thread
 * that computes pi could be written as follows:
 *
 * <p><blockquote><pre>
 * class PiRun implements Runnable {
 *     public void run() {
 *         // compute pi
 *         ...
 *     }
 * }
 * </pre></blockquote>
 * <p>The following code would then create a thread and start it running:
 *
 * <p><blockquote><pre>
 * PiRun p = new PiRun();
 * new KThread(p).fork();
 * </pre></blockquote>
 */
public class KThread {
    /**
     * Get the current thread.
     *
     * @return	the current thread.
     */
    public static KThread currentThread() {
	Lib.assertTrue(currentThread != null);
	return currentThread;
    }
    
    /**
     * Allocate a new <tt>KThread</tt>. If this is the first <tt>KThread</tt>,
     * create an idle thread as well.//为什么要创建一个空闲线程
     */
    public KThread() {
	if (currentThread != null) {
	    tcb = new TCB();//每个线程都需要一个TCB来控制
	}	    
	else {
	    readyQueue = ThreadedKernel.scheduler.newThreadQueue(false);
	    readyQueue.acquire(this);	    

	    currentThread = this;
	    tcb = TCB.currentTCB();
	    name = "main";
	    restoreState();

	    createIdleThread();
	}
    }

    /**
     * Allocate a new KThread.
     *
     * @param	target	the object whose <tt>run</tt> method is called.
     */
    public KThread(Runnable target) {
	this();
	this.target = target;
    }

    /**
     * Set the target of this thread.
     *
     * @param	target	the object whose <tt>run</tt> method is called.
     * @return	this thread.
     */
    public KThread setTarget(Runnable target) {
	Lib.assertTrue(status == statusNew);
	
	this.target = target;
	return this;
    }

    /**
     * Set the name of this thread. This name is used for debugging purposes
     * only.
     *
     * @param	name	the name to give to this thread.
     * @return	this thread.
     */
    public KThread setName(String name) {
	this.name = name;
	return this;
    }

    /**
     * Get the name of this thread. This name is used for debugging purposes
     * only.
     *
     * @return	the name given to this thread.
     */     
    public String getName() {
	return name;
    }

    /**
     * Get the full name of this thread. This includes its name along with its
     * numerical ID. This name is used for debugging purposes only.
     *
     * @return	the full name given to this thread.
     */
    public String toString() {
	return (name + " (#" + id + ")");
    }

    /**
     * Deterministically and consistently compare this thread to another
     * thread.
     */
    public int compareTo(Object o) {
	KThread thread = (KThread) o;

	if (id < thread.id)
	    return -1;
	else if (id > thread.id)
	    return 1;
	else
	    return 0;
    }

    /**
     * Causes this thread to begin execution. The result is that two threads
     * are running concurrently: the current thread (which returns from the
     * call to the <tt>fork</tt> method) and the other thread (which executes
     * its target's <tt>run</tt> method).
     */
    public void fork() {
	Lib.assertTrue(status == statusNew);
	Lib.assertTrue(target != null);
	
	Lib.debug(dbgThread,
		  "Forking thread: " + toString() + " Runnable: " + target);
	
	boolean intStatus = Machine.interrupt().disable();//关中断

	tcb.start(new Runnable() {
		public void run() {
//			System.out.println("进入run");
		    runThread();
		}
	    });//在这里面已经创建了java线程了

	ready();
	
	Machine.interrupt().restore(intStatus);//idle的时候还是false，之后都是enable开中断
	
	//System.out.println(KThread.currentThread().name);
	
    }

    private void runThread() {
	begin();
	target.run();//这个target是谁到底，new PingTest实例
	finish();
    }

    private void begin() {
	Lib.debug(dbgThread, "Beginning thread: " + toString());
	
	Lib.assertTrue(this == currentThread);//这个地方的this指谁！！！！！

	restoreState();

	Machine.interrupt().enable();//开中断 
    }

    /**
     * Finish the current thread and schedule it to be destroyed when it is
     * safe to do so. This method is automatically called when a thread's
     * <tt>run</tt> method returns, but it may also be called directly.
     *
     * The current thread cannot be immediately destroyed because its stack and
     * other execution state are still in use. Instead, this thread will be
     * destroyed automatically by the next thread to run, when it is safe to
     * delete this thread.
     */
    /*
    public static void finish() {
    //System.out.println("结束了");
	Lib.debug(dbgThread, "Finishing thread: " + currentThread.toString());
	
	Machine.interrupt().disable();

	Machine.autoGrader().finishingCurrentThread();

	Lib.assertTrue(toBeDestroyed == null);
	toBeDestroyed = currentThread;


	currentThread.status = statusFinished;
	
	sleep();
    }
   */
    
    public static void finish() {
    	Lib.debug(dbgThread, "Finishing thread: " + currentThread.toString());
    	
    	Machine.interrupt().disable();

    	Machine.autoGrader().finishingCurrentThread();

    	Lib.assertTrue(toBeDestroyed == null);
    	toBeDestroyed = currentThread;


    	currentThread.status = statusFinished;
    	KThread joinedKThread;
        if (currentThread.joinQueue != null)
           while ((joinedKThread = currentThread.joinQueue.nextThread()) != null)
                joinedKThread.ready();
    	sleep();
        }
        

    /**
     * Relinquish the CPU if any other thread is ready to run. If so, put the
     * current thread on the ready queue, so that it will eventually be
     * rescheuled.
     *
     * <p>
     * Returns immediately if no other thread is ready to run. Otherwise
     * returns when the current thread is chosen to run again by
     * <tt>readyQueue.nextThread()</tt>.
     *
     * <p>
     * Interrupts are disabled, so that the current thread can atomically add
     * itself to the ready queue and switch to the next thread. On return,
     * restores interrupts to the previous state, in case <tt>yield()</tt> was
     * called with interrupts disabled.
     */
    public static void yield() {
	Lib.debug(dbgThread, "Yielding thread: " + currentThread.toString());
	//System.out.println("Yielding thread: " + currentThread.toString());
//	System.out.println("Yielding thread: " + currentTCB.toString());
	Lib.assertTrue(currentThread.status == statusRunning);
	
	boolean intStatus = Machine.interrupt().disable();//关中断

	currentThread.ready();

	runNextThread();
	
	Machine.interrupt().restore(intStatus);//开中断
    }

    /**
     * Relinquish the CPU, because the current thread has either finished or it
     * is blocked. This thread must be the current thread.
     *
     * <p>
     * If the current thread is blocked (on a synchronization primitive, i.e.
     * a <tt>Semaphore</tt>, <tt>Lock</tt>, or <tt>Condition</tt>), eventually
     * some thread will wake this thread up, putting it back on the ready queue
     * so that it can be rescheduled. Otherwise, <tt>finish()</tt> should have
     * scheduled this thread to be destroyed by the next thread to run.
     */
    public static void sleep() {
	Lib.debug(dbgThread, "Sleeping thread: " + currentThread.toString());
	
	Lib.assertTrue(Machine.interrupt().disabled());

	if (currentThread.status != statusFinished)
	    currentThread.status = statusBlocked;
	//System.out.println("执行下一个线程");
	runNextThread();
	
    }

    /**
     * Moves this thread to the ready state and adds this to the scheduler's
     * ready queue.
     */
    public void ready() {
	Lib.debug(dbgThread, "Ready thread: " + toString());
	
	Lib.assertTrue(Machine.interrupt().disabled());
	Lib.assertTrue(status != statusReady);
	
	status = statusReady;
	if (this != idleThread)
	    readyQueue.waitForAccess(this);
	
	Machine.autoGrader().readyThread(this);
    }

    /**
     * Waits for this thread to finish. If this thread is already finished,
     * return immediately. This method must only be called once; the second
     * call is not guaranteed to return. This thread must not be the current
     * thread.
     */
    
    public void join() {
	Lib.debug(dbgThread, "Joining to thread: " + toString());

	Lib.assertTrue(this != currentThread);//确保要join的线程不是线程本身
	
	if(this.status == statusFinished)
		return;
	boolean intStatus = Machine.interrupt().disable();
	if (joinQueue == null) {
          joinQueue = ThreadedKernel.scheduler.newThreadQueue(true);
           // Notify this thread queue that a thread has received access,without going through request() and nextThread()
          joinQueue.acquire(this);
	 }
	 joinQueue.waitForAccess(currentThread);
	  KThread.sleep();
	  Machine.interrupt().restore(intStatus);
    }
    
    /*
    public void join() {
    	Lib.debug(dbgThread, "Joining to thread: " + toString());

    	Lib.assertTrue(this != currentThread);

        }
*/
    /**
     * Create the idle thread. Whenever there are no threads ready to be run,
     * and <tt>runNextThread()</tt> is called, it will run the idle thread. The
     * idle thread must never block, and it will only be allowed to run when
     * all other threads are blocked.
     *
     * <p>
     * Note that <tt>ready()</tt> never adds the idle thread to the ready set.
     */
    private static void createIdleThread() {
	Lib.assertTrue(idleThread == null);
	
	idleThread = new KThread(new Runnable() {
	    public void run() { while (true) yield(); }
	});
	idleThread.setName("idle");

	Machine.autoGrader().setIdleThread(idleThread);
	
	idleThread.fork();
    }
    
    /**
     * Determine the next thread to run, then dispatch the CPU to the thread
     * using <tt>run()</tt>.
     */
    private static void runNextThread() {
	KThread nextThread = readyQueue.nextThread();
	//System.out.println(nextThread);
	if (nextThread == null)
	    nextThread = idleThread;

	nextThread.run();
    }

    /**
     * Dispatch the CPU to this thread. Save the state of the current thread,
     * switch to the new thread by calling <tt>TCB.contextSwitch()</tt>, and
     * load the state of the new thread. The new thread becomes the current
     * thread.
     *
     * <p>
     * If the new thread and the old thread are the same, this method must
     * still call <tt>saveState()</tt>, <tt>contextSwitch()</tt>, and
     * <tt>restoreState()</tt>.
     *
     * <p>
     * The state of the previously running thread must already have been
     * changed from running to blocked or ready (depending on whether the
     * thread is sleeping or yielding).
     *
     * @param	finishing	<tt>true</tt> if the current thread is
     *				finished, and should be destroyed by the new
     *				thread.
     */
    private void run() {
	Lib.assertTrue(Machine.interrupt().disabled());

	Machine.yield();

	currentThread.saveState();
	//System.out.println("name "+currentThread.name+"     status "+currentThread.status);
	Lib.debug(dbgThread, "Switching from: " + currentThread.toString()
		  + " to: " + toString());
	//System.out.println("Switching from: " + currentThread.toString()
	  //+ " to: " + toString());
	currentThread = this;

	tcb.contextSwitch();

	currentThread.restoreState();
    }

    /**
     * Prepare this thread to be run. Set <tt>status</tt> to
     * <tt>statusRunning</tt> and check <tt>toBeDestroyed</tt>.
     */
    protected void restoreState() {
	Lib.debug(dbgThread, "Running thread: " + currentThread.toString());
	
	Lib.assertTrue(Machine.interrupt().disabled());
	Lib.assertTrue(this == currentThread);
	Lib.assertTrue(tcb == TCB.currentTCB());

	Machine.autoGrader().runningThread(this);
	
	status = statusRunning;

	if (toBeDestroyed != null) {
	    toBeDestroyed.tcb.destroy();
	    toBeDestroyed.tcb = null;
	    toBeDestroyed = null;
	}
    }

    /**
     * Prepare this thread to give up the processor. Kernel threads do not
     * need to do anything here.
     */
    protected void saveState() {
	Lib.assertTrue(Machine.interrupt().disabled());
	Lib.assertTrue(this == currentThread);
    }

    private static class PingTest implements Runnable {
	PingTest(int which) {
	    this.which = which;
	}
	
	public void run() {
	    for (int i=0; i<5; i++) {
		    String name = currentThread.name;
			System.out.println(name+"  thread " + which + " looped "
					   + i + " times");
			System.out.println(Interrupt.privilege.stats.totalTicks);
			//System.out.println(numCreated);
			currentThread.yield();
			//System.out.println(Interrupt.privilege.stats.totalTicks);
			//ThreadedKernel.alarm.waitUntil(3000);
	    }	    
//	    KThreadTest.simpleJoinTest();
	    
	}	
	private int which;
    }

    public static class joinTest implements Runnable{
		joinTest(int which){
			this.which = which;
		}
		
		public void run(){
			 for (int i=0; i<5; i++) {
			    String name = currentThread.name;
				System.out.println(name+"  thread " + which + " looped "
						   + i + " times");
				currentThread.yield();
		    }	 
		}	
		private int which;
	}
    
    private static class Condition2Test {
    	public Condition2Test(){		
    	}
    	
    	public void con2test(){
    		KThread thread1 = new KThread(new Runnable(){		
    			public void run(){
    				System.out.println("thread1 goes to sleep");
    				ctLock.acquire();
    				condition.sleep();
    				System.out.println("thread1 requires lock");
    				ctLock.release();
    				System.out.println("thread1 is woken");
    				//count.save(20);
    			}
    		});
    		
    		KThread thread2 = new KThread(new Runnable(){
    			public void run(){
    				System.out.println("thread2 goes to sleep");
    				ctLock.acquire();
    				condition.sleep();
    				System.out.println("thread2 requires lock");
    				ctLock.release();
    				System.out.println("thread2 is woken");
    				//count.save(20);
    			}
    		});
    		
    		KThread thread3 = new KThread(new Runnable(){
    			public void run(){
    				System.out.println("thread3 waking up all threads");
    				ctLock.acquire();
    				condition.wakeAll();				
    				ctLock.release();
    				System.out.println("thread1 and thread2 woken up");
    			}
    		});
    		
    		thread1.fork();
    		thread2.fork();
    		thread3.fork();
    		thread1.join();
    		thread2.join();
    		thread3.join();	
    	}    	
    	private Lock ctLock = new Lock();
    	private Condition2 condition = new Condition2(ctLock);
    }

    private static class AlarmTest implements Runnable {
    	AlarmTest(int which){
    		this.which = which;
    	}
    	public void run(){
    		for (int i=0; i<5; i++) {
    			KThread thread = new KThread(new Runnable(){
    				public void run() {
    					ThreadedKernel.alarm.waitUntil(1000);
    				}
    			}).setName("thread："+i);
    			thread.fork();   	
    	    }
    		ThreadedKernel.alarm.waitUntil(3000);
    	}
    	private int which; 	
    }

    /**
     * Tests whether this module is working.
     */
    public static void selfTest() {
	Lib.debug(dbgThread, "Enter KThread.selfTest");
	
	/*default*/
    //new KThread(new PingTest(1)).setName("fork").fork();
	//new PingTest(0).run();
	
	task1();
	
	/*task 2*/
    task2();
      
	/*task 3*/
    task3();
	
	
	/*task 4*/
 	task4();
	
	/*task 5*/
	task5();
	
	/*task 6*/
	task6();
    }
    
    private static void task1(){
    	 System.out.println("**************task1  测试开始***************");
    	 KThread jointhread = new KThread(new joinTest(1)).setName("join");
    	 jointhread.fork();
    	 jointhread.join();//jointhread join主线程
    	 new joinTest(0).run();
    }
    
    private static void task2(){
    	System.out.println("**************task2  测试开始***************");
    	Condition2Test con = new Condition2Test();
    	con.con2test();
    }
    
    private static void task3(){
    	System.out.println("**************task3  测试开始***************");
//    	new KThread(new AlarmTest(1)).setName("testAlarm").fork(); 	
    	new AlarmTest(0).run();
    }
    
    private static void task4(){
    	System.out.println("**************task4  测试开始***************");
    	new CommunicatorTest().commTest(1);
    }
    private static void task5(){
    	System.out.println("**************task5  测试开始***************");
    	PriorityScheduler priority = new PriorityScheduler();
//    	ThreadedKernel.scheduler.setPriority(currentThread(), 1);
    	KThread thread1 = new KThread(new Runnable() {
    	    public void run() {
    	    	System.out.println("1");
//    	        currentThread().yield();
    	    }
    	});
    	
    	KThread thread2 = new KThread(new Runnable() {
    	    public void run() {  	    	
    	    	System.out.println("2");  
    	    	//currentThread().yield();
    	    }
    	});
    	
    	KThread thread3 = new KThread(new Runnable() {
    	    public void run() {
//    	    	currentThread().yield();
    	    	System.out.println("3");
    	    	
    	    }
    	});
    	
    	boolean intStatus = Machine.interrupt().disable();
    	
    	ThreadedKernel.scheduler.setPriority(thread1, 3);
    	ThreadedKernel.scheduler.setPriority(thread2, 4);
    	ThreadedKernel.scheduler.setPriority(thread3, 5);
    	priority.setPriority(thread1, 3);
    	priority.setPriority(thread2, 4);
    	priority.setPriority(thread3, 1);
    	priority.setPriority(currentThread,1);
    	
//    	priority.getThreadState(thread1).waited.acquire(thread2);
		Machine.interrupt().restore(intStatus);

    	thread1.setName("thread1").fork();
    	thread2.setName("thread2").fork();
    	thread3.setName("thread3").fork();
    	
//    	priority.getThreadState(thread1).waitForAccess(priority.getThreadState(thread2).waitQueue);
    	currentThread().yield();
    	System.out.println("main");
    }
    
    private static void task6(){
    	System.out.println("**************task6  测试开始***************");
    	Boat boat = new Boat();
    	boat.selfTest();
    }
    private static final char dbgThread = 't';

    /**
     * Additional state used by schedulers.
     *
     * @see	nachos.threads.PriorityScheduler.ThreadState
     */
    public Object schedulingState = null;

    private static final int statusNew = 0;
    private static final int statusReady = 1;
    private static final int statusRunning = 2;
    private static final int statusBlocked = 3;
    private static final int statusFinished = 4;

    /**
     * The status of this thread. A thread can either be new (not yet forked),
     * ready (on the ready queue but not running), running, or blocked (not
     * on the ready queue and not running).
     */
    private int status = statusNew;
    private String name = "(unnamed thread)";
    private Runnable target;
    private TCB tcb;

    /**
     * Unique identifer for this thread. Used to deterministically compare
     * threads.
     */
    private int id = numCreated++;
    /** Number of times the KThread constructor was called. */
    private static int numCreated = 0;//KThread构造器被调用的次数

    private static ThreadQueue readyQueue = null;
    private static ThreadQueue joinQueue = null;//被join的线程队列
    private static KThread currentThread = null;
    private static KThread toBeDestroyed = null;
    private static KThread idleThread = null;

}
