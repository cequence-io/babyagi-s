package io.cequence.cohereapi.service

import io.cequence.cohereapi.model.{ChatModelId, ChatSettings, ClassifySettings, EmbedModelId, EmbedSettings, RerankModelId, RerankSettings}

trait CohereConsts {
  object Defaults {
    val Embed = EmbedSettings(
      model = EmbedModelId.embed_english_v2_0
    )

    val Rerank = RerankSettings(
      model = RerankModelId.rerank_english_v3_0
    )

    val Classify = ClassifySettings(
      model = EmbedModelId.embed_english_v2_0
    )

    val Chat = ChatSettings(
      model = ChatModelId.command_r_plus_08_2024
    )
  }
}