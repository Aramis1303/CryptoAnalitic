/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eva.cryptobot;

import java.util.TimeZone;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 *
 * @author Home
 */
public class CryptoBot extends Application {
    
    private static TraderProcess trader;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        
        // Устанавливаем временную зону = 0, чтобы избежать путаницы в поясах.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        
        // Создаем коллекцию потоков обработки бирж
        trader = new TraderProcess("bittrex");
        
        // Запуск GUI
        launch(args);
    }
    
    @Override
    public void start(Stage primaryStage) {
        
        //Tab menu = new Tab("Menu");
        ManagementPanel mainPane = new ManagementPanel(trader);
        
        mainPane.setId("pane");
        
        Scene scene = new Scene(mainPane);
        // Подключаем таблицу стилей
        //scene.getStylesheets().add(CryptoBot.class.getResource("style.css").toExternalForm()); 
        
        // Начать с полноэкранного режима
        primaryStage.setMaximized(true);
        // Завершение при закрытии окна
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>(){
            @Override
            public void handle(WindowEvent event) {
                trader.stop();
            }
        });
        primaryStage.setTitle("CryptoBot");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
