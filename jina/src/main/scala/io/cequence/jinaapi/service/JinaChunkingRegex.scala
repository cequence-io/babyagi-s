package io.cequence.jinaapi.service

// taken from https://gist.github.com/hanxiao/3f60354cf6dc5ac698bc9154163b4e6a
// and modified to work with Scala
// acknowledging the original author: https://github.com/hanxiao and Jina team
object JinaChunkingRegex {

  // Define variables for magic numbers
  val MAX_HEADING_LENGTH = 7
  val MAX_HEADING_CONTENT_LENGTH = 200
  val MAX_HEADING_UNDERLINE_LENGTH = 200
  val MAX_HTML_HEADING_ATTRIBUTES_LENGTH = 100
  val MAX_LIST_ITEM_LENGTH = 200
  val MAX_NESTED_LIST_ITEMS = 6
  val MAX_LIST_INDENT_SPACES = 7
  val MAX_BLOCKQUOTE_LINE_LENGTH = 200
  val MAX_BLOCKQUOTE_LINES = 15
  val MAX_CODE_BLOCK_LENGTH = 1500
  val MAX_CODE_LANGUAGE_LENGTH = 20
  val MAX_INDENTED_CODE_LINES = 20
  val MAX_TABLE_CELL_LENGTH = 200
  val MAX_TABLE_ROWS = 20
  val MAX_HTML_TABLE_LENGTH = 2000
  val MIN_HORIZONTAL_RULE_LENGTH = 3
  val MAX_SENTENCE_LENGTH = 400
  val MAX_QUOTED_TEXT_LENGTH = 300
  val MAX_PARENTHETICAL_CONTENT_LENGTH = 200
  val MAX_NESTED_PARENTHESES = 5
  val MAX_MATH_INLINE_LENGTH = 100
  val MAX_MATH_BLOCK_LENGTH = 500
  val MAX_PARAGRAPH_LENGTH = 1000
  val MAX_STANDALONE_LINE_LENGTH = 800
  val MAX_HTML_TAG_ATTRIBUTES_LENGTH = 100
  val MAX_HTML_TAG_CONTENT_LENGTH = 1000
  val LOOKAHEAD_RANGE = 100  // Number of characters to look ahead for a sentence boundary

  // Emoji_Presentation and Extended_Pictographic are not supported so we must explicitly include it
  val EMOJI_RANGE =
    """[\u1F600-\u1F64F]|""" + // Emoticons
      """[\u1F300-\u1F5FF]|""" + // Misc Symbols and Pictographs
      """[\u1F680-\u1F6FF]|""" + // Transport and Map
      """[\u2600-\u26FF]|""" + // Misc symbols
      """[\u2700-\u27BF]""" // Dingbats

  val AVOID_AT_START = """[\s\]})>,']"""
//  val PUNCTUATION = """[.!?…]|\.\.\.|[\u2026\u2047-\u2049]|[\p{Emoji_Presentation}\p{Extended_Pictographic}]"""
  val PUNCTUATION = s"""[.!?…]|\\.\\.\\.|[\u2026\u2047-\u2049]|$EMOJI_RANGE"""

  val QUOTE_END = """(?:'(?=`)|''(?=``))"""
  // TODO: Negative Lookbehind vs. Negative Lookahead
  val SENTENCE_END = s"""(?:$PUNCTUATION(?!$AVOID_AT_START(?=$PUNCTUATION))|$QUOTE_END)(?=\\S|$$)"""
  val SENTENCE_BOUNDARY = s"""(?:$SENTENCE_END|(?=[\r\n]|$$))"""
  val LOOKAHEAD_PATTERN = s"""(?:(?!$SENTENCE_END).){1,$LOOKAHEAD_RANGE}$SENTENCE_END"""
  val NOT_PUNCTUATION_SPACE = s"""(?!$PUNCTUATION\\s)"""
  val SENTENCE_PATTERN = s"""$NOT_PUNCTUATION_SPACE(?:[^\r\n]{1,{MAX_LENGTH}}$SENTENCE_BOUNDARY|[^\r\n]{1,{MAX_LENGTH}}(?=$PUNCTUATION|$QUOTE_END)(?:$LOOKAHEAD_PATTERN)?)$AVOID_AT_START*"""

