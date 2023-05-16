package io.cequence.babyagis.next.providers

import java.{util => ju}
import ai.djl.huggingface.tokenizers.{Encoding, HuggingFaceTokenizer}
import ai.onnxruntime.OrtSession.Result
import ai.onnxruntime.{OnnxTensor, OrtEnvironment}
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import java.nio.file.Paths
import java.util
import scala.concurrent.{ExecutionContext, Future}

private class ONNXEmbeddingsProvider(
  tokenizerPath: String,
  modelPath: String,
  normalize: Boolean,
  modelDisplayName: String)(
  implicit ec: ExecutionContext
) extends EmbeddingsProvider {

  private val tokenizer = HuggingFaceTokenizer.newInstance(Paths.get(tokenizerPath))
  private val environment = OrtEnvironment.getEnvironment
  private val session = environment.createSession(modelPath)

  private val logger = LoggerFactory.getLogger(this.getClass)

  override def apply(input: Seq[String]): Future[Seq[Seq[Double]]] = Future {
    val start = new ju.Date
    val encodings = tokenizer.batchEncode(input.toArray)

    encodings.map { encoding =>
      val attentionMasks = encoding.getAttentionMask
      val inputs = prepareONNXInputs(encoding)

      val results = session.run(inputs)

      try {
        processResults(results, attentionMasks).toSeq
      } finally {
        logger.info(s"ONNX-based embedding with the model '${modelDisplayName}' took ${new ju.Date().getTime - start.getTime} ms for ${input.size} inputs with ${input.map(_.length).sum} characters in total.")
        results.close()
      }
    }.toSeq
  }

  // TODO: session.close

  private def prepareONNXInputs(encoding: Encoding) = {
    // aux function to create a tensor
    def createTensor(array: Array[Long]): OnnxTensor = {
      val matrix = new Array[Array[Long]](1)
      matrix(0) = array
      OnnxTensor.createTensor(environment, matrix)
    }

    val inputIds = createTensor(encoding.getIds)
    val attentionMasks = createTensor(encoding.getAttentionMask)
    val tokenTypeIds = createTensor(encoding.getTypeIds)

    val inputs = new util.HashMap[String, OnnxTensor]()
    inputs.put("input_ids", inputIds)
    inputs.put("attention_mask", attentionMasks)
    inputs.put("token_type_ids", tokenTypeIds)
    inputs
  }

  private def processResults(
    results: Result,
    attentionMask: Array[Long]
  ): Array[Double] = {
    val lastHiddenState = results.get(0).getValue.asInstanceOf[Array[Array[Array[Float]]]]
    val attentionMaskSum = attentionMask.sum

    // mask out states with attention mask = 0
    val lastHiddenStateMasked = lastHiddenState(0).zip(attentionMask).map { case (tokenVector, attentionMask) =>
      if (attentionMask == 0) tokenVector.map(_ => 0.0f) else tokenVector
    }

    val embeddings = lastHiddenStateMasked.transpose.map(_.sum / attentionMaskSum)

    // (Optionally) normalize embeddings
    if (normalize) {
      val norm = math.sqrt(embeddings.map(x => x * x).sum)
      embeddings.map(_ / norm)
    } else
      embeddings.map(_.toDouble)
  }

  override def modelName = s"ONNX: $modelDisplayName"
}

object ONNXEmbeddingsProvider {
  def apply(
    config: Config)(
    implicit ec: ExecutionContext
  ): EmbeddingsProvider = {
    val actualConfig = config.getConfig("onnx-embeddings")

    val tokenizerPath = actualConfig.getString("tokenizerPath")
    val modelPath = actualConfig.getString("modelPath")
    val normalize = actualConfig.getBoolean("normalize")
    val modelDisplayName = actualConfig.getString("modelDisplayName")

    new ONNXEmbeddingsProvider(tokenizerPath, modelPath, normalize, modelDisplayName)
  }

  def apply(
    tokenizerPath: String,
    modelPath: String,
    normalize: Boolean,
    modelDisplayName: String)(
    implicit ec: ExecutionContext
  ): EmbeddingsProvider =
    new ONNXEmbeddingsProvider(tokenizerPath, modelPath, normalize, modelDisplayName)
}