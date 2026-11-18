const {
  applyCors,
  verifyClient,
  checkRateLimit,
  getGeminiKeysRotated,
  sanitizeError,
  fetchWithTimeout
} = require('./_shared');

const TRANSLATE_MODELS = [
  'gemini-3.1-flash-lite',
  'gemini-3.5-flash-lite',
  'gemini-flash-latest',
  'gemini-3.8-flash'
];

const MAX_PAYLOAD_BYTES = 1_600_000; // 1.6 MB max payload guard

module.exports = async function handler(req, res) {
  if (applyCors(req, res)) return;
  if (req.method !== 'POST') {
    return res.status(405).json({ ok: false, error: 'Method not allowed' });
  }
  if (!verifyClient(req, res)) return;
  if (!checkRateLimit(req, res)) return;

  const keys = getGeminiKeysRotated();
  if (keys.length === 0) {
    return res.status(500).json({ ok: false, error: 'Serverda GEMINI_API_KEY sozlanmagan' });
  }

  const body = req.body || {};
  if (!body.contents || !Array.isArray(body.contents) || body.contents.length === 0 || body.contents.length > 3) {
    return res.status(400).json({ ok: false, error: 'Invalid contents payload' });
  }

  const payload = {
    contents: body.contents,
    generationConfig: body.generationConfig || {
      temperature: 0.1,
      maxOutputTokens: 260,
      responseMimeType: 'application/json'
    }
  };
  const payloadStr = JSON.stringify(payload);
  if (payloadStr.length > MAX_PAYLOAD_BYTES) {
    return res.status(413).json({ ok: false, error: 'Audio hajmi juda katta (maks 1.5MB)' });
  }

  let lastStatus = 502;
  let lastError = 'Google AI javob bermadi';

  for (let kIdx = 0; kIdx < keys.length; kIdx++) {
    const apiKey = keys[kIdx];
    for (const model of TRANSLATE_MODELS) {
      try {
        const gRes = await fetchWithTimeout(
          `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${encodeURIComponent(apiKey)}`,
          {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: payloadStr
          },
          9000
        );

        if (!gRes.ok) {
          const errText = await gRes.text();
          lastStatus = gRes.status;
          lastError = sanitizeError(`${model} (HTTP ${gRes.status}): ${errText}`);
          if (gRes.status === 401 || gRes.status === 403 || gRes.status === 429) {
            break;
          }
          if (gRes.status === 400) {
            return res.status(400).json({ ok: false, error: lastError });
          }
          continue;
        }

        const json = await gRes.json();
        const candidate = json.candidates && json.candidates[0];
        const textPart = candidate && candidate.content && candidate.content.parts
          ? candidate.content.parts.find(p => p.text)
          : null;

        if (textPart && textPart.text) {
          return res.status(200).json({
            ok: true,
            text: textPart.text,
            model
          });
        }
      } catch (err) {
        lastError = sanitizeError(`${model}: ${err.name === 'AbortError' ? 'timeout' : err.message}`);
      }
    }
  }

  return res.status(lastStatus >= 400 && lastStatus < 600 ? lastStatus : 502).json({
    ok: false,
    error: lastError
  });
};
