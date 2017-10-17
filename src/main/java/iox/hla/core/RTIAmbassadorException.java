package iox.hla.core;

public class RTIAmbassadorException extends Exception {

	private static final long serialVersionUID = -3215222336696888004L;

public RTIAmbassadorException(String message) {
    super(message);
  }
  
  public RTIAmbassadorException(String message, Throwable cause) {
    super(message, cause);
  }
  
  public RTIAmbassadorException(Throwable cause) {
    super(cause);
  }
}
