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

package org.wso2.extension.siddhi.execution.bayesianml.model;

import org.apache.log4j.Logger;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.testng.annotations.Test;
import org.wso2.extension.siddhi.execution.bayesianml.exception.InvalidInputValueException;

import static org.nd4j.linalg.ops.transforms.Transforms.sigmoid;


public class SoftmaxRegressionTest {

    private static final Logger logger = Logger.getLogger(SoftmaxRegressionTest.class.getName());

    @Test
    public void testSoftmaxRegression() throws InvalidInputValueException {

        int n, d;
        double[] locWeights = {};
        INDArray data, targets, w;
        n = 1000;
        d = 5;

        SoftmaxRegression model = new SoftmaxRegression();
        model.setNumClasses(2);
        model.setNumFeatures(d);
        model.setLearningRate(0.05);
        model.setNumSamples(1);

        model.initiateModel();


        data = Nd4j.rand(new int[]{n, d}, -1, 1,
                Nd4j.getRandomFactory().getNewRandomInstance());
        w = Nd4j.create(new double[]{0.8, -1.2, 2.5, -0.8, 3.3}, new int[]{d, 1});
        targets = sigmoid(data.mmul(w)).gt(0.5);
        targets = Nd4j.concat(1, targets, targets.mul(-1).add(1));

        for (int i = 0; i < data.shape()[0]; i++) {
            double[] features = data.getRow(i).toDoubleVector();
            double[] target = targets.getRow(i).toDoubleVector();
            locWeights = model.update(features, target)[0];
        }

//        double precision = 0.001;
//        AssertJUnit.assertArrayEquals(w.toDoubleVector(), locWeights.toDoubleVector(), precision);

//        double[][] testData = new double[][]{
//                new double[]{0.32, 0.40},
//                new double[]{1, -0.85},
//                new double[]{0.0, 0.24},
//        };
//        double[] expected = sigmoid(Nd4j.create(testData).mmul(w)).gt(0.5).toDoubleVector();

        int predSamples = 1000;

        model.setPredictionSamples(predSamples);
        double[][] testData = data.toDoubleMatrix();
        double[] expected = targets.argMax(1).toDoubleVector();

        int count = 0;
        for (int i = 0; i < testData.length; i++) {
            double[] pred = model.predict(testData[i]);
            if ((int) pred[0] != (int) expected[i]) {
                count++;
            }
//            AssertJUnit.assertEquals(expected[i],
//                    model.predict(Nd4j.create(testData[i]), pred_samples).toDoubleVector()[0], precision);
        }
        logger.info(1 - ((count + 0.0) / testData.length));
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
