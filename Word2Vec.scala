// Copyright 2013 trananh
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

import java.io._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer


/** A simple binary file reader.
  * @constructor Create a binary file reader.
  * @param file The binary file to be read.
  *
  * @author trananh
  */
class VecBinaryReader(val file: File) {

  /** Overloaded constructor */
  def this(filename: String) = this(new File(filename))

  /** ASCII values for common delimiter characters */
  private val SPACE = 32
  private val LF = 10

  /** Open input streams */
  private val fis = new FileInputStream(file)
  private val bis = new BufferedInputStream(fis)
  private val dis = new DataInputStream(bis)

  /** Close the stream. */
  def close() {
    dis.close()
    bis.close()
    fis.close()
  }

  /** Read the next byte.
    * @return The next byte from the file.
    */
  def read(): Byte = dis.readByte()

  /** Read the next token as a string, using the provided delimiters as breaking points.
    * @param delimiters ASCII code of delimiter characters (default to SPACE and LINE-FEED).
    * @return String representation of the next token.
    */
  def readToken(delimiters: Set[Int] = Set(SPACE, LF)): String = {
    val bytes = new ArrayBuffer[Byte]()
    val sb = new StringBuilder()
    var byte = dis.readByte()
    while (!delimiters.contains(byte)) {
      bytes.append(byte)
      byte = dis.readByte()
    }
    sb.append(new String(bytes.toArray[Byte])).toString()
  }

  /** Read next 4 bytes as a floating-point number.
    * @return The floating-point value of the next 4 bytes.
    */
  def readFloat(): Float = {
    // We need to reverse the byte order here due to endian-compatibility.
    java.lang.Float.intBitsToFloat(java.lang.Integer.reverseBytes(dis.readInt()))
  }

}


/** A Scala port of the word2vec model.  This interface allows the user to access the vector representations
  * output by the word2vec tool, as well as perform some common operations on those vectors.  It does NOT
  * implement the actual continuous bag-of-words and skip-gram architectures for computing the vectors.
  *
  * More information on word2vec can be found here: https://code.google.com/p/word2vec/
  *
  * Example usage:
  * {{{
  * val model = new Word2Vec()
  * model.load("vectors.bin")
  * val results = model.distance(Set("france"), N = 10)
  * 
  * model.pprint(results)
  * }}}
  *
  * @constructor Create a word2vec model.
  *
  * @author trananh
  */
class Word2Vec {

  /** Map of words and their associated vector representations */
  private val vocab = new mutable.HashMap[String, Array[Float]]()

  /** Number of words */
  private var numWords = 0

  /** Number of floating-point values associated with each word (i.e., length of the vectors) */
  private var vecSize = 0

  /** Load data from a binary file.
    * @param filename Path to file containing word projections in the BINARY FORMAT.
    */
  def load(filename: String): Unit = load(new File(filename))

  /** Load data from a binary file.
    * @param file The file containing word projections in the BINARY FORMAT.
    */
  def load(file: File): Unit = {
    // Check edge case
    if (!file.exists()) {
      throw new FileNotFoundException("Binary vector file not found <" + file.toString + ">")
    }

    // Create new reader to read data
    val reader = new VecBinaryReader(file)

    // Read header info
    numWords = Integer.parseInt(reader.readToken())
    vecSize = Integer.parseInt(reader.readToken())

    // Read the vocab words and their associated vector representations
    var word = ""
    val vector = new Array[Float](vecSize)
    for (_ <- 0 until numWords) {
      // Read the word
      word = reader.readToken()

      // Read the vector representation (each vector contains vecSize number of floats)
      var len = 0f
      for (i <- 0 until vector.length) {
        vector(i) = reader.readFloat()
        len += (vector(i) * vector(i))
      }

      // Find the magnitude of the vector so we can normalize each value by the magnitude
      len = Math.sqrt(len).asInstanceOf[Float]

      // Store the normalized vector representation, keyed by the word
      vocab.put(word, vector.map(_ / len))

      // Eat up the next delimiter character
      reader.read()
    }

    // Finally, close the reader
    reader.close()
  }

  /** Check if the word is present in the vocab map.
    * @param word Word to be checked.
    * @return True if the word is in the vocab map.
    */
  def contains(word: String): Boolean = {
    vocab.get(word).isDefined
  }

  /** Compute the cosine similarity score between two normalized vectors.
    * @param vec1 The first vector.
    * @param vec2 The other vector.
    * @return The cosine similarity score of the two vectors.
    */
  private def cosine(vec1: Array[Float], vec2: Array[Float]): Float = {
    assert(vec1.length == vec2.length, "Uneven vectors!")
    var dist = 0f
    for (i <- 0 until vec1.length) dist += (vec1(i) * vec2(i))
    dist
  }

  /** Compute the cosine similarity score between the vector representations of the words.
    * @param word1 The first word.
    * @param word2 The other word.
    * @return The cosine similarity score between the vector representations of the words.
    */
  def cosine(word1: String, word2: String): Float = {
    assert(contains(word1) || contains(word2))
    cosine(vocab.get(word1).get, vocab.get(word2).get)
  }

