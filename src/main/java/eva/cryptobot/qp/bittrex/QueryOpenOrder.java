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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Натали
 */
public class QueryOpenOrder implements Runnable{
    
    private TraderProcess tp;
    private String market;
    private Thread process;
    
    
    public QueryOpenOrder(TraderProcess tp, String market){
        this.tp = tp;
        this.market = market;
        
        process = new Thread (this);
        process.setName(tp.getName() + ".QueryOpenOrder");
        process.start();
    }
    
    @Override
    public void run() {
        try {
            List <String> uuids = new ArrayList<>();
            StringBuilder JSON = new StringBuilder();
            DataInputStream input = null;
            String nonce = String.valueOf(System.currentTimeMillis());
            String message = "";
            boolean result;
            
            String uri = "https://bittrex.com/api/v1.1/market/getopenorders?apikey="+ tp.getKeys().getApiKey() + "&nonce=" + nonce + "&market=" + market;
            
            URL url = new URL(uri);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("apisign", getkey(tp.getKeys().getApiSecret(), uri));
            con.setRequestMethod("GET");
            con.connect();
            con.getInputStream();

            input = new DataInputStream(con.getInputStream());

            byte [] b = new byte[1];
            while(-1 != input.read(b,0,1)) {
                JSON.append(new String(b));
            }
            con.disconnect();
            
            JSONObject jsonObj = new JSONObject(new String(JSON));
            result = jsonObj.getBoolean("success");
            // Дописать проверку и вывод сообщения jsonObj.getBoolean("success");            
            if (!result) {
                message = jsonObj.getString("message");
                System.out.println(message);
            }
            else {
                String uuid;
                message = "success";
                JSONArray jsonRes = jsonObj.getJSONArray("result");
                for (Object jO: jsonRes){
                    uuids.add(((JSONObject)jO).getString("OrderUuid"));
                }
            }
            
            // Ищем совпадения по uuid
            for (Map.Entry <String, String> entry: tp.getSql().readOpenedOrder().entrySet()) {
                boolean finded = true;
                String uuidClosed = null;
                for (String uuid: uuids){
                    if(entry.getValue().equals(uuid)) {
                        finded = false;
                        uuidClosed = uuid;
                    }
                }
                
                if (finded) {
                    tp.getSql().updateOrder(uuidClosed, "closed");
                }
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
