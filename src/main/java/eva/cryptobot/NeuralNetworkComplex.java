/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eva.cryptobot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;
import org.deeplearning4j.eval.RegressionEvaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.graph.MergeVertex;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.MultiDataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.dataset.api.preprocessor.AbstractMultiDataSetNormalizer;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.MultiDataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.MultiNormalizerMinMaxScaler;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.dataset.api.preprocessor.serializer.NormalizerSerializer;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.RmsProp;
import org.nd4j.linalg.lossfunctions.LossFunctions;

/**
 *
 * @author Natali
 */
public class NeuralNetworkComplex {
    private int lengthOfHistory;
    private int quantityOfClasses;
    
    private List <MultiDataSet> fullData;
    private MultiDataSet trainData;
    private MultiDataSet testData;
 
    private AbstractMultiDataSetNormalizer normalizer;
    
    private ComputationGraph networkModel;
    private ComputationGraphConfiguration configOfNet;
     
    private double currPrecision = 0;
    
    private boolean isLoaded = false;
    
    // Создание новой сети
    public NeuralNetworkComplex (int inputs) {
        this.lengthOfHistory = inputs;
        this.quantityOfClasses = 3;

        this.normalizer = new MultiNormalizerMinMaxScaler(-1, 1);
        
        configOfNet = new NeuralNetConfiguration.Builder()
                .graphBuilder()
                .addInputs("hi", "lo", "vo")
                .addLayer("lstm_hi", new LSTM.Builder()
                        .nIn(inputs)
                        .nOut(inputs)
                        .activation(Activation.TANH)
                        .l2(0.0001)
                        .updater(Updater.ADADELTA)
                        .learningRate(0.0001)
                        .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)
                        .gradientNormalizationThreshold(10)
                        .build(), "hi")
                .addLayer("lstm_lo", new LSTM.Builder()
                        .nIn(inputs)
                        .nOut(inputs)
                        .activation(Activation.TANH)
                        .l2(0.0001)
                        .updater(Updater.ADADELTA)
                        .learningRate(0.0001)
                        .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)
                        .gradientNormalizationThreshold(10)
                        .build(), "lo")
                .addLayer("lstm_vo", new LSTM.Builder()
                        .nIn(inputs)
                        .nOut(inputs)
                        .activation(Activation.TANH)
                        .l2(0.0001)
                        .updater(Updater.ADADELTA)
                        .learningRate(0.0001)
                        .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)
                        .gradientNormalizationThreshold(10)
                        .build(), "vo")
                //.addVertex("merge", new MergeVertex(), "lstm_hi", "lstm_lo", "lstm_vo")
                .addLayer("lstm_all", new LSTM.Builder()
                        .nIn(inputs*3)
                        .nOut(inputs)
                        .activation(Activation.SOFTSIGN)
                        .l2(0.0001)
                        .updater(Updater.ADADELTA)
                        .learningRate(0.0001)
                        .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)
                        .gradientNormalizationThreshold(10)
                        .build(), "lstm_hi", "lstm_lo", "lstm_vo")
                .addLayer("hi_out", new RnnOutputLayer.Builder(LossFunctions.LossFunction.L2)
                        .nIn(inputs)
                        .nOut(1)
                        .activation(Activation.SOFTSIGN)
                        .l2(0.0001)
                        .updater(Updater.ADADELTA)
                        .learningRate(0.0001)
                        .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)
                        .gradientNormalizationThreshold(10)
                        .build(), "lstm_all")
                .addLayer("lo_out", new RnnOutputLayer.Builder(LossFunctions.LossFunction.L2)
                        .nIn(inputs)
                        .nOut(1)
                        .activation(Activation.SOFTSIGN)
                        .l2(0.0001)
                        .updater(Updater.ADADELTA)
                        .learningRate(0.0001)
                        .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)
                        .gradientNormalizationThreshold(10)
                        .build(), "lstm_all")
                .addLayer("vo_out", new RnnOutputLayer.Builder(LossFunctions.LossFunction.L2)
                        .nIn(inputs)
                        .nOut(1)
                        .activation(Activation.SOFTSIGN)
                        .l2(0.0001)
                        .updater(Updater.ADADELTA)
                        .learningRate(0.0001)
                        .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)
                        .gradientNormalizationThreshold(10)
                        .build(), "lstm_all")
                .setOutputs("hi_out", "lo_out", "vo_out")
                .build();
 
        this.networkModel = new ComputationGraph(configOfNet);
        networkModel.init();
        networkModel.setListeners(new ScoreIterationListener(100));
        
        fullData = new ArrayList<>();
    }
    // Загрузка ранее сохранённой сети
    public NeuralNetworkComplex (int inputs, String path) throws IOException {
        
        Nd4j.getRandom().setSeed(12345);
        
        this.networkModel = ModelSerializer.restoreComputationGraph(path, true);
        this.lengthOfHistory = inputs;
        this.quantityOfClasses = 1;
        
        fullData = new ArrayList<>();
        
        isLoaded = true;
    }
    
    
    public void putData (List <Candle> candles) {
        // Если все временные ряды коротки для обучения
        if (!(candles.size() > (lengthOfHistory + 1))) throw new RuntimeException("This seria too short.");
        
        INDArray [] feturesAll = new INDArray [3];
        INDArray [] labelAll = new INDArray [3];
        // Создаем наборы данных для обучения, заполняя 3D матрицу [кол-во примеров][кол-во входных данных примера][кол-во временых смещений]
        
        feturesAll[0] = Nd4j.zeros(1, lengthOfHistory, candles.size() - (lengthOfHistory + 1));
        feturesAll[1] = Nd4j.zeros(1, lengthOfHistory, candles.size() - (lengthOfHistory + 1));
        feturesAll[2] = Nd4j.zeros(1, lengthOfHistory, candles.size() - (lengthOfHistory + 1));
        //feturesAll[3] = Nd4j.zeros(1, 7, candles.size() - (lengthOfHistory + 1));
        
        labelAll[0] = Nd4j.zeros(1, 1, candles.size() - (lengthOfHistory + 1));
        labelAll[1] = Nd4j.zeros(1, 1, candles.size() - (lengthOfHistory + 1));
        labelAll[2] = Nd4j.zeros(1, 1, candles.size() - (lengthOfHistory + 1));
        
        //Calendar calendar = new GregorianCalendar();
        
        // Свеча с индексом 0 сама старая.
        List <Double> volumes = new ArrayList<> ();
        for (int time_segment = 0; time_segment < candles.size() - (lengthOfHistory + 1); time_segment++) {
            
            int percent_hi = 0;
            int percent_lo = 0;
            
            for (int input_number = 0; input_number < lengthOfHistory; input_number++) {
                // Номелизуем показатели в процентах
                percent_hi = (int)(candles.get(time_segment + input_number + 1).getHight()/candles.get(time_segment + input_number).getHight()*100 -100);
                if (percent_hi > 50) percent_hi = 50;
                if (percent_hi < -50) percent_hi = -50;
                percent_lo = (int)(candles.get(time_segment + input_number + 1).getLow()/candles.get(time_segment + input_number).getLow()*100 -100);
                if (percent_lo > 50) percent_lo = 50;
                if (percent_lo < -50) percent_lo = -50;
                
                feturesAll[0].putScalar(0, input_number, time_segment, (double)percent_hi);
                feturesAll[1].putScalar(0, input_number, time_segment, (double)percent_lo);
                volumes.add(candles.get(time_segment + input_number + 1).getVolume());
            }
            
            // Сумируем объем за периуд
            double volume_sum = 0.0;
            for (int i = 0; i < volumes.size(); i++) {
                volume_sum += volumes.get(i);
            }
            // Заполняем массив объемов продаж
            for (int input_number = 0; input_number < lengthOfHistory; input_number++) {
                feturesAll[2].putScalar(0, input_number, time_segment, (double)((int)(candles.get(time_segment + input_number + 1).getVolume()/volume_sum * 100) -50));
            }
            
            // Заполняем день недели
            //calendar.setTime(new Date(candles.get(time_segment + lengthOfHistory + 1).getTime()));
            //feturesAll[4].putScalar(0, calendar.get(Calendar.DAY_OF_WEEK) -1, time_segment, 1);
            
            percent_hi = (int)(candles.get(time_segment + lengthOfHistory + 1).getHight()/candles.get(time_segment + lengthOfHistory).getHight()*100 -100);
            if (percent_hi > 50) percent_hi = 50;
            if (percent_hi < -50) percent_hi = -50;
            percent_lo = (int)(candles.get(time_segment + lengthOfHistory + 1).getLow()/candles.get(time_segment + lengthOfHistory).getLow()*100 -100);
            if (percent_lo > 50) percent_lo = 50;
            if (percent_lo < -50) percent_lo = -50;

            labelAll[0].putScalar(0, 0, time_segment, (double)percent_hi);
            labelAll[1].putScalar(0, 0, time_segment, (double)percent_lo);
            labelAll[2].putScalar(0, 0, time_segment, (double)((int)(candles.get(time_segment + lengthOfHistory + 1).getVolume()/volume_sum * 100 -50)));
        
            volumes.clear();
        }
        
        fullData.add(new MultiDataSet(feturesAll, labelAll));
    }

