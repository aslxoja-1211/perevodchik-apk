// Shared secure helpers for Ovozli Tarjimon Serverless Proxy

function applyCors(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, X-App-Client');
  if (req.method === 'OPTIONS') {
    res.status(204).end();
    return true;
  }
  return false;
}

function verifyClient(req, res) {
  const expected = process.env.APP_CLIENT_ID;
  if (!expected) return true;
  const provided = req.headers['x-app-client'];
  if (provided !== expected) {
    res.status(403).json({ ok: false, error: 'Forbidden client' });
    return false;
  }
  return true;
}

function getGeminiKeys() {
  const keys = [];
  if (process.env.GEMINI_API_KEYS) {
    for (const k of process.env.GEMINI_API_KEYS.split(',')) {
      const trimmed = k.trim();
      if (trimmed.length >= 20) keys.push(trimmed);
    }
  }
  for (const name of ['GEMINI_API_KEY', 'GEMINI_API_KEY_2', 'GEMINI_API_KEY_3']) {
    const val = (process.env[name] || '').trim();
    if (val.length >= 20 && !keys.includes(val)) {
      keys.push(val);
    }
  }
  return keys;
}

async function fetchWithTimeout(url, options, timeoutMs) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(url, { ...options, signal: controller.signal });
  } finally {
    clearTimeout(timer);
  }
}

module.exports = {
  applyCors,
  verifyClient,
  getGeminiKeys,
  fetchWithTimeout
};
