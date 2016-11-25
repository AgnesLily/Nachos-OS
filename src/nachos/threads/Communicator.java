package nachos.threads;

import java.util.LinkedList;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.//������һ��ʱ�����ߺ�˵�߶��ڵȴ�
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
	private Lock lock = new Lock();
	private int word;
	private static int speakerNum = 0;
	private static int listenerNum = 0;
	LinkedList<Integer> words = new LinkedList<Integer>();
	Condition2 speaker = new Condition2(lock);
	Condition2 listener = new Condition2(lock);
    public Communicator() {
    	    	
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
    	boolean  intStatus = Machine.interrupt().disable();
    	lock.acquire();
    	if(listenerNum == 0){//û�����ߣ�˵��˯�ߣ��洢˵�ߵĻ�
    		speakerNum++;
    		words.offer(word);
    		speaker.sleep();
    		//System.out.println("daozhelile");
    		listener.wake();
    		//listenerNum--;
    	}else {
    		speakerNum++;
    		words.offer(word);//׼����Ϣ����������       
    		listener.wake();//��������
    		listenerNum--;
    	}
    	//System.out.print(word);
    	System.out.println("   " + KThread.currentThread().getName() + " ������Ϣ " + word);  
    	lock.release();//�ͷ���         
    	Machine.interrupt().restore(intStatus);//���ж�       
    	return;
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
    	boolean  intStatus = Machine.interrupt().disable();
    	lock.acquire();
    	if(speakerNum != 0){//��˵��
    		listenerNum++;
    		speaker.wake();//����˵��
    		listener.sleep();//����˯��
    		listenerNum--;
    	}else {//û��˵��
    		listenerNum++;
    		listener.sleep();
    		listenerNum--;
    	}
    	               
    	lock.release();//�ͷ���         
    	Machine.interrupt().restore(intStatus);//���ж�          	
    	return words.poll();
    	
    }
}
