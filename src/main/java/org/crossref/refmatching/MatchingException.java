package org.crossref.refmatching;

/**
 * RefMatching-specific error.
 * 
 * @author joe.aparo
 */
public class MatchingException extends RuntimeException {
    public MatchingException(String msg) {
        super(msg);
    }
    
    public MatchingException(Exception ex) {
        super(ex);
    }

    public MatchingException(String msg, Exception ex) {
        super(msg, ex);
    }
}
