package testGit

import java.util

import org.apache.spark.SparkConf

object scala extends App {

  val conf = new SparkConf()
    .setAppName("AAAAAAAAAAAAAAAAAh")
    .setMaster("local[*]")
  val sc = new org.apache.spark.SparkContext(conf)
  sc.setLogLevel("ERROR")


  // Créa -> sort1, sort2

  class Creature(var name: String = "", var spellList: Array[String]) extends java.io.Serializable {}
  var creatureArray = new Array[Creature](3)
  creatureArray(0) = new Creature("Archange de lumière", Array("Bénédiction", "Résurrection"))
  creatureArray(1) = new Creature("Prêtre", Array("Bénédiction", "Soin"))
  creatureArray(2) = new Creature("Mage", Array("Soin", "Invocation d'un familier", "Boule de feu"))
  val result = sc.makeRDD(creatureArray)
  // faudrait avoir: (1, A), (2, A)
  //                 (2, B), (3, B)
  //                 (1, C), (3, C)
  result.map(el => (el.spellList.last, el.name))

  var newResult = result.flatMap(crea => {
    //var list = Set[Tuple2]()
    var list : List[(String,String)] = List()
    crea.spellList.foreach(spell => list = list:+((spell, crea.name)))
    list
  })
  var latestResult = newResult.reduceByKey((accum, n) => (accum + " | " + n))
  latestResult.foreach(el => println(el._1 + " => " + el._2))
}
