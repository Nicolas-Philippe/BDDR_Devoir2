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

  var monstreList = new ListBuffer[Monstre]()

  /*Récupération de tous les liens de tous les monstres dans tous les bestiaires*/
  for( elmt <- bestiaryArray) {
    /*Récupération de la page */
    val html = Source.fromURL(elmt)
    val htmlString = html.mkString

    /*Récupère l'indice du début du premier lien*/
    var indexStartLink = htmlString.indexOf("class=\"index\"")
    indexStartLink = htmlString.indexOf("<a href=\"", indexStartLink)

    /*Indice qui signal la fin du bloc dans lequel on doit rechercher les liens des monstre*/
    val indexEndSearch = htmlString.indexOf("<div class = \"footer\">", indexStartLink)


    /*On s'arrête si on ne trouve plus de lien ou sinon n'est plus dans le bloc de recherche*/
    while(indexStartLink != -1 && indexStartLink < indexEndSearch ){

      indexStartLink += "<a href=\"".length
      val indexEndLink = htmlString.indexOf("\">", indexStartLink)

      /*Récupère une partie du lien puis on la complete */
      var monsterLink = htmlString.substring(indexStartLink, indexEndLink)
      monsterLink = elmt+"/../"+monsterLink

      /*Récupère le nom du monstre qui est contenu dans le lien trouvé précedement*/
      var monsterName = ""
      val indexName = htmlString.indexOf("#",indexStartLink)

      /*Il y a deux type soit nomMonstre.html#nomMonstre-complet soit juste nomMonstre.html */
      if(indexName > indexEndLink){
        monsterName = htmlString.substring(indexStartLink, htmlString.indexOf(".html", indexStartLink))
      }else{
        monsterName = htmlString.substring(indexName+1, indexEndLink)
      }

      val addMonstre = new Monstre(monsterName,monsterLink)

      /*Permet de supprimer la classe global d'un montre si c'est sur la même page*/
      if(monstreList.length > 0) {
        if (addMonstre.getName() contains (monstreList.last.getName())) {
          monstreList -= monstreList.last
        }
      }

      monstreList += addMonstre

      /*Passe au lien suivant*/
      indexStartLink = htmlString.indexOf("<a href=\"", indexStartLink)

    }

  }
  println(monstreList)
  println(monstreList.length)
  //println("taille de la liste : ", monstreList.length)



  /*Traitement des doublons*/

  /*Récupération de tous les sorts d'un monstre*/
  //var monstreArray = new Array[Monstre](monstreLinkList.length)
  val indexArrayMonstre = 0

  for(elmt <- monstreList){
    val  html2 = Source.fromURL(elmt.getLink())
    val htmlString2 = html2.mkString

    /*Récupération des sorts du monstres*/ //Attention il y a des doublons on verra plus tard
    var indexSpell = htmlString2.indexOf("<div class = \"body\">")
    indexSpell = htmlString2.indexOf("/spells/",indexSpell)
    //println("indexSpell 1 : ", indexSpell)
    while(indexSpell != -1){

      indexSpell = htmlString2.indexOf(">",indexSpell) +">".length
      val nomSpell = htmlString2.substring(indexSpell,htmlString2.indexOf("</a>",indexSpell))

      elmt.addSpell(nomSpell)
      indexSpell = htmlString2.indexOf("/spells/",indexSpell)

    }

    println(elmt)
  }
  //println(monstreArray.length)

  println(monstreList)

}

class Monstre(val name : String, val link : String) extends Serializable {
  private  var spells =  ArrayBuffer[String]()

  override def  toString() : String = {
    return "[name : " + name + ", link : " + link + "spells : "+ spells+ "]"

  }

  def addSpell(spell : String) : Unit = {
    spells += spell
  }

  def getName() : String = {
    return name
  }
  def getLink() : String = {
    return link
  }

}

