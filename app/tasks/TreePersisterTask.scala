package tasks

import java.io.{BufferedWriter, File, FileOutputStream, FileWriter}
import java.time.format.DateTimeFormatter

import javax.inject.Inject
import akka.actor.ActorRef
import akka.actor.ActorSystem
import controllers.Tree
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import java.time.ZonedDateTime

import play.api.Configuration


class TreePersisterTask @Inject()(configuration: Configuration)(actorSystem: ActorSystem)(implicit executionContext: ExecutionContext) {
  private val dateTimeFormat = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

  def getFilePath = {

    val path = configuration.get[String]("datafBasePath")
    val directory = new File(path)
    if (!directory.exists()) {
      directory.mkdirs()
    }
    path
  }

  actorSystem.scheduler.schedule(initialDelay = 2.minutes, interval = 15.minutes) {
    // the block of code that will be executed
    val copy = Tree.getLatestCopy
    copy.map(dataToBackup => {
      val fileName = ZonedDateTime.now.format(dateTimeFormat)
      val file = new File(getFilePath + s"/$fileName.json")
      val bw = new BufferedWriter(new FileWriter(file))
      bw.write(Json.stringify(Json.toJson(dataToBackup.values)))
      bw.close()
    })
  }
}