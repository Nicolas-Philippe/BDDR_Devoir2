import scala.io.Source
import scala.io.Codec
import java.nio.charset.CodingErrorAction

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer

import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.types.{BooleanType, StringType, StructField, StructType}
import org.apache.spark.sql.DataFrame

object freestyle extends App {
  implicit val codec = Codec("UTF-8")
  codec.onMalformedInput(CodingErrorAction.REPLACE)
  codec.onUnmappableCharacter(CodingErrorAction.REPLACE)

  var bestiaryArray = new Array[String](5)
  bestiaryArray(0) = "http://legacy.aonprd.com/bestiary/monsterIndex.html"
  bestiaryArray(1) = "http://legacy.aonprd.com//bestiary2/additionalMonsterIndex.html"
  bestiaryArray(2) = "http://legacy.aonprd.com/bestiary3/monsterIndex.html"
  bestiaryArray(3) = "http://legacy.aonprd.com/bestiary4/monsterIndex.html"
  bestiaryArray(4) = "http://legacy.aonprd.com/bestiary5/index.html"

  var monstreList = new ListBuffer[String]()
  /*Récupération de tous les monstres dans tous les bestiaires*/
  for( elmt <- bestiaryArray) {
    val html = Source.fromURL(elmt)
    val htmlString = html.mkString

    var indexBody = htmlString.indexOf("class=\"index\"")
    indexBody = htmlString.indexOf("<a href=\"", indexBody)
    val indexEnd = htmlString.indexOf("<div class = \"footer\">", indexBody)

    /*Récupération de tous les liens des monstres d'une page */

    while(indexBody != -1 && indexBody < indexEnd ){
      println("nouveau tour : ")
      indexBody += "<a href=\"".length

      var monsterLink = htmlString.substring(indexBody, htmlString.indexOf("\">", indexBody))

      println("monster link : ", monsterLink)
      if(!monstreList.isEmpty){
        /*Permet de supprimer la classe global d'un montre si c'est sur la même page*/

         monsterLink = elmt+"/../"+monsterLink
        if(monsterLink contains(monstreList.last)) {
          monstreList-= monstreList.last
        }
      }else{
        monsterLink = elmt+"/../"+monsterLink
      }

      monstreList += monsterLink
      println(monstreList)
      indexBody = htmlString.indexOf("<a href=\"", indexBody)
    }

  }

  println("taille de la liste : ", monstreList.length)

  /*
  /*Traitement des doublons*/

  /*Récupération de tous les sorts d'un monstre*/
  var monstreArray = new Array[Monstre](monstreList.length)
  val indexArrayMonstre = 0

  for(elmt <- monstreList){
    var  html2 = Source.fromURL(elmt)
    val htmlString2 = html2.mkString
    /*Récupération du nom du monstre */
    var indexNom = htmlString2.indexOf("monster-header") + "monster-header\">".length
    var nomMonstre =  htmlString2.substring(indexNom, htmlString2.indexOf("</h1>", indexNom))
    println("nom monstre", nomMonstre)
    monstreArray(indexArrayMonstre) = new Monstre(nomMonstre)
    /*Récupération des sorts du monstres*/
    var indexSpell = indexNom
    indexSpell = htmlString2.indexOf("/spells/",indexSpell)
    println("indexSpell 1 : ", indexSpell)
    while(indexSpell != -1){

      indexSpell = htmlString2.indexOf("<em>",indexSpell)+ "<em>".length
      println("indexSpell 2 : ", indexSpell)
      var nomSpell = htmlString2.substring(indexSpell,htmlString2.indexOf("</em>",indexSpell))
      //println("nom spell : ",nomSpell)
      monstreArray(indexArrayMonstre).addSpell(nomSpell)
      indexSpell = htmlString2.indexOf("/spells/",indexSpell)
      println("indexSpell 3 : ", indexSpell)
    }

  }

   */

}

class Monstre(val name : String) extends Serializable {
  var spells =  ArrayBuffer[String]()
  def addSpell(spell : String) : Unit = {
    spells += spell
  }
}