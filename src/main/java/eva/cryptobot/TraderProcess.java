/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eva.cryptobot;

import cryptobot.FileWritter;
import eva.cryptobot.qp.bittrex.QueryQuickData;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/*
 * @author Ermolenko Vadim
 */

public class TraderProcess implements Runnable {
    
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    
    
    private String burse;
    private Thread process;
    private Keys keys;
    
    private SQLQuery sql;
    
    private Map <String, Status> markets;
    private Map <String, Double> currentPrices;
    
    private boolean isTrading = false;
    private double volumeTrade;
    
    private boolean isStop = false;
    
    private NeuralNetworkComplex neunet_comlex_h;
    private NeuralNetworkComplex neunet_comlex_d;
    
    private QueryQuickData qqd;
    
    private long nextDaily;
    private long nextHourly;
    
    private final String pathNN = System.getProperty("user.dir");
    
    public TraderProcess (String burse) throws IOException, Exception {
        
        this.burse = burse;
        this.isStop = false;
        this.sql = new SQLQuery(burse);
        
        // Часовой вывод производим через 2 минуты после начала нового дня
        nextDaily = new Date().getTime() / 24 * 60 * 60 * 1000;
        nextDaily *= 24 * 60 * 60 * 1000;
        //nextDaily += 120 * 1000;
		
        // Часовой вывод производим через 1 минуту после начала нового часа
        nextHourly = new Date().getTime() / 60 * 60 * 1000;
        nextHourly *= 60 * 60 * 1000;
        nextHourly += 60 * 1000;
		
        // Загрузка/Содание сети и нормализатора
        neunet_comlex_d = null;
        if (FileWritter.existFile(pathNN + "/neunet_comlex_d.zip") && FileWritter.existFile(pathNN + "/normal_comlex_d.zip")) {
            neunet_comlex_d = new NeuralNetworkComplex(90, pathNN + "/neunet_comlex_d.zip");
            neunet_comlex_d.loadNormalizer(pathNN + "/normal_comlex_d.zip");
        }
        else {
            throw new RuntimeException("Can't open \"" + pathNN + "/neunet_comlex_d.zip\"" + " or \"" + pathNN + "/normal_comlex_d.zip\"");
        }
        
        neunet_comlex_h = null;
        if (FileWritter.existFile(pathNN + "/neunet_comlex_h.zip") && FileWritter.existFile(pathNN + "/normal_comlex_h.zip")) {
            neunet_comlex_h = new NeuralNetworkComplex(90, pathNN + "/neunet_comlex_h.zip");
            neunet_comlex_h.loadNormalizer(pathNN + "/normal_comlex_h.zip");
        }
        else {
            throw new RuntimeException("Can't open \"" + pathNN + "/neunet_comlex_h.zip\"" + " or \"" + pathNN + "/normal_comlex_h.zip\"");
        }
        
        
        // Запрашиваем маркеты из базы
        markets = sql.readMarkets();
        currentPrices = new HashMap <>();
        // Создаем перечень текущих цен
        for (Map.Entry <String, Status> entry: markets.entrySet()) {
            if (entry.getValue().equals(Status.WORKING)) {
                currentPrices.put(entry.getKey(), new Double(0));
            }
        }
        
        //this.keys = readKeyFromFile();
        
        process = new Thread (this);
        process.setName(burse);
        process.start();
        
        qqd = new QueryQuickData(this);
        System.out.println(TraderProcess.class.getName() + " created.");
    }
    
