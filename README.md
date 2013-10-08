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

#### Distance - Find N best matches
```scala
val results = model.distance(List("france"), N = 10)
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
model.pprint( model.distance(List("france", "usa")) )
```
```
                                              Word       Cosine distance
------------------------------------------------------------------------
                                       netherlands              0.691459
                                       switzerland              0.672526
                                           belgium              0.656425
                                            canada              0.641793
                                            russia              0.612469
                                                 .              .
                                                 .              .
                                                 .              .
                                           croatia              0.451900
                                            vantaa              0.450767
                                            roissy              0.448256
                                            norway              0.447392
                                              cuba              0.446168
```

```scala
model.pprint( model.distance(List("france", "usa", "usa")) )
```
```
                                              Word       Cosine distance
------------------------------------------------------------------------
                                            canada              0.631119
                                       switzerland              0.626366
                                       netherlands              0.621275
                                            russia              0.569951
                                           belgium              0.560368
                                                 .              .
                                                 .              .
                                                 .              .
                                             osaka              0.418143
                                               eas              0.417097
                                           antholz              0.415458
                                           fukuoka              0.414105
                                           zealand              0.413075
```

#### Analogy - King is to Queen, as Man is to ???
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

#### Ranking - Rank a set of words by their respective distance to search term
```scala
model.pprint( model.rank("apple", Set("orange", "soda", "lettuce")) )
```
```
                                              Word       Cosine distance
------------------------------------------------------------------------
                                            orange              0.203808
                                           lettuce              0.132007
                                              soda              0.075649
```


## Compatibility

- **[09/2013]** The code was tested to work with models trained using revision
[r33](http://word2vec.googlecode.com/svn/trunk/?p=33) of the word2vec toolkit.
It should also work with future revisions, assuming that the output format does
not change.
