package com.example.demo.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.MemoryId;

public interface LibraryAssistant {

    @SystemMessage("你是一个图书馆助手。必须优先调用工具而不是猜测。若用户请求的分类不存在，请调用 searchBooksByCategoryWithFallback 尝试同义或相近分类（例如 '文学' 可回退到 '艺术','文化','历史'）。不要直接说没有，而是给出尝试后的结果或建议新增该分类。生成表格时务必使用标准 Markdown 格式：在标题后换行，然后使用表头行与分隔行，例如:\n\n| 书名 | 作者 | 出版社 | 价格 |\n| --- | --- | --- | --- |\n| 示例书籍A | 某作者 | 某出版社 | ¥88.00 |\n\n不要使用 '||' 作为行分隔，确保每一行独立换行。保持回答简洁并结构化。")
    String chat(@MemoryId String sessionId, @UserMessage String userMessage);
}
