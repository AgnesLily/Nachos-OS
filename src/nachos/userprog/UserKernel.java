package nachos.userprog;

import java.util.LinkedList;

import com.sun.org.apache.bcel.internal.generic.NEW;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
    /**
     * Allocate a new user kernel.
     */
    public UserKernel() {
	super();
    }

    /**
     * Initialize this kernel. Creates a synchronized console and sets the
     * processor's exception handler.
     */
    public void initialize(String[] args) {
	super.initialize(args);
    //比原先多加了同步控制
	console = new SynchConsole(Machine.console());
	
	//新建了一个中断处理
	Machine.processor().setExceptionHandler(new Runnable() {
		public void run() { exceptionHandler(); }
	    });
	memoryLinkedList = new LinkedList(); // 初始化内存链表
	for (int i = 0; i < Machine.processor().getNumPhysPages(); i++)
		memoryLinkedList.add((Integer) i); // 将虚拟内存分页并放入链表中

	allocateMemoryLock = new Lock();// 初始化内存分配锁
    }

    /**
     * Test the console device.
     */	
    public void selfTest() {
	//super.selfTest();

	System.out.println("Testing the console device. Typed characters");
	System.out.println("will be echoed until q is typed.");

	char c;

	do {
	    c = (char) console.readByte(true);
	    console.writeByte(c);
	}
	while (c != 'q');

	System.out.println("");
    }

    /**
     * Returns the current process.
     *
     * @return	the current process, or <tt>null</tt> if no process is current.
     */
    public static UserProcess currentProcess() {
	if (!(KThread.currentThread() instanceof UThread))
	    return null;
	
	return ((UThread) KThread.currentThread()).process;
    }

    /**
     * The exception handler. This handler is called by the processor whenever
     * a user instruction causes a processor exception.
     *
     * <p>
     * When the exception handler is invoked, interrupts are enabled, and the
     * processor's cause register contains an integer identifying the cause of
     * the exception (see the <tt>exceptionZZZ</tt> constants in the
     * <tt>Processor</tt> class). If the exception involves a bad virtual
     * address (e.g. page fault, TLB miss, read-only, bus error, or address
     * error), the processor's BadVAddr register identifies the virtual address
     * that caused the exception.
     */
    public void exceptionHandler() {
	Lib.assertTrue(KThread.currentThread() instanceof UThread);

	System.out.println("*******************");
	
	UserProcess process = ((UThread) KThread.currentThread()).process;
	int cause = Machine.processor().readRegister(Processor.regCause);
	process.handleException(cause);
    }

    /**
     * Start running user programs, by creating a process and running a shell
     * program in it. The name of the shell program it must run is returned by
     * <tt>Machine.getShellProgramName()</tt>.
     *
     * @see	nachos.machine.Machine#getShellProgramName
     */
    public void run() {
	super.run();
	System.out.println("runnnn");
	UserProcess process = UserProcess.newUserProcess();//新建一个用户进程
	
	String shellProgram = Machine.getShellProgramName();//所需要执行的程序
	System.out.println(shellProgram);
	Lib.assertTrue(process.execute(shellProgram, new String[] { }));//开始执行

	KThread.currentThread().finish();
	System.out.println("run finish");
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	super.terminate();
    }

    /** Globally accessible reference to the synchronized console. */
    public static SynchConsole console;

    // dummy variables to make javac smarter
    private static Coff dummy1 = null;
    
    public static Lock allocateMemoryLock;// 内存分配锁，在用户程序申请内存页的时候使用
    public static LinkedList<Integer> memoryLinkedList;// 内存页的链表，用于存放空闲内存页的编号
}