  // TODO: Scala's regex engine does not support the global (`g`) flag as in JavaScript. Instead, you need to use methods like `findAllIn` to iterate over all matches.
  val regex = new scala.util.matching.Regex(
    "(?m)(" +
      // 1. Headings (Setext-style, Markdown, and HTML-style, with length constraints)
      s"""(?:^(?:[#*=-]{1,${MAX_HEADING_LENGTH}}|\\w[^\\r\\n]{0,${MAX_HEADING_CONTENT_LENGTH}}\\r?\\n[-=]{2,${MAX_HEADING_UNDERLINE_LENGTH}}|<h[1-6][^>]{0,${MAX_HTML_HEADING_ATTRIBUTES_LENGTH}}>)[^\\r\\n]{1,${MAX_HEADING_CONTENT_LENGTH}}(?:</h[1-6]>)?(?:\\r?\\n|$$))""" +
      "|" +
      // New pattern for citations
      s"""(?:\\[[0-9]+\\][^\\r\\n]{1,${MAX_STANDALONE_LINE_LENGTH}})""" +
      "|" +
      // 2. List items (bulleted, numbered, lettered, or task lists, including nested, up to three levels, with length constraints)
      s"""(?:(?:^|\\r?\\n)[ \\t]{0,3}(?:[-*+•]|\\d{1,3}\\.\\w\\.|\\[[ xX]\\])[ \\t]+${SENTENCE_PATTERN.replaceAll("\\{MAX_LENGTH\\}", MAX_LIST_ITEM_LENGTH.toString)})""" +
      // originally: `(?:(?:\\r?\\n[ \\t]{2,5}(?:[-*+•]|\\d{1,3}\\.\\w\\.|\\[[ xX]\\])[ \\t]+${SENTENCE_PATTERN.replace(/{MAX_LENGTH}/g, String(MAX_LIST_ITEM_LENGTH))}){0,${MAX_NESTED_LIST_ITEMS}}` + TODO: do we need ) after MAX_LIST_ITEM_LENGTH.toString)} ?
      s"""(?:(?:\\r?\\n[ \\t]{2,5}(?:[-*+•]|\\d{1,3}\\.\\w\\.|\\[[ xX]\\])[ \\t]+${SENTENCE_PATTERN.replaceAll("\\{MAX_LENGTH\\}", MAX_LIST_ITEM_LENGTH.toString)}{0,${MAX_NESTED_LIST_ITEMS}}""" +
      s"""(?:\\r?\\n[ \\t]{4,${MAX_LIST_INDENT_SPACES}}(?:[-*+•]|\\d{1,3}\\.\\w\\.|\\[[ xX]\\])[ \\t]+${SENTENCE_PATTERN.replaceAll("\\{MAX_LENGTH\\}", MAX_LIST_ITEM_LENGTH.toString)}){0,${MAX_NESTED_LIST_ITEMS}})?)""" +
      "|" +
      // 3. Block quotes (including nested quotes and citations, up to three levels, with length constraints)
      s"""(?:(?:^>(?:>|\\s{2,}){0,2}${SENTENCE_PATTERN.replaceAll("\\{MAX_LENGTH\\}", MAX_BLOCKQUOTE_LINE_LENGTH.toString)}\\r?\\n?){1,${MAX_BLOCKQUOTE_LINES}})""" +
      "|" +
      // 4. Code blocks (fenced, indented, or HTML pre/code tags, with length constraints)
      s"""(?:(?:^|\\r?\\n)(?:```|~~~)(?:\\w{0,${MAX_CODE_LANGUAGE_LENGTH}})?\\r?\\n[\\s\\S]{0,${MAX_CODE_BLOCK_LENGTH}}?(?:```|~~~)\\r?\\n?""" +
      s"""|(?:(?:^|\\r?\\n)(?: {4}|\\t)[^\\r\\n]{0,${MAX_LIST_ITEM_LENGTH}}(?:\\r?\\n(?: {4}|\\t)[^\\r\\n]{0,${MAX_LIST_ITEM_LENGTH}}){0,${MAX_INDENTED_CODE_LINES}}\\r?\\n?)""" +
      s"""|(?:<pre>(?:<code>)?[\\s\\S]{0,${MAX_CODE_BLOCK_LENGTH}}?(?:</code>)?</pre>))""" +
      "|" +
      // 5. Tables (Markdown, grid tables, and HTML tables, with length constraints)
      s"""(?:(?:^|\\r?\\n)(?:\\|[^\\r\\n]{0,${MAX_TABLE_CELL_LENGTH}}\\|(?:\\r?\\n\\|[-:]{1,${MAX_TABLE_CELL_LENGTH}}\\|){0,1}(?:\\r?\\n\\|[^\\r\\n]{0,${MAX_TABLE_CELL_LENGTH}}\\|){0,${MAX_TABLE_ROWS}}""" +
      s"""|<table>[\\s\\S]{0,${MAX_HTML_TABLE_LENGTH}}?</table>))""" +
      "|" +
      // 6. Horizontal rules (Markdown and HTML hr tag)
      s"""(?:^(?:[-*_]){${MIN_HORIZONTAL_RULE_LENGTH},}\\s*$$|<hr\\s*/?>)""" +
      "|" +
      // 10. Standalone lines or phrases (including single-line blocks and HTML elements, with length constraints)
      s"""(?!${AVOID_AT_START})(?:^(?:<[a-zA-Z][^>]{0,${MAX_HTML_TAG_ATTRIBUTES_LENGTH}}>)?${SENTENCE_PATTERN.replaceAll("\\{MAX_LENGTH\\}", MAX_STANDALONE_LINE_LENGTH.toString)}(?:</[a-zA-Z]+>)?(?:\\r?\\n|$$))""" +
      "|" +
      // 7. Sentences or phrases ending with punctuation (including ellipsis and Unicode punctuation)
      s"""(?!${AVOID_AT_START})${SENTENCE_PATTERN.replaceAll("\\{MAX_LENGTH\\}", MAX_SENTENCE_LENGTH.toString)}""" +
      "|" +
      // 8. Quoted text, parenthetical phrases, or bracketed content (with length constraints)
      "(?:" +
      s"""(?<!\\w)\"\"\"[^\"]{0,${MAX_QUOTED_TEXT_LENGTH}}\"\"\"(?!\\w)""" +
      s"""|(?<!\\w)(?:['\"`'"])[^\\r\\n]{0,${MAX_QUOTED_TEXT_LENGTH}}\\1(?!\\w)""" +
      s"""|(?<!\\w)`[^\\r\\n]{0,${MAX_QUOTED_TEXT_LENGTH}}'(?!\\w)""" +
      s"""|(?<!\\w)``[^\\r\\n]{0,${MAX_QUOTED_TEXT_LENGTH}}''(?!\\w)""" +
      s"""|\\([^\\r\\n()]{0,${MAX_PARENTHETICAL_CONTENT_LENGTH}}(?:\\([^\\r\\n()]{0,${MAX_PARENTHETICAL_CONTENT_LENGTH}}\\)[^\\r\\n()]{0,${MAX_PARENTHETICAL_CONTENT_LENGTH}}){0,${MAX_NESTED_PARENTHESES}}\\)""" +
      s"""|\\[[^\\r\\n\\[\\]]{0,${MAX_PARENTHETICAL_CONTENT_LENGTH}}(?:\\[[^\\r\\n\\[\\]]{0,${MAX_PARENTHETICAL_CONTENT_LENGTH}}\\][^\\r\\n\\[\\]]{0,${MAX_PARENTHETICAL_CONTENT_LENGTH}}){0,${MAX_NESTED_PARENTHESES}}\\]""" +
      s"""|\\$$[^\\r\\n$$]{0,${MAX_MATH_INLINE_LENGTH}}\\$$""" +
      s"""|`[^`\\r\\n]{0,${MAX_MATH_INLINE_LENGTH}}`""" +
      ")" +
      "|" +
      // 9. Paragraphs (with length constraints)
      s"""(?!${AVOID_AT_START})(?:(?:^|\\r?\\n\\r?\\n)(?:<p>)?${SENTENCE_PATTERN.replaceAll("\\{MAX_LENGTH\\}", MAX_PARAGRAPH_LENGTH.toString)}(?:</p>)?(?=\\r?\\n\\r?\\n|$$))""" +
      "|" +
      // 11. HTML-like tags and their content (including self-closing tags and attributes, with length constraints)
      s"""(?:<[a-zA-Z][^>]{0,${MAX_HTML_TAG_ATTRIBUTES_LENGTH}}(?:>[\\s\\S]{0,${MAX_HTML_TAG_CONTENT_LENGTH}}?</[a-zA-Z]+>|\\s*/>))""" +
      "|" +
      // 12. LaTeX-style math expressions (inline and block, with length constraints)
      s"""(?:(?:\\$$\\$$[\\s\\S]{0,${MAX_MATH_BLOCK_LENGTH}}?\\$$\\$$)|(?:\\$$[^\\$$\\r\\n]{0,${MAX_MATH_INLINE_LENGTH}}\\$$))""" +
      "|" +
      // 14. Fallback for any remaining content (with length constraints)
      s"""(?!${AVOID_AT_START})${SENTENCE_PATTERN.replaceAll("\\{MAX_LENGTH\\}", MAX_STANDALONE_LINE_LENGTH.toString)}""" +
    ")",
  )
}
