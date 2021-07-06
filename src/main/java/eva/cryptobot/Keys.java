/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eva.cryptobot;

import java.io.Serializable;

/**
 *
 * @author ermolenko
 */
public class Keys  implements Serializable {
    private String apiKey;
    private String apiSecret;
    
    public Keys (String apiKey, String apiSecret){
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public String getApiSecret() {
        return apiSecret;
    }
    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }
}
