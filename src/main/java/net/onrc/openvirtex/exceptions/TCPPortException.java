package net.onrc.openvirtex.exceptions;

public class TCPPortException  extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public TCPPortException(final Exception e){
		super(e);
	}
	public TCPPortException(String string){
		super(string);
	}

}
