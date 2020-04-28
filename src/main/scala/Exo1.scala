import scala.io.Source
import scala.io.Codec
import java.nio.charset.CodingErrorAction
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._

object freestyle extends App {
  implicit val codec: Codec = Codec("UTF-8")
  codec.onMalformedInput(CodingErrorAction.REPLACE)
  codec.onUnmappableCharacter(CodingErrorAction.REPLACE)

  var bestiaryArray = new Array[String](5)
  bestiaryArray(0) = "http://legacy.aonprd.com/bestiary/monsterIndex.html"
  bestiaryArray(1) = "http://legacy.aonprd.com//bestiary2/additionalMonsterIndex.html"
  bestiaryArray(2) = "http://legacy.aonprd.com/bestiary3/monsterIndex.html"
  bestiaryArray(3) = "http://legacy.aonprd.com/bestiary4/monsterIndex.html"
  bestiaryArray(4) = "http://legacy.aonprd.com/bestiary5/index.html"

  var monstreList = new ListBuffer[Monstre]()

  /**Récupération de tous les liens de tous les monstres dans tous les bestiaires**/
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
      if(monstreList.nonEmpty) {
        if (addMonstre.getName() contains (monstreList.last.getName())) {
          monstreList -= monstreList.last
        }
      }

      monstreList += addMonstre

      /*Passe au lien suivant*/
      indexStartLink = htmlString.indexOf("<a href=\"", indexStartLink)

    }

  }


  /**Récupération de tous les sorts d'un monstre**/

  for(elmt <- monstreList){
    val  html2 = Source.fromURL(elmt.getLink())
    val htmlString2 = html2.mkString

   val debutString = "<h1 id=\""+elmt.getName()+"\""
    var indexSpell = htmlString2.indexOf(debutString)
    indexSpell = htmlString2.indexOf("/spells/",indexSpell)
    var indexEndSpell =   htmlString2.indexOf("<h1>",indexSpell)
    /*détermine la fin des sorts d'un monstre*/
    if(indexEndSpell == -1){
      indexEndSpell = htmlString2.indexOf("<div class = \"footer\">",indexSpell)
    }
    /*Récupération des sorts*/
    while(indexSpell != -1 && (indexSpell < indexEndSpell )){

      indexSpell = htmlString2.indexOf("#",indexSpell) +"#".length
      val nomSpell = htmlString2.substring(indexSpell,htmlString2.indexOf("\"",indexSpell))
      /*ajoute les sorts que s'il n'est pas déjà présents*/
      var spellExistance = 0
      breakable{
        for(spell <- elmt.getSpells()){
          if(spell == nomSpell){
            spellExistance = 1
            break
          }
        }
      }
      if(spellExistance==0){
        elmt.addSpell(nomSpell)
      }

      /*passe au sort suivant*/
      indexSpell = htmlString2.indexOf("/spells/",indexSpell)

    }
  }

/*Affiche que le monstre avec ses sorts*/
  for(elmt <- monstreList){
    print(elmt.getName() + " => ")
    for(elmt2<- elmt.getSpells()){
      print(elmt2 + " | ")
    }
    println()
  }


  /*Transdormation de la liste en RDD*/
  val conf = new SparkConf()
    .setAppName("Spell Monstre")
    .setMaster("local[*]")
  val sc = new SparkContext(conf)
  sc.setLogLevel("ERROR")

  val resultatRDD = sc.makeRDD(monstreList) //met les data dans la rdd


  //Création d'une structure donnée avec les sorts d'un monstre puis son nom
  resultatRDD.map(el => (el.getSpells(), el.name))

  var newResult = resultatRDD.flatMap(crea => {
    var list : List[(String,String)] = List()
    //Création d'un élément de la liste pour chaque sort de chaque monstre: (Heal, Archange), (Résurrection, Archange) etc...
    crea.getSpells().foreach(spell => list = list:+((spell, crea.name)))
    list
  })
  // Pour chaque clé (sort) différentes,
  // concatène les strings des monstres pour en faire un seul string pour chaque sort
  var latestResult = newResult.reduceByKey((accum, n) => (accum + " | " + n))
  //Affiche les résultats
  latestResult.foreach(el => println(el._1 + " => " + el._2))


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

  def getSpells() : ArrayBuffer[String] = {
    return spells
  }

}



