/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eva.cryptobot.qp.bittrex;
 
import eva.cryptobot.TraderProcess;
import java.io.DataInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import org.json.JSONObject;
 
/*
 * @author Ermolenko Vadim
 */
public class QueryQuickData implements Runnable {
    
    private TraderProcess tp;
    private String name;
    private Thread process;
    private boolean isStop = false;
    
    public QueryQuickData (TraderProcess tp) {
        this.tp = tp;
         
        process = new Thread (this);
        process.setName("QueryQuickData");
        process.start();
    }
     
    @Override
    public void run() {
        
        while (!isStop) {
            for (Map.Entry <String, Double> entry: tp.getCurrentPrices().entrySet()) {
                
                StringBuilder JSON = new StringBuilder();
                DataInputStream input = null;

                try{
                    URL url = new URL("https://bittrex.com/api/v1.1/public/getticker?market=" + entry.getKey());
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    input = new DataInputStream(con.getInputStream());

                    byte [] b = new byte[1];
                    while(-1 != input.read(b,0,1)) {
                       JSON.append(new String(b));
                    }
                    con.disconnect();

                    JSONObject jsonObj = new JSONObject(new String(JSON));

                    if (jsonObj.getString("message").equals("INVALID_MARKET")){
                        System.out.println(entry.getKey() + ": INVALID_MARKET");
                        return;
                    } else if (!jsonObj.getString("message").equals("")){
                        System.out.println("QueryQuickData " + entry.getKey() + ": message=" + jsonObj.getString("message"));
                    }

                    JSONObject jResult = jsonObj.getJSONObject("result");
                    synchronized(tp.getCurrentPrices()){
                        tp.getCurrentPrices().replace(entry.getKey(), jResult.getDouble("Last"));
                    }
                }
                catch (Exception ex) {
                    System.out.println(ex);
                    continue;
                }
            }
            
            if (isStop) break;
            
            try {
                Thread.sleep(12 * 1000);
            } catch (InterruptedException ex) {
                System.out.println(ex);
                continue;
            }
        }
    }
    
    public void stop() {
        isStop = true;
    }
}
 