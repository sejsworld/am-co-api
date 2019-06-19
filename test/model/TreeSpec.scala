package model


import controllers.Model.Node
import controllers.Tree
import org.scalatestplus.play._

import scala.collection.mutable

class TreeSpec extends PlaySpec {

  "A Tree" must {
    "return empty list for root children when only one node excists" in {
      Tree.children("root") mustBe Nil
    }
    "add nodes to root" in {
      Tree.addNode("A","root")
      Tree.addNode("B","root")
      Tree.addNode("C","root")

      Tree.children("root") mustBe List(Node("A",1,Some("root"),"root"), Node("C",1,Some("root"),"root"), Node("B",1,Some("root"),"root"))
    }
    "recalcualte hight of children and node when parent is udpated for node" in {
      Tree.addNode("A","root")
      Tree.addNode("AA","A")
      Tree.addNode("AB","A")
      Tree.addNode("AC","A")
      Tree.addNode("AAA","AA")
      Tree.addNode("AAB","AA")
      Tree.addNode("AAC","AA")
      Tree.addNode("B","root")
      Tree.addNode("C","root")

      Tree.children("AA").head.height mustBe 3
      Tree.updateParent("AA","root")
      Tree.children("AA").head.height mustBe 2
    }
  }
}