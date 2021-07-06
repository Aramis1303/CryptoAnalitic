/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eva.cryptobot;

/**
 *
 * @author username
 */
public class Classification {
    
    static int getClassByPercent_15(double p) {
        if      (p < -15)   return 0;
        else if (p < -8)    return 1;
        else if (p < -5.0)  return 2;
        else if (p < -3.4)  return 3;
        else if (p < -2.3)  return 4;
        else if (p < -1.5)  return 5;
        else if (p < -1.0)  return 6;
        else if (p > 15)    return 14;
        else if (p > 8)     return 13;
        else if (p > 5.0)   return 12;
        else if (p > 3.4)   return 11;
        else if (p > 2.3)   return 10;
        else if (p > 1.5)   return 9;
        else if (p > 1.0)   return 8;
        else return 7;
    }
    
    static double getPercentByClass_15(int p) {
        if      (p == 0) return -15.0;
        else if (p == 1) return -8.0;
        else if (p == 2) return -5.0;
        else if (p == 3) return -3.4;
        else if (p == 4) return -2.3;
        else if (p == 5) return -1.5;
        else if (p == 6) return -1.0;
        else if (p == 7) return 0;
        else if (p == 8) return 1.0;
        else if (p == 9) return 1.5;
        else if (p == 10) return 2.3;
        else if (p == 11) return 3.4;
        else if (p == 12) return 5.0;
        else if (p == 13) return 8.0;
        else if (p == 14) return 15.0;
        else throw new RuntimeException ("Number of class can't be less 0 or bigger 14");
    }
    
    
}