////////////////////////////////////////////
    public void prepareDateForFit () {
        
        Random r = new Random();
        
        normalizer.fitLabel(true);
        if(!isLoaded) normalizer.fit(MultiDataSet.merge(fullData));
        
        if (fullData.size() <= 1) {
            throw new RuntimeException ("Quantity of MultiDataSet less 2. Add more timeseries than 1.");
        }
        else if (fullData.size() / 10 <= 1) {
            int number = fullData.size() -1;
            number = r.nextInt(number);
            
            testData = fullData.get(number);
            fullData.remove(number);
            trainData = MultiDataSet.merge(fullData);
        }
        else{
            List <MultiDataSet> mdsTrain = new ArrayList <>();
            
            for (int i = 0; i < fullData.size() / 10; i++) {
                int number = fullData.size() -1;
                number = r.nextInt(number);

                mdsTrain.add(fullData.get(number));
                fullData.remove(number);
            }
            
            testData = MultiDataSet.merge(mdsTrain);
            trainData = MultiDataSet.merge(fullData);
        }
        
        System.out.println(testData);
        System.out.println("=================\n=================\n=================\n=================\n=================\n");
        normalizer.transform(trainData);
        normalizer.transform(testData);
        
        System.out.println(testData);
    }
     
    // Тренировать сеть: ручной режим
    public void trainingNetwork (int epoch) {
        long start = new Date().getTime();
         
        if (epoch < 1) throw new RuntimeException ("Argument of trainingNetwork can't be less 1");
        
        for(int i=0; i < epoch; i++ ) {
            networkModel.fit(trainData);
        }
        
        System.out.println("trainingNetwork: " + ((new Date().getTime() - start)/1000) + " sec.");
    }
    // Протестировать сеть
    public void testNetwork () {
        
        Random r = new Random();
        
        INDArray [] real_out = networkModel.output(testData.getFeatures());
        
        double [] mae = new double[real_out.length];
        double [] rmse = new double[real_out.length];
        
        // Расчет ошибок
        for (int d = 0; d < real_out.length; d++) {
            for (int c = 0; c < testData.getLabels(0).getColumn(0).columns(); c++) {
                mae[d] += Math.abs(real_out[d].getColumn(0).getColumn(c).getDouble(0) - testData.getLabels(d).getColumn(0).getColumn(c).getDouble(0));
                rmse[d] += Math.pow(real_out[d].getColumn(0).getColumn(c).getDouble(0) - testData.getLabels(d).getColumn(0).getColumn(c).getDouble(0), 2);
            }
            mae[d] = mae[d] / (testData.getLabels(0).getColumn(0).columns() - 1);
            rmse[d] = Math.sqrt(rmse[d] / (testData.getLabels(0).getColumn(0).columns() - 1)); 
        }
        
        System.out.println("mae[hi]: \t" + mae[0] + "\tmae[lo]: \t" + mae[1] + "\tmae[vo]: \t" + mae[2] + "\tmae[AVERAGE]: \t" + ((mae[0] + mae[1] + mae[2])/3));
        System.out.println("rmse[hi]: \t" + rmse[0] + "\trmse[lo]: \t" + rmse[1] + "\trmse[vo]: \t" + rmse[2] + "\trmse[AVERAGE]: \t" + ((rmse[0] + rmse[1] + rmse[2])/3));
        
        currPrecision = 1 - ((mae[0] + mae[1] + mae[2])/3);
        
        // Отображение случайных покаателей для наглядности степени ошибок
        System.out.println("Сравнение случайно выбранных показателей:");
        for (int d = 0; d < real_out.length; d++) {
            System.out.println("demension: " + d);
            int items = testData.getLabels(0).getColumn(0).columns() -1;
            for (int i = 0; i < 10; i++) {
                System.out.print(real_out[d].getColumn(0).getColumn(r.nextInt(items)).getDouble(0) + " <> ");
                System.out.println(testData.getLabels(d).getColumn(0).getColumn(r.nextInt(items)).getDouble(0));
            }
        }
    }
    
    // Использовать обученную сеть 
    // Свеча с индексом 0 должна находится сама новая.
    public Candle prediction (List <Candle> candles) {
        if (candles.size() != lengthOfHistory + 1) throw new RuntimeException("It needs "+ (lengthOfHistory + 1) +" candles for prediction.");
        
        INDArray [] feturesAll = new INDArray [3];
        INDArray [] labelAll = new INDArray [3];
        
        feturesAll[0] = Nd4j.zeros(lengthOfHistory);
        feturesAll[1] = Nd4j.zeros(lengthOfHistory);
        feturesAll[2] = Nd4j.zeros(lengthOfHistory);
        
        labelAll[0] = Nd4j.zeros(1);
        labelAll[1] = Nd4j.zeros(1);
        labelAll[2] = Nd4j.zeros(1);
        
        double percent_hi = 0;
        double percent_lo = 0;
                
        List <Double> volumes = new ArrayList<> ();
        for (int input_number = 0; input_number < lengthOfHistory; input_number++) {
            percent_hi = (double)((int)(candles.get(input_number).getHight()/candles.get(input_number + 1).getHight()*100 -100));
            if (percent_hi > 50) percent_hi = 50;
            if (percent_hi < -50) percent_hi = -50;
            percent_lo = (double)((int)(candles.get(input_number).getLow()/candles.get(input_number + 1).getLow()*100 -100));
            if (percent_lo > 50) percent_lo = 50;
            if (percent_lo < -50) percent_lo = -50;
            
            feturesAll[0].putScalar(lengthOfHistory - (input_number + 1), percent_hi);
            feturesAll[1].putScalar(lengthOfHistory - (input_number + 1), percent_lo);
            volumes.add(candles.get(input_number + 1).getVolume());
        }
        
        // Сумируем объем за периуд
        double volume_sum = 0.0;
        for (int i = 0; i < volumes.size(); i++) {
            volume_sum += volumes.get(i);
        }
        // Заполняем массив объемов продаж
        for (int input_number = 0; input_number < lengthOfHistory; input_number++) {
            feturesAll[2].putScalar(lengthOfHistory - (input_number + 1), (double)((int)candles.get(input_number).getVolume()/volume_sum*100 -50));
        }
        
        
        MultiDataSet predictionData = new MultiDataSet(feturesAll, labelAll);
        normalizer.transform(predictionData);
        INDArray[] output = networkModel.output(predictionData.getFeatures());
        
        normalizer.revertLabels(output);
        
        return new Candle(
                candles.get(0).getTime() + (24 * 60 * 60 * 1000), //Time
                output[0].getRow(0).getColumn(0).getDouble(0) / 100 * candles.get(0).getHight() + candles.get(0).getHight(),  //Hight
                output[1].getRow(0).getColumn(0).getDouble(0) / 100 * candles.get(0).getLow() + candles.get(0).getLow(),  //Low
                0.0, 
                0.0, 
                output[2].getRow(0).getColumn(0).getDouble(0) / 100 * candles.get(0).getVolume() + candles.get(0).getVolume() +50   //Volume
        );
    }
    
    // Сохранить сеть
    public void saveNeuralNetwork (String pathZipFile) throws IOException {
        File f = new File(pathZipFile);
        if (f.exists()) {
            f.delete();
        }
        ModelSerializer.writeModel(networkModel, pathZipFile, true);
    }
    // Сохранить нормализацию
    public void saveNormalizer(String pathFile) throws IOException {
        File f = new File(pathFile);
        if (f.exists()) {
            f.delete();
        }
        NormalizerSerializer.getDefault().write(normalizer, new File(pathFile));
    }
 
    // Востановить нормализацию
    public void loadNormalizer(String pathFile) throws Exception {
        normalizer = NormalizerSerializer.getDefault().restore(new File(pathFile));
    }
    // GET AND SET
    public double getCurrentPrecision () {
        return currPrecision;
    }
     
}
