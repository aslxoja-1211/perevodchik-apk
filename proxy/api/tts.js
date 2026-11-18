const {
  applyCors,
  verifyClient,
  checkRateLimit,
  getGeminiKeysRotated,
  sanitizeError,
  fetchWithTimeout
} = require('./_shared');

const TTS_MODELS = ['gemini-3.8-flash-tts', 'gemini-3.1-flash-tts-preview'];

const memCache = new Map();
const MAX_CACHE = 50;

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
  const text = (body.text || '').trim();
  const voice = (body.voice || 'Aoede').trim().slice(0, 32);

  if (!text) {
    return res.status(400).json({ ok: false, error: 'Missing text' });
  }

  const safeText = text.slice(0, 400);
  const cacheKey = `${voice}:${safeText.toLowerCase()}`;
  if (memCache.has(cacheKey)) {
    const cached = memCache.get(cacheKey);
    return res.status(200).json({ ok: true, audioBase64: cached.data, mimeType: cached.mimeType, cached: true });
  }

  const payloadStr = JSON.stringify({
    contents: [{ parts: [{ text: safeText }] }],
    generationConfig: {
      responseModalities: ['AUDIO'],
      speechConfig: {
        voiceConfig: {
          prebuiltVoiceConfig: {
            voiceName: voice
          }
        }
      }
    }
  });

  let lastStatus = 502;
  let lastError = 'TTS javob bermadi';

  for (let kIdx = 0; kIdx < keys.length; kIdx++) {
    const apiKey = keys[kIdx];
    for (const model of TTS_MODELS) {
      try {
        const gRes = await fetchWithTimeout(
          `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${encodeURIComponent(apiKey)}`,
          {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: payloadStr
          },
          8000
        );

        if (!gRes.ok) {
          const errText = await gRes.text();
          lastStatus = gRes.status;
          lastError = sanitizeError(`${model} (HTTP ${gRes.status}): ${errText}`);
          if (gRes.status === 401 || gRes.status === 403 || gRes.status === 429) {
            break;
          }
          continue;
        }

        const json = await gRes.json();
        const part = json.candidates?.[0]?.content?.parts?.find(p => p.inlineData && p.inlineData.data);
        if (part) {
          const audioBase64 = part.inlineData.data;
          const mimeType = part.inlineData.mimeType || 'audio/wav';
          if (safeText.length <= 120) {
            if (memCache.size >= MAX_CACHE) {
              memCache.delete(memCache.keys().next().value);
            }
            memCache.set(cacheKey, { data: audioBase64, mimeType });
          }
          return res.status(200).json({ ok: true, audioBase64, mimeType, model });
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
