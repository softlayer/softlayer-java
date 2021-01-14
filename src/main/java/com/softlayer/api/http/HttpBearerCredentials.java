package com.softlayer.api.http;

/** HTTP Bearer authorization support for IBM IAM Tokens.
 *
 * @see <a href="https://cloud.ibm.com/apidocs/iam-identity-token-api">IAM Tokens</a>
 * @see <a href="https://sldn.softlayer.com/article/authenticating-softlayer-api/">Authenticating SoftLayer API</a>
 */
public class HttpBearerCredentials implements HttpCredentials {

    protected final String token;
    
    public HttpBearerCredentials(String token) {
        this.token = token;
    }

    /**
     * Formats the token into a HTTP Authorization header.
     *
     * @return String
     */
    public String getHeader() {
        return "Bearer " + token;
    }
}
