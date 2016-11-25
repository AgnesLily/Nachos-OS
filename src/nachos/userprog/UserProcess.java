package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {
	int numPhysPages = Machine.processor().getNumPhysPages();
	pageTable = new TranslationEntry[numPhysPages];
	for (int i=0; i<numPhysPages; i++)
	    pageTable[i] = new TranslationEntry(i,i, true,false,false,false);
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {   	
	if (!load(name, args)){
//		System.out.println("excutezheli");
		 return false;		
	}
	   
	
	new UThread(this).setName(name).fork();

	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
	Lib.assertTrue(maxLength >= 0);

	byte[] bytes = new byte[maxLength+1];

	int bytesRead = readVirtualMemory(vaddr, bytes);

	for (int length=0; length<bytesRead; length++) {
	    if (bytes[length] == 0)
		return new String(bytes, 0, length);
	}

	return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     * 从进程的虚拟内存中将数据转化为特定的数组。这个方法处理地址转换的细节。这个方法在发生错误时必须不能
	 * 损坏当前进程，但是应该返回成功复制的字节的数量，如果没有数据复制返回0
     * @param	vaddr	the first byte of virtual memory to read.//虚拟内存要读取的第一个字节
     * @param	data	the array where the data will be stored.//data将被存储的数组地址
     * @param	offset	the first byte to write in the array.//数组中将要写的第一个字节
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.从虚拟内存中返回到数组中的字节数
     * @return	the number of bytes successfully transferred.
     */
    //读内存时，先将虚拟内存地址转换为物理内存地址，然后将内存数据复制到数组中
    public int readVirtualMemory(int vaddr, byte[] data, int offset,int length) {
		Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);
	
		byte[] memory = Machine.processor().getMemory();
		if(length>(pageSize*numPages-vaddr))
			length=pageSize*numPages-vaddr;
		if(data.length-offset<length)    
			length=data.length-offset;
		int transferredbyte=0; //成功读取的字节数
		
		do{
			int pageNum=Processor.pageFromAddress(vaddr+transferredbyte);
			if(pageNum<0||pageNum>=pageTable.length)              
				return 0;
			int pageOffset=Processor.offsetFromAddress(vaddr+transferredbyte); 
			int leftByte=pageSize-pageOffset;           
			int amount=Math.min(leftByte, length-transferredbyte); 
			int realAddress=pageTable[pageNum].ppn*pageSize+pageOffset;     
			System.arraycopy(memory, realAddress, data, offset+transferredbyte,  amount);
			transferredbyte=transferredbyte+amount;		
		}while(transferredbyte<length);	
			return transferredbyte;			
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.//虚拟内存要写入的第一个字节
     * @param	data	the array containing the data to transfer.//包含着需要传递数据的数组
     * @param	offset	the first byte to transfer from the array.//数组中第一个要被存储的字节
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.//数组中需要写入虚拟内存的字节数
     * @return	the number of bytes successfully transferred.//成功写入的字节数
     */
    //项虚拟内存中写入时，先将虚拟内存地址转换为实际物理地址，然后将数组中的数据写入物理内存
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	if(length>(pageSize*numPages-vaddr))
		length=pageSize*numPages-vaddr;
	if(data.length-offset<length)    
		length=data.length-offset;
	int transferredbyte=0; //成功写入的字节数
	
	do{
		int pageNum=Processor.pageFromAddress(vaddr+transferredbyte);
		if(pageNum<0||pageNum>=pageTable.length)              
			return 0;
		int pageOffset=Processor.offsetFromAddress(vaddr+transferredbyte); 
		int leftByte=pageSize-pageOffset;           
		int amount=Math.min(leftByte, length-transferredbyte); 
		int realAddress=pageTable[pageNum].ppn*pageSize+pageOffset;     
		System.arraycopy(data, offset+transferredbyte, memory, realAddress, amount);
		transferredbyte=transferredbyte+amount;		 
	}while(transferredbyte < length);
	// for now, just assume that virtual addresses equal physical addresses
//	if (vaddr < 0 || vaddr >= memory.length)
//	    return 0;
//
//	int amount = Math.min(length, memory.length-vaddr);
//	System.arraycopy(data, offset, memory, vaddr, amount);
//
	return transferredbyte;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    //将可执行的程序与当前进程绑定，并且配置好参数。开始执行，读头信息，并且将sections和参数复制到线程的虚拟内存中
    //模拟从磁盘将程序载入
    private boolean load(String name, String[] args) {
    System.out.println("dfhkash");
	Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
	System.out.println("dfhkash");
	OpenFile executable = ThreadedKernel.fileSystem.open(name, false);//为什么打不开呢
	System.out.println("fasdjkflksdj");
	if (executable == null) {
	    Lib.debug(dbgProcess, "\topen failed");
	    return false;
	}

	try {
	    coff = new Coff(executable);
	}
	catch (EOFException e) {
	    executable.close();
	    Lib.debug(dbgProcess, "\tcoff load failed");
	    return false;
	}

	// make sure the sections are contiguous and start at page 0
	numPages = 0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    if (section.getFirstVPN() != numPages) {
		coff.close();
		Lib.debug(dbgProcess, "\tfragmented executable");
		return false;
	    }
	    numPages += section.getLength();
	}

	// make sure the argv array will fit in one page
	byte[][] argv = new byte[args.length][];
	int argsSize = 0;
	for (int i=0; i<args.length; i++) {
	    argv[i] = args[i].getBytes();
	    // 4 bytes for argv[] pointer; then string plus one for null byte
	    argsSize += 4 + argv[i].length + 1;
	}
	if (argsSize > pageSize) {
	    coff.close();
	    Lib.debug(dbgProcess, "\targuments too long");
	    return false;
	}

	// program counter initially points at the program entry point
	initialPC = coff.getEntryPoint();	

	// next comes the stack; stack pointer initially points to top of it
	numPages += stackPages;
	initialSP = numPages*pageSize;

	// and finally reserve 1 page for arguments
	numPages++;

	if (!loadSections())//这个地方的调用函数需要该技能
	    return false;

	// store arguments in last page
	int entryOffset = (numPages-1)*pageSize;
	int stringOffset = entryOffset + args.length*4;

	this.argc = args.length;
	this.argv = entryOffset;
	
	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
		       argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	}

	return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    //为进程分配内存，将COFF的section加载进内存，如果返回成功，进程将即可执行
//    在loadSection中，在导入coff之前应该创建一个页表，进行物理地址和逻辑地址的关联，然后把程序的每一块按照顺序对应于物理地址导入到内存中。
    protected boolean loadSections() {
    	
    	UserKernel.allocateMemoryLock.acquire();
    	
    	if(numPages > UserKernel.memoryLinkedList.size()){//空闲的页表是否满足进程需要页表大小
    		coff.close();
    		Lib.debug(dbgProcess, "\tinsufficient physical memory");
    		UserKernel.allocateMemoryLock.release();//释放内存锁
    		return false;
    	}
    	
    	pageTable = new TranslationEntry[numPages];// 创建页表
    	for(int i=0;i<numPages;i++){
    		int nextPage = UserKernel.memoryLinkedList.remove();
    		pageTable[i] = new TranslationEntry(i,nextPage, true, false, false, false);   		
    	}
    	UserKernel.allocateMemoryLock.release();   	

    	// load sections
    	for (int s=0; s<coff.getNumSections(); s++) {
    	    CoffSection section = coff.getSection(s);
    	    
    	    Lib.debug(dbgProcess, "\tinitializing " + section.getName()
    		      + " section (" + section.getLength() + " pages)");

    	    for (int i=0; i<section.getLength(); i++) {
    		int vpn = section.getFirstVPN()+i;
    		pageTable[vpn].readOnly = section.isReadOnly();
    		// for now, just assume virtual addresses=physical addresses
    		section.loadPage(i, pageTable[vpn].ppn);
    	    }
    	}
    	
    	return true;
        }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {//释放内存
		UserKernel.allocateMemoryLock.acquire();

		for (int i = 0; i < numPages; i++) {
			UserKernel.memoryLinkedList.add(pageTable[i].ppn);
			// 将该用户进程占用的内存加入空闲内存链表中
			pageTable[i] = null;

		}
		UserKernel.allocateMemoryLock.release();

	}
    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    //初始化程序的寄存器
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {

		Machine.halt();
		
		Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
    }
    
    private int handleCreate(int fileAddress) {
    	String filename = readVirtualMemoryString(fileAddress,256);
        if(filename == null){
        	return -1;
        }
        int fileDescriptor = findEmpty();
        if(fileDescriptor == -1){
        	return -1;
        }
        else {
        	openfile[fileDescriptor] = ThreadedKernel.fileSystem.open(filename,true);
        	return fileDescriptor;
        }
    }
    
	private int findEmpty(){
    	for(int i =0;i<16;i++){
    		if(openfile[i] == null){
    			return i;
    		}
    	}
    	return -1;
    }
	
    private int handleOpen(int fileAddress) {

    	String filename = readVirtualMemoryString(fileAddress,256);
        if(filename == null){
        	return -1;
        }
        int fileDescriptor = findEmpty();
        if(fileDescriptor == -1){
        	return -1;
        }
        else {
        	System.out.println("打开文件");
        	openfile[fileDescriptor] = ThreadedKernel.fileSystem.open(filename,false);
        	return fileDescriptor;
        }
    }
    
    private int handleRead(int fileDescriptor,int bufferAddress,int length) {
    	if(fileDescriptor>15||fileDescriptor<0||openfile[fileDescriptor]==null) 
    		return -1;
    	byte temp[]=new byte[length];
    	int readNumber=openfile[fileDescriptor].read(temp, 0, length);
    	if(readNumber<=0)
    		 return 0;
    	int writeNumber=writeVirtualMemory(bufferAddress,temp);
    	return writeNumber;
    }
    
    private int handleWrite(int fileDescriptor,int bufferAddress,int length) {    	   	
    	if(fileDescriptor>15||fileDescriptor<0||openfile[fileDescriptor]==null) 
    		return -1;
    	byte temp[]=new byte[length];
    	int readNumber=readVirtualMemory(bufferAddress,temp);
    	if(readNumber<=0)
    		 return 0;
    	int writeNumber=openfile[fileDescriptor].write(temp,0,length);
    	if(writeNumber < length){
    		return -1;
    	}
    	return writeNumber;
    }
    
    //删除文件的系统调用
    private int handleClose(int fileDescriptor) {
    	if(fileDescriptor>15||fileDescriptor<0||openfile[fileDescriptor]==null) 
    		return -1;
    	openfile[fileDescriptor].close();
    	openfile[fileDescriptor] = null;   	
    	return 0;
    }
    private int handleUnlink(int fileAddress) {
    	String filename = readVirtualMemoryString(fileAddress, 256);
		if (filename == null)
			return 0;// 文件不存在,不必删除

		if (ThreadedKernel.fileSystem.remove(filename))// 删除磁盘中实际的文件
			return 0;
		else
			return -1;
    }
    

    private static final int
        syscallHalt = 0,
		syscallExit = 1,
		syscallExec = 2,
		syscallJoin = 3,
		syscallCreate = 4,
		syscallOpen = 5,
		syscallRead = 6,
		syscallWrite = 7,
		syscallClose = 8,
		syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
	switch (syscall) {
	case syscallHalt:
	    return handleHalt();
	case syscallCreate:
		return handleCreate(a0);
	case syscallOpen:
		return handleOpen(a0);
	case syscallRead:
		return handleRead(a0,a1,a2);
	case syscallWrite:
		return handleWrite(a0,a1,a2);
	case syscallClose:
		return handleClose(a0);
	case syscallUnlink:
		return handleUnlink(a0);
	case syscallExit:
		
	default:
	    Lib.debug(dbgProcess, "Unknown syscall " + syscall);
	    Lib.assertNotReached("Unknown system call!");
	}
	return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();
	
	System.out.println("hfjksdhfjk");

	switch (cause) {
	case Processor.exceptionSyscall:
	    int result = handleSyscall(processor.readRegister(Processor.regV0),
				       processor.readRegister(Processor.regA0),
				       processor.readRegister(Processor.regA1),
				       processor.readRegister(Processor.regA2),
				       processor.readRegister(Processor.regA3)
				       );
	    processor.writeRegister(Processor.regV0, result);
	    processor.advancePC();
	    break;				       
				       
	default:
	    Lib.debug(dbgProcess, "Unexpected exception: " +
		      Processor.exceptionNames[cause]);
	    Lib.assertNotReached("Unexpected exception");
	}
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    OpenFile openfile[] = new OpenFile[16];// 进程打开的文件表
}
