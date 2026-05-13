const GEMINI_API_KEY = Deno.env.get("GEMINI_API_KEY");
const GEMINI_MODEL = (Deno.env.get("GEMINI_MODEL") ?? "gemini-2.5-flash").replace(
  /^models\//,
  "",
);
const MAX_PROMPT_LENGTH = 8_000;

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") {
    return new Response(null, {
      status: 204,
      headers: corsHeaders,
    });
  }

  if (request.method !== "POST") {
    return jsonResponse({ error: "Method not allowed" }, 405);
  }

  let prompt: string;
  try {
    const body = await request.json();
    prompt = typeof body.prompt === "string" ? body.prompt.trim() : "";
  } catch {
    return jsonResponse({ error: "Request body must be JSON" }, 400);
  }

  if (!prompt) {
    return jsonResponse({ error: "Prompt is required" }, 400);
  }

  if (prompt.length > MAX_PROMPT_LENGTH) {
    return jsonResponse({ error: `Prompt must be ${MAX_PROMPT_LENGTH} characters or fewer` }, 400);
  }

  if (!GEMINI_API_KEY) {
    return streamResponse((controller) => {
      sendEvent(controller, "error", { message: "Gemini API key is not configured" });
      sendEvent(controller, "done", {});
      controller.close();
    });
  }

  const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(
    GEMINI_MODEL,
  )}:streamGenerateContent?alt=sse`;

  let geminiResponse: Response;
  try {
    geminiResponse = await fetch(endpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-goog-api-key": GEMINI_API_KEY,
      },
      body: JSON.stringify({
        contents: [
          {
            role: "user",
            parts: [{ text: prompt }],
          },
        ],
      }),
    });
  } catch {
    return streamResponse((controller) => {
      sendEvent(controller, "error", { message: "Could not reach Gemini" });
      sendEvent(controller, "done", {});
      controller.close();
    });
  }

  if (!geminiResponse.ok || !geminiResponse.body) {
    return streamResponse(async (controller) => {
      const message = await readSafeGeminiError(geminiResponse);
      sendEvent(controller, "error", { message });
      sendEvent(controller, "done", {});
      controller.close();
    });
  }

  return streamResponse(async (controller) => {
    try {
      await forwardGeminiStream(geminiResponse.body!, controller);
      sendEvent(controller, "done", {});
    } catch {
      sendEvent(controller, "error", { message: "Gemini stream failed" });
      sendEvent(controller, "done", {});
    } finally {
      controller.close();
    }
  });
});

function jsonResponse(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      ...corsHeaders,
      "Content-Type": "application/json",
    },
  });
}

function streamResponse(
  start: (controller: ReadableStreamDefaultController<Uint8Array>) => void | Promise<void>,
): Response {
  const stream = new ReadableStream<Uint8Array>({
    start(controller) {
      void start(controller);
    },
  });

  return new Response(stream, {
    headers: {
      ...corsHeaders,
      "Content-Type": "text/event-stream",
      "Cache-Control": "no-cache",
      "X-Accel-Buffering": "no",
      Connection: "keep-alive",
    },
  });
}

async function forwardGeminiStream(
  body: ReadableStream<Uint8Array>,
  controller: ReadableStreamDefaultController<Uint8Array>,
) {
  const reader = body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  while (true) {
    const { done, value } = await reader.read();
    if (done) {
      break;
    }

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split("\n");
    buffer = lines.pop() ?? "";

    for (const line of lines) {
      processGeminiLine(line, controller);
    }
  }

  if (buffer) {
    processGeminiLine(buffer, controller);
  }
}

function processGeminiLine(
  line: string,
  controller: ReadableStreamDefaultController<Uint8Array>,
) {
  const trimmed = line.trim();
  if (!trimmed.startsWith("data:")) {
    return;
  }

  const payload = trimmed.slice("data:".length).trim();
  if (!payload || payload === "[DONE]") {
    return;
  }

  try {
    const parsed = JSON.parse(payload);
    const text = parsed
      .candidates
      ?.flatMap((candidate: GeminiCandidate) => candidate.content?.parts ?? [])
      ?.map((part: GeminiPart) => part.text ?? "")
      ?.join("");

    if (text) {
      sendEvent(controller, "token", { text });
    }
  } catch {
    sendEvent(controller, "error", { message: "Invalid Gemini stream payload" });
  }
}

function sendEvent(
  controller: ReadableStreamDefaultController<Uint8Array>,
  event: string,
  data: Record<string, unknown>,
) {
  const encoded = new TextEncoder().encode(`event: ${event}\ndata: ${JSON.stringify(data)}\n\n`);
  controller.enqueue(encoded);
}

async function readSafeGeminiError(response: Response): Promise<string> {
  try {
    const body = await response.json();
    const message = body?.error?.message;
    if (typeof message === "string" && message) {
      return message;
    }
  } catch {
    // Fall through to status text.
  }

  return response.statusText || "Gemini request failed";
}

type GeminiCandidate = {
  content?: {
    parts?: GeminiPart[];
  };
};

type GeminiPart = {
  text?: string;
};
