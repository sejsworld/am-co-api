package controllers

import java.io.{BufferedWriter, File, FileWriter}

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._

object Model {

  type Error = String
  type Success = String
  type NodeId = String

  case class Node(id: NodeId, height: Int, parent: Option[NodeId], root: NodeId)

  implicit val nodeFormat = Json.format[Node]

}


object Tree {

  import scala.collection.mutable
  import Model._

  val currentRootID = "root"

  private val data = mutable.Map.empty[String, Node]

  if (data.isEmpty) {
    data.put(currentRootID, Node(currentRootID, 0, None, currentRootID))
  }

  private var persistenceCopy:Option[mutable.Map[String, Node]] = Some(mutable.Map.empty[String, Node])


  def get(nodeId: NodeId) = data.get(nodeId)

  def children(parentNodeId: String): List[Node] = data.values.filter(_.parent.exists(_.equalsIgnoreCase(parentNodeId))).toList

  def addNode(nodeId: NodeId, parantNodeId: NodeId): Either[Error, Node] = {
    try {
      assume(!data.contains(nodeId))

      val parent = data.get(parantNodeId)
      parent match {
        case Some(parent) =>
          data.put(nodeId, Node(nodeId, parent.height + 1, Some(parantNodeId), currentRootID))

          updatePersistanceCopy
          data.get(nodeId).map(Right(_)).getOrElse(Left("insertion error"))
        case None => Left(s"parent with nodeId $parantNodeId was not found in tree")
      }
    } catch {
      case ae: AssertionError => Left(s"Node with nodeId $nodeId allready exists in tree")
      case e: Exception => Left("unexpected error")
    }
  }

  //do it recursively for all children
  private def updateHeightForChildren(nodeChildren: List[Node], parentHeight: Int): Boolean = {
    nodeChildren match {
      case Nil => false
      case l: List[Node] =>
        l.map(node => {
          val updatedNode = node.copy(height = parentHeight + 1)
          data.put(node.id, updatedNode)
          updateHeightForChildren(children(updatedNode.id), updatedNode.height)
        })
        true
    }
  }

  def updateParent(nodeId: NodeId, newParentId: NodeId): Node = {
    data.get(nodeId) match {
      case Some(node) if node.parent.exists(_.equalsIgnoreCase(newParentId)) => node
      case Some(node) => {
        val newHeight = data.get(newParentId).map(_.height).getOrElse(0) + 1
        val updatedNode = node.copy(height = newHeight, parent = Some(newParentId))
        data.put(updatedNode.id, updatedNode)
        updateHeightForChildren(children(node.id), updatedNode.height)
        updatePersistanceCopy
        updatedNode
      }
    }
  }

  private def updatePersistanceCopy = {
    persistenceCopy = Some(data.clone())

  }
  def getLatestCopy  = {
    val toBackup = persistenceCopy
    persistenceCopy = None // not relavant to persist if no changes since last time
    toBackup
  }

  //FIXME persist
  //persist()
  def persist(canonicalFilename: String = "AmazingCoAPIData.txt") = {
    //TODO ensure that persistence is thread safe and that the file will not be currupted when several concurrent changes to the tree
    val file = new File(canonicalFilename)
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(Json.stringify(Json.toJson(data.values)))
    bw.close()
  }

  def initializeFromFile(canonicalFilename: String = "AmazingCoAPIData.txt", force: Boolean = false) = ??? //TODO initialize tree from persisted file, if tree size is more than 1 (i.e. containing root) force must be true
}


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's Amazing Co API.
  */
@Singleton
class AmazingCoController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  import Model._

  case class NodeIdDTO(nodeId: NodeId)

  case class NodeWithChildrenDTO(id: NodeId, root: NodeId, parent: Option[NodeId], height: Int, children: Seq[NodeWithChildrenDTO])

  implicit val nodeDTOFormat = Json.format[NodeIdDTO]
  implicit val nodeWithChildrenDTOFormat = Json.format[NodeWithChildrenDTO]


  def children(parentNodeId: NodeId) = Action { implicit request: Request[AnyContent] =>
    val children = Tree.children(parentNodeId)
    Ok(Json.toJson(children))
  }

  def addChild(parentNodeId: NodeId) = Action(parse.json) { implicit request =>
    val body = request.body.as[NodeIdDTO]

    Tree.addNode(body.nodeId, parentNodeId) match {
      case Left(error) => BadRequest(error)
      case Right(node) => Ok(Json.toJson(node))
    }
  }

  def updateParent(nodeId: NodeId) = Action(parse.json) { implicit request =>
    val body = request.body.as[NodeIdDTO]
    val updatedNode = Tree.updateParent(nodeId, body.nodeId)
    Ok(Json.toJson(updatedNode))
  }

  def twoLevels(parentNodeId: NodeId) = Action { implicit request: Request[AnyContent] =>
    val maybeParent = Tree.get(parentNodeId)

    maybeParent.map(parent => {
      val children = Tree.children(parent.id)
      val dtoChildren = children.map(node => NodeWithChildrenDTO(node.id, node.root, node.parent, node.height, List.empty))
      val dto = NodeWithChildrenDTO(parent.id, parent.root, parent.parent, parent.height, dtoChildren)
      Ok(Json.toJson(dto))
    }).getOrElse(NotFound)
  }
}

