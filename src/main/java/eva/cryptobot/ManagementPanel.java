/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eva.cryptobot;

import java.util.Map;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 *
 * @author username
 */
public class ManagementPanel extends BorderPane {
    
    private TraderProcess tp;
    
    private HBox top;
    private HBox bottom;
    
    private VBox left;
    private VBox right;
    
    private VBox center;
    
    public ManagementPanel (TraderProcess tp) {
        
        this.tp = tp;
        
        top = new HBox();
        bottom = new HBox();
        left = new VBox();
        right = new VBox();
        
        this.setTop(top);
        this.setBottom(bottom);
        this.setLeft(left);
        this.setRight(right);
        
        // В левую панель засовываем кнопки для переключения на нужный маркет
        for (Map.Entry<String, Status> entry: tp.getMarkets().entrySet()) {
            if (entry.getValue().equals(Status.WORKING)) {
                Button btn = new Button(entry.getKey());
                final String name = entry.getKey();
                btn.setOnAction(new EventHandler<ActionEvent>() {
                    @Override 
                    public void handle(ActionEvent e) {
                        setCurrentMarket(name);
                    }
                });
                
                left.getChildren().add(btn);
            }
        }
        
        
        
    }
    
    
    private void setCurrentMarket(String name) {
        
    }
    
    
    
    
}
