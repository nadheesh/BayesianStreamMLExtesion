# API Docs - v1.0.0-SNAPSHOT

## Streamingml

### bayesianRegression *<a target="_blank" href="https://wso2.github.io/siddhi/documentation/siddhi-4.0/#stream-processor">(Stream Processor)</a>*

<p style="word-wrap: break-word">This extension predicts using a Bayesian linear regression model.</p>

<span id="syntax" class="md-typeset" style="display: block; font-weight: bold;">Syntax</span>
```
streamingml:bayesianRegression(<STRING> model.name, <INT> prediction.samples, <DOUBLE> model.features)
```

<span id="query-parameters" class="md-typeset" style="display: block; color: rgba(0, 0, 0, 0.54); font-size: 12.8px; font-weight: bold;">QUERY PARAMETERS</span>
<table>
    <tr>
        <th>Name</th>
        <th style="min-width: 20em">Description</th>
        <th>Default Value</th>
        <th>Possible Data Types</th>
        <th>Optional</th>
        <th>Dynamic</th>
    </tr>
    <tr>
        <td style="vertical-align: top">model.name</td>
        <td style="vertical-align: top; word-wrap: break-word">The name of the model to be used</td>
        <td style="vertical-align: top"></td>
        <td style="vertical-align: top">STRING</td>
        <td style="vertical-align: top">No</td>
        <td style="vertical-align: top">No</td>
    </tr>
    <tr>
        <td style="vertical-align: top">prediction.samples</td>
        <td style="vertical-align: top; word-wrap: break-word">The number of samples to be drawn to estimate the prediction</td>
        <td style="vertical-align: top">1000</td>
        <td style="vertical-align: top">INT</td>
        <td style="vertical-align: top">Yes</td>
        <td style="vertical-align: top">No</td>
    </tr>
    <tr>
        <td style="vertical-align: top">model.features</td>
        <td style="vertical-align: top; word-wrap: break-word">The features of the model that need to be attributes of the stream</td>
        <td style="vertical-align: top"></td>
        <td style="vertical-align: top">DOUBLE</td>
        <td style="vertical-align: top">No</td>
        <td style="vertical-align: top">No</td>
    </tr>
</table>
<span id="extra-return-attributes" class="md-typeset" style="display: block; font-weight: bold;">Extra Return Attributes</span>
<table>
    <tr>
        <th>Name</th>
        <th style="min-width: 20em">Description</th>
        <th>Possible Types</th>
    </tr>
    <tr>
        <td style="vertical-align: top">prediction</td>
        <td style="vertical-align: top; word-wrap: break-word">The predicted value (double)</td>
        <td style="vertical-align: top">DOUBLE</td>
    </tr>
    <tr>
        <td style="vertical-align: top">confidence</td>
        <td style="vertical-align: top; word-wrap: break-word">Standard deviation of the predictive distribution</td>
        <td style="vertical-align: top">DOUBLE</td>
    </tr>
</table>

<span id="examples" class="md-typeset" style="display: block; font-weight: bold;">Examples</span>
<span id="example-1" class="md-typeset" style="display: block; color: rgba(0, 0, 0, 0.54); font-size: 12.8px; font-weight: bold;">EXAMPLE 1</span>
```
define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 double, attribute_3 double);

from StreamA#streamingml:bayesianRegression('model1', attribute_0, attribute_1, attribute_2, attribute_3) 
insert all events into OutputStream;
```
<p style="word-wrap: break-word">This query uses a Bayesian regression model named <code>model1</code> to predict the label of the feature vector represented by <code>attribute_0</code>, <code>attribute_1</code>, <code>attribute_2</code>, and <code>attribute_3</code>. The predicted value is emitted to the <code>OutputStream</code> streamalong with the prediction confidence (std of predictive distribution) and the feature vector. As a result, the OutputStream stream is defined as follows: (attribute_0 double, attribute_1 double, attribute_2 double, attribute_3 double, prediction double, confidence double).</p>

### updateBayesianRegression *<a target="_blank" href="https://wso2.github.io/siddhi/documentation/siddhi-4.0/#stream-processor">(Stream Processor)</a>*

<p style="word-wrap: break-word">This extension builds/updates a linear Bayesian regression model.</p>

<span id="syntax" class="md-typeset" style="display: block; font-weight: bold;">Syntax</span>
```
streamingml:updateBayesianRegression(<STRING> model.name, <DOUBLE|INT> model.target, <INT> model.samples, <STRING> model.optimizer, <DOUBLE> learning.rate, <DOUBLE> model.features)
```

