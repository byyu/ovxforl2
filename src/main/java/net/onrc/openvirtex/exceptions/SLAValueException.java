package net.onrc.openvirtex.exceptions;

public class SLAValueException extends Exception {

	private static final long serialVersionUID = 1L;

	public SLAValueException(final Exception e){
		super(e);
	}

	public SLAValueException(String string) {
		super(string);
	}
}
