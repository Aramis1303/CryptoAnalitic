/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eva.cryptobot.qp.bittrex;

import eva.cryptobot.TraderProcess;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONObject;

/**
 *
 * @author Натали
 */
public class QueryBuy implements Runnable{
    
    private TraderProcess tp;
    private String market;
    private double rate;
    private double quantity;
    private Thread process;
    
    public QueryBuy (TraderProcess tp, String market, double rate, double quantity){
        this.tp = tp;
        this.market = market;
        this.rate = rate;
        this.quantity = quantity;
        
        process = new Thread (this);
        process.setName(tp.getName() + ".QueryBuy");
        process.start();
    }
    
    @Override
    public void run() {
        try {
            String JSON = "";
            DataInputStream input = null;
            String nonce = String.valueOf(System.currentTimeMillis());
            String uri = "";
            String message = "";
            boolean result;

            uri = "https://bittrex.com/api/v1.1/market/buylimit?apikey="+ tp.getKeys().getApiKey() + "&nonce=" + nonce + "&market=" + market + "&quantity=" + String.format("%.8f", quantity).replace(',', '.') + "&rate=" + String.format("%.8f", rate).replace(',', '.');
            
            URL url = new URL(uri);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("apisign", getkey(tp.getKeys().getApiSecret(), uri));
            con.setRequestMethod("GET");
            con.connect();
            con.getInputStream();

            input = new DataInputStream(con.getInputStream());

            byte [] b = new byte[1];
            while(-1 != input.read(b,0,1)) {
                JSON += new String(b);
            }
            con.disconnect();
            
            JSONObject jsonObj = new JSONObject(JSON);
            result = jsonObj.getBoolean("success");
            if (result) {
                String uuid;
                message = "success";
                JSONObject jsonObjRes = jsonObj.getJSONObject("result");
                uuid = jsonObjRes.getString("uuid");
                
                tp.getSql().writeOrder(market, "buy", uuid);
            }
            
        } catch (MalformedURLException ex) {
            System.out.println("Exception: " + tp.getName() + ": " + market);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException ex) {
            System.out.println("Exception: " + tp.getName() + ": " + market);
        }
    }
    
    // Шифрование ключей
    private String getkey(String apiSecret, String uri) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA512");
        SecretKeySpec secret = new SecretKeySpec(apiSecret.getBytes(),"HmacSHA512");
        mac.init(secret);
        byte[] digest = mac.doFinal(uri.getBytes());
        String sign = org.apache.commons.codec.binary.Hex.encodeHexString(digest);
        return sign;
    }
    
}
