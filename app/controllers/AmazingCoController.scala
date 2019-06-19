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

  case class Node(id: NodeId, height: Int, parent: Option[NodeId], root: NodeId, children:Seq[NodeId])

  implicit val nodeFormat = Json.format[Node]

}


object Tree {

  import scala.collection.mutable
  import Model._

  val currentRootID = "root"

  private val data = mutable.Map.empty[String, Node]

  if (data.isEmpty) {
    data.put(currentRootID, Node(currentRootID, 0, None, currentRootID,List.empty))
  }

  private var persistenceCopy:Option[mutable.Map[String, Node]] = Some(mutable.Map.empty[String, Node])


  def get(nodeId: NodeId) = data.get(nodeId)

  def children(parentNodeId: String): List[Node] = {
    val childrenIds = data.get(parentNodeId) match {
      case Some(parent) => parent.children
      case None => Nil
    }
    childrenIds.flatMap(data.get(_)).toList
  }

  def addNode(nodeId: NodeId, parantNodeId: NodeId): Either[Error, Node] = {
    try {
      assume(!data.contains(nodeId))

      val parent = data.get(parantNodeId)
      parent match {
        case Some(parent) =>
          data.put(nodeId, Node(nodeId, parent.height + 1, Some(parantNodeId), currentRootID, List.empty))
          val updatedParent = parent.copy(children = parent.children:+nodeId)
          data.put(updatedParent.id, updatedParent)
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

  def updateParent(nodeId: NodeId, newParentId: NodeId): Option[Node] = {
    data.get(nodeId) match {
      case Some(node) if node.parent.exists(_.equalsIgnoreCase(newParentId)) => Some(node)
      case Some(node) => {
        val newHeight = data.get(newParentId).map(_.height).getOrElse(0) + 1
        val updatedNode = node.copy(height = newHeight, parent = Some(newParentId))
        data.put(updatedNode.id, updatedNode)

        val oldParent = node.parent.flatMap(data.get(_))
        oldParent.map(op => {
          val updatedOldParent = op.copy(children = op.children.filter(_ != node.id))
          data.put(updatedOldParent.id, updatedOldParent)
        })

        data.get(newParentId).map(np => {
          val updatedNewParent = np.copy(children = np.children :+ node.id)
          data.put(updatedNewParent.id, updatedNewParent)
        })

        updateHeightForChildren(children(node.id), updatedNode.height)
        updatePersistanceCopy
        Some(updatedNode)
      }
      case None => None
    }
  }

  private def updatePersistanceCopy = {
    persistenceCopy = Some(data.clone())

  }
  def getLatestCopy  = {
    val toBackup = persistenceCopy
    persistenceCopy = None // not relevant to persist if no changes since last persistance
    toBackup
  }

  def initializeFromFile(canonicalFilename: String = "AmazingCoAPIData.txt", force: Boolean = false) = ??? //TODO initialize tree from persisted file, if current tree size is more than 1 (i.e. containing root) force must be true
}


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's Amazing Co API.
  */
@Singleton
class AmazingCoController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  import Model._

  case class NodeIdDTO(nodeId: NodeId)

  implicit val nodeDTOFormat = Json.format[NodeIdDTO]

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
    val updatedNode: Option[Node] = Tree.updateParent(nodeId, body.nodeId)
    updatedNode.map(n=> Ok(Json.toJson(n))).getOrElse(NotFound)
  }
}

