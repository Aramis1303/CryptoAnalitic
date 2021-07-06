/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cryptobot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * @author ermolenko
 */
public class FileWritter {
    
    // Сохранение в файл
    public static void saveToFile (Object obj, String fullFileName) throws FileNotFoundException, IOException{
        synchronized(FileWritter.class){
            FileOutputStream fos = new FileOutputStream(fullFileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(obj);
            oos.flush();
            oos.close();
        }
    }
    
    // Загрузка файла из сети
    public static Object openFromFile (String n) {
        synchronized(FileWritter.class){
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(n);
                ObjectInputStream oin = new ObjectInputStream(fis);
                Object obj = oin.readObject();
                return obj;
            } catch (FileNotFoundException ex) {
                Logger.getLogger(FileWritter.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            } catch (IOException | ClassNotFoundException ex) {
                Logger.getLogger(FileWritter.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            } finally {
                try {
                    fis.close();
                } catch (IOException ex) {
                    Logger.getLogger(FileWritter.class.getName()).log(Level.SEVERE, null, ex);
                    return null;
                }
            }
        }
    }
     
    public static boolean existFile (String filename) {
        synchronized(FileWritter.class){
            final File file = new File(filename);
            if (file.exists()) return true;
            else return false;
        }
    }
    
    public static void createFolder (String path) {
        synchronized(FileWritter.class){
            final File folder = new File(path);
            folder.mkdir();
        }
    }
    
    public static void createFile (String file) {
        synchronized(FileWritter.class){
            PrintWriter writer;
            try {
                writer = new PrintWriter(file, "UTF-8");
                writer.close();
            } catch (FileNotFoundException|UnsupportedEncodingException ex) {
                ex.printStackTrace();
                return;
            }
        }
    }
    
}
