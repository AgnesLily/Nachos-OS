package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }
    
    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	Lib.assertTrue(priority >= priorityMinimum &&
		   priority <= priorityMaximum);
	
	getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMaximum)
	    return false;

	setPriority(thread, priority+1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    public boolean decreasePriority() {
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMinimum)
	    return false;

	setPriority(thread, priority-1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;    

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
	if (thread.schedulingState == null)
	    thread.schedulingState = new ThreadState(thread);

	return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
	PriorityQueue(boolean transferPriority) {
	    this.transferPriority = transferPriority;
	}

	public void waitForAccess(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).waitForAccess(this);
	}

	public void acquire(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).acquire(this);
	}

	public KThread nextThread() {
		//System.out.println("jkjf");
	    Lib.assertTrue(Machine.interrupt().disabled());
	    // implement me
	    if(pickNextThread() == null){	    	
	    	return null;
	    }
	    KThread thread = pickNextThread().thread;//�õ���һ����Ҫִ�е��߳�
//	    System.out.println(thread.getName());
	    getThreadState(thread).acquire(this);//�ڵȴ�������ȥ�����߳�
	    return thread;
	}

	/**
	 * Return the next thread that <tt>nextThread()</tt> would return,
	 * without modifying the state of this queue.
	 *
	 * @return	the next thread that <tt>nextThread()</tt> would
	 *		return.
	 */
	protected ThreadState pickNextThread() {//ѡ�����������ȼ����߳�ִ��
	    // implement me
		if(waitingQueue.isEmpty()){
			return null;
		}
		ThreadState next = getThreadState((KThread)waitingQueue.getFirst());
//		System.out.println(next.thread);
		for(Iterator i = waitingQueue.iterator();i.hasNext();){
			
			ThreadState toNext = getThreadState((KThread)i.next());
//			System.out.println("��һ��"+toNext.thread+" "+toNext.priority);
			if(toNext.getEffectivePriority() > next.getEffectivePriority()){
				next = toNext;
			}		
			//System.out.println(toNext.thread);
			//System.out.println("���ȼ���"+toNext.priority);			
		}
//		System.out.println("����"+next.thread+" "+next.effectivePriority);
		//System.out.println("���ȼ���"+next.priority);
	    return next;
	}
	
	public void print() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    // implement me (if you want)
	}

	/**
	 * <tt>true</tt> if this queue should transfer priority from waiting
	 * threads to the owning thread.
	 */
	public boolean transferPriority;
    LinkedList<KThread> waitingQueue = new LinkedList<KThread>();//�ȴ����ȵ��߳�
    ThreadState lockHolder = null;
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState {//���߳������ȼ���ϵ����
	/**
	 * Allocate a new <tt>ThreadState</tt> object and associate it with the
	 * specified thread.
	 *
	 * @param	thread	the thread this state belongs to.
	 */
	public ThreadState(KThread thread) {
	    this.thread = thread;
	    
	    setPriority(priorityDefault);
	}

	/**
	 * Return the priority of the associated thread.
	 *
	 * @return	the priority of the associated thread.
	 */
	public int getPriority() {
	    return priority;
	}

	/**
	 * Return the effective priority of the associated thread.
	 *
	 * @return	the effective priority of the associated thread.
	 */
	public int getEffectivePriority() {
		//System.out.println("�ҵ���Ч���ȼ���������������������");
	    // implement me
//		if(effectivePriority != expiredEffectivePriority){
//			System.out.println("�����ﷵ���ˣ���");
//			return effectivePriority;
//		}
		
		effectivePriority = priority;
		
		if(waited == null){
			return effectivePriority;			
		}
		//System.out.println(waitQueue);
		for(Iterator i = waited.waitingQueue.iterator();i.hasNext();){
			ThreadState state = getThreadState((KThread)i.next());
			//System.out.println(state.thread);
			if(state.priority > effectivePriority){
				effectivePriority = state.priority;
			}
		}
//		System.out.print(" "+effectivePriority);
	    return effectivePriority;
	}

	/**
	 * Set the priority of the associated thread to the specified value.
	 *
	 * @param	priority	the new priority.
	 */
	public void setPriority(int priority) {
	    if (this.priority == priority)
		return;
	    
	    this.priority = priority;	    
	    // implement me
	}

	/**
	 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
	 * the associated thread) is invoked on the specified priority queue.
	 * The associated thread is therefore waiting for access to the
	 * resource guarded by <tt>waitQueue</tt>. This method is only called
	 * if the associated thread cannot immediately obtain access.
	 *
	 * @param	waitQueue	the queue that the associated thread is
	 *				now waiting on.
	 *
	 * @see	nachos.threads.ThreadQueue#waitForAccess
	 */
	
	public void waitForAccess(PriorityQueue waitQueue) {//����Ĳ����ǵ�ǰϵͳ�е�readyQueue
	    // implement me
		//System.out.println(this.thread);
		//System.out.println("1111111");
		waitQueue.waitingQueue.add(this.thread);//�ڵȴ������м�����߳�
		if(waitQueue.lockHolder == null){
			//waitQueue.lockHolder.effectivePriority = DefaultEffectivePriority;
			return;
		}
//			else{
//			getThreadState(KThread.currentThread()).wait.waitingQueue.add(this.thread);
////			
//		}
		
//		getThreadState(this.thread).wait.remove(waitQueue);
		if(!waitQueue.transferPriority){//�ж��Ƿ�������ȼ���ת
			waitQueue.lockHolder.effectivePriority = expiredEffectivePriority;
		}
	}

	/**
	 * Called when the associated thread has acquired access to whatever is
	 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
	 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
	 * <tt>thread</tt> is the associated thread), or as a result of
	 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
	 *
	 * @see	nachos.threads.ThreadQueue#acquire
	 * @see	nachos.threads.ThreadQueue#nextThread
	 */
	public void acquire(PriorityQueue waitQueue) {//��õȴ������ϵ����ȼ�
	    // implement me
		waitQueue.waitingQueue.remove(this.thread);
		waitQueue.lockHolder = this;
		waitQueue.lockHolder.effectivePriority = expiredEffectivePriority;
		waitQueue.lockHolder.waited = waitQueue;//���ȴ����и�����Դ�����ߵ�threadState��waitqueue�ϣ���Щ�̶߳��ڵȴ���lockHolderִ��������Դ
//		waitQueue.lockHolder.wait.
	}	
	

	/** The thread with which this object is associated. */	   
	protected KThread thread;
	/** The priority of the associated thread. */
	protected int priority;
	protected int effectivePriority = expiredEffectivePriority;
	protected static final int expiredEffectivePriority = -1;
	protected PriorityQueue waited = null;//�ȴ����߳���ռ����Դ���̶߳���
	ThreadState lockHolder = null;
    }
}
