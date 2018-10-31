package org.wso2.extension.siddhi.execution.bayesianml.model;

import org.apache.log4j.Logger;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.testng.annotations.Test;
import org.wso2.extension.siddhi.execution.bayesianml.exception.InvalidInputValueException;

/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
public class LinearRegressionTest {

    private static final Logger logger = Logger.getLogger(LinearRegressionTest.class.getName());

    @Test
    public void testLinearRegression() throws InvalidInputValueException {

        int n, d;
        double[] locWeights = {};
        INDArray data, targets, w;
        n = 1000;
        d = 20;

        data = Nd4j.rand(new int[]{n, d}, 42);
        w = Nd4j.randn(new int[]{d, 1}, 42);
        targets = data.mmul(w);


        LinearRegression model = new LinearRegression();

        model.setNumFeatures(d);
        model.setLearningRate(0.05);
        model.setNumSamples(1);

        model.initiateModel();

//        double[] w = {0.8, -0.43};
//        double[][] data = {
//                new double[]{1.0, 1.0},
//                new double[]{0.9, 0.89},
//                new double[]{0.0, 0.0},
//                new double[]{0.01, 0.4},
//                new double[]{0.80, 0.81},
//                new double[]{0.93, 0.71},
//                new double[]{0.02, 0.30},
//                new double[]{0.29, 0.24},
//
//                new double[]{1.0, 1.0},
//                new double[]{0.9, 0.89},
//                new double[]{0.0, 0.0},
//                new double[]{0.01, 0.4},
//                new double[]{0.80, 0.81},
//                new double[]{0.93, 0.71},
//                new double[]{0.02, 0.30},
//                new double[]{0.29, 0.24},
//                new double[]{1.0, 1.0},
//                new double[]{0.9, 0.89},
//                new double[]{0.0, 0.0},
//                new double[]{0.01, 0.4},
//                new double[]{0.80, 0.81},
//                new double[]{0.93, 0.71},
//                new double[]{0.02, 0.30},
//                new double[]{0.29, 0.24},new double[]{1.0, 1.0},
//                new double[]{0.9, 0.89},
//                new double[]{0.0, 0.0},
//                new double[]{0.01, 0.4},
//                new double[]{0.80, 0.81},
//                new double[]{0.93, 0.71},
//                new double[]{0.02, 0.30},
//                new double[]{0.29, 0.24}
//        };

//        INDArray locWeights = Nd4j.create(1);
//        double[] targets = {0.37,0.3373,0,-0.164,0.2917, 0.4387,-0.113,0.1288,
//                0.37,0.3373,0,-0.164,0.2917, 0.4387,-0.113,0.1288,
//                0.37,0.3373,0,-0.164,0.2917, 0.4387,-0.113,0.1288,
//                0.37,0.3373,0,-0.164,0.2917, 0.4387,-0.113,0.1288};


        for (int i = 0; i < data.shape()[0]; i++) {
            double[] features = data.getRow(i).toDoubleVector();
            double[] target = targets.getRow(i).toDoubleVector();
            locWeights = model.update(features, target)[0];
        }

        SameDiff sd = SameDiff.create();

        // pred based on sampling
        int predSamples = 1000;
        model.setPredictionSamples(predSamples);
        double[] predArr = new double[n];
        for (int i = 0; i < data.shape()[0]; i++) {
            double[] features = data.getRow(i).toDoubleVector();
            predArr[i] = model.predict(features)[0];
        }

        double error1 = targets.squaredDistance(Nd4j.create(predArr)) / n;

        logger.info(error1);

        SDVariable var1 = sd.var(targets);
        SDVariable var2 = sd.var(Nd4j.create(predArr, new int[]{n, 1}));
        double error2 = sd.abs(sd.square(var1.sub(var2)).div(var1).mul(100)).mean().eval().toDoubleVector()[0];

        logger.info(error2);

        // pred based on point estimations
        INDArray pred1 = data.mmul(Nd4j.create(locWeights, new int[]{d, 1}));

        error1 = targets.squaredDistance(pred1) / n;

        logger.info(error1);

        var1 = sd.var(targets);
        var2 = sd.var(pred1);
        error2 = sd.abs(sd.square(var1.sub(var2)).div(var1).mul(100)).mean().eval().toDoubleVector()[0];

        logger.info(error2);
//        double precision = 0.05;
////        AssertJUnit.assertArrayEquals(w.toDoubleVector(), locWeights.toDoubleVector(), precision);
//
//        int predSamples = 10000;
//        AssertJUnit.assertEquals(-0.1032,
//                model.predict(new double[]{0.0, 0.24}, predSamples)[0], precision);
//        AssertJUnit.assertEquals(0.084,
//                model.predict(new double[]{0.32, 0.40}, predSamples)[0], precision);
//        AssertJUnit.assertEquals(0.2105,
//                model.predict(new double[]{0.72, 0.85}, predSamples)[0], precision);
    }

//    @Test
//    public void testPerceptron4Dimensions() {
//        // Perceptron will work only with linearly separable datasets
//        PerceptronModel model = new PerceptronModel();
//        model.update(true, new double[]{1.0, 1.0, 0.2, 0.13});
//        model.update(true, new double[]{0.9, 0.89, 0.3, 0.02});
//        model.update(false, new double[]{0.0, 0.0, 1.0, 0.82});
//        model.update(false, new double[]{0.01, 0.4, 0.77, 0.92});
//        model.update(true, new double[]{0.80, 0.81, 0.11, 0.13});
//        model.update(false, new double[]{0.02, 0.30, 0.88, 0.76});
//        model.update(true, new double[]{0.93, 0.71, 0.02, 0.122});
//        model.update(false, new double[]{0.29, 0.24, 0.98, 0.65});
//
//        AssertJUnit.assertEquals(false, model.classify(new double[]{0.0, 0.0, 0.90, 0.62})[0]);
//        AssertJUnit.assertEquals(false, model.classify(new double[]{0.0, 0.0, 0.77, 1.0})[0]);
//        AssertJUnit.assertEquals(true, model.classify(new double[]{0.990, 0.807, 0.12, 0.15})[0]);
//    }

}