    @Override
    public void run() {
        
        while (true){
            if (isStop) {
                break;
            }
            //////////////////ЛОГИКА БОТА//////////////////////
            if (nextDaily < new Date().getTime() || true) {
                System.out.println("Daily");
                
                for (Map.Entry <String, Status> entry: markets.entrySet()) {
                    if (entry.getValue().equals(Status.WORKING)) {
                        long lastCandelTime = sql.getTimeOfLastJapaneseCandle(entry.getKey(), "1_day");
                        System.out.println(ANSI_CYAN + entry.getKey() + ANSI_RESET);
                        
                        long lastPrediction = 0; // <--------------------------------
                        
                        long lastTime = new Date().getTime() / 24 * 60 * 60 * 1000; // Ровное количество дней, обрезаем текущий не завершенный день 
                        lastTime *= 24 * 60 * 60 * 1000;
                        long previousTime = lastTime - 24 * 60 * 60 * 1000;
                        
                        // Если записи прогнозирования ещё не было в текущем дне, а свечи 
                        if ((previousTime < lastPrediction && lastPrediction < lastCandelTime) || lastPrediction == 0) {
                            final int LENGTH_OF_INPUT = 90;
                            
                            // Нулевой элемент самый свежий //запрашиваем больше данных, чтобы оценить процент угадывания данных нейронной сетью
                            List <Candle> candles = sql.readCandles(entry.getKey(), "1_day", LENGTH_OF_INPUT * 3); 
                            
                            // Коллекция для анализа данных нейронной сетью
                            List <Candle> inputs = new ArrayList<>();
                            
                            // Кол-во правильных ответов и не правильных
                            int count_complex_good = 0;
                            int count_complex_bad = 0;
                            
                            for (int i = 1;  i < candles.size() - (LENGTH_OF_INPUT + 1); i++) {
                                for (int c = 0; c < (LENGTH_OF_INPUT + 1); c++) {
                                    inputs.add(candles.get(i + c));
                                }
                                
                                Candle next = candles.get(i - 1);
                                Candle pred_complex = neunet_comlex_d.prediction(inputs);
                                
                                double complex = (pred_complex.getHight() + pred_complex.getLow())/2;
                                
                                if (next.getTime() == pred_complex.getTime()) {
                                    //System.out.println(ANSI_BLUE + new Date(next.getTime()) + ANSI_RESET);
                                    if (complex < next.getHight() && complex > next.getLow()) {
                                        //System.out.println(ANSI_GREEN + Double.toString(complex) + ANSI_RESET);
                                        count_complex_good ++;
                                    }
                                    else {
                                        //System.out.println(ANSI_RED + Double.toString(complex) + ANSI_RESET);
                                        count_complex_bad ++;
                                    }
                                }
                                else System.out.println(ANSI_RED + new Date(next.getTime()) + "!= " + new Date(pred_complex.getTime()) + ANSI_RESET);
                                inputs.clear();
                            }
                            
                            System.out.println(ANSI_PURPLE + "Comlex: \t" +  count_complex_good + " of " + (count_complex_good + count_complex_bad) + ANSI_RESET);
                            System.out.println(ANSI_PURPLE + "Accuracy: \t" +  ((count_complex_good * 100 / (count_complex_good + count_complex_bad))) + " %" + ANSI_RESET);
                            
                            candles = sql.readCandles(entry.getKey(), "1_day", LENGTH_OF_INPUT +1);
                            Candle pred_complex = neunet_comlex_d.prediction(candles);
                            
                            double complex_av = (pred_complex.getHight() + pred_complex.getLow())/2;
                            //double complex_hi = (pred_complex.getHight() + complex_av) / 2;
                            //double complex_lo = (pred_complex.getLow() + complex_av) / 2;
                            double last_av = (candles.get(0).getHight() + candles.get(0).getLow())/2;
                            
                            sql.writePrediction ("neunet", entry.getKey(), "day", (complex_av/last_av -1) * 100, ((((double)count_complex_good * 100 / ((double)count_complex_good + (double)count_complex_bad)))));
                            
                            //System.out.println(ANSI_BLACK + new Date(pred_complex.getTime()) + ANSI_RESET);
                            //System.out.println(ANSI_CYAN + "Next hight: \t" + Double.toString(complex_hi) + ANSI_RESET);
                            //System.out.println(ANSI_CYAN + "Next low: \t" +  Double.toString(complex_lo) + ANSI_RESET);
                            //System.out.println(ANSI_YELLOW + "==================================================\n==================================================" + ANSI_RESET);
                        }
                    }
                }
                nextDaily += 24 * 60 * 60 * 1000;
            }
            isStop = true;
            /*
            if (nextHourly < new Date().getTime()) {
				for (Map.Entry <String, Status> entry: markets.entrySet()) {
                    if (entry.getValue().equals(Status.WORKING)) {
                        long lastCandel = sql.getTimeOfLastJapaneseCandle(entry.getKey(), "1_hour");
                        long lastPrediction = sql.getTimeOfLastPrediction("neunet", entry.getKey(), "1_hour");
                        long lastTime = new Date().getTime() / 24 * 60 * 60 * 1000; // Ровное количество часов, обрезаем текущий не завершенный день 
                        lastTime *= 24 * 60 * 60 * 1000;
                        long previousTime = lastTime - 24 * 60 * 60 * 1000;
                        
                        // Если записи прогнозирования ещё не было в текущем часе, а свечи 
                        if ((previousTime < lastPrediction && lastPrediction < lastCandel) || lastPrediction == 0) {
                            List <Candle> candles = sql.readCandles(entry.getKey(), "1_hour", 30); // ДАННЫЕ В ОБРАТНОМ ПОРЯДКЕ, НУЖНО ПЕРЕВЕРНУТЬ
                            if (candles != null) {
                                double [] serie_hi = new double [candles.size() - 1];
                                double [] serie_lo = new double [candles.size() - 1];
                                for (int i = candles.size() - 1; i > 0; i--) {
                                    serie_hi [candles.size() - i -1] = (candles.get(i).getHight()/candles.get(i-1).getHight() * 100 -100);
                                    serie_lo [candles.size() - i -1] = (candles.get(i).getLow()/candles.get(i-1).getLow() * 100 -100);
                                }
								
                                double next_hi_price = (candles.get(candles.size() - 1).getHight() + Classification.getPercentByClass(neunet_1_hour_hi.prediction(serie_hi))/100 * candles.get(candles.size() - 1).getHight());
                                double next_lo_price = (candles.get(candles.size() - 1).getLow() +  Classification.getPercentByClass(neunet_1_hour_lo.prediction(serie_lo))/100 * candles.get(candles.size() - 1).getLow());

                                // TO DO: Вместо принта ставим запись в базу и вызываем функцию принятия решения о сделках
                                System.out.println(new Date(lastCandel) + ": next HOUR - > " + entry.getKey());
                                System.out.println(" hight: \t" + next_hi_price);
                                System.out.println(" low: \t" + next_lo_price);
                                System.out.println(" difference: \t" + (next_hi_price / next_lo_price * 100 - 100));
                            }
                        }
                    }
                }
                nextHourly += 60 * 60 * 1000;
            }
            
            try {
                sleep(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
                return;
            }
            */
        }
        
    }
    
    public void stop (){
        qqd.stop();
        isStop = true;
    }
    
    public String getName(){
        return this.burse;
    }
    
    public Keys getKeys(){
        return keys;
    }

    public SQLQuery getSql() {
        return sql;
    }
    
    private Keys readKeyFromFile(){
        
        if (FileWritter.existFile(System.getProperty("user.dir") + "/keys")){
            keys = (Keys)FileWritter.openFromFile(System.getProperty("user.dir") + "/" + burse + "/keys");
        }
        else {
            keys = new Keys("", "");
        }
        return keys;
    }
    
    public void saveToFile(Keys keys){
        try {
            FileOutputStream fos = new FileOutputStream(System.getProperty("user.dir") + "/" + burse + "/keys");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            
            oos.writeObject(keys);
            oos.flush();
            oos.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public void stopTrading(){
        isTrading = false;
    }
    
    public void startTrading(){
        isTrading = true;
    }
    
    public void setVolumeTrade(double volume){
        volumeTrade = volume;
    }

    public Map<String, Status> getMarkets() {
        return markets;
    }

    public Map<String, Double> getCurrentPrices() {
        return currentPrices;
    }
    
}
