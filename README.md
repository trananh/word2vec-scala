word2vec-scala
==============


This is a Scala implementation of the [word2vec](https://code.google.com/p/word2vec/) toolkit's model representation.

This interface allows the user to access the vectors representation output by
the word2vec toolkit. It also implements example operations that can be done
on the vectors (e.g., word-distance, word-analogy).

Note that it does **NOT** implement the actual continuous bag-of-words and
skip-gram architectures for computing the vectors.  You will still need to
download and compile the original word2vec tool if you wish to train new models.


## Includes

The included model (vectors.bin) was trained on the [text8](http://mattmahoney.net/dc/text8.zip) corpus, which contains
the first 100 MB of the "clean" English Wikipedia corpus.  The following training parameters
were used:

```bash
./word2vec -train text8 -output vectors.bin -cbow 0 -size 200 -window 5 -negative 0 -hs 1 -sample 1e-3 -threads 12 -binary 1
```


## Usage


#### Load model
```scala
val model = new Word2Vec()
model.load("vectors.bin")
```


#### Distance
```scala
val results = model.distance(Set("france"), N = 10)
model.pprint(results)
```
```
                                              Word       Cosine distance
------------------------------------------------------------------------
                                           belgium              0.706633
                                             spain              0.672767
                                       netherlands              0.668178
                                             italy              0.616545
                                       switzerland              0.595572
                                        luxembourg              0.591839
                                          portugal              0.564891
                                           germany              0.549196
                                            russia              0.543569
                                           hungary              0.519036
```

```scala
model.pprint( model.distance(Set("color", "fire")) )
```
```
                                              Word       Cosine distance
------------------------------------------------------------------------
                                            colors              0.523039
                                             light              0.521632
                                            colour              0.513820
                                              glow              0.512456
                                             fires              0.507191
                                                 .              .
                                                 .              .
                                                 .              .
                                             burst              0.431774
                                           stripes              0.431767
                                          lighting              0.431510
                                             infra              0.431126
                                            purple              0.429750
```


#### Analogy
```scala
model.pprint( model.analogy("king", "queen", "man", N = 10) )
```
```
                                              Word       Cosine distance
------------------------------------------------------------------------
                                             woman              0.547376
                                              girl              0.509787
                                              baby              0.473137
                                            spider              0.450589
                                              love              0.433065
                                        prostitute              0.433034
                                             loves              0.422127
                                            beauty              0.421060
                                             bride              0.413417
                                              lady              0.406856
```




## Compatibility

- **[09/2013]** The code was tested to work with models trained using revision
[r33](http://word2vec.googlecode.com/svn/trunk/?p=33) of the word2vec toolkit.
It should also work with future revisions, assuming that the output format does
not change.

