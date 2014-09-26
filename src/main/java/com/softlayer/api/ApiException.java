package com.softlayer.api;

/** Base exception for all errors that occur inside the API */
@SuppressWarnings("serial")
public class ApiException extends RuntimeException {
    
    public static ApiException fromError(String message, String code, int status) {
        switch (status) {
            case BadRequest.STATUS:
                return new BadRequest(message, code);
            case Unauthorized.STATUS:
                return new Unauthorized(message, code);
            case NotFound.STATUS:
                return new NotFound(message, code);
            case Internal.STATUS:
                return new Internal(message, code);
            default:
                return new ApiException(message, code, status);
        }
    }

    public final String code;
    public final int status;
    
    public ApiException(String message, String code, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }
    
    @Override
    public String getLocalizedMessage() {
        return getMessage() + "(code: " + code + ", status: " + status + ')';
    }
    
    public static class BadRequest extends ApiException {
        public static final int STATUS = 400;
        
        public BadRequest(String message, String code) {
            super(message, code, STATUS);
        }
    }
    
    public static class Unauthorized extends ApiException {
        public static final int STATUS = 401;
        
        public Unauthorized(String message, String code) {
            super(message, code, STATUS);
        }
    }
    
    public static class NotFound extends ApiException {
        public static final int STATUS = 404;
        
        public NotFound(String message, String code) {
            super(message, code, STATUS);
        }
    }
    
    public static class Internal extends ApiException {
        public static final int STATUS = 500;
        
        public Internal(String message, String code) {
            super(message, code, STATUS);
        }
    }
}
