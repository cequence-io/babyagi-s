package io.cequence.mistral.service

import io.cequence.wsclient.stream.{AsyncStream, Bytes}

import java.nio.channels.FileChannel
import java.nio.file.{Files, Path, StandardOpenOption}
import scala.concurrent.{blocking, ExecutionContext, Future}
import scala.util.Try

// Writes a stream to a new temp file - for engines that send a file but cannot stream a request
// body (ws-client-core has no engine-agnostic streamed body). The file writes block, so they are
// marked blocking for the ExecutionContext. On any failure (creating, writing or closing) the
// file is deleted and the future fails; deleteOnExit is a last-resort fallback. The caller
// deletes the file once it has been sent. The same helper is in azure-form/src/main/scala/io/cequence/azureform/service/TempFileSink.scala
// (the modules share no code).
private[service] object TempFileSink {

  def apply(
    source: AsyncStream[Bytes],
    prefix: String
  )(
    implicit ec: ExecutionContext
  ): Future[Path] =
    Future(blocking {
      val tempFile = Files.createTempFile(prefix, ".tmp")
      tempFile.toFile.deleteOnExit()
      val channel =
        try FileChannel.open(tempFile, StandardOpenOption.WRITE)
        catch {
          case e: Throwable =>
            delete(tempFile)
            throw e
        }
      (tempFile, channel)
    }).flatMap { case (tempFile, channel) =>
      Future.unit
        .flatMap(_ => // a synchronous throw of runForeach fails the future too
          source.runForeach { bytes =>
            val buffer = bytes.asByteBuffer
            blocking {
              while (buffer.hasRemaining) channel.write(buffer)
            }
          }
        )
        .transform { result =>
          val closed = Try(blocking(channel.close()))
          val written = result.flatMap(_ => closed).map(_ => tempFile)
          if (written.isFailure) delete(tempFile)
          written
        }
    }

  def delete(file: Path): Unit = {
    Try(Files.deleteIfExists(file)) // ignore deletion errors (deleteOnExit is the fallback)
    ()
  }
}
