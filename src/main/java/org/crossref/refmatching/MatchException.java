package org.crossref.refmatching;

/**
 * Reference matching-specific error.
 * 
 * @author joe.aparo
 */
public class MatchException extends RuntimeException {

    public MatchException(String msg) {
        super(msg);
    }
    
    public MatchException(Exception ex) {
        super(ex);
    }

    public MatchException(String msg, Exception ex) {
        super(msg, ex);
    }
}