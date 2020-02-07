package db

import akka.actor.ActorSystem

import scala.io.Source

/**
  * reads tsv-file and call proc for every line
  */
object Tsv {
  def apply(path: String, proc: (Seq[String], Seq[String]) => _)(implicit system: ActorSystem): Unit = {
    val lines = Source.fromFile(path).getLines()
    val header = lines.next().split("\t")
    var counter = 0

    while (lines.hasNext) {
      if (counter % 1000000 == 0)
        system.log.debug(s"Reading [$path] => ${counter / 1000000}M records processed")
      proc(header, lines.next().split("\t"))
      counter += 1
    }
  }
}