  /** Find the N closest terms in the vocab to the input word(s).
    * @param input The input word(s).
    * @param N The maximum number of terms to return (default to 40).
    * @return The N closest terms in the vocab to the input word(s) and their associated cosine similarity scores.
    */
  def distance(input: List[String], N: Integer = 40): List[(String, Float)] = {
    // Check for edge cases
    if (input.size == 0) return List[(String, Float)]()
    input.foreach(w => {
      if (!contains(w)) {
        println("Out of dictionary word!")
        return List[(String, Float)]()
      }
    })

    // Find the vector representation for the input. If multiple words, then aggregate (sum) their vectors.
    val vector = new Array[Float](vecSize)
    input.foreach(w => for (j <- 0 until vector.length) vector(j) += vocab.get(w).get(j))

    // Normalize the vector representation of the input.
    var len = 0f
    for (i <- 0 until vector.length) len += (vector(i) * vector(i))
    len = Math.sqrt(len).asInstanceOf[Float]
    for (i <- 0 until vector.length) vector(i) /= len

    // Maintain the top/closest terms using a priority queue, ordered by the cosine similarity score.
    // Note: We invert the distance here because a priority queue will dequeue the highest priority element,
    //       but we would like it to dequeue the lowest scoring element instead.
    val top = new mutable.PriorityQueue[(String, Float)]()(Ordering.by(-_._2))

    // Iterate over each token in the vocab and compute its cosine score to the input.
    var dist = 0f
    val inputSet = input.toSet[String]
    vocab.foreach(entry => {
      // Skip tokens that are in the input.
      if (!inputSet.contains(entry._1)) {
        dist = cosine(vector, entry._2)
        top.enqueue((entry._1, dist))
        if (top.length > N) {
          // If the queue contains over N elements, then dequeue the highest priority element
          // (which will be the element with the lowest cosine score).
          top.dequeue()
        }
      }
    })

    // Return the top N results as a sorted list.
    assert(top.length <= N)
    top.toList.sortWith(_._2 > _._2)
  }

  /** Find the N closest terms in the vocab to the analogy:
    * - [word1] is to [word2] as [word3] is to ???
    *
    * The algorithm operates as follow:
    * - Find a vector approximation of the missing word = vec([word2]) - vec([word1]) + vec([word3]).
    * - Return words closest to the approximated vector.
    *
    * @param word1 First word in the analogy [word1] is to [word2] as [word3] is to ???.
    * @param word2 Second word in the analogy [word1] is to [word2] as [word3] is to ???
    * @param word3 Third word in the analogy [word1] is to [word2] as [word3] is to ???.
    * @param N The maximum number of terms to return (default to 40).
    *
    * @return The N closest terms in the vocab to the analogy and their associated cosine similarity scores.
    */
  def analogy(word1: String, word2: String, word3: String, N: Integer = 40): List[(String, Float)] = {
    // Check for edge cases
    if (!contains(word1) || !contains(word2) || !contains(word3)) {
      println("Out of dictionary word!")
      return List[(String, Float)]()
    }

    // Find the vector approximation for the missing analogy.
    val vector = new Array[Float](vecSize)
    for (j <- 0 until vector.length)
      vector(j) = vocab.get(word2).get(j) - vocab.get(word1).get(j) + vocab.get(word3).get(j)

    // Normalize the vector representation.
    var len = 0f
    for (i <- 0 until vector.length) len += (vector(i) * vector(i))
    len = Math.sqrt(len).asInstanceOf[Float]
    for (i <- 0 until vector.length) vector(i) /= len

    // Maintain the top/closest terms using a priority queue, ordered by the cosine similarity score.
    // Note: We invert the distance here because a priority queue will dequeue the highest priority element,
    //       but we would like it to dequeue the lowest scoring element instead.
    val top = new mutable.PriorityQueue[(String, Float)]()(Ordering.by(-_._2))

    // Iterate over each token in the vocab and compute its cosine score to the input.
    var dist = 0f
    vocab.foreach(entry => {
      // Skip tokens that are in the input.
      if (!word1.equals(entry._1) && !word2.equals(entry._1) && !word3.equals(entry._1)) {
        dist = cosine(vector, entry._2)
        top.enqueue((entry._1, dist))
        if (top.length > N) {
          // If the queue contains over N elements, then dequeue the highest priority element
          // (which will be the element with the lowest cosine score).
          top.dequeue()
        }
      }
    })

    // Return the top N results as a sorted list.
    assert(top.length <= N)
    top.toList.sortWith(_._2 > _._2)
  }

  /** Pretty print the list of words and their associated scores.
    * @param words List of (word, score) pairs to be printed.
    */
  def pprint(words: List[(String, Float)]) = {
    println("\n%50s".format("Word") + (" " * 7) + "Cosine distance\n" + ("-" * 72))
    println(words.map(s => "%50s".format(s._1) + (" " * 7) + "%15f".format(s._2)).mkString("\n"))
  }

}


/** ********************************************************************************
  * Demo of the Scala ported word2vec model.
  * ********************************************************************************
  */
object Word2Vec {

  /** Demo. */
  def main(args: Array[String]) {
    // Load word2vec model from binary file.
    val model = new Word2Vec()
    model.load("./vectors.bin")

    // distance: Find N closest words
    model.pprint(model.distance(List("france"), N = 10))
    model.pprint(model.distance(List("france", "usa")))
    model.pprint(model.distance(List("france", "usa", "usa")))

    // analogy: "king" is to "queen", as "man" is to ?
    model.pprint(model.analogy("king", "queen", "man", N = 10))
  }

}