<span id="query-parameters" class="md-typeset" style="display: block; color: rgba(0, 0, 0, 0.54); font-size: 12.8px; font-weight: bold;">QUERY PARAMETERS</span>
<table>
    <tr>
        <th>Name</th>
        <th style="min-width: 20em">Description</th>
        <th>Default Value</th>
        <th>Possible Data Types</th>
        <th>Optional</th>
        <th>Dynamic</th>
    </tr>
    <tr>
        <td style="vertical-align: top">model.name</td>
        <td style="vertical-align: top; word-wrap: break-word">The name of the model to be built/updated.</td>
        <td style="vertical-align: top"></td>
        <td style="vertical-align: top">STRING</td>
        <td style="vertical-align: top">No</td>
        <td style="vertical-align: top">No</td>
    </tr>
    <tr>
        <td style="vertical-align: top">model.target</td>
        <td style="vertical-align: top; word-wrap: break-word">The target attribute (dependant variable) of the dataset</td>
        <td style="vertical-align: top"></td>
        <td style="vertical-align: top">DOUBLE<br>INT</td>
        <td style="vertical-align: top">No</td>
        <td style="vertical-align: top">No</td>
    </tr>
    <tr>
        <td style="vertical-align: top">model.samples</td>
        <td style="vertical-align: top; word-wrap: break-word">Number of samples used to construct the gradients</td>
        <td style="vertical-align: top">1</td>
        <td style="vertical-align: top">INT</td>
        <td style="vertical-align: top">Yes</td>
        <td style="vertical-align: top">No</td>
    </tr>
    <tr>
        <td style="vertical-align: top">model.optimizer</td>
        <td style="vertical-align: top; word-wrap: break-word">The type of optimization used</td>
        <td style="vertical-align: top">ADAM</td>
        <td style="vertical-align: top">STRING</td>
        <td style="vertical-align: top">Yes</td>
        <td style="vertical-align: top">No</td>
    </tr>
    <tr>
        <td style="vertical-align: top">learning.rate</td>
        <td style="vertical-align: top; word-wrap: break-word">The learning rate of the updater</td>
        <td style="vertical-align: top">0.05</td>
        <td style="vertical-align: top">DOUBLE</td>
        <td style="vertical-align: top">Yes</td>
        <td style="vertical-align: top">No</td>
    </tr>
    <tr>
        <td style="vertical-align: top">model.features</td>
        <td style="vertical-align: top; word-wrap: break-word">Features of the model that need to be attributes of the stream.</td>
        <td style="vertical-align: top"></td>
        <td style="vertical-align: top">DOUBLE</td>
        <td style="vertical-align: top">No</td>
        <td style="vertical-align: top">No</td>
    </tr>
</table>
<span id="extra-return-attributes" class="md-typeset" style="display: block; font-weight: bold;">Extra Return Attributes</span>
<table>
    <tr>
        <th>Name</th>
        <th style="min-width: 20em">Description</th>
        <th>Possible Types</th>
    </tr>
    <tr>
        <td style="vertical-align: top">featureWeight</td>
        <td style="vertical-align: top; word-wrap: break-word">Weight of the &lt;feature.name&gt; of the model.</td>
        <td style="vertical-align: top">DOUBLE</td>
    </tr>
    <tr>
        <td style="vertical-align: top">featureConfidence</td>
        <td style="vertical-align: top; word-wrap: break-word">Weight of the &lt;feature.name&gt; of the model.</td>
        <td style="vertical-align: top">DOUBLE</td>
    </tr>
</table>

<span id="examples" class="md-typeset" style="display: block; font-weight: bold;">Examples</span>
<span id="example-1" class="md-typeset" style="display: block; color: rgba(0, 0, 0, 0.54); font-size: 12.8px; font-weight: bold;">EXAMPLE 1</span>
```
define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 double, attribute_3 double, attribute_4 double );

from StreamA#streamingml:updateBayesianRegression('model1', attribute_4, 0.01, attribute_0, attribute_1, attribute_2, attribute_3) 
insert all events into outputStream;
```
<p style="word-wrap: break-word">This query builds/updates a Bayesian Linear regression model named <code>model1</code> with a <code>0.01</code> learning rate using <code>attribute_0</code>, <code>attribute_1</code>, <code>attribute_2</code>, and <code>attribute_3</code> as features, and <code>attribute_4</code> as the label. Updated weights of the model are emitted to the OutputStream stream.</p>

