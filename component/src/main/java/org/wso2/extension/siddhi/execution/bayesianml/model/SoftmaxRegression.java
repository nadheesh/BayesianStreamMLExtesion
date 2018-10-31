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
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.wso2.extension.siddhi.execution.bayesianml.distribution.CategoricalDistribution;
import org.wso2.extension.siddhi.execution.bayesianml.distribution.NormalDistribution;

import static org.nd4j.linalg.ops.transforms.Transforms.softmax;

import java.util.HashMap;

/**
 * implements Bayesian Softmax regression model.
 * <p>
 * minimize the negative ELBO
 * ELBO = E[log(P(yVar|xVar,weights)] - D_KL[weights,prior]
 */
public class SoftmaxRegression extends BaseModel {

    private static final Logger logger = Logger.getLogger(SoftmaxRegression.class.getName());

    private NormalDistribution weights;
    private SDVariable loss;

    // model specific configurations
    private int numClasses;


    public SoftmaxRegression() {
        super();
        numClasses = 2;
    }

    @Override
    SDVariable[] specifyModel() {

        // initiateModel placeholders
        this.xVar = sd.var("xVar", 1, numFeatures);
        this.yVar = sd.var("yVar", 1);


        // initiateModel trainable variables
        SDVariable weightLoc, weightScale;
        weightLoc = sd.var("wLoc", numFeatures, numClasses);
        weightScale = sd.softplus("wScale", sd.var(numFeatures, numClasses)); // softplus ensures non-zero scale

        // construct the variational distribution for weights
        weights = new NormalDistribution(weightLoc, weightScale, sd);
        // construct the prior distribution for weigths
//        NormalDistribution prior = new NormalDistribution(sd.var(Nd4j.ones(numFeatures, 1).mul(priorLoc)),
//                sd.var(Nd4j.ones(numFeatures, 1).mul(priorScale)), sd);

        // computing the log-likelihood loss
        SDVariable[] logpArr = new SDVariable[numSamples];
        for (int i = 0; i < numSamples; i++) {
            SDVariable logits = xVar.mmul(weights.sample()); // logits
            CategoricalDistribution likelihood = new CategoricalDistribution(logits, sd);
            logpArr[i] = likelihood.logProbability(yVar);
        }
        SDVariable logpLoss = sd.neg(sd.mergeAvg(logpArr));

        loss = logpLoss;

        return new SDVariable[]{weightLoc, weightScale};
//        try {
//            SDVariable klLoss = weights.klDivergence(prior);
//            loss = logpLoss.add(klLoss);
//        } catch (NotImplementedException e) {
//            loss = logpLoss;
//        }

        // setting the updaters
    }


    @Override
    INDArray predictionFromMean(INDArray predictiveMean) {
        return predictiveMean.argMax();
    }

    @Override
    protected double[][] getUpdatedWeights() {
        return new double[][]{weights.getLoc().getArr().reshape(-1).toDoubleVector(),
                weights.getScale().getArr().reshape(-1).toDoubleVector()};
    }

    @Override
    public HashMap<String, Double> evaluate(INDArray features) {
        return null;
    }

    @Override
    INDArray estimatePredictiveDistribution(double[] features, int nSamples) {
        INDArray featureArr = Nd4j.create(features);

        INDArray loc, scale;

        loc = this.weights.getLoc().getArr().reshape(numFeatures * numClasses, 1);
        scale = this.weights.getScale().getArr().reshape(numFeatures * numClasses, 1);

        INDArray zSamples = Nd4j.randn(new long[]{numFeatures * numClasses, nSamples});

        INDArray weights = zSamples.mulColumnVector(scale).
                addColumnVector(loc).reshape(new int[]{numFeatures, numClasses * nSamples});
        INDArray predLogits = featureArr.mmul(weights).reshape(numClasses, nSamples);
        return softmax(predLogits.transpose()).transpose();
    }

    public void setNumClasses(int val) {
        numClasses = val;
    }


}
